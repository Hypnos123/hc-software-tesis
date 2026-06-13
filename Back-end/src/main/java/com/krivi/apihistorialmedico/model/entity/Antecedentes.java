package com.krivi.apihistorialmedico.model.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "antecedentes")
public class Antecedentes {
  @Id
  @Column(name = "idantecedentes")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer idAntecedentes;
  private String alimentacion;
  private String habitos;
  private String vivienda;
  @Column(name = "desarrollopsicomotor")
  private String desarrolloPsicomotor;
  private String vacunas;
  private String educacion;
  @Column(name = "enfermedadesprevias")
  private String enfermedadesPrevias;
  @Column(name = "cirugiasprevias")
  private String cirugiasPrevias;
  @Column(name = "alergiamedicamentos" )
  private String alergiaMedicamentos;

  @ManyToOne
  @JoinColumn(name = "idpaciente", nullable = false)
  @JsonBackReference
  Paciente paciente;




}
