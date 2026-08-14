package com.krivi.apihistorialmedico.business.services;

import com.krivi.apihistorialmedico.model.api.FusionarHistoriasClinicasRequest;
import com.krivi.apihistorialmedico.model.api.FusionarHistoriasClinicasResponse;

public interface FusionHistoriaClinicaService {
  FusionarHistoriasClinicasResponse fusionar(Integer idUsuario, Integer idHistoriaSecundaria, FusionarHistoriasClinicasRequest request);
}
