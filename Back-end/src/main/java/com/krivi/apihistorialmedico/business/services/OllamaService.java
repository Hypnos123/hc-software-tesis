package com.krivi.apihistorialmedico.business.services;

import com.krivi.apihistorialmedico.model.api.OllamaInterpretacionResponse;

public interface OllamaService {
  OllamaInterpretacionResponse interpretar(String mensaje);
}
