package com.krivi.apihistorialmedico.model.api;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PacienteArchivadoResumenResponse {
  private Integer idPaciente;
  private String nombreCompleto;
  private String dni;
  private LocalDateTime fechaArchivado;
  private String usuarioResponsable;
  private String motivoArchivado;
  private String estadoRegistro;
  private Integer idPacientePrincipal;
  private String nombrePacientePrincipal;
  private Integer idAuditoria;
}
