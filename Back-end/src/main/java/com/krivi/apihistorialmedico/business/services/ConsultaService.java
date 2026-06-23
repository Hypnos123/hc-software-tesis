package com.krivi.apihistorialmedico.business.services;

import com.krivi.apihistorialmedico.model.api.ConsultaRequest;
import com.krivi.apihistorialmedico.model.api.ConsultaResponse;
import com.krivi.apihistorialmedico.model.api.ResponseModelGet;
import com.krivi.apihistorialmedico.model.api.ResponseModelSet;

public interface ConsultaService {

  ResponseModelGet<ConsultaResponse> getAllActive(Integer idUsuario);

  ResponseModelGet<ConsultaResponse> findById(int idAnalisis, Integer idUsuario);

  ResponseModelGet<ConsultaResponse> findByHistoriaClinica(int idHistoriaClinica);

  ResponseModelSet save(ConsultaRequest analisisRequest);

  ResponseModelSet update(ConsultaRequest analisisRequest);

  ResponseModelSet finalizarAtencion(int idConsulta, ConsultaRequest request, Integer idUsuario);


}
