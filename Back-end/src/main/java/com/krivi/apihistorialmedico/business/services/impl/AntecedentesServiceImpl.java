package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.services.AntecedentesService;
import com.krivi.apihistorialmedico.model.api.AntecedentesRequest;
import com.krivi.apihistorialmedico.model.api.AntecedentesResponse;
import com.krivi.apihistorialmedico.model.api.ResponseModelGet;
import com.krivi.apihistorialmedico.model.api.ResponseModelSet;
import com.krivi.apihistorialmedico.model.entity.Antecedentes;
import com.krivi.apihistorialmedico.model.entity.Paciente;
import com.krivi.apihistorialmedico.repository.AntecedentesRepository;
import com.krivi.apihistorialmedico.util.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.krivi.apihistorialmedico.util.Constant.*;

@Service
@Slf4j
public class AntecedentesServiceImpl implements AntecedentesService {

  @Autowired
  AntecedentesRepository antecedentesRepository;

  @Override
  public ResponseModelGet<AntecedentesResponse> getAllActive() {
    List<AntecedentesResponse> antecedentesResponseList = new ArrayList<>();
    antecedentesRepository.findAll().forEach(antecedentes -> antecedentesResponseList.add(toResponse(antecedentes)));

    ResponseModelGet<AntecedentesResponse> responseModelGet = new ResponseModelGet<>();
    responseModelGet.setData(antecedentesResponseList);
    responseModelGet.setMensaje(Constant.MENSAJE_CONSULTA_OK);
    return responseModelGet;
  }

  @Override
  public ResponseModelGet<AntecedentesResponse> findById(int idAntecedente) {
    List<AntecedentesResponse> antecedentesResponseList = new ArrayList<>();
    antecedentesRepository.findById(idAntecedente).ifPresent(antecedentes -> antecedentesResponseList.add(toResponse(antecedentes)));

    ResponseModelGet<AntecedentesResponse> responseModelGet = new ResponseModelGet<>();
    responseModelGet.setData(antecedentesResponseList);
    responseModelGet.setMensaje(Constant.MENSAJE_CONSULTA_OK);
    return responseModelGet;
  }

  @Override
  public ResponseModelGet<AntecedentesResponse> findByPaciente(int idPaciente) {
    List<AntecedentesResponse> antecedentesResponseList = new ArrayList<>();
    antecedentesRepository.findByPacienteIdPaciente(idPaciente)
        .forEach(antecedentes -> antecedentesResponseList.add(toResponse(antecedentes)));

    ResponseModelGet<AntecedentesResponse> responseModelGet = new ResponseModelGet<>();
    responseModelGet.setData(antecedentesResponseList);
    responseModelGet.setMensaje(Constant.MENSAJE_CONSULTA_OK);
    return responseModelGet;
  }

  @Override
  public ResponseModelSet save(AntecedentesRequest analisisRequest) {
    ResponseModelSet responseModelSet = new ResponseModelSet();
    try {
      Antecedentes antecedentesResponse = antecedentesRepository.save(toEntity(analisisRequest));
      responseModelSet.setIdGenerado(antecedentesResponse.getIdAntecedentes());
      responseModelSet.setMensaje(MENSAJE_GUARDAR_OK);
      return responseModelSet;

    } catch (Exception e) {
      log.error("save(): " + e.getMessage());
      responseModelSet.setMensaje(MENSAJE_GUARDAR_ERROR);
      return responseModelSet;
    }
  }

  @Override
  public ResponseModelSet update(AntecedentesRequest analisisRequest) {

    ResponseModelSet responseModelSet = new ResponseModelSet();
    try {
      antecedentesRepository.save(toEntity(analisisRequest));
      responseModelSet.setMensaje(MENSAJE_EDITAR_OK);
      return responseModelSet;

    } catch (Exception e) {
      log.error("update(): {}", e.getMessage());
      responseModelSet.setMensaje(MENSAJE_GUARDAR_ERROR);
      return responseModelSet;
    }
  }

  private Antecedentes toEntity(AntecedentesRequest analisisRequest) {
    Antecedentes antecedentes = new Antecedentes();
    antecedentes.setIdAntecedentes(analisisRequest.getIdAntecedentes());
    antecedentes.setAlimentacion(analisisRequest.getAlimentacion());
    antecedentes.setHabitos(analisisRequest.getHabitos());
    antecedentes.setVivienda(analisisRequest.getVivienda());
    antecedentes.setDesarrolloPsicomotor(analisisRequest.getDesarrolloPsicomotor());
    antecedentes.setVacunas(analisisRequest.getVacunas());
    antecedentes.setEducacion(analisisRequest.getEducacion());
    antecedentes.setEnfermedadesPrevias(analisisRequest.getEnfermedadesPrevias());
    antecedentes.setCirugiasPrevias(analisisRequest.getCirugiasPrevias());
    antecedentes.setAlergiaMedicamentos(analisisRequest.getAlergiaMedicamentos());
    antecedentes.setPaciente(new Paciente(analisisRequest.getIdPaciente()));
    return antecedentes;
  }

  private AntecedentesResponse toResponse(Antecedentes antecedentes) {
    return AntecedentesResponse.builder()
        .idAntecedentes(antecedentes.getIdAntecedentes())
        .alimentacion(antecedentes.getAlimentacion())
        .habitos(antecedentes.getHabitos())
        .vivienda(antecedentes.getVivienda())
        .desarrolloPsicomotor(antecedentes.getDesarrolloPsicomotor())
        .vacunas(antecedentes.getVacunas())
        .educacion(antecedentes.getEducacion())
        .enfermedadesPrevias(antecedentes.getEnfermedadesPrevias())
        .cirugiasPrevias(antecedentes.getCirugiasPrevias())
        .alergiaMedicamentos(antecedentes.getAlergiaMedicamentos())
        .idPaciente(antecedentes.getPaciente().getIdPaciente())
        .nombreApellidos(antecedentes.getPaciente().getApellidos() + " " + antecedentes.getPaciente().getNombres())
        .build();
  }
}
