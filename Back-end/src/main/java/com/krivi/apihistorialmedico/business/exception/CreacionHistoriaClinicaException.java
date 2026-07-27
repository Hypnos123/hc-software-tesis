package com.krivi.apihistorialmedico.business.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CreacionHistoriaClinicaException extends RuntimeException {
  private final String codigo;
  private final HttpStatus status;

  public CreacionHistoriaClinicaException(String codigo, String mensaje, HttpStatus status) {
    super(mensaje);
    this.codigo = codigo;
    this.status = status;
  }
}
