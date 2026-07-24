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
public class BusquedaConsultasMedicasResponse {
  private boolean encontrado;
  private String tipoResultado;
  private List<ConsultaMedicaPacienteResponse> pacientes;
  private String mensaje;
}
