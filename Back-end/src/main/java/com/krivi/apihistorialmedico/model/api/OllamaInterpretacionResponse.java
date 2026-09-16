package com.krivi.apihistorialmedico.model.api;

public record OllamaInterpretacionResponse(
    String categoria,
    String intencion,
    String dni,
    String nombre,
    Integer limite,
    String fechaInicio,
    String fechaFin
) {
  public OllamaInterpretacionResponse(String categoria, String intencion, String dni, String nombre) {
    this(categoria, intencion, dni, nombre, null, null, null);
  }

  public OllamaInterpretacionResponse(
      String categoria, String intencion, String dni, String nombre, Integer limite
  ) {
    this(categoria, intencion, dni, nombre, limite, null, null);
  }
}
