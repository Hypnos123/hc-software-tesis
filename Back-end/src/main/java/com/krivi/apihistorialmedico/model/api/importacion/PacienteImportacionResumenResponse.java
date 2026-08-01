package com.krivi.apihistorialmedico.model.api.importacion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PacienteImportacionResumenResponse {
  private int registrosAnalizados;
  private int validos;
  private int conErrores;
  private int filasConDniDuplicado;
  private int gruposDniDuplicados;
  private int dniExistentes;
  private int conAdvertencias;
  private int filasVaciasIgnoradas;
}
