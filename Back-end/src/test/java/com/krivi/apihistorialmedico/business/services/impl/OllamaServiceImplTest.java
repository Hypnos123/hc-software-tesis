package com.krivi.apihistorialmedico.business.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krivi.apihistorialmedico.business.exception.OllamaException;
import com.krivi.apihistorialmedico.model.api.OllamaInterpretacionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OllamaServiceImplTest {
  private MockRestServiceServer server;
  private OllamaServiceImpl service;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    service = new OllamaServiceImpl(
        builder.baseUrl("http://localhost:11434").build(),
        new ObjectMapper(),
        "http://localhost:11434",
        "qwen3:1.7b"
    );
  }

  @Test
  void interpretaMensajeYDevuelveSoloContenidoJson() {
    server.expect(requestTo("http://localhost:11434/api/chat"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().json("""
            {
              "model": "qwen3:1.7b",
              "stream": false,
              "format": "json",
              "messages": [
                {"role": "system"},
                {"role": "user", "content": "¿Existe un paciente con DNI 72845292?"}
              ]
            }
            """, false))
        .andRespond(withSuccess("""
            {
              "message": {
                "role": "assistant",
                "content": "{\"categoria\":\"PACIENTES\",\"intencion\":\"CONSULTAR_EXISTENCIA\",\"dni\":\"72845292\",\"nombre\":null}"
              }
            }
            """, MediaType.APPLICATION_JSON));

    OllamaInterpretacionResponse response = service.interpretar(
        "¿Existe un paciente con DNI 72845292?"
    );

    assertThat(response.categoria()).isEqualTo("PACIENTES");
    assertThat(response.intencion()).isEqualTo("CONSULTAR_EXISTENCIA");
    assertThat(response.dni()).isEqualTo("72845292");
    assertThat(response.nombre()).isNull();
    server.verify();
  }

  @Test
  void informaCuandoOllamaDevuelveContenidoInvalido() {
    server.expect(requestTo("http://localhost:11434/api/chat"))
        .andRespond(withSuccess("{\"message\":{\"content\":\"no es json\"}}", MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> service.interpretar("mensaje"))
        .isInstanceOf(OllamaException.class)
        .hasMessage("Ollama devolvió una interpretación con formato inválido.");
  }
}
