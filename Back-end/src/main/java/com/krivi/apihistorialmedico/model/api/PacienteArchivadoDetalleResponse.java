package com.krivi.apihistorialmedico.model.api;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PacienteArchivadoDetalleResponse {
  private Integer idPaciente;
  private String nombres;
  private String apellidos;
  private String dni;
  private String estadoRegistro;
  private LocalDateTime fechaArchivado;
  private String motivoArchivado;
  private String detalleMotivoArchivado;
  private Integer idAuditoria;
  private String usuarioResponsable;
  private Integer idEmpleado;
  private String empleadoResponsable;
  private String cargo;
  private String origen;
  private LocalDateTime fechaAuditoria;
  private String estadoAnterior;
  private String estadoNuevo;
  private boolean requirioRevisionClinica;
  private boolean confirmoRevisionClinica;
  private PacientePrincipalArchivadoResponse pacientePrincipal;
  private long cantidadHistoriasClinicas;
  private long cantidadConsultas;
  private long cantidadAntecedentes;
}
