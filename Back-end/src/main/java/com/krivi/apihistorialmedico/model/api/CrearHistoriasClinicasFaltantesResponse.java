package com.krivi.apihistorialmedico.model.api;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CrearHistoriasClinicasFaltantesResponse {
  private int totalSolicitados;
  private int totalProcesados;
  private int creadas;
  private int omitidas;
  private int noEncontrados;
  private int inactivos;
  private int errores;
  private List<CreacionHistoriaClinicaFaltanteResponse> resultados;
}
