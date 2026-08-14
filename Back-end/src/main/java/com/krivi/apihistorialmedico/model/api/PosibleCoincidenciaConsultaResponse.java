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
public class PosibleCoincidenciaConsultaResponse {
  private String clasificacion;
  private Integer idConsultaA;
  private Integer idHistoriaClinicaA;
  private Integer idConsultaB;
  private Integer idHistoriaClinicaB;
  private List<String> criteriosCoincidentes;
  private String advertencia;
}
