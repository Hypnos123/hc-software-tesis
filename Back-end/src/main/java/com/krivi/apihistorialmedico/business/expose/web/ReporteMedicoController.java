package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.exception.ConsultaReporteException;
import com.krivi.apihistorialmedico.business.exception.ReporteMedicoGeneracionException;
import com.krivi.apihistorialmedico.business.services.ConsultaReporteQueryService;
import com.krivi.apihistorialmedico.business.services.ReporteMedicoService;
import com.krivi.apihistorialmedico.model.api.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@RestController
@RequestMapping("/api/reportes-medicos")
@Slf4j
public class ReporteMedicoController {
  private static final MediaType PDF = MediaType.APPLICATION_PDF;

  private final ConsultaReporteQueryService consultaReporteQueryService;
  private final ReporteMedicoService reporteMedicoService;

  public ReporteMedicoController(ConsultaReporteQueryService consultaReporteQueryService,
      ReporteMedicoService reporteMedicoService) {
    this.consultaReporteQueryService = consultaReporteQueryService;
    this.reporteMedicoService = reporteMedicoService;
  }

  @GetMapping(value = "/consultas/{idConsulta}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> evaluacionMedica(@PathVariable Integer idConsulta) {
    ReporteMedicoDocumento documento = consultaReporteQueryService.seleccionarConsultaIndividual(idConsulta);
    byte[] pdf = reporteMedicoService.generarEvaluacionMedica(documento);
    return respuestaPdf(pdf, "evaluacion-medica-consulta-" + idConsulta + ".pdf");
  }

