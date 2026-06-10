package com.krivi.apihistorialmedico.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name ="detallepermiso")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DetallePermiso {

  @Id
  @Column(name = "iddetallepermiso")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer idDetallePermiso;

  @Column(name = "idusuario")
  private Integer idUsuario;

  @Column(name = "idmenu")
  private Integer idMenu;


}
