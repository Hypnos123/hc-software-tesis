package com.krivi.apihistorialmedico.model.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoriaClinicaAnalisisDetalladoResponse {
  private Integer idHistoriaClinica;
  private Integer idPaciente;
  private String dni;
  private String nombreCompleto;
  private LocalDateTime fechaCreacion;
  private LocalDateTime ultimaActualizacion;
  private long cantidadConsultas;
  private LocalDateTime ultimaActividadClinica;
  private long cantidadConsultasPendientes;
  private long cantidadConsultasAtendidas;
  private int camposClinicosInformados;
  private int puntajeRiquezaClinica;
  private long cantidadConsultasExclusivas;
  private List<ConsultaHistoriaAnalisisResponse> consultasExclusivas;
}
