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
public class DuplicadosPacientesResponse {
  private boolean hayDuplicados;
  private int totalGrupos;
  private List<GrupoDuplicadoDniResponse> duplicados;
}
