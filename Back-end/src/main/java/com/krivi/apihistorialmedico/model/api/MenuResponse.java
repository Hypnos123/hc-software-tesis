package com.krivi.apihistorialmedico.model.api;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class MenuResponse {

  private Integer idMenu;
  private String nombre;
  private String ruta;
  private String imagen;
  private Boolean estado;

}
