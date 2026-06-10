package com.krivi.apihistorialmedico.model.api;

import lombok.Builder;
import lombok.Data;

import java.util.Date;


@Data
@Builder
public class PacienteResponse {


  private Integer idPaciente;
  private String nombres;
  private String apellidos;
  private Date fechaIngreso;
  private Date fechaNacimiento;
  private String estadoCivil;
  private String numDocumento;
  private String sexo;
  private String direccion;
  private String distrito;
  private String traidoPor;


}
