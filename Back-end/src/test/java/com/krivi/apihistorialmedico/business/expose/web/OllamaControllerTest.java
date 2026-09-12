package com.krivi.apihistorialmedico.business.expose.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.krivi.apihistorialmedico.business.exception.OllamaException;
import com.krivi.apihistorialmedico.business.services.OllamaService;
import com.krivi.apihistorialmedico.business.services.OllamaEjecucionService;
import com.krivi.apihistorialmedico.model.api.OllamaInterpretacionResponse;
import com.krivi.apihistorialmedico.model.api.OllamaEjecucionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OllamaControllerTest {
  private OllamaService ollamaService;
  private OllamaEjecucionService ollamaEjecucionService;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    ollamaService = org.mockito.Mockito.mock(OllamaService.class);
    ollamaEjecucionService = org.mockito.Mockito.mock(OllamaEjecucionService.class);
    mockMvc = MockMvcBuilders
        .standaloneSetup(new OllamaController(ollamaService, ollamaEjecucionService))
        .build();
  }

  @Test
  void devuelveLaInterpretacionSinEnvoltorio() throws Exception {
    when(ollamaService.interpretar("consulta"))
        .thenReturn(new OllamaInterpretacionResponse("PACIENTES", "BUSCAR", null, "Ana"));

    mockMvc.perform(post("/ollama/interpretar")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"mensaje\":\"consulta\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.categoria").value("PACIENTES"))
        .andExpect(jsonPath("$.intencion").value("BUSCAR"))
        .andExpect(jsonPath("$.nombre").value("Ana"));
  }

  @Test
  void ejecutaLaVerificacionEnElEndpointTemporal() throws Exception {
    when(ollamaEjecucionService.ejecutar("consulta")).thenReturn(
        new OllamaEjecucionResponse(
            "PACIENTES",
            "VERIFICAR_EXISTENCIA",
            true,
            "72845292",
            "El paciente se encuentra registrado."
        )
    );

    mockMvc.perform(post("/ollama/ejecutar")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"mensaje\":\"consulta\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.categoria").value("PACIENTES"))
        .andExpect(jsonPath("$.intencion").value("VERIFICAR_EXISTENCIA"))
        .andExpect(jsonPath("$.encontrado").value(true))
        .andExpect(jsonPath("$.dni").value("72845292"));
  }

  @Test
  void devuelveServicioNoDisponibleConMensajeClaro() throws Exception {
    when(ollamaService.interpretar("consulta"))
        .thenThrow(new OllamaException("No se pudo conectar con Ollama."));

    mockMvc.perform(post("/ollama/interpretar")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"mensaje\":\"consulta\"}"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.codigo").value("OLLAMA_NO_DISPONIBLE"))
        .andExpect(jsonPath("$.mensaje").value("No se pudo conectar con Ollama."));
  }
}
