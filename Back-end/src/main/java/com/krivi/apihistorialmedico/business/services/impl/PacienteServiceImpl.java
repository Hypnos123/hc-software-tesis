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

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
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
          .edad(calcularEdad(paciente.getFechaNacimiento()))
          .estadoCivil(normalizeEstadoCivil(paciente.getEstadoCivil()))
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
          .edad(calcularEdad(paciente.getFechaNacimiento()))
          .estadoCivil(normalizeEstadoCivil(paciente.getEstadoCivil()))
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
        .edad(calcularEdad(paciente.getFechaNacimiento()))
        .estadoCivil(normalizeEstadoCivil(paciente.getEstadoCivil()))
        .numDocumento(paciente.getNumDocumento())
        .sexo(paciente.getSexo())
        .direccion(paciente.getDireccion())
        .distrito(paciente.getDistrito())
        .traidoPor(paciente.getTraidoPor())
        .build();
  }

  private void validarFechaNacimiento(Date fechaNacimiento) {
    if (fechaNacimiento == null) {
      throw new IllegalArgumentException("La fecha de nacimiento es obligatoria.");
    }

    LocalDate nacimiento = toLocalDate(fechaNacimiento);
    LocalDate hoy = LocalDate.now();

    if (nacimiento.isAfter(hoy)) {
      throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura.");
    }

    int edad = Period.between(nacimiento, hoy).getYears();
    if (edad < 0 || edad > 120) {
      throw new IllegalArgumentException("La edad calculada debe estar entre 0 y 120 años.");
    }
  }

  private Integer calcularEdad(Date fechaNacimiento) {
    if (fechaNacimiento == null) return null;
    return Period.between(toLocalDate(fechaNacimiento), LocalDate.now()).getYears();
  }

  private LocalDate toLocalDate(Date fecha) {
    return fecha.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
  }

  private String normalizeEstadoCivil(String estadoCivil) {
    if (estadoCivil == null || estadoCivil.trim().isEmpty()) {
      return estadoCivil;
    }

    String normalized = java.text.Normalizer.normalize(estadoCivil, java.text.Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .trim()
        .toUpperCase();

    if (normalized.startsWith("SOLTER")) return "SOLTERO";
    if (normalized.startsWith("CASAD")) return "CASADO";
    if (normalized.startsWith("DIVORCIAD")) return "DIVORCIADO";
    if (normalized.startsWith("VIUD")) return "VIUDO";

    return normalized;
  }

  @Override
  public ResponseModelSet save(PacienteRequest pacienteRequest) {
    ResponseModelSet responseModelSet = new ResponseModelSet();
    try {
      validarFechaNacimiento(pacienteRequest.getFechaNacimiento());
      Paciente paciente = new Paciente();
      paciente.setNombres(pacienteRequest.getNombres());
      paciente.setApellidos(pacienteRequest.getApellidos());
      paciente.setFechaIngreso(pacienteRequest.getFechaIngreso());
      paciente.setFechaNacimiento(pacienteRequest.getFechaNacimiento());
      paciente.setEstadoCivil(normalizeEstadoCivil(pacienteRequest.getEstadoCivil()));
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
      validarFechaNacimiento(pacienteRequest.getFechaNacimiento());
      Paciente paciente = new Paciente();
      paciente.setIdPaciente(pacienteRequest.getIdPaciente());
      paciente.setNombres(pacienteRequest.getNombres());
      paciente.setApellidos(pacienteRequest.getApellidos());
      paciente.setFechaIngreso(pacienteRequest.getFechaIngreso());
      paciente.setFechaNacimiento(pacienteRequest.getFechaNacimiento());
      paciente.setEstadoCivil(normalizeEstadoCivil(pacienteRequest.getEstadoCivil()));
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
