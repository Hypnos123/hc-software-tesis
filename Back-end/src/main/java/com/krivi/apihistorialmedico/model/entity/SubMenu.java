package com.krivi.apihistorialmedico.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name="submenu")
public class SubMenu {
  @Id
  @Column(name = "idsubmenu")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer idSubMenu;
  private String nombre;
  private String ruta;
  private String estado;

  @ManyToOne
  @JoinColumn(name = "idmenu", nullable = false)
  @JsonIgnore
  Menu menu;

}
