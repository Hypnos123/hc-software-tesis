package com.krivi.apihistorialmedico.model.api;

import lombok.*;

@Builder
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class UsuarioResponse {

  private Integer idUsuario;
  private String usuario;
  private String contrasena;
  private String tipoUsuario;
  private Boolean estado;
  private Integer idEmpleado;
  private String apellidoYNombre;


}
