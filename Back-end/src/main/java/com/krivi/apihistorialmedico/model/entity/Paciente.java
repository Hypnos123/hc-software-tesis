package com.krivi.apihistorialmedico.model.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "paciente")
public class Paciente {

  private static final ZoneId ZONA_HORARIA_LIMA = ZoneId.of("America/Lima");

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "idpaciente")
  private Integer idPaciente;
  @Column(name = "fechacreacion", nullable = false, updatable = false)
  private LocalDateTime fechaCreacion;
  private String nombres;
  private String apellidos;
  @Column(name = "fechaingreso")
  private Date fechaIngreso;
  @Column(name = "fechanacimiento")
  private Date fechaNacimiento;
  @Column(name = "estadocivil")
  private String estadoCivil;
  @Column(name = "numdocumento")
  private String numDocumento;
  private String sexo;
  private String direccion;
  private String distrito;
  @Column(name = "traidopor")
  private String traidoPor;

  public Paciente(Integer idPaciente) {
    this.idPaciente = idPaciente;
  }

  @PrePersist
  void asignarFechaCreacion() {
    if (fechaCreacion == null) {
      fechaCreacion = LocalDateTime.now(ZONA_HORARIA_LIMA);
    }
  }

  @OneToMany(mappedBy = "paciente" , cascade = CascadeType.ALL, orphanRemoval = false)
  @JsonManagedReference
  private List<Antecedentes> antecedentes;

  @OneToMany(mappedBy = "paciente" , cascade = CascadeType.ALL, orphanRemoval = false)
  @JsonManagedReference
  private List<Consulta> consultas;





}
