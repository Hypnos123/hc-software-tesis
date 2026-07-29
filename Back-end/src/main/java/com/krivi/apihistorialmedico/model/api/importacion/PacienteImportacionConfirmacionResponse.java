package com.krivi.apihistorialmedico.model.api.importacion;

import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionEstado;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PacienteImportacionConfirmacionResponse {
  private UUID importacionId;
  private PacienteImportacionEstado estado;
  private int registrados;
  private int noRegistrados;
  @Builder.Default
  private List<PacienteImportacionResultadoRegistroResponse> resultados = new ArrayList<>();
}
