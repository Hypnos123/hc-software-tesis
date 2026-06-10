package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.services.ConsultaService;
import com.krivi.apihistorialmedico.model.api.*;
import com.krivi.apihistorialmedico.model.entity.*;
import com.krivi.apihistorialmedico.repository.ConsultaRepository;
import com.krivi.apihistorialmedico.repository.PacienteRepository;
import com.krivi.apihistorialmedico.repository.TipoEnfermedadRepository;
import com.krivi.apihistorialmedico.repository.UsuarioRepository;
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
public class ConsultaServiceImpl implements ConsultaService {

  @Autowired
  ConsultaRepository consultaRepository;

  @Autowired
  PacienteRepository pacienteRepository;

  @Autowired
  TipoEnfermedadRepository tipoEnfermedadRepository;

  @Autowired
  UsuarioRepository usuarioRepository;

  @Override
  public ResponseModelGet<ConsultaResponse> getAllActive() {
    List<ConsultaResponse> consultaResponseList = new ArrayList<>();
    consultaRepository.findAll().forEach(consulta -> {

      consultaResponseList.add(ConsultaResponse.builder()
          .idConsulta(consulta.getIdConsulta())
          .presionArterial(consulta.getPresionArterial())
          .frecuenciaCardiaca(consulta.getFrecuenciaCardiaca())
          .frecuenciaRespiratoria(consulta.getFrecuenciaRespiratoria())
          .talla(consulta.getTalla())
          .temperatura(consulta.getTemperatura())
          .peso(consulta.getPeso())
          .fechaConsulta(consulta.getFechaConsulta())
          .tiempoEnfermedad(consulta.getTiempoEnfermedad())
          .idTipoEnfermedad(consulta.getTipoEnfermedad().getIdTipoEnfermedad())
              .relatoPaciente(consulta.getRelatoPaciente())
              .idPaciente(consulta.getPaciente().getIdPaciente())
              .idUsuario(consulta.getUsuario().getIdUsuario())
          .build());
    });

    ResponseModelGet<ConsultaResponse> responseModelGet = new ResponseModelGet<>();
    responseModelGet.setData(consultaResponseList);
    responseModelGet.setMensaje(Constant.MENSAJE_CONSULTA_OK);
    return responseModelGet;
  }

  @Override
  public ResponseModelGet<ConsultaResponse> findById(int idConsulta) {
    List<ConsultaResponse> consultaResponseList = new ArrayList<>();
    Consulta consulta = consultaRepository.findById(idConsulta).orElse(null);

      consultaResponseList.add(ConsultaResponse.builder()
          .idConsulta(consulta.getIdConsulta())
          .presionArterial(consulta.getPresionArterial())
          .frecuenciaCardiaca(consulta.getFrecuenciaCardiaca())
          .frecuenciaRespiratoria(consulta.getFrecuenciaRespiratoria())
          .talla(consulta.getTalla())
          .temperatura(consulta.getTemperatura())
          .peso(consulta.getPeso())
          .fechaConsulta(consulta.getFechaConsulta())
          .tiempoEnfermedad(consulta.getTiempoEnfermedad())
          .idTipoEnfermedad(consulta.getTipoEnfermedad().getIdTipoEnfermedad())
          .relatoPaciente(consulta.getRelatoPaciente())
          .idPaciente(consulta.getPaciente().getIdPaciente())
          .idUsuario(consulta.getUsuario().getIdUsuario())
          .build());


    ResponseModelGet<ConsultaResponse> responseModelGet = new ResponseModelGet<>();
    responseModelGet.setData(consultaResponseList);
    responseModelGet.setMensaje(Constant.MENSAJE_CONSULTA_OK);
    return responseModelGet;
  }

  @Override
  public ResponseModelSet save(ConsultaRequest consultaRequest) {
    ResponseModelSet responseModelSet = new ResponseModelSet();
    try {
      Consulta consulta = new Consulta();
      consulta.setPresionArterial(consultaRequest.getPresionArterial());
      consulta.setFrecuenciaCardiaca(consulta.getFrecuenciaCardiaca());
      consulta.setFrecuenciaRespiratoria(consulta.getFrecuenciaRespiratoria());
      consulta.setTalla(consulta.getTalla());
      consulta.setTemperatura(consulta.getTemperatura());
      consulta.setPeso(consulta.getPeso());
      consulta.setFechaConsulta(consulta.getFechaConsulta());
      consulta.setTiempoEnfermedad(consulta.getTiempoEnfermedad());
      consulta.setRelatoPaciente(consultaRequest.getRelatoPaciente());
      consulta.setTipoEnfermedad(new TipoEnfermedad(consultaRequest.getIdTipoEnfermedad()));
      consulta.setPaciente(new Paciente(consultaRequest.getIdPaciente()));
      consulta.setUsuario(new Usuario(consultaRequest.getIdUsuario()));

      consultaRepository.save(consulta);
      responseModelSet.setMensaje(MENSAJE_GUARDAR_OK);
      return responseModelSet;

    } catch (Exception e) {
      log.error("save(): {}", e.getMessage());
      responseModelSet.setMensaje(MENSAJE_GUARDAR_ERROR);
      return responseModelSet;
    }
  }

  @Override
  public ResponseModelSet update(ConsultaRequest consultaRequest) {
    ResponseModelSet responseModelSet = new ResponseModelSet();
    try {
      Consulta consulta = consultaRepository.findById(consultaRequest.getIdConsulta()).orElse(null);
      if (consulta != null) {
        consulta.setPresionArterial(consultaRequest.getPresionArterial());
        consulta.setFrecuenciaCardiaca(consultaRequest.getFrecuenciaCardiaca());
        consulta.setFrecuenciaRespiratoria(consultaRequest.getFrecuenciaRespiratoria());
        consulta.setTalla(consultaRequest.getTalla());
        consulta.setTemperatura(consultaRequest.getTemperatura());
        consulta.setPeso(consultaRequest.getPeso());
        consulta.setFechaConsulta(consultaRequest.getFechaConsulta());
        consulta.setTiempoEnfermedad(consultaRequest.getTiempoEnfermedad());
        consulta.setRelatoPaciente(consultaRequest.getRelatoPaciente());

        Paciente paciente = pacienteRepository.findById(consultaRequest.getIdPaciente()).orElse(null);
        TipoEnfermedad tipoEnfermedad = tipoEnfermedadRepository.findById(consultaRequest.getIdTipoEnfermedad()).orElse(null);
        Usuario usuario = usuarioRepository.findById(consultaRequest.getIdUsuario()).orElse(null);

        consulta.setPaciente(paciente);
        consulta.setTipoEnfermedad(tipoEnfermedad);
        consulta.setUsuario(usuario);

        consultaRepository.save(consulta);
        responseModelSet.setMensaje(MENSAJE_GUARDAR_OK);
      } else {
        responseModelSet.setMensaje(MENSAJE_GUARDAR_ERROR);
      }
      return responseModelSet;
    } catch (Exception e) {
      log.error("update(): {}", e.getMessage());
      responseModelSet.setMensaje(MENSAJE_GUARDAR_ERROR);
      return responseModelSet;
    }
  }
  }

