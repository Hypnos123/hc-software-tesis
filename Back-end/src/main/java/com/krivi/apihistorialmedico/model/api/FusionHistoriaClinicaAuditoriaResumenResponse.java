package com.krivi.apihistorialmedico.model.api;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FusionHistoriaClinicaAuditoriaResumenResponse {
  private Integer idAuditoria;
  private Integer idPaciente;
  private String nombrePaciente;
  private String dni;
  private Integer idHistoriaPrincipal;
  private Integer idHistoriaEliminada;
  private long consultasTransferidas;
  private LocalDateTime fecha;
  private String usuarioResponsable;
  private String resultado;
}
