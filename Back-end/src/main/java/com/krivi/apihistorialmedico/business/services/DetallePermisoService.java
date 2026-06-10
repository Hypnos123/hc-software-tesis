package com.krivi.apihistorialmedico.business.services;



import com.krivi.apihistorialmedico.model.api.DetallePermisoRequest;
import com.krivi.apihistorialmedico.model.api.DetallePermisoResponse;
import com.krivi.apihistorialmedico.model.api.ResponseModelSet;

import java.util.List;

public interface DetallePermisoService {

  ResponseModelSet save(List<DetallePermisoRequest> detallePermisoRequestList);

  void deleteByIdUsuario(int idUsuario);

  List<DetallePermisoResponse> detallePermisosListar(String idUsuario);

}
