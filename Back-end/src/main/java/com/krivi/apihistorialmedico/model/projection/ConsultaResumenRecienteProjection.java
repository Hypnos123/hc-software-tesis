package com.krivi.apihistorialmedico.model.projection;

import java.util.Date;

public interface ConsultaResumenRecienteProjection {
  Integer getIdConsulta();
  Integer getIdHistoriaClinica();
  Date getFechaConsulta();
  String getEspecialidad();
  String getDoctor();
  String getRelatoPaciente();
  String getDiagnostico();
  String getExamenesRecetados();
  String getReceta();
  String getTratamiento();
  Date getProximaCita();
}
