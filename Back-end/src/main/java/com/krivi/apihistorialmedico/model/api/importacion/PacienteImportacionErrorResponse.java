package com.krivi.apihistorialmedico.model.api.importacion;

import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionErrorCodigo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PacienteImportacionErrorResponse {
  private int numeroFila;
  private PacienteImportacionErrorCodigo codigo;
  private String campo;
  private String mensaje;
}
