package com.krivi.apihistorialmedico.model.projection;

import java.time.LocalDateTime;
import java.util.Date;

public interface ConsultaResumenRecienteProjection {
  Integer getIdConsulta();
  Integer getIdHistoriaClinica();
  LocalDateTime getFechaAtencion();
  String getEspecialidad();
  String getDoctor();
  String getRelatoPaciente();
  String getDiagnostico();
  String getExamenesRecetados();
  String getReceta();
  String getTratamiento();
  Date getProximaCita();
}
