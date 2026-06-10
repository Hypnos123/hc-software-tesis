package com.krivi.apihistorialmedico.business.services.impl;


import com.krivi.apihistorialmedico.business.services.UsuarioService;
import com.krivi.apihistorialmedico.model.api.*;
import com.krivi.apihistorialmedico.model.entity.Empleado;
import com.krivi.apihistorialmedico.model.entity.Usuario;
import com.krivi.apihistorialmedico.model.projection.Id;
import com.krivi.apihistorialmedico.model.projection.UsuarioLoginProjection;
import com.krivi.apihistorialmedico.repository.DetallePermisoRepository;
import com.krivi.apihistorialmedico.repository.UsuarioRepository;
import com.krivi.apihistorialmedico.util.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.krivi.apihistorialmedico.util.Constant.MENSAJE_GUARDAR_ERROR;
import static com.krivi.apihistorialmedico.util.Constant.MENSAJE_GUARDAR_OK;


@Slf4j
@Service
public class UsuarioServiceImpl implements UsuarioService {
  @Autowired
  UsuarioRepository usuarioRepository;

  @Autowired
  DetallePermisoRepository detallePermisoRepository;

  @Override
  public List<UsuarioResponse> getAllActive() {
    List<UsuarioResponse> usuarioResponseList = new ArrayList<>();
    try {

      usuarioRepository.findByEstadoOrderByIdUsuarioDesc(true).forEach(usuario -> {
        usuarioResponseList.add(UsuarioResponse.builder()
            .idUsuario(usuario.getIdUsuario())
            .usuario(usuario.getUsuario())
            .contrasena(usuario.getContrasena())
            .tipoUsuario(usuario.getTipoUsuario())
            .estado(usuario.getEstado())
            .idEmpleado(usuario.getEmpleado().getIdEmpleado())
            .apellidoYNombre(usuario.getEmpleado().getApellidos() + " " + usuario.getEmpleado().getNombres())
            .build());

      });

      return usuarioResponseList;
    }catch(Exception ex){
      log.error(ex.getMessage());
      return usuarioResponseList;
    }
  }

  @Override
  public UsuarioResponse findById(int idUsuario) {
     Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);

    return UsuarioResponse.builder()
        .idUsuario(usuario.getIdUsuario())
        .usuario(usuario.getUsuario())
        .contrasena(usuario.getContrasena())
        .idEmpleado(usuario.getEmpleado().getIdEmpleado())
        .tipoUsuario(usuario.getTipoUsuario())
        .estado(usuario.getEstado())
        .apellidoYNombre(usuario.getEmpleado().getApellidos() + " " +usuario.getEmpleado().getNombres())
        .build();
  }


  @Override
  public ResponseModelSet save(UsuarioRequest usuarioRequest) {
    ResponseModelSet responseModelSet = new ResponseModelSet();
    try {

      if (usuarioRequest.getUsuario() != null && !usuarioRequest.getUsuario().isEmpty()) {
      Optional<Usuario> usuarioExistente = usuarioRepository.findByUsuarioAndEstado(usuarioRequest.getUsuario().trim(),true);
      if (usuarioExistente.isPresent()) {
        responseModelSet.setMensaje("El usuario ya existe.");
        responseModelSet.setError("El usuario ya existe.");
        return responseModelSet;
      }}

      Usuario usuario = new Usuario();
      usuario.setIdUsuario(null);
      usuario.setUsuario(usuarioRequest.getUsuario().trim());
      usuario.setContrasena(usuarioRequest.getContrasena().trim());
      usuario.setTipoUsuario(usuarioRequest.getTipoUsuario().trim());
      usuario.setEmpleado(new Empleado(usuarioRequest.getIdEmpleado()));
      usuario.setEstado(true);

      Usuario usuarioResponse = usuarioRepository.save(usuario);

      responseModelSet.setIdGenerado(usuarioResponse.getIdUsuario());

      responseModelSet.setMensaje(MENSAJE_GUARDAR_OK);

      return responseModelSet;
    } catch (Exception e) {
      log.error("save(): " + e.getMessage());
      responseModelSet.setMensaje(Constant.MENSAJE_GUARDAR_ERROR);
      responseModelSet.setError(e.getMessage());
      return responseModelSet;
    }
  }

  @Override
  public ResponseModelSet update(UsuarioRequest usuarioRequest) {
    ResponseModelSet responseModelSet = new ResponseModelSet();


    if (!Objects.requireNonNullElse(usuarioRequest.getUsuario(), "").isEmpty()) {
      Usuario usuarioRegistrado = usuarioRepository.findById(usuarioRequest.getIdUsuario()).orElse(new Usuario());
      Optional<Usuario> usuarioExistente = usuarioRepository.findByUsuarioAndEstado(usuarioRequest.getUsuario(), true);
      if (usuarioExistente.isPresent()) {
        if (!usuarioRegistrado.getUsuario().equals(usuarioExistente.get().getUsuario())) {
          responseModelSet.setMensaje("Ya existe un Usuario con este número de documento.");
          responseModelSet.setError("Ya existe un Usuario con este número de documento.");
          return responseModelSet;
        }
      }
    }

    try {
      Usuario usuario = new Usuario();
      usuario.setIdUsuario(usuarioRequest.getIdUsuario());
      usuario.setUsuario(usuarioRequest.getUsuario().trim());
      usuario.setContrasena(usuarioRequest.getContrasena().trim());
      usuario.setTipoUsuario(usuarioRequest.getTipoUsuario().trim());
      usuario.setEmpleado(new Empleado(usuarioRequest.getIdEmpleado()));
      usuario.setEstado(true);

      usuarioRepository.save(usuario);


      responseModelSet.setMensaje(MENSAJE_GUARDAR_OK);

      return responseModelSet;
    } catch (Exception e) {
      log.error("save(): " + e.getMessage());
      responseModelSet.setMensaje(MENSAJE_GUARDAR_ERROR);
      responseModelSet.setError(e.getMessage());
      return responseModelSet;
    }
  }

  @Override
  public void setInactive(int idUsuario) {
    usuarioRepository.setInactive(idUsuario);
  }

  @Override
  public List<Id> getUltimoRegistro() {
    return usuarioRepository.getUltimoRegistro();
  }

  @Override
  public ResponseModelGet<LoginResponse> login(String usuario, String contrasena) {

    LoginResponse loginResponse = new LoginResponse();
    UsuarioLoginProjection usuarioLoginProjection = usuarioRepository.getLogin(usuario, contrasena).get(0);
    loginResponse.setUsuario(usuarioLoginProjection);
    loginResponse.setDetallePermisos(detallePermisoRepository.getDetallePermisoLogin(usuarioLoginProjection.getIdUsuario()));

    ResponseModelGet<LoginResponse> responseModelGet = new ResponseModelGet<>();
    responseModelGet.setData(Arrays.asList(loginResponse));
    responseModelGet.setMensaje(Constant.MENSAJE_CONSULTA_OK);
    return responseModelGet;
  }
}
