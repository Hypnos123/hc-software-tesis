package com.krivi.apihistorialmedico.model.importacion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PacienteImportacion {
  private UUID importacionId;
  private Integer usuarioId;
  private String versionPlantilla;
  private Instant fechaCreacion;
  private Instant fechaExpiracion;
  private PacienteImportacionEstado estado;
  private PacienteImportacionResumen resumen;
  @Builder.Default
  private List<PacienteImportacionFila> filas = new ArrayList<>();
}
