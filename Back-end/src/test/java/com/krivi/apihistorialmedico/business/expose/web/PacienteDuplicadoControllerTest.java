package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.exception.PacienteDuplicadoException;
import com.krivi.apihistorialmedico.business.services.PacienteDuplicadoService;
import com.krivi.apihistorialmedico.config.WebCorsConfig;
import com.krivi.apihistorialmedico.model.api.PacienteDuplicadoComparacionResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PacienteDuplicadoControllerTest {
  private PacienteDuplicadoService service;
  private MockMvc mockMvc;
  private AnnotationConfigWebApplicationContext context;

  @BeforeEach
  void setUp() {
    context = new AnnotationConfigWebApplicationContext();
    context.setServletContext(new MockServletContext());
    context.register(WebMvcTestConfiguration.class, WebCorsConfig.class);
    context.refresh();
    service = context.getBean(PacienteDuplicadoService.class);
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
    PacienteDuplicadoService pacienteDuplicadoService() {
      return mock(PacienteDuplicadoService.class);
    }

    @Bean
    PacienteDuplicadoController pacienteDuplicadoController(PacienteDuplicadoService service) {
      return new PacienteDuplicadoController(service);
    }
  }

  @Test
  void devuelveComparacionPorDni() throws Exception {
    when(service.compararPorDni("01234567")).thenReturn(PacienteDuplicadoComparacionResponse.builder()
        .dni("01234567").cantidadPacientesActivos(2).esDuplicado(true).pacientes(List.of())
        .resultado("DUPLICADOS_ENCONTRADOS").razonesRecomendacion(List.of()).build());

    mockMvc.perform(get("/api/pacientes/duplicados")
            .param("dni", "01234567")
            .header("Origin", "http://localhost:4200"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
        .andExpect(jsonPath("$.dni").value("01234567"))
        .andExpect(jsonPath("$.cantidadPacientesActivos").value(2));
    verify(service).compararPorDni("01234567");
  }

  @Test
  void aceptaPreflightDeAngularParaConsultarDuplicados() throws Exception {
    mockMvc.perform(options("/api/pacientes/duplicados")
            .param("dni", "01234567")
            .header("Origin", "http://localhost:4200")
            .header("Access-Control-Request-Method", "GET")
            .header("Access-Control-Request-Headers", "x-usuario-id"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
        .andExpect(header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("GET")))
        .andExpect(header().string("Access-Control-Allow-Headers", org.hamcrest.Matchers.containsString("x-usuario-id")));
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
