package com.krivi.apihistorialmedico.business.services.impl;


import com.krivi.apihistorialmedico.business.services.DetallePermisoService;
import com.krivi.apihistorialmedico.model.api.DetallePermisoRequest;
import com.krivi.apihistorialmedico.model.api.DetallePermisoResponse;
import com.krivi.apihistorialmedico.model.api.ResponseModelSet;
import com.krivi.apihistorialmedico.model.entity.DetallePermiso;
import com.krivi.apihistorialmedico.repository.DetallePermisoRepository;
import com.krivi.apihistorialmedico.util.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;



@Service
@Slf4j
public class DetallePermisoServiceImpl implements DetallePermisoService {
  @Autowired
  DetallePermisoRepository detallePermisoRepository;

  @Override
  public ResponseModelSet save(List<DetallePermisoRequest> detallePermisoRequestList) {

    ResponseModelSet responseModelSet = new ResponseModelSet();
    try {
      detallePermisoRequestList.forEach(detallePermiso -> {
        detallePermisoRepository.save(new DetallePermiso(
            null,
            detallePermiso.getIdUsuario(),
            detallePermiso.getIdMenu()
        ));
      });
      responseModelSet.setMensaje(Constant.MENSAJE_GUARDAR_OK);
      return responseModelSet;
    } catch (Exception e) {
      log.error("save(): " + e.getMessage());
      responseModelSet.setMensaje(Constant.MENSAJE_GUARDAR_ERROR);
      return responseModelSet;
    }

  }

  @Override
  public void deleteByIdUsuario(int idUsuario) {
    detallePermisoRepository.deleteByIdUsuario(idUsuario);
  }

  @Override
  public List<DetallePermisoResponse> detallePermisosListar(String idUsuario) {
    return detallePermisoRepository.detallePermisosListar(idUsuario);
  }
}
