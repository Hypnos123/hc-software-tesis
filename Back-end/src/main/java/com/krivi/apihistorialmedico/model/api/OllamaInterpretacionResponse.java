package com.krivi.apihistorialmedico.model.api;

public record OllamaInterpretacionResponse(
    String categoria,
    String intencion,
    String dni,
    String nombre
) {
}
