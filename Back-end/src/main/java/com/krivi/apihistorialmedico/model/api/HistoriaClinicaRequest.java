package com.krivi.apihistorialmedico.model.api;

import lombok.Data;

import java.time.LocalDate;

@Data
public class HistoriaClinicaRequest {
  private Integer idHistoriaClinica;
  private Integer idPaciente;
  private LocalDate fechaIngreso;
  private LocalDate fechaNacimiento;
  private String apellidos;
  private String nombres;
  private String estadoCivil;
  private String dni;
  private String enfermedadesPrevias;
  private String cirugiasPrevias;
  private String alergiaMedicamentos;
}
