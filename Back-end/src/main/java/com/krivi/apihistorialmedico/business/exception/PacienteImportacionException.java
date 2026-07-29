package com.krivi.apihistorialmedico.business.exception;

import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionErrorCodigo;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Getter
public class PacienteImportacionException extends RuntimeException {
  private final PacienteImportacionErrorCodigo codigo;
  private final HttpStatus estadoHttp;
  private final Map<String, Object> detalles;

  public PacienteImportacionException(
      PacienteImportacionErrorCodigo codigo,
      String mensaje,
      HttpStatus estadoHttp
  ) {
    this(codigo, mensaje, estadoHttp, Map.of());
  }

  public PacienteImportacionException(
      PacienteImportacionErrorCodigo codigo,
      String mensaje,
      HttpStatus estadoHttp,
      Map<String, Object> detalles
  ) {
    super(mensaje);
    this.codigo = codigo;
    this.estadoHttp = estadoHttp;
    this.detalles = detalles == null ? Map.of() : Map.copyOf(detalles);
  }
}
