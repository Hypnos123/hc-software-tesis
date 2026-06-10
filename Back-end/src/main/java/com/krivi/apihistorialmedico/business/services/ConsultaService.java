package com.krivi.apihistorialmedico.business.services;

import com.krivi.apihistorialmedico.model.api.ConsultaRequest;
import com.krivi.apihistorialmedico.model.api.ConsultaResponse;
import com.krivi.apihistorialmedico.model.api.ResponseModelGet;
import com.krivi.apihistorialmedico.model.api.ResponseModelSet;

public interface ConsultaService {

  ResponseModelGet<ConsultaResponse> getAllActive();

  ResponseModelGet<ConsultaResponse> findById(int idAnalisis);

  ResponseModelSet save(ConsultaRequest analisisRequest);

  ResponseModelSet update(ConsultaRequest analisisRequest);


}
