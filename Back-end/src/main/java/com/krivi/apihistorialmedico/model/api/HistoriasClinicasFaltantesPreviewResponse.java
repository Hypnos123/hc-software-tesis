package com.krivi.apihistorialmedico.model.api;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class HistoriasClinicasFaltantesPreviewResponse {
  private int cantidad;
  private List<PacienteSinHistoriaClinicaResponse> pacientes;
}
