package com.krivi.apihistorialmedico.model.api.importacion;

import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionErrorCodigo;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionFilaEstado;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PacienteImportacionResultadoRegistroResponse {
  private int numeroFila;
  private String dni;
  private PacienteImportacionFilaEstado estado;
  private Integer idPaciente;
  @Builder.Default
  private java.util.List<PacienteImportacionErrorResponse> errores = new java.util.ArrayList<>();
}
