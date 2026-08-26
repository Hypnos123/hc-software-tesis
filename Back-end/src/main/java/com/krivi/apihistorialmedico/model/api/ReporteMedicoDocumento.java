package com.krivi.apihistorialmedico.model.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteMedicoDocumento {
  private ReporteConsultaAlcance alcance;
  private LocalDate fecha;
  private LocalDate fechaDesde;
  private LocalDate fechaHasta;
  private long totalConsultasEncontradas;
  private long consultasAtendidasIncluidas;
  private long consultasNoAtendidasExcluidas;
  private ReporteMedicoPaciente paciente;
  private List<Integer> idsHistoriasClinicasIncluidas;
  private List<ReporteMedicoConsulta> consultas;
}
