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
public class GrupoDuplicadoDniResponse {
  private String tipo;
  private String valorCoincidente;
  private int cantidad;
  private List<PacienteDuplicadoItemResponse> pacientes;
}
