package com.krivi.apihistorialmedico.model.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteMedicoConsulta {
  private Integer idConsulta;
  private Integer idHistoriaClinica;
  private LocalDateTime fechaEfectiva;
  private OrigenFechaConsultaReporte origenFechaEfectiva;
  private Integer edadPaciente;
  private String especialidad;
  private String medicoResponsable;
  private String diagnostico;
  private String examenesRecetados;
  private String receta;
  private String tratamiento;
  private LocalDate proximaCita;
}
