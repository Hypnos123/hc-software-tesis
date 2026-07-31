package com.krivi.apihistorialmedico.model.api.importacion;

import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionFilaEstado;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PacienteImportacionFilaResumenResponse {
  private int numeroFila;
  private String nombreCompleto;
  private String dni;
  private PacienteImportacionFilaEstado estado;
  private int cantidadErrores;
  private int cantidadAdvertencias;
  private String observaciones;
}
