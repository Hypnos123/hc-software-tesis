package com.krivi.apihistorialmedico.model.api;

import lombok.Data;
import java.util.List;

@Data
public class FusionarHistoriasClinicasRequest {
  private Integer idHistoriaPrincipal;
  private String contrasena;
  private Boolean confirmacion;
  private String motivo;
  private String detalle;
  private String origen;
  private Long cantidadEsperadaPrincipal;
  private Long cantidadEsperadaSecundaria;
  private List<Integer> idsConsultasEsperadasPrincipal;
  private List<Integer> idsConsultasEsperadasSecundaria;
  private String tokenAnalisis;
}
