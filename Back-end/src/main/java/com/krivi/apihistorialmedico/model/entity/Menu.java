package com.krivi.apihistorialmedico.model.entity;

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
@Table(name = "menu")
public class Menu {

  @Id
  @Column(name = "idmenu")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer idMenu;
  private String nombre;
  private String ruta;
  private String imagen;
  private Boolean estado;

  @OneToMany(mappedBy = "menu", cascade = CascadeType.ALL, orphanRemoval = false)
  private List<SubMenu> subMenu;

}
