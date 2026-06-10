package com.krivi.apihistorialmedico.model.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "paciente")
public class Paciente {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "idpaciente")
  private Integer idPaciente;
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

  @OneToMany(mappedBy = "paciente" , cascade = CascadeType.ALL, orphanRemoval = false)
  @JsonManagedReference
  private List<Antecedentes> antecedentes;

  @OneToMany(mappedBy = "paciente" , cascade = CascadeType.ALL, orphanRemoval = false)
  @JsonManagedReference
  private List<Consulta> consultas;





}
