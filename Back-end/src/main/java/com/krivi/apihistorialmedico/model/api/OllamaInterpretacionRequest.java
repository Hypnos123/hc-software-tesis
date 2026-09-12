package com.krivi.apihistorialmedico.model.api;

import jakarta.validation.constraints.NotBlank;

public record OllamaInterpretacionRequest(
    @NotBlank(message = "El mensaje es obligatorio.") String mensaje
) {
}
