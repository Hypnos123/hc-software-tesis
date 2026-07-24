package com.krivi.apihistorialmedico.model.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultaMedicaAdministrativaResponse {
  private Integer idConsulta;
  private Integer idPaciente;
  private Integer idHistoriaClinica;
  private String dni;
  private String nombreCompleto;
  private Integer idEmpleado;
  private String nombreMedico;
  private String especialidad;
  private LocalDateTime fechaCreacion;
  private LocalDateTime fechaAtencion;
  private String estado;
}
