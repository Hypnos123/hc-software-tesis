package com.krivi.apihistorialmedico.model.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;


@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ResponseModelGet<T> {

  private List<T> data;
  private String mensaje;
  private String error;

  public ResponseModelGet() {
  }

  public ResponseModelGet(List<T> data, String mensaje, String error) {
    this.data = data;
    this.mensaje = mensaje;
    this.error = error;
  }

  public List<T> getData() {
    return data;
  }

  public void setData(List<T> data) {
    this.data = data;
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
}
