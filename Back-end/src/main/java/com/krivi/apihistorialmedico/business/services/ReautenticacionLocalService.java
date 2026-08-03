package com.krivi.apihistorialmedico.business.services;

import com.krivi.apihistorialmedico.model.api.ReautenticacionRequest;
import com.krivi.apihistorialmedico.model.api.ReautenticacionResponse;

public interface ReautenticacionLocalService {
  ReautenticacionResponse reautenticar(Integer idUsuarioActual, ReautenticacionRequest request);

  ReautenticacionResponse validarAdministrador(Integer idUsuarioActual);
}
