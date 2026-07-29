package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.services.importacion.PacientePlantillaService;
import com.krivi.apihistorialmedico.model.importacion.plantilla.PacientePlantillaExcel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PacienteImportacionControllerTest {
  private PacientePlantillaService service;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    service = mock(PacientePlantillaService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new PacienteImportacionController(service)).build();
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
}
