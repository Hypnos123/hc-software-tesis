package com.krivi.apihistorialmedico.business.services;

import com.krivi.apihistorialmedico.model.api.HistoriaClinicaRequest;
import com.krivi.apihistorialmedico.model.api.HistoriaClinicaResponse;
import com.krivi.apihistorialmedico.model.api.ResponseModelGet;
import com.krivi.apihistorialmedico.model.api.ResponseModelSet;
import com.krivi.apihistorialmedico.model.api.BusquedaHistoriasClinicasResponse;
import com.krivi.apihistorialmedico.model.api.EstadisticasHistoriasClinicasResponse;
import com.krivi.apihistorialmedico.model.api.DuplicadosHistoriasClinicasResponse;

public interface HistoriaClinicaService {
  ResponseModelGet<HistoriaClinicaResponse> getAll();
  ResponseModelGet<HistoriaClinicaResponse> findById(int idHistoriaClinica);
  ResponseModelGet<HistoriaClinicaResponse> findByPaciente(int idPaciente);
  ResponseModelSet save(HistoriaClinicaRequest request);
  ResponseModelSet update(HistoriaClinicaRequest request);
  BusquedaHistoriasClinicasResponse buscarParaIntegracion(String criterio);
  EstadisticasHistoriasClinicasResponse obtenerEstadisticasParaIntegracion();
  DuplicadosHistoriasClinicasResponse obtenerDuplicadosParaIntegracion();
}
