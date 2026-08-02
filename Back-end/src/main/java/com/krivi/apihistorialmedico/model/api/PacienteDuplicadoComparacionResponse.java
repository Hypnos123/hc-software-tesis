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
public class PacienteDuplicadoComparacionResponse {
  private String dni;
  private int cantidadPacientesActivos;
  private boolean esDuplicado;
  private List<PacienteDuplicadoDetalleResponse> pacientes;
  private Integer idPacienteRecomendado;
  private List<String> razonesRecomendacion;
  private boolean permitirArchivadoSimple;
  private boolean requiereRevision;
  private String resultado;
  private String mensaje;
  private String advertencia;
}
