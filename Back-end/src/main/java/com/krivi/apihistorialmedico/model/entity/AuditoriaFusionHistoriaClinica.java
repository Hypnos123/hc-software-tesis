package com.krivi.apihistorialmedico.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter @Setter @NoArgsConstructor @Entity
@Table(name = "auditoriafusionhistoriaclinica")
public class AuditoriaFusionHistoriaClinica {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "idauditoria")
  private Integer idAuditoria;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "idhistoriaprincipal", nullable = false)
  private HistoriaClinica historiaPrincipal;
  @Column(name = "idhistoriaeliminada", nullable = false) private Integer idHistoriaEliminada;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "idpaciente", nullable = false)
  private Paciente paciente;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "idusuario", nullable = false)
  private Usuario usuario;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "idempleado", nullable = false)
  private Empleado empleado;
  @Column(nullable = false, length = 65) private String cargo;
  @Column(nullable = false, length = 20) private String origen;
  @Column(nullable = false, length = 45) private String motivo;
  @Column(length = 500) private String detalle;
  @Column(name = "consultasantesprincipal", nullable = false) private long consultasAntesPrincipal;
  @Column(name = "consultasantessecundaria", nullable = false) private long consultasAntesSecundaria;
  @Column(name = "consultastransferidas", nullable = false) private long consultasTransferidas;
  @Column(name = "consultasdespuesprincipal", nullable = false) private long consultasDespuesPrincipal;
  @Column(nullable = false, length = 30) private String resultado;
  @Column(nullable = false) private LocalDateTime fecha;
  @PrePersist void prePersist() { if (fecha == null) fecha = LocalDateTime.now(ZoneId.of("America/Lima")); }
}
