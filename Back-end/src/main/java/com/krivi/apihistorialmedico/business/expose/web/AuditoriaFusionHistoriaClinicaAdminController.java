package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.exception.ConsultaFusionAuditoriaException;
import com.krivi.apihistorialmedico.business.exception.ReautenticacionException;
import com.krivi.apihistorialmedico.business.services.AuditoriaFusionHistoriaClinicaAdminService;
import com.krivi.apihistorialmedico.model.api.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/auditoria/fusiones-historias-clinicas")
public class AuditoriaFusionHistoriaClinicaAdminController {
  private final AuditoriaFusionHistoriaClinicaAdminService service;

  public AuditoriaFusionHistoriaClinicaAdminController(AuditoriaFusionHistoriaClinicaAdminService service) { this.service = service; }

  @GetMapping
  public ResponseEntity<PaginaResponse<FusionHistoriaClinicaAuditoriaResumenResponse>> listar(
      @RequestHeader(value = "X-Usuario-Id", required = false) Integer idUsuarioActual,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String sort, @RequestParam(required = false) String search,
      @RequestParam(required = false) String dni, @RequestParam(required = false) Integer idPaciente,
      @RequestParam(required = false) Integer idHistoriaPrincipal,
      @RequestParam(required = false) Integer idHistoriaEliminada,
      @RequestParam(required = false) Integer idUsuario, @RequestParam(required = false) String resultado,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
    return ResponseEntity.ok(service.listar(idUsuarioActual, page, size, sort, search, dni, idPaciente,
        idHistoriaPrincipal, idHistoriaEliminada, idUsuario, resultado, desde, hasta));
  }

  @GetMapping("/{idAuditoria}")
  public ResponseEntity<FusionHistoriaClinicaAuditoriaDetalleResponse> detalle(
      @RequestHeader(value = "X-Usuario-Id", required = false) Integer idUsuarioActual,
      @PathVariable Integer idAuditoria) {
    return ResponseEntity.ok(service.obtenerDetalle(idUsuarioActual, idAuditoria));
  }

  @ExceptionHandler(ConsultaFusionAuditoriaException.class)
  public ResponseEntity<Map<String, String>> consultaError(ConsultaFusionAuditoriaException e) {
    return ResponseEntity.status(e.getStatus()).body(Map.of("resultado", e.getResultado(), "mensaje", e.getMessage()));
  }

  @ExceptionHandler(ReautenticacionException.class)
  public ResponseEntity<Map<String, String>> autorizacionError(ReautenticacionException e) {
    return ResponseEntity.status(e.getStatus()).body(Map.of("resultado", e.getResultado(), "mensaje", e.getMessage()));
  }
}
