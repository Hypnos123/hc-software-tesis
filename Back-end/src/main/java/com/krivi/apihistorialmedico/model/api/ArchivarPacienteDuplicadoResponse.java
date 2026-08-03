package com.krivi.apihistorialmedico.model.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchivarPacienteDuplicadoResponse {
  private boolean archivado;
  private Integer idPacienteArchivado;
  private Integer idPacientePrincipal;
  private String dni;
  private String estadoAnterior;
  private String estadoNuevo;
  private Integer idAuditoria;
  private String usuarioResponsable;
  private String cargoResponsable;
  private boolean requiereRevisionClinica;
  private boolean revisionClinicaConfirmada;
  private String resultado;
  private String mensaje;
}
