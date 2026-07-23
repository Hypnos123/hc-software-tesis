package com.krivi.apihistorialmedico.model.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusquedaHistoriasClinicasResponse {
  private boolean encontrado;
  private String tipoResultado;
  private List<HistoriaClinicaIntegracionItemResponse> historiasClinicas;
  private String mensaje;
}
