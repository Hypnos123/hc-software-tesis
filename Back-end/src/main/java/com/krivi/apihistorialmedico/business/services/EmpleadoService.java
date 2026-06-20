package com.krivi.apihistorialmedico.business.services;

import com.krivi.apihistorialmedico.model.api.EmpleadoRequest;
import com.krivi.apihistorialmedico.model.api.EmpleadoResponse;
import com.krivi.apihistorialmedico.model.api.ResponseModelGet;
import com.krivi.apihistorialmedico.model.api.ResponseModelSet;

public interface EmpleadoService {

  ResponseModelGet<EmpleadoResponse> getAll();

  ResponseModelGet<EmpleadoResponse> getAllActive();

  ResponseModelGet<EmpleadoResponse> findById(int idEmpleado);

  ResponseModelSet save(EmpleadoRequest empleadoRequest);

  ResponseModelSet update(EmpleadoRequest empleadoRequest);

  ResponseModelSet changeStatus(int idEmpleado);
}
