package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.services.HistoriaClinicaService;
import com.krivi.apihistorialmedico.model.api.*;
import com.krivi.apihistorialmedico.model.entity.*;
import com.krivi.apihistorialmedico.repository.*;
import com.krivi.apihistorialmedico.util.Constant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
public class HistoriaClinicaServiceImpl implements HistoriaClinicaService {
  @Autowired HistoriaClinicaRepository historiaClinicaRepository;
  @Autowired PacienteRepository pacienteRepository;
  @Autowired AntecedentesRepository antecedentesRepository;

  public ResponseModelGet<HistoriaClinicaResponse> getAll() {
    List<HistoriaClinicaResponse> data = new ArrayList<>();
    historiaClinicaRepository.findAll().forEach(h -> data.add(toResponse(h)));
    return response(data);
  }

  public ResponseModelGet<HistoriaClinicaResponse> findById(int id) {
    return response(historiaClinicaRepository.findById(id).map(this::toResponse).map(List::of).orElse(List.of()));
  }

  public ResponseModelGet<HistoriaClinicaResponse> findByPaciente(int idPaciente) {
    return response(historiaClinicaRepository.findByPacienteIdPaciente(idPaciente).map(this::toResponse).map(List::of).orElse(List.of()));
  }

  public ResponseModelSet save(HistoriaClinicaRequest request) {
    ResponseModelSet r = new ResponseModelSet();
    if (request.getIdPaciente() == null) { r.setMensaje("Debe seleccionar un paciente registrado."); return r; }
    Optional<Paciente> paciente = pacienteRepository.findById(request.getIdPaciente());
    if (paciente.isEmpty()) { r.setMensaje("El paciente seleccionado no existe."); return r; }
    Optional<HistoriaClinica> existente = historiaClinicaRepository.findByPacienteIdPaciente(request.getIdPaciente());
    if (existente.isPresent()) { r.setMensaje("El paciente seleccionado ya cuenta con una historia clínica."); r.setIdGenerado(existente.get().getIdHistoriaClinica()); return r; }
    HistoriaClinica h = new HistoriaClinica();
    h.setPaciente(paciente.get());
    HistoriaClinica saved = historiaClinicaRepository.save(h);
    r.setIdGenerado(saved.getIdHistoriaClinica());
    r.setMensaje(Constant.MENSAJE_GUARDAR_OK);
    return r;
  }

  public ResponseModelSet update(HistoriaClinicaRequest request) {
    ResponseModelSet r = new ResponseModelSet();
    Optional<HistoriaClinica> historia = historiaClinicaRepository.findById(request.getIdHistoriaClinica());
    if (historia.isEmpty()) { r.setMensaje("Historia clínica no encontrada."); return r; }
    historiaClinicaRepository.save(historia.get());
    r.setMensaje(Constant.MENSAJE_EDITAR_OK);
    r.setIdGenerado(historia.get().getIdHistoriaClinica());
    return r;
  }

  private ResponseModelGet<HistoriaClinicaResponse> response(List<HistoriaClinicaResponse> data) { ResponseModelGet<HistoriaClinicaResponse> r = new ResponseModelGet<>(); r.setData(data); r.setMensaje(Constant.MENSAJE_CONSULTA_OK); return r; }

  private HistoriaClinicaResponse toResponse(HistoriaClinica h) {
    Paciente p = h.getPaciente();
    Antecedentes a = antecedentesRepository.findByPacienteIdPaciente(p.getIdPaciente()).stream().findFirst().orElse(null);
    return HistoriaClinicaResponse.builder().idHistoriaClinica(h.getIdHistoriaClinica()).fechaCreacion(h.getFechaCreacion()).ultimaActualizacion(h.getUltimaActualizacion()).idPaciente(p.getIdPaciente()).nombres(p.getNombres()).apellidos(p.getApellidos()).fechaIngreso(p.getFechaIngreso()).fechaNacimiento(p.getFechaNacimiento()).estadoCivil(p.getEstadoCivil()).numDocumento(p.getNumDocumento()).edad(edad(p.getFechaNacimiento())).enfermedadesPrevias(a == null ? null : a.getEnfermedadesPrevias()).cirugiasPrevias(a == null ? null : a.getCirugiasPrevias()).alergiaMedicamentos(a == null ? null : a.getAlergiaMedicamentos()).build();
  }

  private Integer edad(Date fechaNacimiento) {
    if (fechaNacimiento == null) return null;
    LocalDate birth = fechaNacimiento.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    LocalDate now = LocalDate.now();
    int age = now.getYear() - birth.getYear();
    if (now.getDayOfYear() < birth.getDayOfYear()) age--;
    return age;
  }
}
