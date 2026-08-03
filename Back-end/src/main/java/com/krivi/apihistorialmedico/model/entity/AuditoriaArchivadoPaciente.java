package com.krivi.apihistorialmedico.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "auditoriaarchivadopaciente")
public class AuditoriaArchivadoPaciente {
  private static final ZoneId ZONA_HORARIA_LIMA = ZoneId.of("America/Lima");

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "idauditoria")
  private Integer idAuditoria;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "idpacientearchivado", nullable = false)
  private Paciente pacienteArchivado;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "idpacienteprincipal", nullable = false)
  private Paciente pacientePrincipal;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "idusuario", nullable = false)
  private Usuario usuario;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "idempleado", nullable = false)
  private Empleado empleado;

  @Column(nullable = false, length = 65)
  private String cargo;
  @Column(nullable = false, length = 15)
  private String dni;
  @Column(nullable = false, length = 45)
  private String motivo;
  @Column(length = 500)
  private String detalle;
  @Column(name = "estadoanterior", nullable = false, length = 20)
  private String estadoAnterior;
  @Column(name = "estadonuevo", nullable = false, length = 20)
  private String estadoNuevo;
  @Column(name = "requiriorevisionclinica", nullable = false)
  private boolean requirioRevisionClinica;
  @Column(name = "confirmorevisionclinica", nullable = false)
  private boolean confirmoRevisionClinica;
  @Column(nullable = false, length = 20)
  private String origen;
  @Column(nullable = false)
  private LocalDateTime fecha;
  @Column(name = "nombrepacientearchivado", nullable = false, length = 250)
  private String nombrePacienteArchivado;
  @Column(name = "nombrepacienteprincipal", nullable = false, length = 250)
  private String nombrePacientePrincipal;
  @Column(name = "usuarioresponsable", nullable = false, length = 120)
  private String usuarioResponsable;

  @PrePersist
  void asignarFecha() {
    if (fecha == null) fecha = LocalDateTime.now(ZONA_HORARIA_LIMA);
  }
}
