package com.krivi.apihistorialmedico.model.api;

import com.krivi.apihistorialmedico.model.projection.DetallePermisoLoginProjection;

import java.util.List;

public class LoginResponse {

  private UsuarioLoginResponse usuario;
  private List<DetallePermisoLoginProjection> detallePermisos;

  public LoginResponse() {
  }

  public UsuarioLoginResponse getUsuario() {
    return usuario;
  }

  public void setUsuario(UsuarioLoginResponse usuario) {
    this.usuario = usuario;
  }

  public List<DetallePermisoLoginProjection> getDetallePermisos() {
    return detallePermisos;
  }

  public void setDetallePermisos(List<DetallePermisoLoginProjection> detallePermisos) {
    this.detallePermisos = detallePermisos;
  }
}
