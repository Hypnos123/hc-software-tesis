package com.krivi.apihistorialmedico.model.api.importacion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PacienteImportacionConfirmacionResumenResponse {
  private int filasValidasEnPrevisualizacion;
  private int pacientesRegistrados;
  private int omitidosPorDniExistente;
  private int erroresAlRegistrar;
}
