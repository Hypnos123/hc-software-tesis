package com.krivi.apihistorialmedico.model.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultaDniResponse {
  private String dni;
  private String nombres;
  private String apellidos;
  private Date fechaNacimiento;
  private Integer edad;
  private String sexo;
  private String fuente;
}
