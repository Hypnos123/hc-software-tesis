package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.services.PacienteService;
import com.krivi.apihistorialmedico.model.api.*;
import com.krivi.apihistorialmedico.model.entity.Antecedentes;
import com.krivi.apihistorialmedico.model.entity.Paciente;
import com.krivi.apihistorialmedico.repository.PacienteRepository;
import com.krivi.apihistorialmedico.util.Constant;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Array;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.krivi.apihistorialmedico.util.Constant.*;

@Service
@Slf4j
public class PacienteServiceImpl implements PacienteService {

  @Autowired
  PacienteRepository pacienteRepository;

  @Override
  public ResponseModelGet<PacienteResponse> getAllActive() {
    List<PacienteResponse> pacienteResponseList = new ArrayList<>();
    pacienteRepository.findAll().forEach(paciente -> {

      pacienteResponseList.add(PacienteResponse.builder()
          .idPaciente(paciente.getIdPaciente())
          .nombres(paciente.getNombres())
          .apellidos(paciente.getApellidos())
          .fechaIngreso(paciente.getFechaIngreso())
          .fechaNacimiento(paciente.getFechaNacimiento())
          .estadoCivil(paciente.getEstadoCivil())
          .numDocumento(paciente.getNumDocumento())
          .sexo(paciente.getSexo())
          .direccion(paciente.getDireccion())
          .distrito(paciente.getDistrito())
          .traidoPor(paciente.getTraidoPor())
          .build());
    });

    ResponseModelGet<PacienteResponse> responseModelGet = new ResponseModelGet<>();
    responseModelGet.setData(pacienteResponseList);
    responseModelGet.setMensaje(Constant.MENSAJE_CONSULTA_OK);
    return responseModelGet;
  }

  @Override
  public ResponseModelGet<PacienteResponse> findById(int idPaciente) {
    List<PacienteResponse> pacienteResponseList = new ArrayList<>();
    Paciente paciente = pacienteRepository.findById(idPaciente).orElse(null);

      pacienteResponseList.add(PacienteResponse.builder()
          .idPaciente(paciente.getIdPaciente())
          .nombres(paciente.getNombres())
          .apellidos(paciente.getApellidos())
          .fechaIngreso(paciente.getFechaIngreso())
          .fechaNacimiento(paciente.getFechaNacimiento())
          .estadoCivil(paciente.getEstadoCivil())
          .numDocumento(paciente.getNumDocumento())
          .sexo(paciente.getSexo())
          .direccion(paciente.getDireccion())
          .distrito(paciente.getDistrito())
          .traidoPor(paciente.getTraidoPor())
          .build());


    ResponseModelGet<PacienteResponse> responseModelGet = new ResponseModelGet<>();
    responseModelGet.setData(pacienteResponseList);
    responseModelGet.setMensaje(Constant.MENSAJE_CONSULTA_OK);
    return responseModelGet;
  }

  @Override
  public ResponseModelGet<PacienteResponse> search(String nombre, String dni, Integer limit) {
    int safeLimit = limit == null || limit < 1 || limit > 25 ? 10 : limit;
    List<PacienteResponse> pacienteResponseList = new ArrayList<>();
    Iterable<Paciente> pacientes;
    if (dni != null && !dni.trim().isEmpty()) {
      pacientes = pacienteRepository.searchByDni(dni.trim(), safeLimit);
    } else if (nombre != null && nombre.trim().length() >= 2) {
      pacientes = pacienteRepository.searchByNombre(nombre.trim(), safeLimit);
    } else {
      pacientes = new ArrayList<>();
    }
    pacientes.forEach(paciente -> pacienteResponseList.add(toResponse(paciente)));
    ResponseModelGet<PacienteResponse> responseModelGet = new ResponseModelGet<>();
    responseModelGet.setData(pacienteResponseList);
    responseModelGet.setMensaje(Constant.MENSAJE_CONSULTA_OK);
    return responseModelGet;
  }

  private PacienteResponse toResponse(Paciente paciente) {
    return PacienteResponse.builder()
        .idPaciente(paciente.getIdPaciente())
        .nombres(paciente.getNombres())
        .apellidos(paciente.getApellidos())
        .fechaIngreso(paciente.getFechaIngreso())
        .fechaNacimiento(paciente.getFechaNacimiento())
        .estadoCivil(paciente.getEstadoCivil())
        .numDocumento(paciente.getNumDocumento())
        .sexo(paciente.getSexo())
        .direccion(paciente.getDireccion())
        .distrito(paciente.getDistrito())
        .traidoPor(paciente.getTraidoPor())
        .build();
  }

  @Override
  public ResponseModelSet save(PacienteRequest pacienteRequest) {
    ResponseModelSet responseModelSet = new ResponseModelSet();
    try {
      Paciente paciente = new Paciente();
      paciente.setNombres(pacienteRequest.getNombres());
      paciente.setApellidos(pacienteRequest.getApellidos());
      paciente.setFechaIngreso(pacienteRequest.getFechaIngreso());
      paciente.setFechaNacimiento(pacienteRequest.getFechaNacimiento());
      paciente.setEstadoCivil(pacienteRequest.getEstadoCivil());
      paciente.setNumDocumento(pacienteRequest.getNumDocumento());
      paciente.setSexo(pacienteRequest.getSexo());
      paciente.setDireccion(pacienteRequest.getDireccion());
      paciente.setDistrito(pacienteRequest.getDistrito());
      paciente.setTraidoPor(pacienteRequest.getTraidoPor());

      Paciente pacienteResponse = pacienteRepository.save(paciente);
      responseModelSet.setIdGenerado(pacienteResponse.getIdPaciente());
      responseModelSet.setMensaje(MENSAJE_GUARDAR_OK);
      return responseModelSet;

    } catch (Exception e) {
      log.error("save(): {}", e.getMessage());
      responseModelSet.setMensaje(MENSAJE_GUARDAR_ERROR);
      return responseModelSet;
    }
  }

  @Override
  public ResponseModelSet update(PacienteRequest pacienteRequest) {
    ResponseModelSet responseModelSet = new ResponseModelSet();
    try {
      Paciente paciente = new Paciente();
      paciente.setIdPaciente(pacienteRequest.getIdPaciente());
      paciente.setNombres(pacienteRequest.getNombres());
      paciente.setApellidos(pacienteRequest.getApellidos());
      paciente.setFechaIngreso(pacienteRequest.getFechaIngreso());
      paciente.setFechaNacimiento(pacienteRequest.getFechaNacimiento());
      paciente.setEstadoCivil(pacienteRequest.getEstadoCivil());
      paciente.setNumDocumento(pacienteRequest.getNumDocumento());
      paciente.setSexo(pacienteRequest.getSexo());
      paciente.setDireccion(pacienteRequest.getDireccion());
      paciente.setDistrito(pacienteRequest.getDistrito());
      paciente.setTraidoPor(pacienteRequest.getTraidoPor());

      pacienteRepository.save(paciente);
      responseModelSet.setMensaje(MENSAJE_EDITAR_OK);
      return responseModelSet;

    } catch (Exception e) {
      log.error("update(): {}", e.getMessage());
      responseModelSet.setMensaje(MENSAJE_GUARDAR_ERROR);
      return responseModelSet;
    }
  }
}
