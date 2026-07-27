package com.krivi.apihistorialmedico.model.api;

import lombok.Data;

import java.time.LocalDate;

@Data
public class HistoriaClinicaUpdateRequest {
  private LocalDate fechaIngreso;
  private LocalDate fechaNacimiento;
  private String apellidos;
  private String nombres;
  private String estadoCivil;
  private String enfermedadesPrevias;
  private String cirugiasPrevias;
  private String alergiaMedicamentos;
}
