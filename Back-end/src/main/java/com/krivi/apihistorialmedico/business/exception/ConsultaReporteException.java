package com.krivi.apihistorialmedico.business.exception;

import lombok.Getter;

@Getter
public class ConsultaReporteException extends RuntimeException {
  private final String codigo;

  public ConsultaReporteException(String codigo, String mensaje) {
    super(mensaje);
    this.codigo = codigo;
  }
}
