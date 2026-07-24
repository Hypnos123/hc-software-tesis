package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.exception.ConsultaMedicaIntegracionException;
import com.krivi.apihistorialmedico.business.services.ConsultaMedicaIntegracionService;
import com.krivi.apihistorialmedico.model.api.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/consultas-medicas")
@Slf4j
public class ConsultaMedicaIntegracionController {
  private final ConsultaMedicaIntegracionService consultaService;

  public ConsultaMedicaIntegracionController(ConsultaMedicaIntegracionService consultaService) { this.consultaService = consultaService; }

  @GetMapping("/buscar")
  public ResponseEntity<BusquedaConsultasMedicasResponse> buscar(@RequestParam(required = false) String criterio) {
    return ResponseEntity.ok(consultaService.buscar(criterio));
  }

  @GetMapping("/estadisticas")
  public ResponseEntity<EstadisticasConsultasMedicasResponse> estadisticas() { return ResponseEntity.ok(consultaService.obtenerEstadisticas()); }

  /** Devuelve las consultas pendientes, ordenadas de la más antigua a la más reciente. */
  @GetMapping("/pendientes")
  public ResponseEntity<ListadoConsultasMedicasResponse> pendientes() { return ResponseEntity.ok(consultaService.obtenerPendientes()); }

  @GetMapping("/ultimas")
  public ResponseEntity<ListadoConsultasMedicasResponse> ultimas(@RequestParam(required = false) Integer limite) {
    return ResponseEntity.ok(consultaService.obtenerUltimas(limite));
  }

  @ExceptionHandler(ConsultaMedicaIntegracionException.class)
  public ResponseEntity<ApiErrorResponse> handleConsultaMedicaIntegracionException(ConsultaMedicaIntegracionException exception) {
    return ResponseEntity.status(exception.getStatus()).body(ApiErrorResponse.builder().codigo(exception.getCodigo()).mensaje(exception.getMessage()).build());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception exception) {
    log.error("ConsultaMedicaIntegracionController: error inesperado", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiErrorResponse.builder().codigo("ERROR_INTERNO").mensaje("No se pudo procesar la solicitud de consultas médicas.").build());
  }
}
