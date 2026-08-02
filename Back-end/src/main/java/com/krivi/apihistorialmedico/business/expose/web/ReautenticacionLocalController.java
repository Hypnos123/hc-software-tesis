package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.exception.ReautenticacionException;
import com.krivi.apihistorialmedico.business.services.ReautenticacionLocalService;
import com.krivi.apihistorialmedico.model.api.ReautenticacionRequest;
import com.krivi.apihistorialmedico.model.api.ReautenticacionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class ReautenticacionLocalController {
  private final ReautenticacionLocalService reautenticacionLocalService;

  public ReautenticacionLocalController(ReautenticacionLocalService reautenticacionLocalService) {
    this.reautenticacionLocalService = reautenticacionLocalService;
  }

  @PostMapping("/reautenticar")
  public ResponseEntity<ReautenticacionResponse> reautenticar(
      @RequestHeader(value = "X-Usuario-Id", required = false) Integer idUsuarioActual,
      @RequestBody(required = false) ReautenticacionRequest request
  ) {
    return ResponseEntity.ok(reautenticacionLocalService.reautenticar(idUsuarioActual, request));
  }

  @ExceptionHandler(ReautenticacionException.class)
  public ResponseEntity<ReautenticacionResponse> manejarReautenticacionException(ReautenticacionException exception) {
    return ResponseEntity.status(exception.getStatus()).body(ReautenticacionResponse.builder()
        .autorizado(false)
        .cargo(exception.getCargo())
        .puedeArchivarPacientes(false)
        .resultado(exception.getResultado())
        .mensaje(exception.getMessage())
        .build());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ReautenticacionResponse> manejarCuerpoInvalido() {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ReautenticacionResponse.builder()
        .autorizado(false)
        .puedeArchivarPacientes(false)
        .resultado("CUERPO_INVALIDO")
        .mensaje("El cuerpo de la solicitud no es válido.")
        .build());
  }
}
