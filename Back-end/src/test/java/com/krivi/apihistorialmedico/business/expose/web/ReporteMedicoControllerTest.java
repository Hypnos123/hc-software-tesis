package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.exception.ConsultaReporteException;
import com.krivi.apihistorialmedico.business.services.ConsultaReporteQueryService;
import com.krivi.apihistorialmedico.business.services.ReporteMedicoService;
import com.krivi.apihistorialmedico.model.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReporteMedicoControllerTest {
  private ConsultaReporteQueryService consultaService;
  private ReporteMedicoService pdfService;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    consultaService = mock(ConsultaReporteQueryService.class);
    pdfService = mock(ReporteMedicoService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new ReporteMedicoController(consultaService, pdfService)).build();
  }

  @Test
  void devuelveEvaluacionIndividualInlineComoPdf() throws Exception {
    ReporteMedicoDocumento documento = documento(7, ReporteConsultaAlcance.ULTIMA, 1, 1);
    byte[] pdf = "%PDF-evaluacion".getBytes(StandardCharsets.US_ASCII);
    when(consultaService.seleccionarConsultaIndividual(31)).thenReturn(documento);
    when(pdfService.generarEvaluacionMedica(documento)).thenReturn(pdf);

    mockMvc.perform(get("/api/reportes-medicos/consultas/31/pdf"))
        .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_PDF))
        .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("inline")))
        .andExpect(header().string("Content-Disposition",
            org.hamcrest.Matchers.containsString("evaluacion-medica-consulta-31.pdf")))
        .andExpect(content().bytes(pdf));
  }

  @Test
  void consultaInexistenteDevuelve404Controlado() throws Exception {
    when(consultaService.seleccionarConsultaIndividual(999)).thenThrow(
        new ConsultaReporteException("CONSULTA_INEXISTENTE", "La consulta indicada no existe."));

    mockMvc.perform(get("/api/reportes-medicos/consultas/999/pdf"))
        .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("CONSULTA_INEXISTENTE"));
    verifyNoInteractions(pdfService);
  }

  @Test
  void consultaPendienteDevuelve422Controlado() throws Exception {
    when(consultaService.seleccionarConsultaIndividual(12)).thenThrow(
        new ConsultaReporteException("CONSULTA_NO_ATENDIDA", "Solo se permiten consultas atendidas."));

    mockMvc.perform(get("/api/reportes-medicos/consultas/12/pdf"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.codigo").value("CONSULTA_NO_ATENDIDA"));
    verifyNoInteractions(pdfService);
  }

  @Test
  void seleccionTodasInformaCasoSeisCuatroDos() throws Exception {
    ReporteMedicoDocumento documento = documento(10, ReporteConsultaAlcance.TODAS, 6, 4);
    when(consultaService.seleccionarConsultasPaciente(eq(10), any())).thenReturn(documento);

    mockMvc.perform(post("/api/reportes-medicos/pacientes/10/consultas/seleccion")
            .contentType(MediaType.APPLICATION_JSON).content("{\"alcance\":\"TODAS\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.idPaciente").value(10))
        .andExpect(jsonPath("$.paciente").value("Ana Pérez Gómez"))
        .andExpect(jsonPath("$.totalConsultasEncontradas").value(6))
        .andExpect(jsonPath("$.consultasAtendidasIncluidas").value(4))
        .andExpect(jsonPath("$.consultasNoAtendidasExcluidas").value(2))
        .andExpect(jsonPath("$.idsHistoriasClinicasIncluidas[0]").value(11))
        .andExpect(jsonPath("$.puedeGenerar").value(true))
        .andExpect(jsonPath("$.mensaje").value(
            "Se encontraron 6 consultas. El reporte incluirá 4 consultas atendidas."));
  }

  @Test
  void pacienteInexistenteDevuelve404Controlado() throws Exception {
    when(consultaService.seleccionarConsultasPaciente(eq(404), any())).thenThrow(
        new ConsultaReporteException("PACIENTE_INEXISTENTE", "El paciente indicado no existe."));

    mockMvc.perform(post("/api/reportes-medicos/pacientes/404/consultas/seleccion")
            .contentType(MediaType.APPLICATION_JSON).content("{\"alcance\":\"TODAS\"}"))
        .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("PACIENTE_INEXISTENTE"));
  }

  @Test
  void fechaFaltanteDevuelve400Controlado() throws Exception {
    when(consultaService.seleccionarConsultasPaciente(eq(10), any())).thenThrow(
        new ConsultaReporteException("FECHA_REQUERIDA", "Debe indicar la fecha del reporte."));

    mockMvc.perform(post("/api/reportes-medicos/pacientes/10/consultas/seleccion")
            .contentType(MediaType.APPLICATION_JSON).content("{\"alcance\":\"FECHA\"}"))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("FECHA_REQUERIDA"));
  }

  @Test
  void seleccionFechaDeserializaFiltroAprobado() throws Exception {
    ReporteMedicoDocumento documento = documento(10, ReporteConsultaAlcance.FECHA, 1, 1);
    documento.setFecha(LocalDate.of(2026, 8, 15));
    when(consultaService.seleccionarConsultasPaciente(eq(10), any())).thenReturn(documento);

    mockMvc.perform(post("/api/reportes-medicos/pacientes/10/consultas/seleccion")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"alcance\":\"FECHA\",\"fecha\":\"2026-08-15\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.alcance").value("FECHA"))
        .andExpect(jsonPath("$.fecha").value("2026-08-15"));

    ArgumentCaptor<ReporteConsultaFiltroRequest> captor = ArgumentCaptor.forClass(ReporteConsultaFiltroRequest.class);
    verify(consultaService).seleccionarConsultasPaciente(eq(10), captor.capture());
    assertEquals(LocalDate.of(2026, 8, 15), captor.getValue().getFecha());
  }

  @Test
  void seleccionRangoDeserializaAmbosExtremos() throws Exception {
    ReporteMedicoDocumento documento = documento(10, ReporteConsultaAlcance.RANGO_FECHAS, 3, 2);
    documento.setFechaDesde(LocalDate.of(2026, 8, 1)); documento.setFechaHasta(LocalDate.of(2026, 8, 31));
    when(consultaService.seleccionarConsultasPaciente(eq(10), any())).thenReturn(documento);

    mockMvc.perform(post("/api/reportes-medicos/pacientes/10/consultas/seleccion")
            .contentType(MediaType.APPLICATION_JSON).content("""
                {"alcance":"RANGO_FECHAS","fechaDesde":"2026-08-01","fechaHasta":"2026-08-31"}
                """))
        .andExpect(status().isOk()).andExpect(jsonPath("$.fechaDesde").value("2026-08-01"))
        .andExpect(jsonPath("$.fechaHasta").value("2026-08-31"));
  }

  @Test
  void rangoInvertidoDevuelve400() throws Exception {
    when(consultaService.seleccionarConsultasPaciente(eq(10), any())).thenThrow(
        new ConsultaReporteException("RANGO_INVALIDO", "La fecha inicial no puede ser posterior a la final."));

    mockMvc.perform(post("/api/reportes-medicos/pacientes/10/consultas/seleccion")
            .contentType(MediaType.APPLICATION_JSON).content("""
                {"alcance":"RANGO_FECHAS","fechaDesde":"2026-08-31","fechaHasta":"2026-08-01"}
                """))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("RANGO_INVALIDO"));
  }

  @Test
  void seleccionSinConsultasPermanece200YNoPuedeGenerar() throws Exception {
    when(consultaService.seleccionarConsultasPaciente(eq(10), any()))
        .thenReturn(documento(10, ReporteConsultaAlcance.TODAS, 0, 0));

    mockMvc.perform(post("/api/reportes-medicos/pacientes/10/consultas/seleccion")
            .contentType(MediaType.APPLICATION_JSON).content("{\"alcance\":\"TODAS\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.totalConsultasEncontradas").value(0))
        .andExpect(jsonPath("$.puedeGenerar").value(false))
        .andExpect(jsonPath("$.mensaje").value("No se encontraron consultas para el criterio indicado."));
  }

  @Test
  void existentesSinAtendidasNoGeneranPdfVacio() throws Exception {
    when(consultaService.seleccionarConsultasPaciente(eq(10), any()))
        .thenReturn(documento(10, ReporteConsultaAlcance.TODAS, 2, 0));

    mockMvc.perform(post("/api/reportes-medicos/pacientes/10/consultas/pdf")
            .contentType(MediaType.APPLICATION_JSON).content("{\"alcance\":\"TODAS\"}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.codigo").value("REPORTE_SIN_CONSULTAS_ATENDIDAS"));
    verifyNoInteractions(pdfService);
  }

  @Test
  void consolidadoDevuelveUnicoPdfInlineYNombreParaTodas() throws Exception {
    ReporteMedicoDocumento documento = documento(10, ReporteConsultaAlcance.TODAS, 4, 4);
    byte[] pdf = "%PDF-consolidado".getBytes(StandardCharsets.US_ASCII);
    when(consultaService.seleccionarConsultasPaciente(eq(10), any())).thenReturn(documento);
    when(pdfService.generarReporteConsultas(documento)).thenReturn(pdf);

    mockMvc.perform(post("/api/reportes-medicos/pacientes/10/consultas/pdf")
            .contentType(MediaType.APPLICATION_JSON).content("{\"alcance\":\"TODAS\"}"))
        .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_PDF))
        .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("inline")))
        .andExpect(header().string("Content-Disposition",
            org.hamcrest.Matchers.containsString("reporte-consultas-78451268.pdf")))
        .andExpect(content().bytes(pdf));
    verify(pdfService, times(1)).generarReporteConsultas(documento);
  }

  @Test
  void nombresDeArchivoRespetanFechaRangoUltimaYFallbackPaciente() throws Exception {
    assertNombreArchivo(ReporteConsultaAlcance.FECHA, "{\"alcance\":\"FECHA\",\"fecha\":\"2026-08-15\"}",
        "reporte-consultas-78451268-20260815.pdf", LocalDate.of(2026, 8, 15), null, null, "78451268");
    assertNombreArchivo(ReporteConsultaAlcance.RANGO_FECHAS,
        "{\"alcance\":\"RANGO_FECHAS\",\"fechaDesde\":\"2026-08-01\",\"fechaHasta\":\"2026-08-31\"}",
        "reporte-consultas-78451268-20260801-20260831.pdf", null,
        LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), "78451268");
    assertNombreArchivo(ReporteConsultaAlcance.ULTIMA, "{\"alcance\":\"ULTIMA\"}",
        "ultima-consulta-10.pdf", null, null, null, null);
  }

  private void assertNombreArchivo(ReporteConsultaAlcance alcance, String request, String nombre,
      LocalDate fecha, LocalDate desde, LocalDate hasta, String dni) throws Exception {
    reset(consultaService, pdfService);
    ReporteMedicoDocumento documento = documento(10, alcance, 1, 1);
    documento.setFecha(fecha); documento.setFechaDesde(desde); documento.setFechaHasta(hasta);
    documento.getPaciente().setDni(dni);
    when(consultaService.seleccionarConsultasPaciente(eq(10), any())).thenReturn(documento);
    when(pdfService.generarReporteConsultas(documento)).thenReturn("%PDF".getBytes(StandardCharsets.US_ASCII));
    mockMvc.perform(post("/api/reportes-medicos/pacientes/10/consultas/pdf")
            .contentType(MediaType.APPLICATION_JSON).content(request))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(nombre)));
  }

  private ReporteMedicoDocumento documento(int idPaciente, ReporteConsultaAlcance alcance, long total, long incluidas) {
    List<ReporteMedicoConsulta> consultas = incluidas == 0 ? List.of()
        : java.util.stream.LongStream.rangeClosed(1, incluidas).mapToObj(id -> ReporteMedicoConsulta.builder()
            .idConsulta((int) id).idHistoriaClinica(11).build()).toList();
    return ReporteMedicoDocumento.builder().alcance(alcance).totalConsultasEncontradas(total)
        .consultasAtendidasIncluidas(incluidas).consultasNoAtendidasExcluidas(total - incluidas)
        .paciente(ReporteMedicoPaciente.builder().idPaciente(idPaciente).nombreCompleto("Ana Pérez Gómez")
            .dni("78451268").build())
        .idsHistoriasClinicasIncluidas(incluidas == 0 ? List.of() : List.of(11)).consultas(consultas).build();
  }
}
