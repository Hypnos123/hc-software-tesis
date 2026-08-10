package com.krivi.apihistorialmedico.model.api;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PacienteSinHistoriaClinicaResponse {
  private Integer idPaciente;
  private String nombreCompleto;
  private String dniEnmascarado;
}
