package com.krivi.apihistorialmedico.model.importacion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PacienteImportacionDatos {
  private String apellidos;
  private String nombres;
  private LocalDate fechaNacimiento;
  private String estadoCivil;
  private String dni;
  private String sexo;
  private String direccion;
  private String distrito;
  private String traidoPor;
}
