package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.exception.BusquedaHistoriaClinicaException;
import com.krivi.apihistorialmedico.business.services.HistoriaClinicaService;
import com.krivi.apihistorialmedico.model.api.ApiErrorResponse;
import com.krivi.apihistorialmedico.model.api.BusquedaHistoriasClinicasResponse;
import com.krivi.apihistorialmedico.model.api.DuplicadosHistoriasClinicasResponse;
import com.krivi.apihistorialmedico.model.api.EstadisticasHistoriasClinicasResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/historias-clinicas")
@Slf4j
public class HistoriaClinicaBusquedaController {
  private final HistoriaClinicaService historiaClinicaService;

  public HistoriaClinicaBusquedaController(HistoriaClinicaService historiaClinicaService) {
    this.historiaClinicaService = historiaClinicaService;
  }

  @GetMapping("/buscar")
  public ResponseEntity<BusquedaHistoriasClinicasResponse> buscar(@RequestParam(required = false) String criterio) {
    return ResponseEntity.ok(historiaClinicaService.buscarParaIntegracion(criterio));
  }

  @GetMapping("/estadisticas")
  public ResponseEntity<EstadisticasHistoriasClinicasResponse> estadisticas() {
    return ResponseEntity.ok(historiaClinicaService.obtenerEstadisticasParaIntegracion());
  }

  @GetMapping("/duplicados")
  public ResponseEntity<DuplicadosHistoriasClinicasResponse> duplicados() {
    return ResponseEntity.ok(historiaClinicaService.obtenerDuplicadosParaIntegracion());
  }

  @ExceptionHandler(BusquedaHistoriaClinicaException.class)
  public ResponseEntity<ApiErrorResponse> handleBusquedaHistoriaClinicaException(BusquedaHistoriaClinicaException exception) {
    return ResponseEntity.status(exception.getStatus())
        .body(ApiErrorResponse.builder().codigo(exception.getCodigo()).mensaje(exception.getMessage()).build());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception exception) {
    log.error("HistoriaClinicaBusquedaController: error inesperado", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiErrorResponse.builder().codigo("ERROR_INTERNO").mensaje("No se pudo procesar la solicitud de historias clínicas.").build());
  }
}
