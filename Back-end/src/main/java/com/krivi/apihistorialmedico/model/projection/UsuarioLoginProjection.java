package com.krivi.apihistorialmedico.model.projection;

public interface UsuarioLoginProjection {

  Integer getIdUsuario();
  String getUsuario();
  String getTipoUsuario();

  Integer getEstadoUsuario();

  Integer getIdEmpleado();
  String getNombres();
  String getApellidos();
  String getCargo();

  Integer getEstadoEmpleado();
}
