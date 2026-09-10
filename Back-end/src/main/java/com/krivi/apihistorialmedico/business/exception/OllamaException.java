package com.krivi.apihistorialmedico.business.exception;

public class OllamaException extends RuntimeException {
  public OllamaException(String message, Throwable cause) {
    super(message, cause);
  }

  public OllamaException(String message) {
    super(message);
  }
}
