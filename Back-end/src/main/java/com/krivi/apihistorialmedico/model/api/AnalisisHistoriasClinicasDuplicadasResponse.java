package com.krivi.apihistorialmedico.model.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalisisHistoriasClinicasDuplicadasResponse {
  private String tipoDuplicidad;
  private Integer idHistoriaClinicaRecomendada;
  private List<String> motivosRecomendacion;
  private String resumenComparativo;
  private List<HistoriaClinicaAnalisisDetalladoResponse> historiasComparadas;
  private List<PosibleCoincidenciaConsultaResponse> posiblesCoincidencias;
  private boolean futuraFusionPermitida;
  private String motivoBloqueo;
  private List<String> advertenciasIntegridad;
  private String mensaje;
  private String tokenAnalisis;
}
