package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.services.EmpleadoService;
import com.krivi.apihistorialmedico.model.api.EmpleadoRequest;
import com.krivi.apihistorialmedico.model.api.EmpleadoResponse;
import com.krivi.apihistorialmedico.model.api.ResponseModelGet;
import com.krivi.apihistorialmedico.model.api.ResponseModelSet;
import com.krivi.apihistorialmedico.model.entity.Empleado;
import com.krivi.apihistorialmedico.repository.EmpleadoRepository;
import com.krivi.apihistorialmedico.util.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.krivi.apihistorialmedico.util.Constant.MENSAJE_EDITAR_OK;
import static com.krivi.apihistorialmedico.util.Constant.MENSAJE_GUARDAR_ERROR;
import static com.krivi.apihistorialmedico.util.Constant.MENSAJE_GUARDAR_OK;

@Service
@Slf4j
public class EmpleadoServiceImpl implements EmpleadoService {

  @Autowired
  private EmpleadoRepository empleadoRepository;

  @Override
  public ResponseModelGet<EmpleadoResponse> getAll() {
    return buildResponse(empleadoRepository.findAll());
  }

  @Override
  public ResponseModelGet<EmpleadoResponse> getAllActive() {
    return buildResponse(empleadoRepository.findByEstadoTrue());
  }

  @Override
  public ResponseModelGet<EmpleadoResponse> findById(int idEmpleado) {
    List<EmpleadoResponse> empleadoResponseList = new ArrayList<>();
    empleadoRepository.findById(idEmpleado)
        .ifPresent(empleado -> empleadoResponseList.add(buildEmpleadoResponse(empleado)));

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
      empleado.setCargo(empleadoRequest.getCargo());
      empleado.setEstado(Boolean.TRUE);

      Empleado empleadoResponse = empleadoRepository.save(empleado);
      responseModelSet.setIdGenerado(empleadoResponse.getIdEmpleado());
      responseModelSet.setMensaje(MENSAJE_GUARDAR_OK);
      return responseModelSet;

    } catch (Exception e) {
      log.error("save(): {}", e.getMessage());
      responseModelSet.setMensaje(MENSAJE_GUARDAR_ERROR);
      responseModelSet.setError(e.getMessage());
      return responseModelSet;
    }
  }

  @Override
  public ResponseModelSet update(EmpleadoRequest empleadoRequest) {
    ResponseModelSet responseModelSet = new ResponseModelSet();
    try {
      Empleado empleado = empleadoRepository.findById(empleadoRequest.getIdEmpleado())
          .orElseThrow(() -> new IllegalArgumentException("Empleado no encontrado"));

      empleado.setTipoDocumento(empleadoRequest.getTipoDocumento());
      empleado.setNumDocumento(empleadoRequest.getNumDocumento());
      empleado.setNombres(empleadoRequest.getNombres());
      empleado.setApellidos(empleadoRequest.getApellidos());
      empleado.setDireccion(empleadoRequest.getDireccion());
      empleado.setTelefono(empleadoRequest.getTelefono());
      empleado.setCelular(empleadoRequest.getCelular());
      empleado.setCargo(empleadoRequest.getCargo());

      empleadoRepository.save(empleado);
      responseModelSet.setMensaje(MENSAJE_EDITAR_OK);
      return responseModelSet;

    } catch (Exception e) {
      log.error("update(): {}", e.getMessage());
      responseModelSet.setMensaje(MENSAJE_GUARDAR_ERROR);
      responseModelSet.setError(e.getMessage());
      return responseModelSet;
    }
  }

  @Override
  public ResponseModelSet changeStatus(int idEmpleado) {
    ResponseModelSet responseModelSet = new ResponseModelSet();
    try {
      Empleado empleado = empleadoRepository.findById(idEmpleado)
          .orElseThrow(() -> new IllegalArgumentException("Empleado no encontrado"));

      boolean nuevoEstado = !Boolean.TRUE.equals(empleado.getEstado());
      empleado.setEstado(nuevoEstado);
      empleadoRepository.save(empleado);

      responseModelSet.setMensaje(nuevoEstado
          ? "Empleado activado correctamente"
          : "Empleado desactivado correctamente");
      return responseModelSet;
    } catch (Exception e) {
      log.error("changeStatus(): {}", e.getMessage());
      responseModelSet.setMensaje(MENSAJE_GUARDAR_ERROR);
      responseModelSet.setError(e.getMessage());
      return responseModelSet;
    }
  }

  private ResponseModelGet<EmpleadoResponse> buildResponse(Iterable<Empleado> empleados) {
    List<EmpleadoResponse> empleadoResponseList = new ArrayList<>();
    empleados.forEach(empleado -> empleadoResponseList.add(buildEmpleadoResponse(empleado)));

    ResponseModelGet<EmpleadoResponse> responseModelGet = new ResponseModelGet<>();
    responseModelGet.setData(empleadoResponseList);
    responseModelGet.setMensaje(Constant.MENSAJE_CONSULTA_OK);
    return responseModelGet;
  }

  private EmpleadoResponse buildEmpleadoResponse(Empleado empleado) {
    return EmpleadoResponse.builder()
        .idEmpleado(empleado.getIdEmpleado())
        .tipoDocumento(empleado.getTipoDocumento())
        .numDocumento(empleado.getNumDocumento())
        .nombres(empleado.getNombres())
        .apellidos(empleado.getApellidos())
        .direccion(empleado.getDireccion())
        .telefono(empleado.getTelefono())
        .celular(empleado.getCelular())
        .cargo(empleado.getCargo())
        .estado(empleado.getEstado())
        .build();
  }
}
