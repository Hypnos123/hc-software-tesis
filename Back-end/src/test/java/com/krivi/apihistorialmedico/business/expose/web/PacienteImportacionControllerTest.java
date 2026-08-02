package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.services.importacion.PacientePlantillaService;
import com.krivi.apihistorialmedico.business.services.importacion.PacienteImportacionValidacionService;
import com.krivi.apihistorialmedico.business.services.importacion.PacienteImportacionConfirmacionService;
import com.krivi.apihistorialmedico.business.exception.PacienteImportacionException;
import com.krivi.apihistorialmedico.model.api.importacion.PacienteImportacionResumenResponse;
import com.krivi.apihistorialmedico.model.api.importacion.PacienteImportacionValidacionResponse;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionErrorCodigo;
import com.krivi.apihistorialmedico.model.importacion.plantilla.PacientePlantillaExcel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PacienteImportacionControllerTest {
  private PacientePlantillaService service;
  private PacienteImportacionValidacionService validacionService;
  private PacienteImportacionConfirmacionService confirmacionService;
  private MockMvc mockMvc;
  private AnnotationConfigWebApplicationContext context;

  @BeforeEach
  void setUp() {
    context = new AnnotationConfigWebApplicationContext();
    context.setServletContext(new MockServletContext());
    context.register(WebMvcTestConfiguration.class);
    context.refresh();
    service = context.getBean(PacientePlantillaService.class);
    validacionService = context.getBean(PacienteImportacionValidacionService.class);
    confirmacionService = context.getBean(PacienteImportacionConfirmacionService.class);
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
  }

  @AfterEach
  void tearDown() {
    context.close();
  }

  @Configuration
  @EnableWebMvc
  static class WebMvcTestConfiguration {
    @Bean
    PacientePlantillaService pacientePlantillaService() {
      return mock(PacientePlantillaService.class);
    }

    @Bean
    PacienteImportacionValidacionService pacienteImportacionValidacionService() {
      return mock(PacienteImportacionValidacionService.class);
    }

    @Bean
    PacienteImportacionConfirmacionService pacienteImportacionConfirmacionService() {
      return mock(PacienteImportacionConfirmacionService.class);
    }

    @Bean
    PacienteImportacionController pacienteImportacionController(
        PacientePlantillaService plantillaService,
        PacienteImportacionValidacionService validacionService,
        PacienteImportacionConfirmacionService confirmacionService
    ) {
      return new PacienteImportacionController(plantillaService, validacionService, confirmacionService);
    }
  }

  @Test
  void descargaLaPlantillaConHeadersYContenidoCorrectos() throws Exception {
    byte[] contenido = {1, 2, 3};
    when(service.generarPlantilla()).thenReturn(
        new PacientePlantillaExcel(contenido, "plantilla-importacion-pacientes-v1.0.xlsx")
    );

    mockMvc.perform(get("/paciente/importacion/plantilla")
            .header("Origin", "http://localhost:4200"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
        .andExpect(header().string("Access-Control-Expose-Headers", "Content-Disposition"))
        .andExpect(content().contentType(PacienteImportacionController.XLSX_MEDIA_TYPE))
        .andExpect(header().string(
            "Content-Disposition",
            containsString("plantilla-importacion-pacientes-v1.0.xlsx")
        ))
        .andExpect(content().bytes(contenido));
  }

  @Test
  void aceptaPreflightDeAngularParaLosEndpointsDeImportacion() throws Exception {
    mockMvc.perform(options("/paciente/importacion/validar")
            .header("Origin", "http://localhost:4200")
            .header("Access-Control-Request-Method", "POST")
            .header("Access-Control-Request-Headers", "content-type"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
        .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")))
        .andExpect(header().string("Access-Control-Allow-Headers", containsString("content-type")))
        .andExpect(header().string("Access-Control-Expose-Headers", "Content-Disposition"));
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

  @Test
  void consultaYConfirmaUnaImportacionPorUuid() throws Exception {
    java.util.UUID id = java.util.UUID.randomUUID();
    when(confirmacionService.obtener(id)).thenReturn(PacienteImportacionValidacionResponse.builder()
        .importacionId(id).estado(com.krivi.apihistorialmedico.model.importacion.PacienteImportacionEstado.PREVISUALIZADA)
        .build());
    when(confirmacionService.confirmar(id)).thenReturn(
        com.krivi.apihistorialmedico.model.api.importacion.PacienteImportacionConfirmacionResponse.builder()
            .importacionId(id)
            .estado(com.krivi.apihistorialmedico.model.importacion.PacienteImportacionEstado.CONFIRMADA)
            .build());

    mockMvc.perform(get("/paciente/importacion/{id}", id))
        .andExpect(status().isOk())
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
            "$.estado").value("PREVISUALIZADA"));
    mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            .post("/paciente/importacion/{id}/confirmar", id))
        .andExpect(status().isOk())
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
            "$.estado").value("CONFIRMADA"));
  }
}
