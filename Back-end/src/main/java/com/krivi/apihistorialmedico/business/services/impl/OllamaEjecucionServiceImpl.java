package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.exception.OllamaEjecucionException;
import com.krivi.apihistorialmedico.business.services.OllamaEjecucionService;
import com.krivi.apihistorialmedico.business.services.OllamaService;
import com.krivi.apihistorialmedico.business.services.PacienteService;
import com.krivi.apihistorialmedico.model.api.OllamaEjecucionResponse;
import com.krivi.apihistorialmedico.model.api.OllamaInterpretacionResponse;
import com.krivi.apihistorialmedico.model.api.ResponseModelGet;
import java.util.regex.Pattern;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class OllamaEjecucionServiceImpl implements OllamaEjecucionService {
  private static final String CATEGORIA_PACIENTES = "PACIENTES";
  private static final String INTENCION_VERIFICAR_EXISTENCIA = "VERIFICAR_EXISTENCIA";
  private static final Pattern DNI_PATTERN = Pattern.compile("\\d{8}");

  private final OllamaService ollamaService;
  private final PacienteService pacienteService;

  public OllamaEjecucionServiceImpl(OllamaService ollamaService, PacienteService pacienteService) {
    this.ollamaService = ollamaService;
    this.pacienteService = pacienteService;
  }

  @Override
  public OllamaEjecucionResponse ejecutar(String mensaje) {
    OllamaInterpretacionResponse interpretacion = ollamaService.interpretar(mensaje);
    if (!esVerificacionDePaciente(interpretacion)) {
      return new OllamaEjecucionResponse(
          interpretacion.categoria(),
          interpretacion.intencion(),
          null,
          interpretacion.dni(),
          "Esta intención todavía no está habilitada para ejecución."
      );
    }

    String dni = normalizarDni(interpretacion.dni());
    if (dni == null) {
      return new OllamaEjecucionResponse(
          CATEGORIA_PACIENTES,
          INTENCION_VERIFICAR_EXISTENCIA,
          null,
          null,
          "Falta indicar el DNI del paciente."
      );
    }
    if (!DNI_PATTERN.matcher(dni).matches()) {
      return new OllamaEjecucionResponse(
          CATEGORIA_PACIENTES,
          INTENCION_VERIFICAR_EXISTENCIA,
          null,
          dni,
          "El DNI debe contener exactamente 8 dígitos."
      );
    }

    try {
      ResponseModelGet<?> resultado = pacienteService.search(null, dni, 25);
      boolean encontrado = resultado.getData() != null && !resultado.getData().isEmpty();
      return new OllamaEjecucionResponse(
          CATEGORIA_PACIENTES,
          INTENCION_VERIFICAR_EXISTENCIA,
          encontrado,
          dni,
          encontrado
              ? "El paciente se encuentra registrado."
              : "No se encontró un paciente con ese DNI."
      );
    } catch (DataAccessException exception) {
      throw new OllamaEjecucionException(
          "No se pudo consultar la información de pacientes en este momento.",
          exception
      );
    }
  }

  private boolean esVerificacionDePaciente(OllamaInterpretacionResponse interpretacion) {
    return CATEGORIA_PACIENTES.equals(interpretacion.categoria())
        && INTENCION_VERIFICAR_EXISTENCIA.equals(interpretacion.intencion());
  }

  private String normalizarDni(String dni) {
    return dni == null || dni.isBlank() ? null : dni.trim();
  }
}
