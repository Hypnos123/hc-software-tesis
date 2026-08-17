package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.exception.ConsultaPacienteArchivadoException;
import com.krivi.apihistorialmedico.business.exception.ReautenticacionException;
import com.krivi.apihistorialmedico.business.services.PacienteArchivadoAdminService;
import com.krivi.apihistorialmedico.model.api.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/pacientes-archivados")
public class PacienteArchivadoAdminController {
  private final PacienteArchivadoAdminService service;

  public PacienteArchivadoAdminController(PacienteArchivadoAdminService service) { this.service = service; }

  @GetMapping
  public ResponseEntity<PaginaResponse<PacienteArchivadoResumenResponse>> listar(
      @RequestHeader(value = "X-Usuario-Id", required = false) Integer idUsuario,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String sort, @RequestParam(required = false) String search,
      @RequestParam(required = false) String dni, @RequestParam(required = false) Integer idPaciente,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
    return ResponseEntity.ok(service.listar(idUsuario, page, size, sort, search, dni, idPaciente, desde, hasta));
  }

  @GetMapping("/{idPaciente}")
  public ResponseEntity<PacienteArchivadoDetalleResponse> detalle(
      @RequestHeader(value = "X-Usuario-Id", required = false) Integer idUsuario, @PathVariable Integer idPaciente) {
    return ResponseEntity.ok(service.obtenerDetalle(idUsuario, idPaciente));
  }

  @ExceptionHandler(ConsultaPacienteArchivadoException.class)
  public ResponseEntity<Map<String, String>> consultaError(ConsultaPacienteArchivadoException e) {
    return ResponseEntity.status(e.getStatus()).body(Map.of("resultado", e.getResultado(), "mensaje", e.getMessage()));
  }

  @ExceptionHandler(ReautenticacionException.class)
  public ResponseEntity<Map<String, String>> autorizacionError(ReautenticacionException e) {
    return ResponseEntity.status(e.getStatus()).body(Map.of("resultado", e.getResultado(), "mensaje", e.getMessage()));
  }
}
