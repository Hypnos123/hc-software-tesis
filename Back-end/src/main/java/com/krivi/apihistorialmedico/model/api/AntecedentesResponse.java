package com.krivi.apihistorialmedico.model.api;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AntecedentesResponse {

  Integer idAntecedentes;
  String alimentacion;
  String habitos;
  String vivienda;
  String desarrolloPsicomotor;
  String vacunas;
  String educacion;
  String enfermedadesPrevias;
  String cirugiasPrevias;
  String alergiaMedicamentos;
  Integer idPaciente;
  String nombreApellidos;


}
