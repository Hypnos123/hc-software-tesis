package com.krivi.apihistorialmedico.model.api;

import com.fasterxml.jackson.annotation.JsonAnySetter;

public class ReautenticacionRequest {
  private String contrasena;

  public ReautenticacionRequest() {
  }

  public String getContrasena() {
    return contrasena;
  }

  public void setContrasena(String contrasena) {
    this.contrasena = contrasena;
  }

  @JsonAnySetter
  public void rechazarCampoDesconocido(String nombre, Object valor) {
    throw new IllegalArgumentException("El cuerpo contiene un campo no permitido.");
  }
}
