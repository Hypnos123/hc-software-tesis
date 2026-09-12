package com.krivi.apihistorialmedico.model.api;

public record OllamaEjecucionResponse(
    String categoria,
    String intencion,
    Boolean encontrado,
    String dni,
    String mensaje
) {
}
