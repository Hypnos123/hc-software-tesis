package com.krivi.apihistorialmedico.business.services;


import com.krivi.apihistorialmedico.model.api.*;
import com.krivi.apihistorialmedico.model.projection.Id;

import java.util.List;

public interface UsuarioService {

  List<UsuarioResponse> getAllActive();

  UsuarioResponse findById(int idUsuario);

  ResponseModelSet save(UsuarioRequest usuarioRequest);


  ResponseModelSet update(UsuarioRequest usuarioRequest);



  void setInactive(int idUsuario);

  List<Id> getUltimoRegistro();

  ResponseModelGet<LoginResponse> login(String usuario, String contrasena);


}
