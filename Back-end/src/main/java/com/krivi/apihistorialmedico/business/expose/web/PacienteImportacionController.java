package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.exception.PacienteImportacionException;
import com.krivi.apihistorialmedico.business.services.importacion.PacienteImportacionValidacionService;
import com.krivi.apihistorialmedico.business.services.importacion.PacientePlantillaService;
import com.krivi.apihistorialmedico.model.api.ApiErrorResponse;
import com.krivi.apihistorialmedico.model.api.importacion.PacienteImportacionValidacionResponse;
import com.krivi.apihistorialmedico.model.importacion.plantilla.PacientePlantillaExcel;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/paciente/importacion")
public class PacienteImportacionController {
  public static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
  );

  private final PacientePlantillaService plantillaService;
  private final PacienteImportacionValidacionService validacionService;

  public PacienteImportacionController(
      PacientePlantillaService plantillaService,
      PacienteImportacionValidacionService validacionService
  ) {
    this.plantillaService = plantillaService;
    this.validacionService = validacionService;
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

  @PostMapping(value = "/validar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<PacienteImportacionValidacionResponse> validar(
      @RequestPart("archivo") MultipartFile archivo
  ) {
    return ResponseEntity.ok(validacionService.validar(archivo));
  }

  @ExceptionHandler(PacienteImportacionException.class)
  public ResponseEntity<ApiErrorResponse> handlePacienteImportacionException(
      PacienteImportacionException exception
  ) {
    return ResponseEntity.status(exception.getEstadoHttp()).body(ApiErrorResponse.builder()
        .codigo(exception.getCodigo().name())
        .mensaje(exception.getMessage())
        .build());
  }
}
