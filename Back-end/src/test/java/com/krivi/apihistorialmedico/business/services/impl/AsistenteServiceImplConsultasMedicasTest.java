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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsistenteServiceImplConsultasMedicasTest {
  private static final String DNI_PRUEBA = "0".repeat(8);
  private static final String NOMBRE_BUSQUEDA = "prueba uno dos";

  @Mock private PacienteRepository pacienteRepository;
  @Mock private HistoriaClinicaRepository historiaClinicaRepository;
  @Mock private ConsultaRepository consultaRepository;
  @Mock private UsuarioRepository usuarioRepository;
  @Mock private HistoriaClinicaService historiaClinicaService;
  @InjectMocks private AsistenteServiceImpl asistenteService;

  @Test
  void atendidasHoyUsaEstadoYFechaAtencionYRespondeCuandoNoHayResultados() {
    when(consultaRepository.countByEstadoAndFechaAtencionGreaterThanEqualAndFechaAtencionLessThan(eq("ATENDIDO"), any(), any())).thenReturn(0L);

    AsistenteResponse response = preguntar("¿Cuántas consultas médicas se atendieron hoy?");

    assertEquals("CONSULTAS_MEDICAS_ATENDIDAS", response.getIntencion());
    assertEquals("Actualmente no hay consultas médicas atendidas el día de hoy.", response.getRespuesta());
    verify(consultaRepository).countByEstadoAndFechaAtencionGreaterThanEqualAndFechaAtencionLessThan(eq("ATENDIDO"), any(), any());
    verify(consultaRepository, never()).countByEstadoAndFechaCreacionGreaterThanEqualAndFechaCreacionLessThan(any(), any(), any());
    verifyNoInteractions(pacienteRepository);
  }

  @Test
  void atendidasHoyDevuelveElConteoCorrecto() {
    when(consultaRepository.countByEstadoAndFechaAtencionGreaterThanEqualAndFechaAtencionLessThan(eq("ATENDIDO"), any(), any())).thenReturn(3L);

    AsistenteResponse response = preguntar("Consultas médicas atendidas hoy");

    assertEquals("Actualmente hay 3 consultas médicas atendidas el día de hoy.", response.getRespuesta());
    assertEquals(3L, response.getDatos().get("cantidad"));
  }

  @Test
  void ultimaConsultaPorDniUsaLaConsultaMasRecienteDelRepositorio() {
    prepararPacientePorDniConHistoria();
    Consulta reciente = consulta(30, "ATENDIDO", LocalDateTime.now());
    when(consultaRepository.findUltimaByHistoriaClinica(8)).thenReturn(Optional.of(reciente));

    AsistenteResponse response = preguntar("¿Cuál fue la última consulta del paciente con DNI " + DNI_PRUEBA + "?");

    assertEquals("CONSULTAS_MEDICAS_PACIENTE_ULTIMA", response.getIntencion());
    assertTrue(response.getRespuesta().contains("ID de consulta médica: 30"));
    assertEquals(1, response.getDatos().get("cantidad"));
    verify(consultaRepository).findUltimaByHistoriaClinica(8);
    verify(consultaRepository, never()).count();
    verify(historiaClinicaRepository, never()).count();
  }

  @Test
  void ultimaConsultaUsaFechaCreacionComoRespaldoCuandoFechaConsultaEsNull() {
    prepararPacientePorDniConHistoria();
    LocalDateTime fechaCreacion = LocalDateTime.of(2026, 2, 3, 10, 15);
    when(consultaRepository.findUltimaByHistoriaClinica(8)).thenReturn(Optional.of(consulta(31, "ATENDIDO", fechaCreacion)));

    AsistenteResponse response = preguntar("Última consulta médica del paciente con DNI " + DNI_PRUEBA);

    assertTrue(response.getRespuesta().contains("03/02/2026 10:15"));
    assertEquals(fechaCreacion, ((List<? extends java.util.Map<?, ?>>) response.getDatos().get("resultados")).getFirst().get("fecha"));
  }

  @Test
  void ultimaConsultaPorNombreCompletoResuelvePacienteYNoCuentaConsultasGenerales() {
    prepararPacientePorNombreConHistoria();
    when(consultaRepository.findUltimaByHistoriaClinica(8)).thenReturn(Optional.of(consulta(32, "ATENDIDO", LocalDateTime.now())));

    AsistenteResponse response = preguntar("¿Cuál fue la última consulta de PACIENTE PRUEBA UNO DOS?");

    assertEquals("CONSULTAS_MEDICAS_PACIENTE_ULTIMA", response.getIntencion());
    verify(pacienteRepository).searchByNombre(NOMBRE_BUSQUEDA, 10);
    verify(consultaRepository, never()).count();
    verify(historiaClinicaRepository, never()).count();
  }

  @Test
  void ultimaConsultaInformaCuandoPacienteNoTieneConsultas() {
    prepararPacientePorDniConHistoria();
    when(consultaRepository.findUltimaByHistoriaClinica(8)).thenReturn(Optional.empty());

    AsistenteResponse response = preguntar("¿Cuál fue la última consulta del paciente con DNI " + DNI_PRUEBA + "?");

    assertEquals("El paciente no tiene consultas médicas registradas.", response.getRespuesta());
  }

  @Test
  void ultimaConsultaInformaCuandoPacienteNoTieneHistoria() {
    prepararPacientePorDniSinHistoria();

    AsistenteResponse response = preguntar("¿Cuál fue la última consulta del paciente con DNI " + DNI_PRUEBA + "?");

    assertEquals("El paciente está registrado, pero no cuenta con una historia clínica. Por lo tanto, no tiene consultas médicas registradas.", response.getRespuesta());
    verifyNoInteractions(consultaRepository);
  }

  @Test
  void pendientesPorDniConsultaSoloElEstadoPendiente() {
    prepararPacientePorDniConHistoria();
    when(consultaRepository.findByHistoriaClinicaIdHistoriaClinicaAndEstadoIgnoreCaseOrderByFechaCreacionAsc(8, "PENDIENTE"))
        .thenReturn(List.of(consulta(40, "PENDIENTE", LocalDateTime.now())));

    AsistenteResponse response = preguntar("El paciente con DNI " + DNI_PRUEBA + " tiene consultas pendientes?");

    assertEquals("CONSULTAS_MEDICAS_PACIENTE_PENDIENTES", response.getIntencion());
    assertTrue(response.getRespuesta().startsWith("Consultas médicas pendientes del paciente:"));
    assertTrue(response.getRespuesta().contains("ID de consulta médica: 40"));
    verify(consultaRepository).findByHistoriaClinicaIdHistoriaClinicaAndEstadoIgnoreCaseOrderByFechaCreacionAsc(8, "PENDIENTE");
    verify(consultaRepository, never()).findByHistoriaClinicaIdHistoriaClinica(8);
    verify(consultaRepository, never()).countByEstado("PENDIENTE");
  }

  @Test
  void pendientesPorNombreDevuelveVariasPendientes() {
    prepararPacientePorNombreConHistoria();
    when(consultaRepository.findByHistoriaClinicaIdHistoriaClinicaAndEstadoIgnoreCaseOrderByFechaCreacionAsc(8, "PENDIENTE"))
        .thenReturn(List.of(consulta(41, "PENDIENTE", LocalDateTime.now()), consulta(42, "PENDIENTE", LocalDateTime.now().plusHours(1))));

    AsistenteResponse response = preguntar("El paciente PACIENTE PRUEBA UNO DOS tiene consultas médicas pendientes?");

    assertEquals(2, response.getDatos().get("cantidad"));
    assertFalse(response.getRespuesta().contains("Estado: Atendido"));
    verify(pacienteRepository).searchByNombre(NOMBRE_BUSQUEDA, 10);
  }

  @Test
  void pendientesInformaCuandoNoExisten() {
    prepararPacientePorDniConHistoria();
    when(consultaRepository.findByHistoriaClinicaIdHistoriaClinicaAndEstadoIgnoreCaseOrderByFechaCreacionAsc(8, "PENDIENTE")).thenReturn(List.of());

    AsistenteResponse response = preguntar("El paciente con DNI " + DNI_PRUEBA + " tiene consultas pendientes?");

    assertEquals("El paciente no tiene consultas médicas pendientes.", response.getRespuesta());
    assertEquals(0, response.getDatos().get("cantidad"));
  }

  @Test
  void pendientesInformaCuandoPacienteNoTieneHistoria() {
    prepararPacientePorDniSinHistoria();

    AsistenteResponse response = preguntar("El paciente con DNI " + DNI_PRUEBA + " tiene consultas pendientes?");

    assertEquals("El paciente está registrado, pero no cuenta con una historia clínica. Por lo tanto, no tiene consultas médicas pendientes.", response.getRespuesta());
    verifyNoInteractions(consultaRepository);
  }

  @Test
  void mantieneConteoGeneralDePendientesSinResolverPaciente() {
    when(consultaRepository.countByEstado("PENDIENTE")).thenReturn(6L);

    AsistenteResponse response = preguntar("¿Cuántas consultas médicas están pendientes?");

    assertEquals("CONSULTAS_MEDICAS_PENDIENTES", response.getIntencion());
    assertEquals(6L, response.getDatos().get("cantidad"));
    verify(consultaRepository).countByEstado("PENDIENTE");
    verifyNoInteractions(pacienteRepository);
  }

  @Test
  void mantieneTotalGeneralDeConsultasSinResolverPaciente() {
    when(consultaRepository.count()).thenReturn(12L);

    AsistenteResponse response = preguntar("¿Cuántas consultas médicas hay registradas?");

    assertEquals("CONSULTAS_MEDICAS_REGISTRADAS", response.getIntencion());
    assertEquals(12L, response.getDatos().get("cantidad"));
    verifyNoInteractions(pacienteRepository);
  }

  private Paciente prepararPacientePorDniConHistoria() {
    Paciente paciente = paciente();
    when(pacienteRepository.findByNumDocumentoAndEstadoRegistroOrderByIdPacienteAsc(DNI_PRUEBA, EstadoRegistroPaciente.ACTIVO)).thenReturn(List.of(paciente));
    when(historiaClinicaRepository.findByPacienteIdPaciente(5)).thenReturn(Optional.of(historia(paciente)));
    return paciente;
  }

  private void prepararPacientePorDniSinHistoria() {
    Paciente paciente = paciente();
    when(pacienteRepository.findByNumDocumentoAndEstadoRegistroOrderByIdPacienteAsc(DNI_PRUEBA, EstadoRegistroPaciente.ACTIVO)).thenReturn(List.of(paciente));
    when(historiaClinicaRepository.findByPacienteIdPaciente(5)).thenReturn(Optional.empty());
  }

  private void prepararPacientePorNombreConHistoria() {
    Paciente paciente = paciente();
    when(pacienteRepository.searchByNombre(NOMBRE_BUSQUEDA, 10)).thenReturn(List.of(paciente));
    when(historiaClinicaRepository.findByPacienteIdPaciente(5)).thenReturn(Optional.of(historia(paciente)));
  }

  private AsistenteResponse preguntar(String pregunta) {
    AsistenteRequest request = new AsistenteRequest();
    request.setPregunta(pregunta);
    return asistenteService.preguntar(request, null);
  }

  private Paciente paciente() {
    Paciente paciente = new Paciente(5);
    paciente.setNombres("PACIENTE PRUEBA");
    paciente.setApellidos("UNO DOS");
    paciente.setNumDocumento(DNI_PRUEBA);
    return paciente;
  }

  private HistoriaClinica historia(Paciente paciente) {
    HistoriaClinica historia = new HistoriaClinica();
    historia.setIdHistoriaClinica(8);
    historia.setPaciente(paciente);
    return historia;
  }

  private Consulta consulta(int id, String estado, LocalDateTime fechaCreacion) {
    Consulta consulta = new Consulta();
    consulta.setIdConsulta(id);
    consulta.setEstado(estado);
    consulta.setFechaCreacion(fechaCreacion);
    consulta.setHistoriaClinica(historia(paciente()));
    return consulta;
  }
}
