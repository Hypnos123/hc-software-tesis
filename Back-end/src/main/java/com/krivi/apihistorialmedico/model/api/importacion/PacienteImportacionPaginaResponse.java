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
public class PacienteImportacionPaginaResponse {
  private int pagina;
  private int tamano;
  private long totalElementos;
  private int totalPaginas;
  @Builder.Default
  private List<PacienteImportacionFilaResumenResponse> contenido = new ArrayList<>();
}
