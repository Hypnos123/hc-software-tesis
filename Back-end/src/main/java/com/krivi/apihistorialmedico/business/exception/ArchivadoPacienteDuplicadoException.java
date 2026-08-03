package com.krivi.apihistorialmedico.business.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ArchivadoPacienteDuplicadoException extends RuntimeException {
  private final String resultado;
  private final HttpStatus status;

  public ArchivadoPacienteDuplicadoException(String resultado, String mensaje, HttpStatus status) {
    super(mensaje);
    this.resultado = resultado;
    this.status = status;
  }

  public ArchivadoPacienteDuplicadoException(String resultado, String mensaje, HttpStatus status, Throwable cause) {
    super(mensaje, cause);
    this.resultado = resultado;
    this.status = status;
  }
}
