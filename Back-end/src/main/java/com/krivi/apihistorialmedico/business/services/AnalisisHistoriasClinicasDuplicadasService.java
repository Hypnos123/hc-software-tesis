package com.krivi.apihistorialmedico.business.services;

import com.krivi.apihistorialmedico.model.api.AnalisisHistoriasClinicasDuplicadasResponse;

import java.util.List;

public interface AnalisisHistoriasClinicasDuplicadasService {
  AnalisisHistoriasClinicasDuplicadasResponse analizar(List<Integer> idsHistoriasClinicas);
}
