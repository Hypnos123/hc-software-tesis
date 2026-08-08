package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.services.HistoriaClinicaService;
import com.krivi.apihistorialmedico.model.api.AsistenteRequest;
import com.krivi.apihistorialmedico.model.api.AsistenteResponse;
import com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente;
import com.krivi.apihistorialmedico.model.entity.HistoriaClinica;
import com.krivi.apihistorialmedico.model.entity.Paciente;
import com.krivi.apihistorialmedico.repository.ConsultaRepository;
import com.krivi.apihistorialmedico.repository.HistoriaClinicaRepository;
import com.krivi.apihistorialmedico.repository.PacienteRepository;
import com.krivi.apihistorialmedico.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsistenteServiceImplResolucionNombreClinicoTest {
  @Mock private PacienteRepository pacienteRepository;
  @Mock private HistoriaClinicaRepository historiaClinicaRepository;
  @Mock private ConsultaRepository consultaRepository;
  @Mock private UsuarioRepository usuarioRepository;
  @Mock private HistoriaClinicaService historiaClinicaService;
  @InjectMocks private AsistenteServiceImpl asistenteService;

  @Test
  void encuentraSoloPacienteConTodosLosTerminosSinTildesNiMayusculas() {
    Paciente correcto = paciente(1, "Lucía", "Villareal Méndez");
    Paciente incorrecto = paciente(2, "Rafael", "Velasquez Morales");
    prepararListadoClinico(correcto, List.of(correcto, incorrecto));

    AsistenteResponse response = preguntar("Muéstrame las consultas médicas de lucia villareal mendez");

    assertEquals("CONSULTAS_MEDICAS_PACIENTE_LISTADO", response.getIntencion());
    verify(historiaClinicaRepository).findByPacienteIdPaciente(1);
    verify(historiaClinicaRepository, never()).findByPacienteIdPaciente(2);
  }

  @Test
  void encuentraPacienteAunqueLosTerminosTenganOtroOrden() {
    Paciente correcto = paciente(1, "Lucía", "Villareal Méndez");
    prepararListadoClinico(correcto, List.of(correcto));

    preguntar("Muéstrame las consultas médicas de Villareal Mendez Lucia");

    verify(historiaClinicaRepository).findByPacienteIdPaciente(1);
  }

  @Test
  void admiteEquivalenciaControladaEntreSalinaYSalinasSinCoincidenciasParciales() {
    Paciente correcto = paciente(3, "Diego Alonso", "Salinas Vega");
    Paciente incorrecto = paciente(4, "Oliver Okarin", "Vega Anicama");
    prepararListadoClinico(correcto, List.of(correcto, incorrecto));

    preguntar("Muéstrame las consultas de Salina Vega Diego");

    verify(historiaClinicaRepository).findByPacienteIdPaciente(3);
    verify(historiaClinicaRepository, never()).findByPacienteIdPaciente(4);
  }

  @Test
  void paredesGomezNoSeleccionaPacienteSinTodosLosTokens() {
    Paciente correcto = paciente(5, "Ana Lucía", "Paredes Gómez");
    Paciente incorrecto = paciente(6, "Rafael", "Velasquez Morales");
    prepararListadoClinico(correcto, List.of(correcto, incorrecto));

    preguntar("Lista las consultas médicas de Paredes Gomez");

    verify(historiaClinicaRepository).findByPacienteIdPaciente(5);
    verify(historiaClinicaRepository, never()).findByPacienteIdPaciente(6);
  }

  @Test
  void vargasRiosFernandoNoSeleccionaPacienteSinTodosLosTokens() {
    Paciente correcto = paciente(7, "Fernando Josset", "Vargas Ríos");
    Paciente incorrecto = paciente(8, "Rafael", "Velasquez Morales");
    prepararListadoClinico(correcto, List.of(correcto, incorrecto));

    preguntar("Lista las consultas médicas de Vargas Rios Fernando");

    verify(historiaClinicaRepository).findByPacienteIdPaciente(7);
    verify(historiaClinicaRepository, never()).findByPacienteIdPaciente(8);
  }

  @Test
  void apellidoUnicoPuedeResolverPacienteClinico() {
    Paciente correcto = paciente(9, "Paciente Prueba", "Quispe");
    prepararListadoClinico(correcto, List.of(correcto, paciente(10, "Otro Paciente", "Paredes")));

    preguntar("Muéstrame las consultas médicas de Quispe");

    verify(historiaClinicaRepository).findByPacienteIdPaciente(9);
  }

  @Test
  void apellidoCompartidoNoSeleccionaAutomaticamenteYSolicitaDni() {
    Paciente primero = paciente(11, "Paciente Prueba Uno", "Quispe");
    Paciente segundo = paciente(12, "Paciente Prueba Dos", "Quispe");
    when(pacienteRepository.findAllByEstadoRegistroOrderByIdPacienteAsc(EstadoRegistroPaciente.ACTIVO)).thenReturn(List.of(primero, segundo));

    AsistenteResponse response = preguntar("Muéstrame las consultas médicas de Quispe");

    assertEquals("CONSULTAS_MEDICAS_PACIENTE_AMBIGUO", response.getIntencion());
    assertTrue(response.getRespuesta().contains("Ingresa el DNI"));
    verifyNoInteractions(historiaClinicaRepository, consultaRepository);
  }

  @Test
  void consultaIncompletaNoSeleccionaPrimerResultadoAproximado() {
    Paciente primero = paciente(13, "Paciente Prueba Uno", "Vega");
    Paciente segundo = paciente(14, "Paciente Prueba Dos", "Vega");
    Paciente noRelacionado = paciente(15, "Rafael", "Velasquez Morales");
    when(pacienteRepository.findAllByEstadoRegistroOrderByIdPacienteAsc(EstadoRegistroPaciente.ACTIVO)).thenReturn(List.of(primero, segundo, noRelacionado));

    AsistenteResponse response = preguntar("Ver consultas del paciente Vega");

    assertEquals("CONSULTAS_MEDICAS_PACIENTE_AMBIGUO", response.getIntencion());
    verifyNoInteractions(historiaClinicaRepository, consultaRepository);
  }

  @Test
  void busquedaInformativaPorApellidoSigueMostrandoVariasCoincidencias() {
    Paciente primero = paciente(16, "Paciente Prueba Uno", "Quispe");
    Paciente segundo = paciente(17, "Paciente Prueba Dos", "Quispe");
    when(pacienteRepository.searchByNombre("quispe", 5)).thenReturn(List.of(primero, segundo));

    AsistenteResponse response = preguntar("Buscar paciente por nombre Quispe");

    assertEquals("BUSQUEDA_PACIENTE_NOMBRE", response.getIntencion());
    assertEquals(2, ((List<?>) response.getDatos().get("resultados")).size());
    verify(pacienteRepository).searchByNombre("quispe", 5);
  }

  private void prepararListadoClinico(Paciente correcto, List<Paciente> candidatos) {
    HistoriaClinica historia = new HistoriaClinica();
    historia.setIdHistoriaClinica(20 + correcto.getIdPaciente());
    historia.setPaciente(correcto);
    when(pacienteRepository.findAllByEstadoRegistroOrderByIdPacienteAsc(EstadoRegistroPaciente.ACTIVO)).thenReturn(candidatos);
    when(historiaClinicaRepository.findByPacienteIdPaciente(correcto.getIdPaciente())).thenReturn(Optional.of(historia));
    when(consultaRepository.findByHistoriaClinicaOrdenadasPorFecha(historia.getIdHistoriaClinica())).thenReturn(List.of());
  }

  private AsistenteResponse preguntar(String pregunta) {
    AsistenteRequest request = new AsistenteRequest();
    request.setPregunta(pregunta);
    return asistenteService.preguntar(request, null);
  }

  private Paciente paciente(int id, String nombres, String apellidos) {
    Paciente paciente = new Paciente(id);
    paciente.setNombres(nombres);
    paciente.setApellidos(apellidos);
    paciente.setNumDocumento(String.valueOf(id).repeat(8).substring(0, 8));
    return paciente;
  }
}
