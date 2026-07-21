package com.krivi.apihistorialmedico.business.services;

import com.krivi.apihistorialmedico.model.api.PacienteRequest;
import com.krivi.apihistorialmedico.model.api.PacienteResponse;
import com.krivi.apihistorialmedico.model.api.BusquedaPacienteResponse;
import com.krivi.apihistorialmedico.model.api.ResponseModelGet;
import com.krivi.apihistorialmedico.model.api.ResponseModelSet;

public interface PacienteService {

  ResponseModelGet<PacienteResponse> getAllActive();

  ResponseModelGet<PacienteResponse> findById(int idAnalisis);

  ResponseModelGet<PacienteResponse> search(String nombre, String dni, Integer limit);

  BusquedaPacienteResponse buscarParaIntegracion(String criterio);

  ResponseModelSet save(PacienteRequest pacienteRequest);

  ResponseModelSet update(PacienteRequest pacienteRequest);


}
