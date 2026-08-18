package com.krivi.apihistorialmedico.business.exception;

import org.springframework.http.HttpStatus;

public class ResumenConsultasException extends ConsultaMedicaIntegracionException {
  public ResumenConsultasException(String resultado, String mensaje, HttpStatus status) {
    super(resultado, mensaje, status);
  }
}
