package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.exception.OllamaEjecucionException;
import com.krivi.apihistorialmedico.business.services.OllamaEjecucionService;
import com.krivi.apihistorialmedico.business.services.OllamaService;
import com.krivi.apihistorialmedico.business.services.PacienteDuplicadoService;
import com.krivi.apihistorialmedico.business.services.PacienteService;
import com.krivi.apihistorialmedico.model.api.DuplicadosPacientesResponse;
import com.krivi.apihistorialmedico.model.api.OllamaEjecucionResponse;
import com.krivi.apihistorialmedico.model.api.OllamaInterpretacionResponse;
import com.krivi.apihistorialmedico.model.api.PacienteResponse;
import com.krivi.apihistorialmedico.model.api.PacienteDuplicadoComparacionResponse;
import com.krivi.apihistorialmedico.model.api.ResponseModelGet;
import java.util.regex.Pattern;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class OllamaEjecucionServiceImpl implements OllamaEjecucionService {
  private static final String CATEGORIA_PACIENTES = "PACIENTES";
  private static final String INTENCION_VERIFICAR_EXISTENCIA = "VERIFICAR_EXISTENCIA";
  private static final String INTENCION_BUSCAR_PACIENTE = "BUSCAR_PACIENTE";
  private static final String INTENCION_PACIENTES_DUPLICADOS = "PACIENTES_DUPLICADOS";
  private static final Pattern DNI_PATTERN = Pattern.compile("\\d{8}");

  private final OllamaService ollamaService;
  private final PacienteService pacienteService;
  private final PacienteDuplicadoService pacienteDuplicadoService;

  public OllamaEjecucionServiceImpl(
      OllamaService ollamaService,
      PacienteService pacienteService,
      PacienteDuplicadoService pacienteDuplicadoService
  ) {
    this.ollamaService = ollamaService;
    this.pacienteService = pacienteService;
    this.pacienteDuplicadoService = pacienteDuplicadoService;
  }

  @Override
  public OllamaEjecucionResponse ejecutar(String mensaje) {
    OllamaInterpretacionResponse interpretacion = ollamaService.interpretar(mensaje);
    if (esVerificacionDePaciente(interpretacion)) {
      return verificarExistencia(interpretacion);
    }
    if (esBusquedaDePaciente(interpretacion)) {
      return buscarPaciente(interpretacion);
    }
    if (esConsultaPacientesDuplicados(interpretacion)) {
      return consultarPacientesDuplicados(interpretacion);
    }
    return new OllamaEjecucionResponse(
        interpretacion.categoria(),
        interpretacion.intencion(),
        null,
        interpretacion.dni(),
        "Esta intención todavía no está habilitada para ejecución."
    );
  }

  private OllamaEjecucionResponse verificarExistencia(
      OllamaInterpretacionResponse interpretacion
  ) {
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

  private OllamaEjecucionResponse buscarPaciente(OllamaInterpretacionResponse interpretacion) {
    String dni = normalizar(interpretacion.dni());
    String nombre = normalizar(interpretacion.nombre());
    if (dni == null && nombre == null) {
      return new OllamaEjecucionResponse(
          CATEGORIA_PACIENTES,
          INTENCION_BUSCAR_PACIENTE,
          null,
          null,
          null,
          null,
          "Falta indicar el DNI o el nombre del paciente."
      );
    }

    try {
      ResponseModelGet<PacienteResponse> resultado = pacienteService.search(nombre, dni, 25);
      var pacientes = resultado.getData() == null ? java.util.List.<PacienteResponse>of()
          : resultado.getData();
      boolean encontrado = !pacientes.isEmpty();
      return new OllamaEjecucionResponse(
          CATEGORIA_PACIENTES,
          INTENCION_BUSCAR_PACIENTE,
          encontrado,
          dni,
          nombre,
          pacientes,
          encontrado
              ? "Se encontraron pacientes que coinciden con la búsqueda."
              : "No se encontraron pacientes con el criterio indicado."
      );
    } catch (DataAccessException exception) {
      throw new OllamaEjecucionException(
          "No se pudo consultar la información de pacientes en este momento.",
          exception
      );
    }
  }

  private OllamaEjecucionResponse consultarPacientesDuplicados(
      OllamaInterpretacionResponse interpretacion
  ) {
    String dni = normalizar(interpretacion.dni());
    String nombre = normalizar(interpretacion.nombre());
    if (dni == null && nombre != null) {
      return respuestaDuplicados(
          null,
          null,
          "Para consultar pacientes duplicados específicos se requiere el DNI."
      );
    }
    if (dni != null && !DNI_PATTERN.matcher(dni).matches()) {
      return respuestaDuplicados(
          null,
          null,
          "El DNI debe contener exactamente 8 dígitos."
      );
    }

    try {
      if (dni != null) {
        PacienteDuplicadoComparacionResponse comparacion =
            pacienteDuplicadoService.compararPorDni(dni);
        return respuestaDuplicados(null, comparacion, comparacion.getMensaje());
      }
      DuplicadosPacientesResponse grupos = pacienteService.obtenerDuplicadosParaIntegracion();
      String mensaje = grupos.isHayDuplicados()
          ? "Se encontraron grupos de pacientes duplicados activos."
          : "No se encontraron pacientes duplicados activos por DNI.";
      return respuestaDuplicados(grupos, null, mensaje);
    } catch (DataAccessException exception) {
      throw new OllamaEjecucionException(
          "No se pudo consultar la información de pacientes duplicados en este momento.",
          exception
      );
    }
  }

  private OllamaEjecucionResponse respuestaDuplicados(
      DuplicadosPacientesResponse grupos,
      PacienteDuplicadoComparacionResponse comparacion,
      String mensaje
  ) {
    return new OllamaEjecucionResponse(
        CATEGORIA_PACIENTES,
        INTENCION_PACIENTES_DUPLICADOS,
        null,
        comparacion == null ? null : comparacion.getDni(),
        null,
        null,
        grupos,
        comparacion,
        mensaje
    );
  }

  private boolean esVerificacionDePaciente(OllamaInterpretacionResponse interpretacion) {
    return CATEGORIA_PACIENTES.equals(interpretacion.categoria())
        && INTENCION_VERIFICAR_EXISTENCIA.equals(interpretacion.intencion());
  }

  private boolean esBusquedaDePaciente(OllamaInterpretacionResponse interpretacion) {
    return CATEGORIA_PACIENTES.equals(interpretacion.categoria())
        && INTENCION_BUSCAR_PACIENTE.equals(interpretacion.intencion());
  }

  private boolean esConsultaPacientesDuplicados(OllamaInterpretacionResponse interpretacion) {
    return CATEGORIA_PACIENTES.equals(interpretacion.categoria())
        && INTENCION_PACIENTES_DUPLICADOS.equals(interpretacion.intencion());
  }

  private String normalizarDni(String dni) {
    return normalizar(dni);
  }

  private String normalizar(String valor) {
    return valor == null || valor.isBlank() ? null : valor.trim();
  }
}
