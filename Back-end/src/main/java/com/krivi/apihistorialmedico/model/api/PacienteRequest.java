package com.krivi.apihistorialmedico.model.api;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PacienteRequest {

  private Integer idPaciente;
  private String nombres;
  private String apellidos;
  private Date fechaIngreso;
  private Date fechaNacimiento;
  private Integer edad;
  private String estadoCivil;
  private String numDocumento;
  private String sexo;
  private String direccion;
  private String distrito;
  private String traidoPor;


}
