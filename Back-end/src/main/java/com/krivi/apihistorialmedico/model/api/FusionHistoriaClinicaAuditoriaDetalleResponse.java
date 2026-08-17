package com.krivi.apihistorialmedico.model.api;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FusionHistoriaClinicaAuditoriaDetalleResponse {
  private Integer idAuditoria;
  private LocalDateTime fecha;
  private String resultado;
  private String origen;
  private String motivo;
  private String detalle;
  private Integer idPaciente;
  private String nombrePaciente;
  private String dni;
  private Integer idHistoriaPrincipal;
  private Integer idHistoriaEliminada;
  private long consultasAntesPrincipal;
  private long consultasAntesSecundaria;
  private long consultasTransferidas;
  private long consultasDespuesPrincipal;
  private Integer idUsuario;
  private String usuarioResponsable;
  private Integer idEmpleado;
  private String empleadoResponsable;
  private String cargo;
  private String explicacion;
}
