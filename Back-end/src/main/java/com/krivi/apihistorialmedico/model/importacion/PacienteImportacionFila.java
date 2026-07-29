package com.krivi.apihistorialmedico.model.importacion;

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
public class PacienteImportacionFila {
  private int numeroFila;
  private PacienteImportacionFilaEstado estado;
  private PacienteImportacionDatos paciente;
  private PacienteImportacionAntecedentes antecedentes;
  @Builder.Default
  private List<PacienteImportacionError> errores = new ArrayList<>();
  @Builder.Default
  private List<PacienteImportacionAdvertencia> advertencias = new ArrayList<>();
}
