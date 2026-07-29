package com.krivi.apihistorialmedico.model.api.importacion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PacienteImportacionValidacionResponse {
  private PacienteImportacionResumenResponse resumen;
  @Builder.Default
  private List<PacienteImportacionFilaDetalleResponse> filas = new ArrayList<>();
}
