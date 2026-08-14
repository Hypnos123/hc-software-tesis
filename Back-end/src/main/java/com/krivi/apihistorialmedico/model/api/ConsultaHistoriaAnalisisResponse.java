package com.krivi.apihistorialmedico.model.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultaHistoriaAnalisisResponse {
  private Integer idConsulta;
  private String estado;
  private LocalDateTime fechaActividad;
  private Integer idEmpleado;
  private String medico;
  private String diagnosticoResumen;
  private Integer camposClinicosInformados;
  private Integer puntajeRiquezaClinica;
}
