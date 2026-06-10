package com.krivi.apihistorialmedico.model.api;



import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class ConsultaResponse {

   Integer idConsulta;
   String presionArterial;
  String frecuenciaCardiaca;
  String frecuenciaRespiratoria;
  String talla;
  String temperatura;
  Double peso;
  Date fechaConsulta;
  String tiempoEnfermedad;
  String relatoPaciente;
  Integer idPaciente;
  Integer idTipoEnfermedad;
  Integer idUsuario;


}
