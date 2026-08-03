package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.exception.PacienteDuplicadoException;
import com.krivi.apihistorialmedico.business.services.PacienteDuplicadoService;
import com.krivi.apihistorialmedico.model.api.ApiErrorResponse;
import com.krivi.apihistorialmedico.model.api.PacienteDuplicadoComparacionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteDuplicadoController {
  private final PacienteDuplicadoService pacienteDuplicadoService;

  public PacienteDuplicadoController(PacienteDuplicadoService pacienteDuplicadoService) {
    this.pacienteDuplicadoService = pacienteDuplicadoService;
  }

  @GetMapping(value = "/duplicados", params = "dni")
  public ResponseEntity<PacienteDuplicadoComparacionResponse> comparar(
      @RequestParam(required = false) String dni) {
    return ResponseEntity.ok(pacienteDuplicadoService.compararPorDni(dni));
  }

  @ExceptionHandler(PacienteDuplicadoException.class)
  public ResponseEntity<ApiErrorResponse> manejarPacienteDuplicadoException(PacienteDuplicadoException exception) {
    return ResponseEntity.status(exception.getStatus())
        .body(ApiErrorResponse.builder().codigo(exception.getCodigo()).mensaje(exception.getMessage()).build());
  }
}
