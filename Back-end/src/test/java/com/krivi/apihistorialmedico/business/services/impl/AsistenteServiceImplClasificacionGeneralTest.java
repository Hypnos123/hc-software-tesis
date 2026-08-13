package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.services.HistoriaClinicaService;
import com.krivi.apihistorialmedico.model.api.AsistenteRequest;
import com.krivi.apihistorialmedico.model.api.AsistenteResponse;
import com.krivi.apihistorialmedico.model.entity.Consulta;
import com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente;
import com.krivi.apihistorialmedico.model.entity.HistoriaClinica;
import com.krivi.apihistorialmedico.model.entity.Paciente;
import com.krivi.apihistorialmedico.repository.ConsultaRepository;
import com.krivi.apihistorialmedico.repository.HistoriaClinicaRepository;
import com.krivi.apihistorialmedico.repository.PacienteRepository;
import com.krivi.apihistorialmedico.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsistenteServiceImplClasificacionGeneralTest {
  private static final String DNI_PRUEBA = "0".repeat(8);

  @Mock private PacienteRepository pacienteRepository;
  @Mock private HistoriaClinicaRepository historiaClinicaRepository;
  @Mock private ConsultaRepository consultaRepository;
  @Mock private UsuarioRepository usuarioRepository;
  @Mock private HistoriaClinicaService historiaClinicaService;
  @InjectMocks private AsistenteServiceImpl asistenteService;

  @Test
  void clasificaElTotalGeneralDeConsultasAntesDeResolverPacientes() {
    when(consultaRepository.count()).thenReturn(14L);

    AsistenteResponse response = preguntar("¿Cuántas consultas médicas hay registradas?");

    assertEquals("CONSULTAS_MEDICAS_REGISTRADAS", response.getIntencion());
    assertEquals(14L, response.getDatos().get("cantidad"));
    verify(consultaRepository).count();
    verifyNoInteractions(pacienteRepository);
  }

  @Test
  void clasificaElTotalGeneralDeHistoriasAntesDeResolverPacientes() {
    when(historiaClinicaRepository.count()).thenReturn(9L);

    AsistenteResponse response = preguntar("¿Cuántas historias clínicas hay registradas?");

    assertEquals("HISTORIAS_CLINICAS", response.getIntencion());
    assertEquals(9L, response.getDatos().get("cantidad"));
    verify(historiaClinicaRepository).count();
    verifyNoInteractions(pacienteRepository);
  }

  @Test
  void clasificaHistoriasCreadasHoyComoConsultaGeneral() {
    when(historiaClinicaRepository.countByFechaCreacionGreaterThanEqualAndFechaCreacionLessThan(any(), any())).thenReturn(3L);

    AsistenteResponse response = preguntar("Historias clínicas creadas hoy");

    assertEquals("HISTORIAS_CLINICAS", response.getIntencion());
    assertEquals(3L, response.getDatos().get("cantidad"));
    verify(historiaClinicaRepository).countByFechaCreacionGreaterThanEqualAndFechaCreacionLessThan(any(), any());
    verifyNoInteractions(pacienteRepository);
  }

  @Test
  void clasificaConsultasAtendidasHoyComoConsultaGeneral() {
    when(consultaRepository.countByEstadoAndFechaAtencionGreaterThanEqualAndFechaAtencionLessThan(eq("ATENDIDO"), any(), any())).thenReturn(4L);

    AsistenteResponse response = preguntar("Consultas médicas atendidas hoy");

    assertEquals("CONSULTAS_MEDICAS_ATENDIDAS", response.getIntencion());
    assertEquals(4L, response.getDatos().get("cantidad"));
    verify(consultaRepository).countByEstadoAndFechaAtencionGreaterThanEqualAndFechaAtencionLessThan(eq("ATENDIDO"), any(), any());
    verifyNoInteractions(pacienteRepository);
  }

  @Test
  void mantieneConteoGeneralDePacientes() {
    when(pacienteRepository.countByEstadoRegistro(EstadoRegistroPaciente.ACTIVO)).thenReturn(7L);

    AsistenteResponse response = preguntar("¿Cuántos pacientes hay registrados?");

    assertEquals("PACIENTES_REGISTRADOS", response.getIntencion());
    assertEquals(7L, response.getDatos().get("cantidad"));
    verify(pacienteRepository).countByEstadoRegistro(EstadoRegistroPaciente.ACTIVO);
    verificarSinResolucionDePaciente();
  }

  @Test
  void mantieneUltimosPacientesComoConsultaGeneral() {
    Paciente paciente = paciente();
    paciente.setFechaIngreso(new Date());
    when(pacienteRepository.findAllByEstadoRegistroOrderByIdPacienteAsc(EstadoRegistroPaciente.ACTIVO)).thenReturn(List.of(paciente));

    AsistenteResponse response = preguntar("Muéstrame los últimos pacientes registrados");

    assertEquals("ULTIMOS_PACIENTES", response.getIntencion());
    verificarSinResolucionDePaciente();
  }

  @Test
  void mantieneEdadPromedioComoConsultaGeneral() {
    Paciente paciente = paciente();
    paciente.setFechaNacimiento(new Date(0));
    when(pacienteRepository.findAllByEstadoRegistroOrderByIdPacienteAsc(EstadoRegistroPaciente.ACTIVO)).thenReturn(List.of(paciente));

    AsistenteResponse response = preguntar("¿Cuál es la edad promedio de los pacientes?");

    assertEquals("EDAD_PROMEDIO_PACIENTES", response.getIntencion());
    verificarSinResolucionDePaciente();
  }

  @Test
  void mantieneDeteccionGeneralDePacientesDuplicados() {
    Paciente primero = paciente();
    Paciente segundo = paciente();
    segundo.setIdPaciente(6);
    when(pacienteRepository.findAllByEstadoRegistroOrderByIdPacienteAsc(EstadoRegistroPaciente.ACTIVO)).thenReturn(List.of(primero, segundo));

    AsistenteResponse response = preguntar("Detectar posibles pacientes duplicados");

    assertEquals("ANALISIS_DUPLICADOS_PACIENTES", response.getIntencion());
    verificarSinResolucionDePaciente();
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "¿Hay pacientes duplicados?",
      "¿Existen pacientes duplicados?",
      "Existen pacientes duplicados",
      "Detectar pacientes duplicados",
      "Buscar pacientes duplicados",
      "Mostrar pacientes duplicados"
  })
  void priorizaPacientesDuplicadosSobreElConteoGeneral(String pregunta) {
    Paciente primero = paciente();
    Paciente segundo = paciente();
    segundo.setIdPaciente(6);
    when(pacienteRepository.findAllByEstadoRegistroOrderByIdPacienteAsc(EstadoRegistroPaciente.ACTIVO))
        .thenReturn(List.of(primero, segundo));

    AsistenteResponse response = preguntar(pregunta);

    assertEquals("ANALISIS_DUPLICADOS_PACIENTES", response.getIntencion());
    verify(pacienteRepository, never()).countByEstadoRegistro(EstadoRegistroPaciente.ACTIVO);
  }

  @ParameterizedTest
  @ValueSource(strings = { "¿Cuántos pacientes hay?", "¿Cuántos pacientes están registrados?" })
  void conservaElConteoGeneralSinConceptoDeDuplicidad(String pregunta) {
    when(pacienteRepository.countByEstadoRegistro(EstadoRegistroPaciente.ACTIVO)).thenReturn(7L);

    AsistenteResponse response = preguntar(pregunta);

    assertEquals("PACIENTES_REGISTRADOS", response.getIntencion());
    verify(pacienteRepository).countByEstadoRegistro(EstadoRegistroPaciente.ACTIVO);
    verify(pacienteRepository, never()).findAllByEstadoRegistroOrderByIdPacienteAsc(EstadoRegistroPaciente.ACTIVO);
  }

  @Test
  void mantieneConteoDeConsultasPorPacienteConDni() {
    Paciente paciente = paciente();
    HistoriaClinica historia = historia(paciente);
    when(pacienteRepository.findByNumDocumentoAndEstadoRegistroOrderByIdPacienteAsc(DNI_PRUEBA, EstadoRegistroPaciente.ACTIVO)).thenReturn(List.of(paciente));
    when(historiaClinicaRepository.findByPacienteIdPaciente(paciente.getIdPaciente())).thenReturn(Optional.of(historia));
    when(consultaRepository.countByHistoriaClinicaIdHistoriaClinica(historia.getIdHistoriaClinica())).thenReturn(1L);

    AsistenteResponse response = preguntar("¿Cuántas consultas médicas tiene el paciente con DNI " + DNI_PRUEBA + "?");

    assertEquals("CONSULTAS_MEDICAS_PACIENTE_CANTIDAD", response.getIntencion());
    assertEquals(1, response.getDatos().get("cantidad"));
    verify(pacienteRepository).findByNumDocumentoAndEstadoRegistroOrderByIdPacienteAsc(DNI_PRUEBA, EstadoRegistroPaciente.ACTIVO);
    verify(consultaRepository, never()).count();
  }

  @Test
  void mantieneConsultaDeHistoriaPorPacienteConDni() {
    Paciente paciente = paciente();
    when(pacienteRepository.findByNumDocumentoAndEstadoRegistroOrderByIdPacienteAsc(DNI_PRUEBA, EstadoRegistroPaciente.ACTIVO)).thenReturn(List.of(paciente));
    when(historiaClinicaRepository.findByPacienteIdPaciente(paciente.getIdPaciente())).thenReturn(Optional.of(historia(paciente)));

    AsistenteResponse response = preguntar("¿El paciente con DNI " + DNI_PRUEBA + " tiene historia clínica?");

    assertEquals("HISTORIA_CLINICA_EXISTENTE", response.getIntencion());
    verify(pacienteRepository).findByNumDocumentoAndEstadoRegistroOrderByIdPacienteAsc(DNI_PRUEBA, EstadoRegistroPaciente.ACTIVO);
    verify(historiaClinicaRepository, never()).count();
  }

  @Test
  void mantieneConsultasPendientesPorPacienteConNombre() {
    Paciente paciente = paciente();
    HistoriaClinica historia = historia(paciente);
    when(pacienteRepository.findAllByEstadoRegistroOrderByIdPacienteAsc(EstadoRegistroPaciente.ACTIVO)).thenReturn(List.of(paciente));
    when(historiaClinicaRepository.findByPacienteIdPaciente(paciente.getIdPaciente())).thenReturn(Optional.of(historia));
    when(consultaRepository.findByHistoriaClinicaIdHistoriaClinicaAndEstadoIgnoreCaseOrderByFechaCreacionAsc(historia.getIdHistoriaClinica(), "PENDIENTE")).thenReturn(List.of(consulta(historia, "PENDIENTE")));

    AsistenteResponse response = preguntar("¿El paciente PRUEBA UNO tiene consultas médicas pendientes?");

    assertEquals("CONSULTAS_MEDICAS_PACIENTE_PENDIENTES", response.getIntencion());
    verify(pacienteRepository).findAllByEstadoRegistroOrderByIdPacienteAsc(EstadoRegistroPaciente.ACTIVO);
    verify(consultaRepository, never()).countByEstado("PENDIENTE");
  }

  private AsistenteResponse preguntar(String pregunta) {
    AsistenteRequest request = new AsistenteRequest();
    request.setPregunta(pregunta);
    return asistenteService.preguntar(request, null);
  }

  private void verificarSinResolucionDePaciente() {
    verify(pacienteRepository, never()).searchByNombre(any(), anyInt());
    verify(pacienteRepository, never()).findByNumDocumentoAndEstadoRegistroOrderByIdPacienteAsc(any(), any());
    verify(pacienteRepository, never()).findByIdPacienteAndEstadoRegistro(anyInt(), any());
  }

  private Paciente paciente() {
    Paciente paciente = new Paciente(5);
    paciente.setNombres("PACIENTE");
    paciente.setApellidos("PRUEBA UNO");
    paciente.setNumDocumento(DNI_PRUEBA);
    return paciente;
  }

  private HistoriaClinica historia(Paciente paciente) {
    HistoriaClinica historia = new HistoriaClinica();
    historia.setIdHistoriaClinica(8);
    historia.setPaciente(paciente);
    historia.setFechaCreacion(LocalDateTime.now());
    return historia;
  }

  private Consulta consulta(HistoriaClinica historia, String estado) {
    Consulta consulta = new Consulta();
    consulta.setIdConsulta(11);
    consulta.setHistoriaClinica(historia);
    consulta.setEstado(estado);
    consulta.setFechaCreacion(LocalDateTime.now());
    return consulta;
  }
}
