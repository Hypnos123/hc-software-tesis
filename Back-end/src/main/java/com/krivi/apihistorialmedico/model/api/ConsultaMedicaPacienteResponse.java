package com.krivi.apihistorialmedico.model.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConsultaMedicaPacienteResponse {
  private Integer idPaciente;
  private String dni;
  private String nombreCompleto;
  private Boolean tieneHistoriaClinica;
  private Long totalConsultas;
  private Long consultasPendientes;
  private Long consultasAtendidas;
  private List<ConsultaMedicaAdministrativaResponse> consultas;
}
