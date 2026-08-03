package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.services.AntecedentesService;
import com.krivi.apihistorialmedico.model.api.AntecedentesRequest;
import com.krivi.apihistorialmedico.model.api.AntecedentesResponse;
import com.krivi.apihistorialmedico.model.api.ResponseModelGet;
import com.krivi.apihistorialmedico.model.api.ResponseModelSet;
import com.krivi.apihistorialmedico.model.entity.Antecedentes;
import com.krivi.apihistorialmedico.model.entity.Paciente;
import com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente;
import com.krivi.apihistorialmedico.repository.AntecedentesRepository;
import com.krivi.apihistorialmedico.repository.PacienteRepository;
import com.krivi.apihistorialmedico.util.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.krivi.apihistorialmedico.util.Constant.*;

@Service
@Slf4j
public class AntecedentesServiceImpl implements AntecedentesService {

  @Autowired
  AntecedentesRepository antecedentesRepository;

  @Autowired
  PacienteRepository pacienteRepository;

  @Override
  public ResponseModelGet<AntecedentesResponse> getAllActive() {
    List<AntecedentesResponse> antecedentesResponseList = new ArrayList<>();
    antecedentesRepository.findAllByPacienteEstadoRegistro(EstadoRegistroPaciente.ACTIVO)
        .forEach(antecedentes -> antecedentesResponseList.add(toResponse(antecedentes)));

    ResponseModelGet<AntecedentesResponse> responseModelGet = new ResponseModelGet<>();
    responseModelGet.setData(antecedentesResponseList);
    responseModelGet.setMensaje(Constant.MENSAJE_CONSULTA_OK);
    return responseModelGet;
  }

  @Override
  public ResponseModelGet<AntecedentesResponse> findById(int idAntecedente) {
    List<AntecedentesResponse> antecedentesResponseList = new ArrayList<>();
    antecedentesRepository.findByIdAntecedentesAndPacienteEstadoRegistro(idAntecedente, EstadoRegistroPaciente.ACTIVO)
        .ifPresent(antecedentes -> antecedentesResponseList.add(toResponse(antecedentes)));

    ResponseModelGet<AntecedentesResponse> responseModelGet = new ResponseModelGet<>();
    responseModelGet.setData(antecedentesResponseList);
    responseModelGet.setMensaje(Constant.MENSAJE_CONSULTA_OK);
    return responseModelGet;
  }

  @Override
  public ResponseModelGet<AntecedentesResponse> findByPaciente(int idPaciente) {
    List<AntecedentesResponse> antecedentesResponseList = new ArrayList<>();
    antecedentesRepository.findByPacienteIdPacienteAndPacienteEstadoRegistro(idPaciente, EstadoRegistroPaciente.ACTIVO)
        .forEach(antecedentes -> antecedentesResponseList.add(toResponse(antecedentes)));

    ResponseModelGet<AntecedentesResponse> responseModelGet = new ResponseModelGet<>();
    responseModelGet.setData(antecedentesResponseList);
    responseModelGet.setMensaje(Constant.MENSAJE_CONSULTA_OK);
    return responseModelGet;
  }

  @Override
  @Transactional
  public ResponseModelSet save(AntecedentesRequest analisisRequest) {
    ResponseModelSet responseModelSet = new ResponseModelSet();
    Paciente paciente = obtenerPacienteActivo(analisisRequest);
    Antecedentes antecedentes = new Antecedentes();
    aplicarDatos(antecedentes, analisisRequest);
    antecedentes.setPaciente(paciente);
    Antecedentes antecedentesResponse = antecedentesRepository.save(antecedentes);
    responseModelSet.setIdGenerado(antecedentesResponse.getIdAntecedentes());
    responseModelSet.setMensaje(MENSAJE_GUARDAR_OK);
    return responseModelSet;
  }

  @Override
  @Transactional
  public ResponseModelSet update(AntecedentesRequest analisisRequest) {
    ResponseModelSet responseModelSet = new ResponseModelSet();
    Paciente paciente = obtenerPacienteActivo(analisisRequest);
    if (analisisRequest.getIdAntecedentes() == null) {
      throw new IllegalArgumentException("El identificador de antecedentes es obligatorio para actualizar.");
    }
    Antecedentes antecedentes = antecedentesRepository.findByIdAntecedentesAndPacienteEstadoRegistro(
            analisisRequest.getIdAntecedentes(), EstadoRegistroPaciente.ACTIVO)
        .orElseThrow(() -> new IllegalArgumentException("Los antecedentes no existen o pertenecen a un paciente archivado."));
    if (!paciente.getIdPaciente().equals(antecedentes.getPaciente().getIdPaciente())) {
      throw new IllegalArgumentException("Los antecedentes no corresponden al paciente indicado.");
    }
    aplicarDatos(antecedentes, analisisRequest);
    antecedentesRepository.save(antecedentes);
    responseModelSet.setMensaje(MENSAJE_EDITAR_OK);
    return responseModelSet;
  }

  private Paciente obtenerPacienteActivo(AntecedentesRequest request) {
    if (request == null || request.getIdPaciente() == null) {
      throw new IllegalArgumentException("El paciente es obligatorio.");
    }
    return pacienteRepository.findByIdPacienteAndEstadoRegistro(request.getIdPaciente(), EstadoRegistroPaciente.ACTIVO)
        .orElseThrow(() -> new IllegalArgumentException("El paciente no existe o está archivado."));
  }

  private void aplicarDatos(Antecedentes antecedentes, AntecedentesRequest analisisRequest) {
    antecedentes.setAlimentacion(analisisRequest.getAlimentacion());
    antecedentes.setHabitos(analisisRequest.getHabitos());
    antecedentes.setVivienda(analisisRequest.getVivienda());
    antecedentes.setDesarrolloPsicomotor(analisisRequest.getDesarrolloPsicomotor());
    antecedentes.setVacunas(analisisRequest.getVacunas());
    antecedentes.setEducacion(analisisRequest.getEducacion());
    antecedentes.setEnfermedadesPrevias(analisisRequest.getEnfermedadesPrevias());
    antecedentes.setCirugiasPrevias(analisisRequest.getCirugiasPrevias());
    antecedentes.setAlergiaMedicamentos(analisisRequest.getAlergiaMedicamentos());
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
