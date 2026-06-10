package com.krivi.apihistorialmedico.business.services;

import com.krivi.apihistorialmedico.model.api.AntecedentesRequest;
import com.krivi.apihistorialmedico.model.api.AntecedentesResponse;
import com.krivi.apihistorialmedico.model.api.ResponseModelGet;
import com.krivi.apihistorialmedico.model.api.ResponseModelSet;

public interface AntecedentesService {


  ResponseModelGet<AntecedentesResponse> getAllActive();

  ResponseModelGet<AntecedentesResponse> findById(int idAnalisis);

  ResponseModelSet save(AntecedentesRequest analisisRequest);

  ResponseModelSet update(AntecedentesRequest analisisRequest);


}
