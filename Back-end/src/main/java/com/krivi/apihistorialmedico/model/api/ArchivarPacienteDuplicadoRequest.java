package com.krivi.apihistorialmedico.model.api;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArchivarPacienteDuplicadoRequest {
  private Integer idPacientePrincipal;
  private String motivo;
  private String detalleMotivo;
  private String contrasena;
  private Boolean confirmarRevisionClinica;
  private String origen;

  @JsonAnySetter
  public void rechazarCampoDesconocido(String nombre, Object valor) {
    throw new IllegalArgumentException("El cuerpo contiene un campo no permitido.");
  }
}
