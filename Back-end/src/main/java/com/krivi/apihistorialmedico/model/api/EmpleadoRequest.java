package com.krivi.apihistorialmedico.model.api;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class EmpleadoRequest {

  private Integer idEmpleado;
  private String tipoDocumento;
  private String numDocumento;
  private String nombres;
  private String apellidos;
  private String direccion;
  private String telefono;
  private String celular;

}
