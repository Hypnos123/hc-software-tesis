package com.krivi.apihistorialmedico.business.services;

import com.krivi.apihistorialmedico.model.api.*;
import com.krivi.apihistorialmedico.model.entity.TipoEnfermedad;

public interface TipoEnfermedadService {

  ResponseModelGet<TipoEnfermedadResponse> getAllActive();

  ResponseModelGet<TipoEnfermedadResponse> findById(int idTipoEnfermedad);

  ResponseModelSet save(TipoEnfermedadRequest tipoEnfermedadRequest);

  ResponseModelSet update(TipoEnfermedadRequest tipoEnfermedadRequest);


}
