package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.exception.DniConsultaException;
import com.krivi.apihistorialmedico.business.services.DniConsultaService;
import com.krivi.apihistorialmedico.model.api.ApiErrorResponse;
import com.krivi.apihistorialmedico.model.api.ConsultaDniResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ia")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET})
public class IaDniController {
  private final DniConsultaService dniConsultaService;

  public IaDniController(DniConsultaService dniConsultaService) {
    this.dniConsultaService = dniConsultaService;
  }

  @GetMapping("/consultar-dni/{dni}")
  public ResponseEntity<ConsultaDniResponse> consultarDni(@PathVariable String dni) {
    return ResponseEntity.ok(dniConsultaService.consultar(dni));
  }

  @ExceptionHandler(DniConsultaException.class)
  public ResponseEntity<ApiErrorResponse> handleDniConsultaException(DniConsultaException exception) {
    return ResponseEntity.status(exception.getStatus()).body(
        ApiErrorResponse.builder()
            .codigo(exception.getCodigo())
            .mensaje(exception.getMessage())
            .build()
    );
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleException() {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
        ApiErrorResponse.builder()
            .codigo("ERROR_INTERNO")
            .mensaje("No se pudo consultar el DNI.")
            .build()
    );
  }
}
