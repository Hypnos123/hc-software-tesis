package com.krivi.apihistorialmedico.business.services;

import com.krivi.apihistorialmedico.model.api.PacienteRequest;
import com.krivi.apihistorialmedico.model.api.PacienteResponse;
import com.krivi.apihistorialmedico.model.api.ResponseModelGet;
import com.krivi.apihistorialmedico.model.api.ResponseModelSet;

public interface PacienteService {

  ResponseModelGet<PacienteResponse> getAllActive();

  ResponseModelGet<PacienteResponse> findById(int idAnalisis);

  ResponseModelSet save(PacienteRequest pacienteRequest);

  ResponseModelSet update(PacienteRequest pacienteRequest);


}
