package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.exception.OllamaException;
import com.krivi.apihistorialmedico.business.services.OllamaService;
import com.krivi.apihistorialmedico.model.api.ApiErrorResponse;
import com.krivi.apihistorialmedico.model.api.OllamaInterpretacionRequest;
import com.krivi.apihistorialmedico.model.api.OllamaInterpretacionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ollama")
public class OllamaController {
  private final OllamaService ollamaService;

  public OllamaController(OllamaService ollamaService) {
    this.ollamaService = ollamaService;
  }

  @PostMapping("/interpretar")
  public OllamaInterpretacionResponse interpretar(
      @Valid @RequestBody OllamaInterpretacionRequest request
  ) {
    return ollamaService.interpretar(request.mensaje());
  }

  @ExceptionHandler(OllamaException.class)
  public ResponseEntity<ApiErrorResponse> handleOllamaException(OllamaException exception) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
        ApiErrorResponse.builder()
            .codigo("OLLAMA_NO_DISPONIBLE")
            .mensaje(exception.getMessage())
            .build()
    );
  }
}
