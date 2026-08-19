package com.krivi.apihistorialmedico.model.projection;

import java.util.Date;

public interface ConsultaFuncionesVitalesProjection {
  Integer getIdConsulta();
  Date getFechaConsulta();
  String getPresionArterial();
  String getFrecuenciaCardiaca();
  String getFrecuenciaRespiratoria();
  String getTalla();
  String getTemperatura();
  Double getPeso();
}
