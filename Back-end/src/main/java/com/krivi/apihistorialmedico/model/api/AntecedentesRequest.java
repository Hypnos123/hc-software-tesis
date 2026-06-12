package com.krivi.apihistorialmedico.model.api;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
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
