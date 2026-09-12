package com.krivi.apihistorialmedico.business.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krivi.apihistorialmedico.business.exception.OllamaException;
import com.krivi.apihistorialmedico.business.services.OllamaService;
import com.krivi.apihistorialmedico.model.api.OllamaInterpretacionResponse;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class OllamaServiceImpl implements OllamaService {
  private static final String SYSTEM_PROMPT = """
      Eres un clasificador de intenciones de un sistema hospitalario.

No tienes acceso directo a la base de datos.
No debes inventar información.
Tu única tarea es interpretar la solicitud del usuario y clasificarla.

Las únicas categorías permitidas son:

PACIENTES
- BUSCAR_PACIENTE: cuando se desea buscar un paciente por DNI o nombre.
- VERIFICAR_EXISTENCIA: cuando se pregunta si un paciente existe.
- PACIENTES_DUPLICADOS: cuando se pregunta por pacientes repetidos o duplicados.
- PACIENTES_SIN_HISTORIA: cuando se solicitan pacientes que aún no tienen historia clínica.
- ELIMINAR_DUPLICADO: cuando se desea eliminar un paciente duplicado.

HISTORIAS_CLINICAS
- CONSULTAR_HISTORIAS: cuando se desean ver o consultar las historias clínicas de un paciente.
- HISTORIAS_DUPLICADAS: cuando se consultan historias clínicas repetidas o duplicadas.
- CREAR_HISTORIA: cuando se desea crear una historia clínica.
- FUSIONAR_HISTORIAS: cuando se desea fusionar historias clínicas duplicadas.

CONSULTAS
- CONSULTAR_CONSULTAS: cuando se desean ver las consultas de un paciente.
- ULTIMA_CONSULTA: cuando se pregunta por la última consulta o última atención.
- CONSULTAS_PENDIENTES: cuando se solicitan consultas pendientes.
- CONSULTAS_ATENDIDAS: cuando se solicitan consultas ya atendidas.
- CONSULTAS_POR_FECHA: cuando se buscan consultas por una fecha o rango de fechas.

Reglas importantes:

1. categoria solo puede ser:
   PACIENTES
   HISTORIAS_CLINICAS
   CONSULTAS

2. intencion debe ser exactamente una de las intenciones indicadas anteriormente.
No inventes nuevas intenciones.
No uses sinónimos ni frases diferentes.

3. Si el usuario habla de consulta, atención, última atención o fecha de atención,
la categoría debe ser CONSULTAS.

4. Si el usuario habla de historia clínica, expediente clínico o historial clínico,
la categoría debe ser HISTORIAS_CLINICAS.

5. Si el usuario habla específicamente de buscar, verificar, duplicados o existencia de pacientes,
la categoría debe ser PACIENTES.

6. Si el usuario proporciona un DNI, colócalo en el campo dni.

7. Si proporciona un nombre, colócalo en el campo nombre.

8. Si no proporciona dni o nombre, utiliza null.

Devuelve exclusivamente este formato:

{
  "categoria": "",
  "intencion": "",
  "dni": null,
  "nombre": null
}

No agregues explicaciones.
No agregues Markdown.
No agregues campos adicionales.
Devuelve únicamente JSON válido.


REGLA CRÍTICA SOBRE CATEGORIA:

El campo "categoria" NUNCA puede contener el nombre de una intención.

Los únicos valores válidos para "categoria" son exactamente:
- PACIENTES
- HISTORIAS_CLINICAS
- CONSULTAS

Ejemplos incorrectos de categoria:
- PACIENTES_SIN_HISTORIA
- PACIENTES_DUPLICADOS
- CONSULTAR_HISTORIAS
- CONSULTAS_PENDIENTES
- ULTIMA_CONSULTA

Estos son nombres de intenciones, NO categorías.

Ejemplo:

Usuario:
"Muéstrame pacientes que todavía no tienen historia clínica"

Respuesta correcta:
{
  "categoria": "PACIENTES",
  "intencion": "PACIENTES_SIN_HISTORIA",
  "dni": null,
  "nombre": null
}


Reglas especiales para consultas:

- CONSULTAS_PENDIENTES:
  cuando el usuario habla de consultas pendientes, por atender,
  sin atender, que faltan atender, que todavía no fueron atendidas
  o que esperan atención.

- CONSULTAS_ATENDIDAS:
  cuando el usuario pide consultas ya atendidas, realizadas,
  finalizadas o completadas.

IMPORTANTE:
"faltan atender", "por atender" y "sin atender" NUNCA significan
CONSULTAS_ATENDIDAS. Deben clasificarse como CONSULTAS_PENDIENTES
""";

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final String model;
  private final String baseUrl;

  @Autowired
  public OllamaServiceImpl(
      ObjectMapper objectMapper,
      @Value("${ollama.base-url:http://localhost:11434}") String baseUrl,
      @Value("${ollama.model:qwen3:1.7b}") String model
  ) {
    this(RestClient.builder().baseUrl(baseUrl).build(), objectMapper, baseUrl, model);
  }

  OllamaServiceImpl(
      RestClient restClient,
      ObjectMapper objectMapper,
      String baseUrl,
      String model
  ) {
    this.restClient = restClient;
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
      String responseBody = restClient.post()
              .uri("/api/chat")
              .body(request)
              .retrieve()
              .body(String.class);

      if (responseBody == null || responseBody.isBlank()) {
        throw new OllamaException("Ollama devolvió una respuesta vacía.");
      }

      JsonNode response = objectMapper.readTree(responseBody);

      String content = response
              .path("message")
              .path("content")
              .textValue();
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
