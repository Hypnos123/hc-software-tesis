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
public class EstadisticasConsultasMedicasResponse {
  private LocalDate fecha;
  private long totalConsultas;
  private long creadasHoy;
  private long atendidasHoy;
  private long totalPendientes;
  private long totalAtendidas;
}
