package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.exception.ConsultaReporteException;
import com.krivi.apihistorialmedico.model.api.ReporteConsultaAlcance;
import com.krivi.apihistorialmedico.model.api.ReporteConsultaFiltroRequest;
import com.krivi.apihistorialmedico.model.api.ReporteMedicoDocumento;
import com.krivi.apihistorialmedico.model.api.OrigenFechaConsultaReporte;
import com.krivi.apihistorialmedico.model.entity.Consulta;
import com.krivi.apihistorialmedico.model.entity.Empleado;
import com.krivi.apihistorialmedico.model.entity.HistoriaClinica;
import com.krivi.apihistorialmedico.model.entity.Paciente;
import com.krivi.apihistorialmedico.repository.ConsultaRepository;
import com.krivi.apihistorialmedico.repository.PacienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultaReporteQueryServiceImplTest {
  private static final ZoneId LIMA = ZoneId.of("America/Lima");
  @Mock ConsultaRepository consultaRepository;
  @Mock PacienteRepository pacienteRepository;
  private ConsultaReporteQueryServiceImpl service;
  private Paciente paciente;

  @BeforeEach
  void setUp() {
    service = new ConsultaReporteQueryServiceImpl(consultaRepository, pacienteRepository);
    paciente = paciente(8, LocalDate.of(1990, 8, 20));
  }

  @Test
  void seleccionaConsultaIndividualAtendida() {
    Consulta consulta = consulta(10, "  atendido  ", LocalDateTime.of(2026, 8, 20, 10, 30), 3);
    when(consultaRepository.findByIdForReporte(10)).thenReturn(Optional.of(consulta));

    ReporteMedicoDocumento documento = service.seleccionarConsultaIndividual(10);

    assertEquals(1, documento.getTotalConsultasEncontradas());
    assertEquals(1, documento.getConsultasAtendidasIncluidas());
    assertEquals(0, documento.getConsultasNoAtendidasExcluidas());
    assertEquals(10, documento.getConsultas().getFirst().getIdConsulta());
  }

  @Test
  void rechazaConsultaIndividualPendiente() {
    when(consultaRepository.findByIdForReporte(11))
        .thenReturn(Optional.of(consulta(11, "PENDIENTE", LocalDateTime.now(), 3)));

    ConsultaReporteException error = assertThrows(ConsultaReporteException.class,
        () -> service.seleccionarConsultaIndividual(11));

    assertEquals("CONSULTA_NO_ATENDIDA", error.getCodigo());
  }

  @Test
  void todasCuentaSeisConsultasEIncluyeSoloCuatroAtendidas() {
    List<Consulta> consultas = List.of(
        consulta(1, "ATENDIDO", fecha(1), 10), consulta(2, "PENDIENTE", fecha(2), 10),
        consulta(3, "ATENDIDO", fecha(3), 10), consulta(4, "ATENDIDO", fecha(4), 10),
        consulta(5, "PENDIENTE", fecha(5), 10), consulta(6, "ATENDIDO", fecha(6), 10));
    prepararPaciente(consultas);

    ReporteMedicoDocumento documento = service.seleccionarConsultasPaciente(8, filtro(ReporteConsultaAlcance.TODAS));

    assertEquals(6, documento.getTotalConsultasEncontradas());
    assertEquals(4, documento.getConsultasAtendidasIncluidas());
    assertEquals(2, documento.getConsultasNoAtendidasExcluidas());
    assertEquals(List.of(1, 3, 4, 6), ids(documento));
  }

  @Test
  void ultimaIgnoraPendientePosterior() {
    Consulta atendida = consulta(20, "ATENDIDO", LocalDateTime.of(2026, 5, 10, 9, 0), 1);
    Consulta pendientePosterior = consulta(21, "PENDIENTE", LocalDateTime.of(2026, 5, 15, 9, 0), 1);
    prepararPaciente(List.of(atendida, pendientePosterior));

    ReporteMedicoDocumento documento = service.seleccionarConsultasPaciente(8, filtro(ReporteConsultaAlcance.ULTIMA));

    assertEquals(List.of(20), ids(documento));
    assertEquals(1, documento.getTotalConsultasEncontradas());
    assertEquals(0, documento.getConsultasNoAtendidasExcluidas());
  }

  @Test
  void fechaCuentaTodosLosEstadosDelDiaEIncluyeAtendidas() {
    LocalDate dia = LocalDate.of(2026, 6, 12);
    prepararPaciente(List.of(
        consulta(1, "ATENDIDO", dia.atTime(8, 0), 1),
        consulta(2, "PENDIENTE", dia.atTime(18, 0), 1),
        consulta(3, "ATENDIDO", dia.plusDays(1).atStartOfDay(), 1)));
    ReporteConsultaFiltroRequest filtro = ReporteConsultaFiltroRequest.builder()
        .alcance(ReporteConsultaAlcance.FECHA).fecha(dia).build();

    ReporteMedicoDocumento documento = service.seleccionarConsultasPaciente(8, filtro);

    assertEquals(2, documento.getTotalConsultasEncontradas());
    assertEquals(1, documento.getConsultasAtendidasIncluidas());
    assertEquals(1, documento.getConsultasNoAtendidasExcluidas());
    assertEquals(List.of(1), ids(documento));
  }

  @Test
  void rangoIncluyeAmbosExtremosYOrdenaCronologicamente() {
    LocalDate desde = LocalDate.of(2026, 3, 1);
    LocalDate hasta = LocalDate.of(2026, 3, 31);
    prepararPaciente(List.of(
        consulta(31, "ATENDIDO", hasta.atTime(23, 59), 1),
        consulta(15, "ATENDIDO", LocalDate.of(2026, 3, 15).atStartOfDay(), 1),
        consulta(1, "ATENDIDO", desde.atStartOfDay(), 1),
        consulta(40, "ATENDIDO", hasta.plusDays(1).atStartOfDay(), 1)));
    ReporteConsultaFiltroRequest filtro = ReporteConsultaFiltroRequest.builder()
        .alcance(ReporteConsultaAlcance.RANGO_FECHAS).fechaDesde(desde).fechaHasta(hasta).build();

    ReporteMedicoDocumento documento = service.seleccionarConsultasPaciente(8, filtro);

    assertEquals(3, documento.getTotalConsultasEncontradas());
    assertEquals(List.of(1, 15, 31), ids(documento));
  }

  @Test
  void rechazaRangoInvertido() {
    ReporteConsultaFiltroRequest filtro = ReporteConsultaFiltroRequest.builder()
        .alcance(ReporteConsultaAlcance.RANGO_FECHAS)
        .fechaDesde(LocalDate.of(2026, 4, 2)).fechaHasta(LocalDate.of(2026, 4, 1)).build();

    ConsultaReporteException error = assertThrows(ConsultaReporteException.class,
        () -> service.seleccionarConsultasPaciente(8, filtro));

    assertEquals("RANGO_INVALIDO", error.getCodigo());
  }

  @Test
  void desempataMismaFechaPorIdConsulta() {
    LocalDateTime mismaFecha = LocalDateTime.of(2026, 7, 1, 12, 0);
    prepararPaciente(List.of(consulta(9, "ATENDIDO", mismaFecha, 1), consulta(3, "ATENDIDO", mismaFecha, 1)));

    ReporteMedicoDocumento todas = service.seleccionarConsultasPaciente(8, filtro(ReporteConsultaAlcance.TODAS));
    ReporteMedicoDocumento ultima = service.seleccionarConsultasPaciente(8, filtro(ReporteConsultaAlcance.ULTIMA));

    assertEquals(List.of(3, 9), ids(todas));
    assertEquals(List.of(9), ids(ultima));
  }

  @Test
  void conservaHistoriasDeCadaConsultaSinDuplicarListaDeCabecera() {
    prepararPaciente(List.of(
        consulta(1, "ATENDIDO", fecha(1), 22),
        consulta(2, "ATENDIDO", fecha(2), 11),
        consulta(3, "ATENDIDO", fecha(3), 22)));

    ReporteMedicoDocumento documento = service.seleccionarConsultasPaciente(8, filtro(ReporteConsultaAlcance.TODAS));

    assertEquals(List.of(22, 11, 22), documento.getConsultas().stream().map(c -> c.getIdHistoriaClinica()).toList());
    assertEquals(List.of(11, 22), documento.getIdsHistoriasClinicasIncluidas());
  }

  @Test
  void aplicaFallbackDeFechaAtencionConsultaYCreacion() {
    Consulta conAtencion = consulta(1, "ATENDIDO", LocalDateTime.of(2026, 1, 3, 15, 0), 1);
    conAtencion.setFechaConsulta(java.sql.Date.valueOf(LocalDate.of(2026, 1, 2)));
    conAtencion.setFechaCreacion(LocalDateTime.of(2026, 1, 1, 8, 0));
    Consulta conFechaConsulta = consulta(2, "ATENDIDO", null, 1);
    conFechaConsulta.setFechaConsulta(java.sql.Date.valueOf(LocalDate.of(2026, 2, 2)));
    conFechaConsulta.setFechaCreacion(LocalDateTime.of(2026, 2, 1, 8, 0));
    Consulta conCreacion = consulta(3, "ATENDIDO", null, 1);
    conCreacion.setFechaCreacion(LocalDateTime.of(2026, 3, 3, 9, 0));
    prepararPaciente(List.of(conCreacion, conFechaConsulta, conAtencion));

    ReporteMedicoDocumento documento = service.seleccionarConsultasPaciente(8, filtro(ReporteConsultaAlcance.TODAS));

    assertEquals(LocalDateTime.of(2026, 1, 3, 15, 0), documento.getConsultas().get(0).getFechaEfectiva());
    assertEquals(LocalDateTime.of(2026, 2, 2, 0, 0), documento.getConsultas().get(1).getFechaEfectiva());
    assertEquals(LocalDateTime.of(2026, 3, 3, 9, 0), documento.getConsultas().get(2).getFechaEfectiva());
    assertEquals(List.of(OrigenFechaConsultaReporte.FECHA_ATENCION,
            OrigenFechaConsultaReporte.FECHA_CONSULTA, OrigenFechaConsultaReporte.FECHA_CREACION),
        documento.getConsultas().stream().map(c -> c.getOrigenFechaEfectiva()).toList());
  }

  @Test
  void calculaEdadEnLaFechaEfectiva() {
    Consulta antesDelCumpleanios = consulta(1, "ATENDIDO", LocalDateTime.of(2026, 8, 19, 10, 0), 1);
    Consulta enCumpleanios = consulta(2, "ATENDIDO", LocalDateTime.of(2026, 8, 20, 10, 0), 1);
    prepararPaciente(List.of(antesDelCumpleanios, enCumpleanios));

    ReporteMedicoDocumento documento = service.seleccionarConsultasPaciente(8, filtro(ReporteConsultaAlcance.TODAS));

    assertEquals(35, documento.getConsultas().get(0).getEdadPaciente());
    assertEquals(36, documento.getConsultas().get(1).getEdadPaciente());
  }

  @Test
  void toleraPacienteDoctorHistoriaYCamposOpcionalesIncompletos() {
    paciente.setNombres("   "); paciente.setApellidos(null); paciente.setNumDocumento(" "); paciente.setFechaNacimiento(null);
    Consulta incompleta = consulta(1, "ATENDIDO", null, 1);
    incompleta.setHistoriaClinica(null); incompleta.setDoctorResponsable(null); incompleta.setEspecialidadRequerida(" ");
    incompleta.setDiagnostico(null); incompleta.setExamenesRecetados(" "); incompleta.setReceta(null);
    incompleta.setTratamiento(""); incompleta.setProximaCita(null);
    prepararPaciente(List.of(incompleta));

    ReporteMedicoDocumento documento = assertDoesNotThrow(
        () -> service.seleccionarConsultasPaciente(8, filtro(ReporteConsultaAlcance.TODAS)));

    assertNull(documento.getPaciente().getNombreCompleto());
    assertNull(documento.getConsultas().getFirst().getEdadPaciente());
    assertNull(documento.getConsultas().getFirst().getMedicoResponsable());
    assertTrue(documento.getIdsHistoriasClinicasIncluidas().isEmpty());
  }

  private void prepararPaciente(List<Consulta> consultas) {
    when(pacienteRepository.findById(8)).thenReturn(Optional.of(paciente));
    when(consultaRepository.findByPacienteIdForReporte(8)).thenReturn(consultas);
  }

  private ReporteConsultaFiltroRequest filtro(ReporteConsultaAlcance alcance) {
    return ReporteConsultaFiltroRequest.builder().alcance(alcance).build();
  }

  private List<Integer> ids(ReporteMedicoDocumento documento) {
    return documento.getConsultas().stream().map(c -> c.getIdConsulta()).toList();
  }

  private LocalDateTime fecha(int dia) {
    return LocalDateTime.of(2026, 1, dia, 9, 0);
  }

  private Paciente paciente(int id, LocalDate nacimiento) {
    Paciente resultado = new Paciente(); resultado.setIdPaciente(id); resultado.setNombres("Ana María");
    resultado.setApellidos("Peña Ríos"); resultado.setNumDocumento("12345678");
    resultado.setFechaNacimiento(java.sql.Date.valueOf(nacimiento)); return resultado;
  }

  private Consulta consulta(int id, String estado, LocalDateTime fechaAtencion, int idHistoria) {
    Consulta resultado = new Consulta(); resultado.setIdConsulta(id); resultado.setPaciente(paciente);
    resultado.setEstado(estado); resultado.setFechaAtencion(fechaAtencion);
    resultado.setFechaCreacion(fechaAtencion == null ? LocalDateTime.of(2026, 1, 1, 7, 0) : fechaAtencion.minusHours(1));
    HistoriaClinica historia = new HistoriaClinica(); historia.setIdHistoriaClinica(idHistoria); historia.setPaciente(paciente);
    resultado.setHistoriaClinica(historia); resultado.setEspecialidadRequerida("MEDICINA_GENERAL");
    Empleado doctor = new Empleado(); doctor.setNombres("José"); doctor.setApellidos("Núñez"); resultado.setDoctorResponsable(doctor);
    resultado.setDiagnostico("Diagnóstico"); resultado.setExamenesRecetados("Exámenes");
    resultado.setReceta("Receta"); resultado.setTratamiento("Tratamiento");
    resultado.setProximaCita(Date.from(LocalDate.of(2026, 12, 1).atStartOfDay(LIMA).toInstant()));
    return resultado;
  }
}
