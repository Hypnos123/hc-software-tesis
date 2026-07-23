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
public class HistoriaClinicaIntegracionItemResponse {
  private Integer idHistoriaClinica;
  private Integer idPaciente;
  private String dni;
  private String nombreCompleto;
  private LocalDateTime fechaCreacion;
}
