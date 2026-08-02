package com.krivi.apihistorialmedico.business.services;

import com.krivi.apihistorialmedico.model.api.PacienteDuplicadoComparacionResponse;

public interface PacienteDuplicadoService {
  PacienteDuplicadoComparacionResponse compararPorDni(String dni);
}
