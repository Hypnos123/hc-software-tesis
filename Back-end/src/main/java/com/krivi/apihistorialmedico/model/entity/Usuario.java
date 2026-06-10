package com.krivi.apihistorialmedico.model.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
@Table(name="usuario")
public class Usuario {

  @Id
  @Column(name = "idusuario")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer idUsuario;
  private String usuario;
  private String contrasena;
  @Column(name = "tipousuario")
  private String tipoUsuario;
  private Boolean estado;

  @ManyToOne
  @JoinColumn(name = "idempleado", nullable = false)
  @JsonBackReference
  private Empleado empleado;

  @OneToMany(mappedBy = "usuario" , cascade = CascadeType.ALL, orphanRemoval = false)
  @JsonManagedReference
  private List<Consulta> consultas;


  public Usuario(Integer idUsuario) {
    this.idUsuario = idUsuario;
  }
}
