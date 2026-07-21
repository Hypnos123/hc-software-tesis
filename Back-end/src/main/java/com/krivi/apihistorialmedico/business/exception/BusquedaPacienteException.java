package com.krivi.apihistorialmedico.business.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusquedaPacienteException extends RuntimeException {
  private final String codigo;
  private final HttpStatus status;

  public BusquedaPacienteException(String codigo, String mensaje, HttpStatus status) {
    super(mensaje);
    this.codigo = codigo;
    this.status = status;
  }
}
