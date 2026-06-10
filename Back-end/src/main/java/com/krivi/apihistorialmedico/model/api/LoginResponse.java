package com.krivi.apihistorialmedico.model.api;



import com.krivi.apihistorialmedico.model.projection.DetallePermisoLoginProjection;
import com.krivi.apihistorialmedico.model.projection.UsuarioLoginProjection;

import java.util.List;


public class LoginResponse {

 UsuarioLoginProjection usuario;
 List<DetallePermisoLoginProjection> detallePermisos;

  public LoginResponse() {
  }

  public UsuarioLoginProjection getUsuario() {
    return usuario;
  }

  public void setUsuario(UsuarioLoginProjection usuario) {
    this.usuario = usuario;
  }

  public List<DetallePermisoLoginProjection> getDetallePermisos() {
    return detallePermisos;
  }

  public void setDetallePermisos(List<DetallePermisoLoginProjection> detallePermisos) {
    this.detallePermisos = detallePermisos;
  }
}
