package com.krivi.apihistorialmedico.business.exception;

public class ReporteMedicoGeneracionException extends RuntimeException {
  public ReporteMedicoGeneracionException(String mensaje, Throwable causa) {
    super(mensaje, causa);
  }
}
