package com.krivi.apihistorialmedico.model.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteConsultaFiltroRequest {
  private ReporteConsultaAlcance alcance;
  private LocalDate fecha;
  private LocalDate fechaDesde;
  private LocalDate fechaHasta;
}
