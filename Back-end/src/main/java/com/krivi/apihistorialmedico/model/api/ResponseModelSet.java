package com.krivi.apihistorialmedico.model.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ResponseModelSet {

  private String mensaje;
  private String error;
  private Integer idGenerado;

  public ResponseModelSet() {
  }

  public ResponseModelSet(String mensaje, String error, Integer idGenerado) {
    this.mensaje = mensaje;
    this.error = error;
    this.idGenerado = idGenerado;
  }

  public String getMensaje() {
    return mensaje;
  }

  public void setMensaje(String mensaje) {
    this.mensaje = mensaje;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public Integer getIdGenerado() {
    return idGenerado;
  }

  public void setIdGenerado(Integer idGenerado) {
    this.idGenerado = idGenerado;
  }
}
