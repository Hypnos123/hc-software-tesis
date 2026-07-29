package com.krivi.apihistorialmedico.model.api.importacion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.time.Instant;
import java.util.UUID;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionEstado;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PacienteImportacionValidacionResponse {
  private UUID importacionId;
  private PacienteImportacionEstado estado;
  private Instant expiraEn;
  private PacienteImportacionResumenResponse resumen;
  @Builder.Default
  private List<PacienteImportacionFilaDetalleResponse> filas = new ArrayList<>();
}
