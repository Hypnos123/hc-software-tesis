package com.krivi.apihistorialmedico.model.importacion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PacienteImportacionAntecedentes {
  private String alimentacion;
  private String habitos;
  private String vivienda;
  private String desarrolloPsicomotor;
  private String vacunas;
  private String educacion;
  private String enfermedadesPrevias;
  private String cirugiasPrevias;
  private String alergiasMedicamentos;
}
