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
@Table(name = "tipoenfermedad")
public class TipoEnfermedad {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "idtipoenfermedad")
  private Integer idTipoEnfermedad;
  private String descripcion;

  @OneToMany(mappedBy = "tipoEnfermedad" , cascade = CascadeType.ALL, orphanRemoval = false)
  @JsonManagedReference
  private List<Consulta> consultas;

  public TipoEnfermedad(Integer idTipoEnfermedad) {
    this.idTipoEnfermedad = idTipoEnfermedad;
  }
}
