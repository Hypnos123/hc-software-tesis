package com.krivi.apihistorialmedico.model.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteMedicoPaciente {
  private Integer idPaciente;
  private String nombreCompleto;
  private String dni;
  private LocalDate fechaNacimiento;
}
