package com.krivi.apihistorialmedico.model.api.importacion;

import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionAntecedentes;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionDatos;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionFilaEstado;
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
public class PacienteImportacionFilaDetalleResponse {
  private int numeroFila;
  private String nombreCompleto;
  private String dni;
  private PacienteImportacionFilaEstado estado;
  private PacienteImportacionDatos paciente;
  private PacienteImportacionAntecedentes antecedentes;
  @Builder.Default
  private List<PacienteImportacionErrorResponse> errores = new ArrayList<>();
  @Builder.Default
  private List<PacienteImportacionAdvertenciaResponse> advertencias = new ArrayList<>();
}
