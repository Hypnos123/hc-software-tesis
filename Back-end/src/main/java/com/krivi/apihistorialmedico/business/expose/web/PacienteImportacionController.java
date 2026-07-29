package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.services.importacion.PacientePlantillaService;
import com.krivi.apihistorialmedico.model.importacion.plantilla.PacientePlantillaExcel;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/paciente/importacion")
public class PacienteImportacionController {
  public static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
  );

  private final PacientePlantillaService plantillaService;

  public PacienteImportacionController(PacientePlantillaService plantillaService) {
    this.plantillaService = plantillaService;
  }

  @GetMapping("/plantilla")
  public ResponseEntity<Resource> descargarPlantilla() {
    PacientePlantillaExcel plantilla = plantillaService.generarPlantilla();
    ContentDisposition disposition = ContentDisposition.attachment()
        .filename(plantilla.nombreArchivo(), StandardCharsets.UTF_8)
        .build();

    return ResponseEntity.ok()
        .contentType(XLSX_MEDIA_TYPE)
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
        .contentLength(plantilla.contenido().length)
        .body(new ByteArrayResource(plantilla.contenido()));
  }
}
