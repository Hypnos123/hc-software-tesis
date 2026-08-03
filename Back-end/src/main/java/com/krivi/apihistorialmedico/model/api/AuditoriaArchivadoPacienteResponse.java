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
public class AuditoriaArchivadoPacienteResponse {
  private Integer idAuditoria;
  private Integer idPacienteArchivado;
  private Integer idPacientePrincipal;
  private Integer idUsuario;
  private Integer idEmpleado;
  private String cargo;
  private String dni;
  private String motivo;
  private String detalle;
  private String estadoAnterior;
  private String estadoNuevo;
  private boolean requirioRevisionClinica;
  private boolean confirmoRevisionClinica;
  private String origen;
  private LocalDateTime fecha;
  private String nombrePacienteArchivado;
  private String nombrePacientePrincipal;
  private String usuarioResponsable;
}
