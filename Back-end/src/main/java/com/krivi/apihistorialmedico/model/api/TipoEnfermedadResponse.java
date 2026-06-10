package com.krivi.apihistorialmedico.model.api;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class TipoEnfermedadResponse {

  private Integer idTipoEnfermedad;
  private String descripcion;
}
