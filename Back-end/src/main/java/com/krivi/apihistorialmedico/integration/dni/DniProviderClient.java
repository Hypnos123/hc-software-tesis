package com.krivi.apihistorialmedico.integration.dni;

import com.krivi.apihistorialmedico.model.api.ConsultaDniResponse;

public interface DniProviderClient {
  ConsultaDniResponse consultar(String dni);
}
