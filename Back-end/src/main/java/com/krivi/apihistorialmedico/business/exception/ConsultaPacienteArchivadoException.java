package com.krivi.apihistorialmedico.business.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ConsultaPacienteArchivadoException extends RuntimeException {
  private final String resultado;
  private final HttpStatus status;

  public ConsultaPacienteArchivadoException(String resultado, String mensaje, HttpStatus status) {
    super(mensaje);
    this.resultado = resultado;
    this.status = status;
  }
}
