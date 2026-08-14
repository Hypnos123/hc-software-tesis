package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.exception.*;
import com.krivi.apihistorialmedico.business.services.FusionHistoriaClinicaService;
import com.krivi.apihistorialmedico.model.api.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/historias-clinicas")
public class FusionHistoriaClinicaController {
  private final FusionHistoriaClinicaService service;
  public FusionHistoriaClinicaController(FusionHistoriaClinicaService service) { this.service = service; }
  @PostMapping("/{idHistoriaSecundaria}/fusionar")
  public ResponseEntity<FusionarHistoriasClinicasResponse> fusionar(@RequestHeader(value="X-Usuario-Id", required=false) Integer idUsuario,
      @PathVariable Integer idHistoriaSecundaria, @RequestBody(required=false) FusionarHistoriasClinicasRequest request) {
    return ResponseEntity.ok(service.fusionar(idUsuario, idHistoriaSecundaria, request));
  }
  @ExceptionHandler(FusionHistoriaClinicaException.class)
  public ResponseEntity<FusionarHistoriasClinicasResponse> error(FusionHistoriaClinicaException e) {
    return ResponseEntity.status(e.getStatus()).body(FusionarHistoriasClinicasResponse.builder().fusionada(false).resultado(e.getResultado()).mensaje(e.getMessage()).build());
  }
  @ExceptionHandler(ReautenticacionException.class)
  public ResponseEntity<FusionarHistoriasClinicasResponse> reautenticacion(ReautenticacionException e) {
    return ResponseEntity.status(e.getStatus()).body(FusionarHistoriasClinicasResponse.builder().fusionada(false).resultado(e.getResultado()).mensaje(e.getMessage()).build());
  }
}
