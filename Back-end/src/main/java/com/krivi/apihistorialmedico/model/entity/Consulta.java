package com.krivi.apihistorialmedico.model.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "consulta")
public class Consulta {
  @Id
  @Column(name = "idconsulta")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer idConsulta;
  @Column(name = "presionarterial")
  private String presionArterial;
  @Column(name = "frecuenciacardiaca")
  private String frecuenciaCardiaca;
  @Column(name = "frecuenciarespiratoria")
  private String frecuenciaRespiratoria;
  private String talla;
  private String temperatura;
  private Double peso;
  @Column(name = "fechaconsulta")
  private Date fechaConsulta;
  @Column(name = "tiempoenfermedad")
  private String tiempoEnfermedad;
  @Column(name = "relatopaciente")
  private String relatoPaciente;
  @Column(name = "especialidadrequerida")
  private String especialidadRequerida;
  private String diagnostico;
  @Column(name = "examenesrecetados")
  private String examenesRecetados;
  private String receta;
  private String tratamiento;
  @Column(name = "proximacita")
  private Date proximaCita;
  @Column(name = "fechacreacion", nullable = false, updatable = false)
  private LocalDateTime fechaCreacion;
  private String estado;

  @ManyToOne
  @JoinColumn(name = "idpaciente", nullable = false)
  @JsonBackReference
  Paciente paciente;

  @ManyToOne
  @JoinColumn(name = "idtipoenfermedad", nullable = false)
  @JsonBackReference
  TipoEnfermedad tipoEnfermedad;

  @ManyToOne
  @JoinColumn(name = "idusuario")
  @JsonBackReference
  Usuario usuario;

  @ManyToOne
  @JoinColumn(name = "idhistoriaclinica", nullable = false)
  @JsonBackReference
  HistoriaClinica historiaClinica;

  @ManyToOne
  @JoinColumn(name = "idempleado", nullable = false)
  @JsonBackReference
  Empleado doctorResponsable;

  @PrePersist
  void prePersist() {
    if (fechaCreacion == null) fechaCreacion = LocalDateTime.now();
    if (estado == null || estado.trim().isEmpty()) estado = "PENDIENTE";
  }
}
