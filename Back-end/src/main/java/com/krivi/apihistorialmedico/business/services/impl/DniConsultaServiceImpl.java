package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.exception.DniConsultaException;
import com.krivi.apihistorialmedico.business.services.DniConsultaService;
import com.krivi.apihistorialmedico.integration.dni.DniProviderClient;
import com.krivi.apihistorialmedico.model.api.ConsultaDniResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class DniConsultaServiceImpl implements DniConsultaService {
  private final DniProviderClient dniProviderClient;

  public DniConsultaServiceImpl(DniProviderClient dniProviderClient) {
    this.dniProviderClient = dniProviderClient;
  }

  @Override
  public ConsultaDniResponse consultar(String dni) {
    if (dni == null || !dni.matches("\\d{8}")) {
      throw new DniConsultaException(
          "DNI_INVALIDO",
          "El DNI debe contener exactamente 8 dígitos.",
          HttpStatus.BAD_REQUEST
      );
    }

    return dniProviderClient.consultar(dni);
  }
}
