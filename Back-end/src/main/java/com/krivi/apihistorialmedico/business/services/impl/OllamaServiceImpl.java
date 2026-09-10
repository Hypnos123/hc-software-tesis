package com.krivi.apihistorialmedico.business.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krivi.apihistorialmedico.business.exception.OllamaException;
import com.krivi.apihistorialmedico.business.services.OllamaService;
import com.krivi.apihistorialmedico.model.api.OllamaInterpretacionResponse;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class OllamaServiceImpl implements OllamaService {
  private static final String SYSTEM_PROMPT = """
      Eres un clasificador de intenciones de un sistema hospitalario.
      No tienes acceso directo a la base de datos y no debes inventar si un paciente existe o no.
      Tu única tarea es interpretar la solicitud del usuario.
      Las únicas categorías permitidas son: PACIENTES, HISTORIAS_CLINICAS y CONSULTAS.
      Devuelve exclusivamente un objeto JSON válido con los campos categoria, intencion, dni y nombre.
      Usa null para dni o nombre cuando el usuario no proporcione ese dato.
      No agregues explicaciones, Markdown ni campos adicionales.
      """;

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final String model;
  private final String baseUrl;

  public OllamaServiceImpl(
      RestClient.Builder restClientBuilder,
      ObjectMapper objectMapper,
      @Value("${ollama.base-url:http://localhost:11434}") String baseUrl,
      @Value("${ollama.model:qwen3:1.7b}") String model
  ) {
    this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    this.objectMapper = objectMapper;
    this.model = model;
    this.baseUrl = baseUrl;
  }

  @Override
  public OllamaInterpretacionResponse interpretar(String mensaje) {
    Map<String, Object> request = Map.of(
        "model", model,
        "stream", false,
        "format", "json",
        "messages", List.of(
            Map.of("role", "system", "content", SYSTEM_PROMPT),
            Map.of("role", "user", "content", mensaje)
        )
    );

    try {
      JsonNode response = restClient.post()
          .uri("/api/chat")
          .body(request)
          .retrieve()
          .body(JsonNode.class);
      String content = response == null ? null : response.path("message").path("content").textValue();
      if (content == null || content.isBlank()) {
        throw new OllamaException("Ollama devolvió una respuesta sin interpretación.");
      }
      return objectMapper.readValue(content, OllamaInterpretacionResponse.class);
    } catch (JsonProcessingException exception) {
      throw new OllamaException("Ollama devolvió una interpretación con formato inválido.", exception);
    } catch (RestClientException exception) {
      throw new OllamaException(
          "No se pudo conectar con Ollama. Verifique que esté disponible en "
              + baseUrl + ".",
          exception
      );
    }
  }

}
