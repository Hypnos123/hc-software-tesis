package com.krivi.apihistorialmedico.model.api;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreacionHistoriaClinicaFaltanteResponse {
  private Integer idPaciente;
  private EstadoCreacionHistoriaClinicaFaltante estado;
  private Integer idHistoriaClinica;
}
