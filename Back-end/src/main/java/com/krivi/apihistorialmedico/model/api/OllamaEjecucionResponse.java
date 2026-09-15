package com.krivi.apihistorialmedico.model.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OllamaEjecucionResponse(
    String categoria,
    String intencion,
    Boolean encontrado,
    String dni,
    String nombre,
    List<PacienteResponse> pacientes,
    DuplicadosPacientesResponse gruposDuplicados,
    PacienteDuplicadoComparacionResponse comparacionDuplicados,
    List<HistoriaClinicaIntegracionItemResponse> historias,
    DuplicadosHistoriasClinicasResponse gruposHistoriasDuplicadas,
    String mensaje
) {
  public OllamaEjecucionResponse(
      String categoria,
      String intencion,
      Boolean encontrado,
      String dni,
      String mensaje
  ) {
    this(categoria, intencion, encontrado, dni, null, null, null, null, null, null, mensaje);
  }

  public OllamaEjecucionResponse(
      String categoria,
      String intencion,
      Boolean encontrado,
      String dni,
      String nombre,
      List<PacienteResponse> pacientes,
      String mensaje
  ) {
    this(categoria, intencion, encontrado, dni, nombre, pacientes, null, null, null, null,
        mensaje);
  }
}
