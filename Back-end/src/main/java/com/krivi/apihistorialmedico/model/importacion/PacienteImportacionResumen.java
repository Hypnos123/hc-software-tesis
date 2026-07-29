package com.krivi.apihistorialmedico.model.importacion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PacienteImportacionResumen {
  private int registrosAnalizados;
  private int validos;
  private int conErrores;
  private int filasConDniDuplicado;
  private int gruposDniDuplicados;
  private int dniExistentes;
  private int conAdvertencias;
  private int filasVaciasIgnoradas;
}
