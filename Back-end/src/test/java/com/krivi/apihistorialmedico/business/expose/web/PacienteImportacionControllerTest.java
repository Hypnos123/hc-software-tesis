package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.services.importacion.PacientePlantillaService;
import com.krivi.apihistorialmedico.business.services.importacion.PacienteImportacionValidacionService;
import com.krivi.apihistorialmedico.business.exception.PacienteImportacionException;
import com.krivi.apihistorialmedico.model.api.importacion.PacienteImportacionResumenResponse;
import com.krivi.apihistorialmedico.model.api.importacion.PacienteImportacionValidacionResponse;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionErrorCodigo;
import com.krivi.apihistorialmedico.model.importacion.plantilla.PacientePlantillaExcel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.HttpStatus;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PacienteImportacionControllerTest {
  private PacientePlantillaService service;
  private PacienteImportacionValidacionService validacionService;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    service = mock(PacientePlantillaService.class);
    validacionService = mock(PacienteImportacionValidacionService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new PacienteImportacionController(
        service,
        validacionService
    )).build();
  }

  @Test
  void descargaLaPlantillaConHeadersYContenidoCorrectos() throws Exception {
    byte[] contenido = {1, 2, 3};
    when(service.generarPlantilla()).thenReturn(
        new PacientePlantillaExcel(contenido, "plantilla-importacion-pacientes-v1.0.xlsx")
    );

    mockMvc.perform(get("/paciente/importacion/plantilla"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(PacienteImportacionController.XLSX_MEDIA_TYPE))
        .andExpect(header().string(
            "Content-Disposition",
            containsString("plantilla-importacion-pacientes-v1.0.xlsx")
        ))
        .andExpect(content().bytes(contenido));
  }

  @Test
  void validaMultipartYDevuelveOkAunqueElResumenContengaErrores() throws Exception {
    var archivo = new org.springframework.mock.web.MockMultipartFile(
        "archivo", "pacientes.xlsx", "application/octet-stream", new byte[]{1}
    );
    var response = PacienteImportacionValidacionResponse.builder()
        .resumen(PacienteImportacionResumenResponse.builder().registrosAnalizados(2).conErrores(1).build())
        .build();
    when(validacionService.validar(org.mockito.ArgumentMatchers.any())).thenReturn(response);

    mockMvc.perform(multipart("/paciente/importacion/validar").file(archivo))
        .andExpect(status().isOk())
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
            "$.resumen.registrosAnalizados").value(2))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
            "$.resumen.conErrores").value(1));
  }

  @Test
  void traduceExcepcionDeImportacionSinExponerDetallesInternos() throws Exception {
    var archivo = new org.springframework.mock.web.MockMultipartFile(
        "archivo", "pacientes.xlsx", "application/octet-stream", new byte[]{1}
    );
    when(validacionService.validar(org.mockito.ArgumentMatchers.any())).thenThrow(
        new PacienteImportacionException(PacienteImportacionErrorCodigo.FORMATO_NO_PERMITIDO,
            "Solo se permiten archivos .xlsx.", HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    );

    mockMvc.perform(multipart("/paciente/importacion/validar").file(archivo))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
            "$.codigo").value("FORMATO_NO_PERMITIDO"))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
            "$.mensaje").value("Solo se permiten archivos .xlsx."));
  }
}
