package com.krivi.apihistorialmedico.model.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Datos mínimos de identificación que se pueden devolver al buscar un paciente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PacienteBusquedaItemResponse {
  private Integer idPaciente;
  private String dni;
  private String nombreCompleto;
  private boolean tieneHistoriaClinica;
}
