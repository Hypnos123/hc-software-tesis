package com.krivi.apihistorialmedico.model.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioRequest {

  private Integer idUsuario;
  private String usuario;
  private String contrasena;
  private String tipoUsuario;
  private Integer idEmpleado;
}