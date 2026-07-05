package com.krivi.apihistorialmedico.business.services;

import com.krivi.apihistorialmedico.model.api.ConsultaDniResponse;

public interface DniConsultaService {
  ConsultaDniResponse consultar(String dni);
}
