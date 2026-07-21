package com.krivi.apihistorialmedico.model.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Resultado de una búsqueda segura de pacientes para integraciones externas.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BusquedaPacienteResponse {
  private boolean encontrado;
  private String tipoResultado;
  private List<PacienteBusquedaItemResponse> pacientes;
  private String mensaje;
}
