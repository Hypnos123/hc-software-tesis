package com.krivi.apihistorialmedico.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "historiaclinica", uniqueConstraints = @UniqueConstraint(name = "uk_historiaclinica_paciente", columnNames = "idpaciente"))
public class HistoriaClinica {
  private static final ZoneId ZONA_HORARIA_LIMA = ZoneId.of("America/Lima");
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "idhistoriaclinica")
  private Integer idHistoriaClinica;

  @Column(name = "fechacreacion", nullable = false, updatable = false)
  private LocalDateTime fechaCreacion;

  @Column(name = "ultimaactualizacion", nullable = false)
  private LocalDateTime ultimaActualizacion;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "idpaciente", nullable = false)
  private Paciente paciente;

  @PrePersist
  void prePersist() {
    LocalDateTime now = LocalDateTime.now(ZONA_HORARIA_LIMA);
    if (fechaCreacion == null) {
      fechaCreacion = now;
    }
    ultimaActualizacion = now;
  }

  @PreUpdate
  void preUpdate() {
    ultimaActualizacion = LocalDateTime.now(ZONA_HORARIA_LIMA);
  }
}
