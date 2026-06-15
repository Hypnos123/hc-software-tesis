package com.krivi.apihistorialmedico.model.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmpleadoRequest {

  private Integer idEmpleado;
  private String tipoDocumento;
  private String numDocumento;
  private String nombres;
  private String apellidos;
  private String direccion;
  private String telefono;
  private String celular;
  private String cargo;
  private Boolean estado;

}
