package com.krivi.apihistorialmedico.model.api;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class ConsultaRequest {
  Integer idConsulta;
  Integer idHistoriaClinica;
  String presionArterial;
  String frecuenciaCardiaca;
  String frecuenciaRespiratoria;
  String talla;
  String temperatura;
  Double peso;
  Date fechaConsulta;
  String tiempoEnfermedad;
  String tipoEnfermedad;
  Integer idTipoEnfermedad;
  String especialidadRequerida;
  Integer idEmpleadoDoctor;
  String relatoPaciente;
  String diagnostico;
  String examenesRecetados;
  String receta;
  String tratamiento;
  Date proximaCita;
  Integer idPaciente;
  Integer idUsuario;
}