  @PostMapping(value = "/pacientes/{idPaciente}/consultas/seleccion",
      consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ReporteConsultaSeleccionResponse> seleccionar(@PathVariable Integer idPaciente,
      @RequestBody ReporteConsultaFiltroRequest filtro) {
    ReporteMedicoDocumento documento = consultaReporteQueryService.seleccionarConsultasPaciente(idPaciente, filtro);
    return ResponseEntity.ok(aSeleccionResponse(documento));
  }

  @PostMapping(value = "/pacientes/{idPaciente}/consultas/pdf",
      consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> reporteConsolidado(@PathVariable Integer idPaciente,
      @RequestBody ReporteConsultaFiltroRequest filtro) {
    ReporteMedicoDocumento documento = consultaReporteQueryService.seleccionarConsultasPaciente(idPaciente, filtro);
    if (documento.getConsultasAtendidasIncluidas() == 0 || documento.getConsultas().isEmpty()) {
      throw new ConsultaReporteException("REPORTE_SIN_CONSULTAS_ATENDIDAS",
          "No existen consultas atendidas para generar el reporte con el criterio indicado.");
    }
    byte[] pdf = reporteMedicoService.generarReporteConsultas(documento);
    return respuestaPdf(pdf, construirNombreConsolidado(documento));
  }

  @ExceptionHandler(ConsultaReporteException.class)
  public ResponseEntity<ApiErrorResponse> manejarConsultaReporte(ConsultaReporteException exception) {
    return ResponseEntity.status(statusConsultaReporte(exception.getCodigo())).body(ApiErrorResponse.builder()
        .codigo(exception.getCodigo()).mensaje(exception.getMessage()).build());
  }

  @ExceptionHandler(ReporteMedicoGeneracionException.class)
  public ResponseEntity<ApiErrorResponse> manejarGeneracion(ReporteMedicoGeneracionException exception) {
    log.error("No se pudo generar el reporte médico", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiErrorResponse.builder()
        .codigo("ERROR_GENERACION_PDF").mensaje("No se pudo generar el reporte médico en PDF.").build());
  }

  private ReporteConsultaSeleccionResponse aSeleccionResponse(ReporteMedicoDocumento documento) {
    boolean puedeGenerar = documento.getConsultasAtendidasIncluidas() > 0;
    return ReporteConsultaSeleccionResponse.builder()
        .idPaciente(documento.getPaciente() == null ? null : documento.getPaciente().getIdPaciente())
        .paciente(documento.getPaciente() == null ? null : documento.getPaciente().getNombreCompleto())
        .alcance(documento.getAlcance()).fecha(documento.getFecha())
        .fechaDesde(documento.getFechaDesde()).fechaHasta(documento.getFechaHasta())
        .totalConsultasEncontradas(documento.getTotalConsultasEncontradas())
        .consultasAtendidasIncluidas(documento.getConsultasAtendidasIncluidas())
        .consultasNoAtendidasExcluidas(documento.getConsultasNoAtendidasExcluidas())
        .idsHistoriasClinicasIncluidas(documento.getIdsHistoriasClinicasIncluidas())
        .puedeGenerar(puedeGenerar).mensaje(mensajeSeleccion(documento)).build();
  }

  private String mensajeSeleccion(ReporteMedicoDocumento documento) {
    long total = documento.getTotalConsultasEncontradas();
    long incluidas = documento.getConsultasAtendidasIncluidas();
    if (total == 0) return "No se encontraron consultas para el criterio indicado.";
    if (incluidas == 0) {
      return "Se encontraron " + total + " consultas, pero ninguna está atendida para incluirla en el reporte.";
    }
    String consultasEncontradas = total == 1 ? "1 consulta" : total + " consultas";
    String consultasIncluidas = incluidas == 1 ? "1 consulta atendida" : incluidas + " consultas atendidas";
    return "Se encontraron " + consultasEncontradas + ". El reporte incluirá " + consultasIncluidas + ".";
  }

  private ResponseEntity<byte[]> respuestaPdf(byte[] pdf, String nombre) {
    ContentDisposition disposition = ContentDisposition.inline()
        .filename(sanitizarNombre(nombre), StandardCharsets.UTF_8).build();
    return ResponseEntity.ok().contentType(PDF).header(HttpHeaders.CONTENT_DISPOSITION,
        disposition.toString()).contentLength(pdf.length).body(pdf);
  }

  private String construirNombreConsolidado(ReporteMedicoDocumento documento) {
    String identificador = documento.getPaciente() == null ? null : documento.getPaciente().getDni();
    if (identificador == null || identificador.isBlank()) {
      identificador = documento.getPaciente() == null || documento.getPaciente().getIdPaciente() == null
          ? "paciente" : String.valueOf(documento.getPaciente().getIdPaciente());
    }
    String base = documento.getAlcance() == ReporteConsultaAlcance.ULTIMA
        ? "ultima-consulta-" + identificador
        : "reporte-consultas-" + identificador;
    if (documento.getAlcance() == ReporteConsultaAlcance.FECHA && documento.getFecha() != null) {
      base += "-" + fechaArchivo(documento.getFecha());
    } else if (documento.getAlcance() == ReporteConsultaAlcance.RANGO_FECHAS
        && documento.getFechaDesde() != null && documento.getFechaHasta() != null) {
      base += "-" + fechaArchivo(documento.getFechaDesde()) + "-" + fechaArchivo(documento.getFechaHasta());
    }
    return base + ".pdf";
  }

  private String fechaArchivo(java.time.LocalDate fecha) {
    return fecha.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
  }

  private String sanitizarNombre(String nombre) {
    String seguro = nombre == null ? "reporte-medico.pdf" : nombre.trim()
        .replaceAll("[^a-zA-Z0-9._-]", "-").replaceAll("-+", "-");
    return seguro.toLowerCase(Locale.ROOT);
  }

  private HttpStatus statusConsultaReporte(String codigo) {
    if (codigo == null) return HttpStatus.BAD_REQUEST;
    if (codigo.endsWith("_INEXISTENTE")) return HttpStatus.NOT_FOUND;
    if (codigo.equals("CONSULTA_NO_ATENDIDA") || codigo.equals("REPORTE_SIN_CONSULTAS_ATENDIDAS")) {
      return HttpStatus.UNPROCESSABLE_ENTITY;
    }
    return HttpStatus.BAD_REQUEST;
  }
}
