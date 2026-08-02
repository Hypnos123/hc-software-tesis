package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.exception.PacienteDuplicadoException;
import com.krivi.apihistorialmedico.business.services.PacienteDuplicadoService;
import com.krivi.apihistorialmedico.model.api.PacienteDuplicadoComparacionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PacienteDuplicadoControllerTest {
  private PacienteDuplicadoService service;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    service = mock(PacienteDuplicadoService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new PacienteDuplicadoController(service)).build();
  }

  @Test
  void devuelveComparacionPorDni() throws Exception {
    when(service.compararPorDni("12345678")).thenReturn(PacienteDuplicadoComparacionResponse.builder()
        .dni("12345678").cantidadPacientesActivos(2).esDuplicado(true).pacientes(List.of())
        .resultado("DUPLICADOS_ENCONTRADOS").razonesRecomendacion(List.of()).build());

    mockMvc.perform(get("/api/pacientes/duplicados").param("dni", "12345678"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dni").value("12345678"))
        .andExpect(jsonPath("$.cantidadPacientesActivos").value(2));
    verify(service).compararPorDni("12345678");
  }

  @Test
  void devuelveBadRequestControladoParaDniVacio() throws Exception {
    when(service.compararPorDni("")).thenThrow(new PacienteDuplicadoException(
        "DNI_INVALIDO", "El DNI es obligatorio.", HttpStatus.BAD_REQUEST));

    mockMvc.perform(get("/api/pacientes/duplicados").param("dni", ""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("DNI_INVALIDO"));
  }
}
