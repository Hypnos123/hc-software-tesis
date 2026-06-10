package com.krivi.apihistorialmedico.model.api;


import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UsuarioRequest {

  private Integer idUsuario;
  private String usuario;
  private String contrasena;
  private String tipoUsuario;
  private Integer idEmpleado;
}
