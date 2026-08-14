package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.exception.BusquedaHistoriaClinicaException;
import com.krivi.apihistorialmedico.business.services.AnalisisHistoriasClinicasDuplicadasService;
import com.krivi.apihistorialmedico.model.api.AnalisisHistoriasClinicasDuplicadasResponse;
import com.krivi.apihistorialmedico.model.api.AnalizarHistoriasClinicasDuplicadasRequest;
import com.krivi.apihistorialmedico.model.api.ApiErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/historias-clinicas/duplicados")
public class AnalisisHistoriasClinicasDuplicadasController {
  private final AnalisisHistoriasClinicasDuplicadasService service;

  public AnalisisHistoriasClinicasDuplicadasController(AnalisisHistoriasClinicasDuplicadasService service) {
    this.service = service;
  }

  @PostMapping("/analizar")
  public ResponseEntity<AnalisisHistoriasClinicasDuplicadasResponse> analizar(
      @RequestBody(required = false) AnalizarHistoriasClinicasDuplicadasRequest request) {
    return ResponseEntity.ok(service.analizar(request == null ? null : request.getIdsHistoriasClinicas()));
  }

  @ExceptionHandler(BusquedaHistoriaClinicaException.class)
  public ResponseEntity<ApiErrorResponse> manejarError(BusquedaHistoriaClinicaException exception) {
    return ResponseEntity.status(exception.getStatus())
        .body(ApiErrorResponse.builder().codigo(exception.getCodigo()).mensaje(exception.getMessage()).build());
  }
}
