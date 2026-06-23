package com.krivi.apihistorialmedico.model.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AsistenteResponse {
  private String intencion;
  private String respuesta;
  private Map<String, Object> datos;
}
