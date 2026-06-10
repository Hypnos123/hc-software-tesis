package com.krivi.apihistorialmedico.model.api;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class DetallePermisoRequest {

  private Integer idUsuario;
  private Integer idMenu;
}
