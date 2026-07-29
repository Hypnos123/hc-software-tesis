package com.krivi.apihistorialmedico.business.services.impl.importacion;

import com.krivi.apihistorialmedico.business.importacion.PacienteExcelTemplateGenerator;
import com.krivi.apihistorialmedico.business.services.importacion.PacientePlantillaService;
import com.krivi.apihistorialmedico.config.PacienteImportacionProperties;
import com.krivi.apihistorialmedico.model.importacion.plantilla.PacientePlantillaExcel;
import org.springframework.stereotype.Service;

@Service
public class PacientePlantillaServiceImpl implements PacientePlantillaService {
  private final PacienteExcelTemplateGenerator generator;
  private final PacienteImportacionProperties properties;

  public PacientePlantillaServiceImpl(
      PacienteExcelTemplateGenerator generator,
      PacienteImportacionProperties properties
  ) {
    this.generator = generator;
    this.properties = properties;
  }

  @Override
  public PacientePlantillaExcel generarPlantilla() {
    String nombre = "plantilla-importacion-pacientes-v" + properties.versionPlantilla() + ".xlsx";
    return new PacientePlantillaExcel(generator.generar(), nombre);
  }
}
