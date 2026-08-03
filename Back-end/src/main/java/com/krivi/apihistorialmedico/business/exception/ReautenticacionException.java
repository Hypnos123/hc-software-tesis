package com.krivi.apihistorialmedico.business.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ReautenticacionException extends RuntimeException {
  private final String resultado;
  private final String cargo;
  private final HttpStatus status;

  public ReautenticacionException(String resultado, String mensaje, String cargo, HttpStatus status) {
    super(mensaje);
    this.resultado = resultado;
    this.cargo = cargo;
    this.status = status;
  }
}
