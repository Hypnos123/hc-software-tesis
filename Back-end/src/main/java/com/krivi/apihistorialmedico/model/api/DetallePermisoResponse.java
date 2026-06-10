package com.krivi.apihistorialmedico.model.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DetallePermisoResponse {

  Integer idDetallePermisos;
  Integer idUsuario;
  Integer idMenu;
  String nombre;
  String usuario;


}
