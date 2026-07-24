package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.exception.ConsultaMedicaIntegracionException;
import com.krivi.apihistorialmedico.model.api.BusquedaConsultasMedicasResponse;
import com.krivi.apihistorialmedico.model.api.EstadisticasConsultasMedicasResponse;
import com.krivi.apihistorialmedico.model.api.ListadoConsultasMedicasResponse;
import com.krivi.apihistorialmedico.model.entity.Consulta;
import com.krivi.apihistorialmedico.model.entity.Empleado;
import com.krivi.apihistorialmedico.model.entity.HistoriaClinica;
import com.krivi.apihistorialmedico.model.entity.Paciente;
import com.krivi.apihistorialmedico.repository.ConsultaRepository;
import com.krivi.apihistorialmedico.repository.HistoriaClinicaRepository;
import com.krivi.apihistorialmedico.repository.PacienteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultaMedicaIntegracionServiceImplTest {
  @Mock ConsultaRepository consultaRepository;
  @Mock PacienteRepository pacienteRepository;
  @Mock HistoriaClinicaRepository historiaClinicaRepository;
  @InjectMocks ConsultaMedicaIntegracionServiceImpl service;

  @Test void buscaPorDniYDevuelveResumenAdministrativoSinDatosClinicos() {
    Paciente paciente = paciente(6, "78451268", "Patricia", "Cárdenas");
    Consulta consulta = consulta(10, paciente, "PENDIENTE");
    when(pacienteRepository.findByNumDocumento("78451268")).thenReturn(Optional.of(paciente));
    when(historiaClinicaRepository.existsByPacienteIdPaciente(6)).thenReturn(true);
    when(consultaRepository.countByPacienteIdPaciente(6)).thenReturn(2L);
    when(consultaRepository.countByPacienteIdPacienteAndEstado(6, "PENDIENTE")).thenReturn(1L);
    when(consultaRepository.countByPacienteIdPacienteAndEstado(6, "ATENDIDO")).thenReturn(1L);
    when(consultaRepository.findAdministrativasRecientesByPacienteId(eq(6), any(Pageable.class))).thenReturn(List.of(consulta));

    BusquedaConsultasMedicasResponse response = service.buscar("78451268");

    assertTrue(response.isEncontrado()); assertEquals("unico", response.getTipoResultado());
    assertEquals(2L, response.getPacientes().getFirst().getTotalConsultas());
    assertEquals("PENDIENTE", response.getPacientes().getFirst().getConsultas().getFirst().getEstado());
    assertEquals("Patricia Cárdenas", response.getPacientes().getFirst().getConsultas().getFirst().getNombreCompleto());
    assertThrows(NoSuchFieldException.class, () -> response.getPacientes().getFirst().getConsultas().getFirst().getClass().getDeclaredField("diagnostico"));
    assertThrows(NoSuchFieldException.class, () -> response.getPacientes().getFirst().getConsultas().getFirst().getClass().getDeclaredField("tratamiento"));
  }

  @Test void buscaPorIdYFormatoPacienteId() {
    Paciente paciente = paciente(8, "12345678", "Ana", "Lima");
    when(pacienteRepository.findById(8)).thenReturn(Optional.of(paciente));
    prepararPacienteSinConsultas(paciente);
    assertTrue(service.buscar("8").isEncontrado());
    assertTrue(service.buscar("paciente:8").isEncontrado());
    verify(pacienteRepository, times(2)).findById(8);
  }

  @Test void buscaPorNombreYNoSeleccionaMultiplesCoincidencias() {
    when(pacienteRepository.searchByNombre("patricia", 5)).thenReturn(List.of(paciente(1, "11111111", "Patricia", "Uno"), paciente(2, "22222222", "Patricia", "Dos")));
    BusquedaConsultasMedicasResponse response = service.buscar("patricia");
    assertTrue(response.isEncontrado()); assertEquals("multiple", response.getTipoResultado()); assertEquals(2, response.getPacientes().size());
    assertNull(response.getPacientes().getFirst().getTotalConsultas());
  }

  @Test void informaPacienteInexistenteYSinConsultas() {
    when(pacienteRepository.findByNumDocumento("99999999")).thenReturn(Optional.empty());
    assertFalse(service.buscar("99999999").isEncontrado());
    Paciente paciente = paciente(4, "44444444", "Luis", "Paz");
    when(pacienteRepository.findById(4)).thenReturn(Optional.of(paciente)); prepararPacienteSinConsultas(paciente);
    BusquedaConsultasMedicasResponse sinConsultas = service.buscar("4");
    assertEquals(0L, sinConsultas.getPacientes().getFirst().getTotalConsultas()); assertTrue(sinConsultas.getPacientes().getFirst().getConsultas().isEmpty());
  }

  @Test void rechazaCriterioNumericoInvalido() {
    ConsultaMedicaIntegracionException error = assertThrows(ConsultaMedicaIntegracionException.class, () -> service.buscar("123456789"));
    assertEquals("CRITERIO_INVALIDO", error.getCodigo());
  }

  @Test void obtieneEstadisticasConRangosAmericaLimaYFechaAtencion() {
    LocalDateTime inicio = LocalDateTime.now(ZoneId.of("America/Lima")).toLocalDate().atStartOfDay();
    when(consultaRepository.count()).thenReturn(10L); when(consultaRepository.countByFechaCreacionGreaterThanEqualAndFechaCreacionLessThan(inicio, inicio.plusDays(1))).thenReturn(2L);
    when(consultaRepository.countByFechaAtencionGreaterThanEqualAndFechaAtencionLessThan(inicio, inicio.plusDays(1))).thenReturn(3L);
    when(consultaRepository.countByEstado("PENDIENTE")).thenReturn(4L); when(consultaRepository.countByEstado("ATENDIDO")).thenReturn(6L);
    EstadisticasConsultasMedicasResponse response = service.obtenerEstadisticas();
    assertEquals(2L, response.getCreadasHoy()); assertEquals(3L, response.getAtendidasHoy()); assertEquals(4L, response.getTotalPendientes()); assertEquals(6L, response.getTotalAtendidas());
  }

  @Test void listaPendientesEstrictamenteYUltimasConLimites() {
    when(consultaRepository.findPendientesAdministrativas()).thenReturn(List.of());
    assertEquals(0, service.obtenerPendientes().getCantidad());
    when(consultaRepository.findUltimasAdministrativas(any(Pageable.class))).thenReturn(List.of());
    service.obtenerUltimas(null); service.obtenerUltimas(1); service.obtenerUltimas(10);
    ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class); verify(consultaRepository, times(3)).findUltimasAdministrativas(captor.capture());
    assertEquals(5, captor.getAllValues().get(0).getPageSize()); assertEquals(1, captor.getAllValues().get(1).getPageSize()); assertEquals(10, captor.getAllValues().get(2).getPageSize());
    assertEquals("LIMITE_INVALIDO", assertThrows(ConsultaMedicaIntegracionException.class, () -> service.obtenerUltimas(0)).getCodigo());
    assertEquals("LIMITE_INVALIDO", assertThrows(ConsultaMedicaIntegracionException.class, () -> service.obtenerUltimas(11)).getCodigo());
  }

  private void prepararPacienteSinConsultas(Paciente paciente) {
    int id = paciente.getIdPaciente(); when(historiaClinicaRepository.existsByPacienteIdPaciente(id)).thenReturn(false);
    when(consultaRepository.countByPacienteIdPaciente(id)).thenReturn(0L); when(consultaRepository.countByPacienteIdPacienteAndEstado(id, "PENDIENTE")).thenReturn(0L); when(consultaRepository.countByPacienteIdPacienteAndEstado(id, "ATENDIDO")).thenReturn(0L);
    when(consultaRepository.findAdministrativasRecientesByPacienteId(eq(id), any(Pageable.class))).thenReturn(List.of());
  }
  private Paciente paciente(int id, String dni, String nombres, String apellidos) { Paciente p = new Paciente(); p.setIdPaciente(id); p.setNumDocumento(dni); p.setNombres(nombres); p.setApellidos(apellidos); return p; }
  private Consulta consulta(int id, Paciente paciente, String estado) { Consulta c = new Consulta(); c.setIdConsulta(id); c.setPaciente(paciente); c.setEstado(estado); c.setFechaCreacion(LocalDateTime.of(2026, 7, 24, 9, 0)); c.setEspecialidadRequerida("MEDICINA_GENERAL"); Empleado e = new Empleado(); e.setIdEmpleado(3); e.setNombres("Marta"); e.setApellidos("Ríos"); c.setDoctorResponsable(e); HistoriaClinica h = new HistoriaClinica(); h.setIdHistoriaClinica(2); c.setHistoriaClinica(h); return c; }
}
