package com.krivi.apihistorialmedico.model.api.importacion;

import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionEstado;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PacienteImportacionPreviewResponse {
  private UUID importacionId;
  private PacienteImportacionEstado estado;
  private Instant fechaExpiracion;
  private PacienteImportacionResumenResponse resumen;
  private PacienteImportacionPaginaResponse primeraPagina;
}
