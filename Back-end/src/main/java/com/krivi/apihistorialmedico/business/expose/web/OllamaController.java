package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.exception.OllamaException;
import com.krivi.apihistorialmedico.business.exception.OllamaEjecucionException;
import com.krivi.apihistorialmedico.business.services.OllamaEjecucionService;
import com.krivi.apihistorialmedico.business.services.OllamaService;
import com.krivi.apihistorialmedico.model.api.ApiErrorResponse;
import com.krivi.apihistorialmedico.model.api.OllamaInterpretacionRequest;
import com.krivi.apihistorialmedico.model.api.OllamaInterpretacionResponse;
import com.krivi.apihistorialmedico.model.api.OllamaEjecucionResponse;
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
  private final OllamaEjecucionService ollamaEjecucionService;

  public OllamaController(
      OllamaService ollamaService,
      OllamaEjecucionService ollamaEjecucionService
  ) {
    this.ollamaService = ollamaService;
    this.ollamaEjecucionService = ollamaEjecucionService;
  }

  @PostMapping("/ejecutar")
  public OllamaEjecucionResponse ejecutar(
      @Valid @RequestBody OllamaInterpretacionRequest request
  ) {
    return ollamaEjecucionService.ejecutar(request.mensaje());
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

  @ExceptionHandler(OllamaEjecucionException.class)
  public ResponseEntity<ApiErrorResponse> handleOllamaEjecucionException(
      OllamaEjecucionException exception
  ) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
        ApiErrorResponse.builder()
            .codigo("PACIENTES_NO_DISPONIBLE")
            .mensaje(exception.getMessage())
            .build()
    );
  }
}
