package com.krivi.apihistorialmedico.model.api;

public record OllamaInterpretacionResponse(
    String categoria,
    String intencion,
    String dni,
    String nombre,
    Integer limite
) {
  public OllamaInterpretacionResponse(String categoria, String intencion, String dni, String nombre) {
    this(categoria, intencion, dni, nombre, null);
  }
}
