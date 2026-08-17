package com.krivi.apihistorialmedico.model.api;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PacientePrincipalArchivadoResponse {
  private Integer idPaciente;
  private String nombreCompleto;
  private String dni;
  private String estadoRegistro;
}
