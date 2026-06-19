package com.krivi.apihistorialmedico.model.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioLoginResponse {

  private Integer idUsuario;
  private String usuario;
  private String tipoUsuario;
  private Boolean estadoUsuario;
  private Integer idEmpleado;
  private String nombres;
  private String apellidos;
  private String cargo;
  private Boolean estadoEmpleado;

}
