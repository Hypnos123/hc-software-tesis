package com.krivi.apihistorialmedico.model.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "empleado")
public class Empleado {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "idempleado")
  private Integer idEmpleado;
  @Column(name = "tipodocumento")
  private String tipoDocumento;
  @Column(name = "numdocumento")
  private String numDocumento;
  private String nombres;
  private String apellidos;
  private String direccion;
  private String telefono;
  private String celular;
  private String cargo;
  private Boolean estado;

  public Empleado(Integer idEmpleado) {
    this.idEmpleado = idEmpleado;
  }

  @OneToMany(mappedBy = "empleado" , cascade = CascadeType.ALL, orphanRemoval = false)
  @JsonManagedReference
  private List<Usuario> usuarios;


}
