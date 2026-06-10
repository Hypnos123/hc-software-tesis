package com.krivi.apihistorialmedico.model.api;


import com.krivi.apihistorialmedico.model.entity.Paciente;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class AntecedentesRequest {


   Integer idAntecedentes;
   String alimentacion;
   String habitos;
   String vivienda;
   String desarrolloPsicomotor;
   String vacunas;
   String educacion;
   String cirugiasPrevias;
   String alergiaMedicamentos;
   Integer idPaciente;

}
