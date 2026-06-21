package com.krivi.apihistorialmedico.model.api;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@Builder
public class ConsultaResponse {
  Integer idConsulta;
  Integer idHistoriaClinica;
  LocalDateTime fechaCreacion;
  String estado;
  String presionArterial;
  String frecuenciaCardiaca;
  String frecuenciaRespiratoria;
  String talla;
  String temperatura;
  Double peso;
  Date fechaConsulta;
  String tiempoEnfermedad;
  String tipoEnfermedad;
  String especialidadRequerida;
  Integer idEmpleadoDoctor;
  String doctorResponsable;
  String relatoPaciente;
  String diagnostico;
  String examenesRecetados;
  String receta;
  String tratamiento;
  Date proximaCita;
  Integer idPaciente;
  String nombres;
  String apellidos;
  String numDocumento;
  Integer edad;
  String enfermedadesPrevias;
  String cirugiasPrevias;
  String alergiaMedicamentos;
  Integer idTipoEnfermedad;
  Integer idUsuario;
}
