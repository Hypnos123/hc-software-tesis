package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.exception.BusquedaPacienteException;
import com.krivi.apihistorialmedico.model.api.BusquedaPacienteResponse;
import com.krivi.apihistorialmedico.model.entity.Paciente;
import com.krivi.apihistorialmedico.repository.HistoriaClinicaRepository;
import com.krivi.apihistorialmedico.repository.PacienteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PacienteServiceImplTest {
  @Mock
  private PacienteRepository pacienteRepository;

  @Mock
  private HistoriaClinicaRepository historiaClinicaRepository;

  @InjectMocks
  private PacienteServiceImpl pacienteService;

  @Test
  void buscaPorDniYDevuelveSoloDatosPermitidos() {
    Paciente paciente = paciente(6, "78451268", "Patricia Elena", "Cárdenas Torres");
    when(pacienteRepository.findByNumDocumento("78451268")).thenReturn(Optional.of(paciente));
    when(historiaClinicaRepository.existsByPacienteIdPaciente(6)).thenReturn(true);

    BusquedaPacienteResponse response = pacienteService.buscarParaIntegracion("78451268");

    assertTrue(response.isEncontrado());
    assertEquals("unico", response.getTipoResultado());
    assertEquals(1, response.getPacientes().size());
    assertEquals("Patricia Elena Cárdenas Torres", response.getPacientes().getFirst().getNombreCompleto());
    assertTrue(response.getPacientes().getFirst().isTieneHistoriaClinica());
    verify(pacienteRepository).findByNumDocumento("78451268");
  }

  @Test
  void buscaPorIdPositivoDeMenosDeOchoDigitos() {
    Paciente paciente = paciente(25, "12345678", "Ana", "Pérez");
    when(pacienteRepository.findById(25)).thenReturn(Optional.of(paciente));
    when(historiaClinicaRepository.existsByPacienteIdPaciente(25)).thenReturn(false);

    BusquedaPacienteResponse response = pacienteService.buscarParaIntegracion("25");

    assertTrue(response.isEncontrado());
    assertEquals("unico", response.getTipoResultado());
    assertFalse(response.getPacientes().getFirst().isTieneHistoriaClinica());
    verify(pacienteRepository).findById(25);
  }

  @Test
  void buscaPorNombreYRetornaMultiplesCoincidencias() {
    Paciente primero = paciente(1, "11111111", "Patricia", "Cárdenas");
    Paciente segundo = paciente(2, "22222222", "Patricia", "Cárdenas Torres");
    when(pacienteRepository.searchByNombre("patricia cardenas", 10)).thenReturn(List.of(primero, segundo));
    when(historiaClinicaRepository.existsByPacienteIdPaciente(1)).thenReturn(true);
    when(historiaClinicaRepository.existsByPacienteIdPaciente(2)).thenReturn(false);

    BusquedaPacienteResponse response = pacienteService.buscarParaIntegracion("patricia cardenas");

    assertTrue(response.isEncontrado());
    assertEquals("multiple", response.getTipoResultado());
    assertEquals(2, response.getPacientes().size());
  }

  @Test
  void devuelveSinResultadosParaBusquedaValida() {
    when(pacienteRepository.findByNumDocumento("78451268")).thenReturn(Optional.empty());

    BusquedaPacienteResponse response = pacienteService.buscarParaIntegracion("78451268");

    assertFalse(response.isEncontrado());
    assertEquals("sin_resultados", response.getTipoResultado());
    assertTrue(response.getPacientes().isEmpty());
    assertEquals("No se encontró ningún paciente con el criterio indicado.", response.getMensaje());
  }

  @Test
  void rechazaCriterioVacioONumericoInvalido() {
    BusquedaPacienteException vacio = assertThrows(BusquedaPacienteException.class,
        () -> pacienteService.buscarParaIntegracion("   "));
    BusquedaPacienteException numeroInvalido = assertThrows(BusquedaPacienteException.class,
        () -> pacienteService.buscarParaIntegracion("123456789"));

    assertEquals(HttpStatus.BAD_REQUEST, vacio.getStatus());
    assertEquals("CRITERIO_VACIO", vacio.getCodigo());
    assertEquals("CRITERIO_INVALIDO", numeroInvalido.getCodigo());
  }

  private Paciente paciente(Integer id, String dni, String nombres, String apellidos) {
    Paciente paciente = new Paciente();
    paciente.setIdPaciente(id);
    paciente.setNumDocumento(dni);
    paciente.setNombres(nombres);
    paciente.setApellidos(apellidos);
    return paciente;
  }
}
