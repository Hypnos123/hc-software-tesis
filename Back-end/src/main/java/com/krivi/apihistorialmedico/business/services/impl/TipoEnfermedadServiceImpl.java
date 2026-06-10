package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.services.TipoEnfermedadService;
import com.krivi.apihistorialmedico.model.api.*;
import com.krivi.apihistorialmedico.model.entity.Antecedentes;
import com.krivi.apihistorialmedico.model.entity.Paciente;
import com.krivi.apihistorialmedico.model.entity.TipoEnfermedad;
import com.krivi.apihistorialmedico.repository.TipoEnfermedadRepository;
import com.krivi.apihistorialmedico.util.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.krivi.apihistorialmedico.util.Constant.MENSAJE_GUARDAR_ERROR;
import static com.krivi.apihistorialmedico.util.Constant.MENSAJE_GUARDAR_OK;

@Service
@Slf4j
public class TipoEnfermedadServiceImpl implements TipoEnfermedadService {

  @Autowired
  TipoEnfermedadRepository tipoEnfermedadRepository;

  @Override
  public ResponseModelGet<TipoEnfermedadResponse> getAllActive() {
    List<TipoEnfermedadResponse> tipoEnfermedadResponseList = new ArrayList<>();
    tipoEnfermedadRepository.findAll().forEach(tipoEnfermedad -> {

      tipoEnfermedadResponseList.add(TipoEnfermedadResponse.builder()
          .idTipoEnfermedad(tipoEnfermedad.getIdTipoEnfermedad())
          .descripcion(tipoEnfermedad.getDescripcion())
          .build());
    });

    ResponseModelGet<TipoEnfermedadResponse> responseModelGet = new ResponseModelGet<>();
    responseModelGet.setData(tipoEnfermedadResponseList);
    responseModelGet.setMensaje(Constant.MENSAJE_CONSULTA_OK);
    return responseModelGet;
  }

  @Override
  public ResponseModelGet<TipoEnfermedadResponse> findById(int idTipoEnfermedad) {
    List<TipoEnfermedadResponse> tipoEnfermedadResponseList = new ArrayList<>();
    TipoEnfermedad tipoEnfermedad = tipoEnfermedadRepository.findById(idTipoEnfermedad).orElse(null);

      tipoEnfermedadResponseList.add(TipoEnfermedadResponse.builder()
          .idTipoEnfermedad(tipoEnfermedad.getIdTipoEnfermedad())
          .descripcion(tipoEnfermedad.getDescripcion())
          .build());


    ResponseModelGet<TipoEnfermedadResponse> responseModelGet = new ResponseModelGet<>();
    responseModelGet.setData(tipoEnfermedadResponseList);
    responseModelGet.setMensaje(Constant.MENSAJE_CONSULTA_OK);
    return responseModelGet;
  }

  @Override
  public ResponseModelSet save(TipoEnfermedadRequest tipoEnfermedadRequest) {
    ResponseModelSet responseModelSet = new ResponseModelSet();
    try {
      TipoEnfermedad tipoEnfermedad = new TipoEnfermedad();

      tipoEnfermedad.setDescripcion(tipoEnfermedadRequest.getDescripcion());


      tipoEnfermedadRepository.save(tipoEnfermedad);
      responseModelSet.setMensaje(MENSAJE_GUARDAR_OK);
      return responseModelSet;

    } catch (Exception e) {
      log.error("save(): {}", e.getMessage());
      responseModelSet.setMensaje(MENSAJE_GUARDAR_ERROR);
      return responseModelSet;
    }
  }

  @Override
  public ResponseModelSet update(TipoEnfermedadRequest tipoEnfermedadRequest) {
      ResponseModelSet responseModelSet = new ResponseModelSet();
    try {
      TipoEnfermedad tipoEnfermedad = new TipoEnfermedad();

      tipoEnfermedad.setIdTipoEnfermedad(tipoEnfermedadRequest.getIdTipoEnfermedad());
      tipoEnfermedad.setDescripcion(tipoEnfermedadRequest.getDescripcion());


      tipoEnfermedadRepository.save(tipoEnfermedad);
      responseModelSet.setMensaje(MENSAJE_GUARDAR_OK);
      return responseModelSet;

    } catch (Exception e) {
      log.error("update(): {}", e.getMessage());
      responseModelSet.setMensaje(MENSAJE_GUARDAR_ERROR);
      return responseModelSet;
    }
  }

}
