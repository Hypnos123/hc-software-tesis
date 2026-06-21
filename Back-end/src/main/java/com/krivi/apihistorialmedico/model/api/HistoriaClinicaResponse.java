package com.krivi.apihistorialmedico.model.api;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@Builder
public class HistoriaClinicaResponse {
  private Integer idHistoriaClinica;
  private LocalDateTime fechaCreacion;
  private LocalDateTime ultimaActualizacion;
  private Integer idPaciente;
  private String nombres;
  private String apellidos;
  private Date fechaIngreso;
  private Date fechaNacimiento;
  private String estadoCivil;
  private String numDocumento;
  private Integer edad;
  private String enfermedadesPrevias;
  private String cirugiasPrevias;
  private String alergiaMedicamentos;
}
