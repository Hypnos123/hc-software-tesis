package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.services.EmpleadoService;
import com.krivi.apihistorialmedico.model.api.*;
import com.krivi.apihistorialmedico.model.entity.Antecedentes;
import com.krivi.apihistorialmedico.model.entity.Empleado;
import com.krivi.apihistorialmedico.model.entity.Paciente;
import com.krivi.apihistorialmedico.repository.EmpleadoRepository;
import com.krivi.apihistorialmedico.util.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.krivi.apihistorialmedico.util.Constant.*;

@Service
@Slf4j
public class EmpleadoServiceImpl implements EmpleadoService {

  @Autowired
  private EmpleadoRepository empleadoRepository;

  @Override
  public ResponseModelGet<EmpleadoResponse> getAllActive() {
    List<EmpleadoResponse> empleadoResponseList = new ArrayList<>();
    empleadoRepository.findAll().forEach(empleado -> {

      empleadoResponseList.add(EmpleadoResponse.builder()
          .idEmpleado(empleado.getIdEmpleado())
          .tipoDocumento(empleado.getTipoDocumento())
          .numDocumento(empleado.getNumDocumento())
          .nombres(empleado.getNombres())
          .apellidos(empleado.getApellidos())
          .direccion(empleado.getDireccion())
          .telefono(empleado.getTelefono())
          .celular(empleado.getCelular())
          .build());
    });

    ResponseModelGet<EmpleadoResponse> responseModelGet = new ResponseModelGet<>();
    responseModelGet.setData(empleadoResponseList);
    responseModelGet.setMensaje(Constant.MENSAJE_CONSULTA_OK);
    return responseModelGet;
  }

  @Override
  public ResponseModelGet<EmpleadoResponse> findById(int idEmpleado) {
    List<EmpleadoResponse> empleadoResponseList = new ArrayList<>();
    Empleado empleado = empleadoRepository.findById(idEmpleado).orElse(null);

      empleadoResponseList.add(EmpleadoResponse.builder()
          .idEmpleado(empleado.getIdEmpleado())
          .tipoDocumento(empleado.getTipoDocumento())
          .numDocumento(empleado.getNumDocumento())
          .nombres(empleado.getNombres())
          .apellidos(empleado.getApellidos())
          .direccion(empleado.getDireccion())
          .telefono(empleado.getTelefono())
          .celular(empleado.getCelular())
          .build());


    ResponseModelGet<EmpleadoResponse> responseModelGet = new ResponseModelGet<>();
    responseModelGet.setData(empleadoResponseList);
    responseModelGet.setMensaje(Constant.MENSAJE_CONSULTA_OK);
    return responseModelGet;
  }

  @Override
  public ResponseModelSet save(EmpleadoRequest empleadoRequest) {
    ResponseModelSet responseModelSet = new ResponseModelSet();
    try {
      Empleado empleado = new Empleado();
      empleado.setTipoDocumento(empleadoRequest.getTipoDocumento());
      empleado.setNumDocumento(empleadoRequest.getNumDocumento());
      empleado.setNombres(empleadoRequest.getNombres());
      empleado.setApellidos(empleadoRequest.getApellidos());
      empleado.setDireccion(empleadoRequest.getDireccion());
      empleado.setTelefono(empleadoRequest.getTelefono());
      empleado.setCelular(empleadoRequest.getCelular());

      empleadoRepository.save(empleado);
      responseModelSet.setMensaje(MENSAJE_GUARDAR_OK);
      return responseModelSet;

    } catch (Exception e) {
      log.error("save(): {}", e.getMessage());
      responseModelSet.setMensaje(MENSAJE_GUARDAR_ERROR);
      return responseModelSet;
    }
  }

  @Override
  public ResponseModelSet update(EmpleadoRequest empleadoRequest) {
    ResponseModelSet responseModelSet = new ResponseModelSet();
    try {
      Empleado empleado = new Empleado();
      empleado.setIdEmpleado(empleadoRequest.getIdEmpleado());
      empleado.setTipoDocumento(empleadoRequest.getTipoDocumento());
      empleado.setNumDocumento(empleadoRequest.getNumDocumento());
      empleado.setNombres(empleadoRequest.getNombres());
      empleado.setApellidos(empleadoRequest.getApellidos());
      empleado.setDireccion(empleadoRequest.getDireccion());
      empleado.setTelefono(empleadoRequest.getTelefono());
      empleado.setCelular(empleadoRequest.getCelular());

      empleadoRepository.save(empleado);
      responseModelSet.setMensaje(MENSAJE_EDITAR_OK);
      return responseModelSet;

    } catch (Exception e) {
      log.error("update(): {}", e.getMessage());
      responseModelSet.setMensaje(MENSAJE_GUARDAR_ERROR);
      return responseModelSet;
    }
  }
}
