package com.krivi.apihistorialmedico.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "historiaclinica", uniqueConstraints = @UniqueConstraint(name = "uk_historiaclinica_paciente", columnNames = "idpaciente"))
public class HistoriaClinica {
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
    LocalDateTime now = LocalDateTime.now();
    fechaCreacion = now;
    ultimaActualizacion = now;
  }

  @PreUpdate
  void preUpdate() {
    ultimaActualizacion = LocalDateTime.now();
  }
}
