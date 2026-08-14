package com.krivi.apihistorialmedico.model.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FusionarHistoriasClinicasResponse {
  private boolean fusionada;
  private Integer idHistoriaPrincipal;
  private Integer idHistoriaEliminada;
  private Integer idPaciente;
  private long cantidadConsultasAntesPrincipal;
  private long cantidadConsultasAntesSecundaria;
  private long cantidadConsultasTransferidas;
  private long cantidadConsultasFinalPrincipal;
  private int posiblesCoincidencias;
  private Integer idAuditoria;
  private String resultado;
  private String mensaje;
}
