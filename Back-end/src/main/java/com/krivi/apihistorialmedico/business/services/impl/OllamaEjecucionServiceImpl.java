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
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class OllamaEjecucionServiceImpl implements OllamaEjecucionService {
  private static final String CATEGORIA_PACIENTES = "PACIENTES";
  private static final String INTENCION_VERIFICAR_EXISTENCIA = "VERIFICAR_EXISTENCIA";
  private static final String INTENCION_BUSCAR_PACIENTE = "BUSCAR_PACIENTE";
  private static final String INTENCION_PACIENTES_DUPLICADOS = "PACIENTES_DUPLICADOS";
  private static final String INTENCION_PACIENTES_DUPLICADOS_ESPECIFICO =
      "PACIENTES_DUPLICADOS_ESPECIFICO";
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
    OllamaInterpretacionResponse interpretacion = normalizarInterpretacion(
        ollamaService.interpretar(mensaje)
    );
    if (esVerificacionDePaciente(interpretacion)) {
      return verificarExistencia(interpretacion);
    }
    if (esBusquedaDePaciente(interpretacion)) {
      return buscarPaciente(interpretacion);
    }
    if (esConsultaDuplicadosEspecificaSinCriterio(interpretacion)) {
      return new OllamaEjecucionResponse(
          CATEGORIA_PACIENTES,
          INTENCION_PACIENTES_DUPLICADOS_ESPECIFICO,
          null,
          null,
          "Indica el DNI o el nombre completo del paciente que deseas verificar."
      );
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

  OllamaInterpretacionResponse normalizarInterpretacion(
      OllamaInterpretacionResponse interpretacion
  ) {
    if (INTENCION_PACIENTES_DUPLICADOS.equals(interpretacion.categoria())
        && INTENCION_PACIENTES_DUPLICADOS.equals(interpretacion.intencion())) {
      return new OllamaInterpretacionResponse(
          CATEGORIA_PACIENTES,
          interpretacion.intencion(),
          interpretacion.dni(),
          interpretacion.nombre()
      );
    }
    return interpretacion;
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
      if (nombre != null) {
        return consultarDuplicadosPorNombre(nombre);
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

  private OllamaEjecucionResponse consultarDuplicadosPorNombre(String nombre) {
    if (normalizarNombre(nombre).split(" ").length < 3) {
      return respuestaDuplicadosPorNombre(
          nombre,
          null,
          "Para verificar duplicados por nombre se necesita el nombre y los dos apellidos."
      );
    }

    ResponseModelGet<PacienteResponse> resultado = pacienteService.search(nombre, null, 25);
    String nombreNormalizado = normalizarNombre(nombre);
    List<PacienteResponse> coincidenciasExactas = resultado.getData() == null
        ? List.of()
        : resultado.getData().stream()
            .filter(paciente -> nombreNormalizado.equals(normalizarNombreCompleto(paciente)))
            .toList();

    String mensaje;
    if (coincidenciasExactas.isEmpty()) {
      mensaje = "No se encontraron pacientes activos con ese nombre completo.";
    } else if (coincidenciasExactas.size() == 1) {
      mensaje = "El nombre completo corresponde a un único paciente activo y no presenta duplicados.";
    } else {
      mensaje = "Se encontraron " + coincidenciasExactas.size()
          + " pacientes activos con el mismo nombre completo.";
    }
    return respuestaDuplicadosPorNombre(nombre, coincidenciasExactas, mensaje);
  }

  private OllamaEjecucionResponse respuestaDuplicadosPorNombre(
      String nombre,
      List<PacienteResponse> pacientes,
      String mensaje
  ) {
    return new OllamaEjecucionResponse(
        CATEGORIA_PACIENTES,
        INTENCION_PACIENTES_DUPLICADOS,
        pacientes == null ? null : !pacientes.isEmpty(),
        null,
        nombre,
        pacientes,
        null,
        null,
        mensaje
    );
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

  private boolean esConsultaDuplicadosEspecificaSinCriterio(
      OllamaInterpretacionResponse interpretacion
  ) {
    return CATEGORIA_PACIENTES.equals(interpretacion.categoria())
        && INTENCION_PACIENTES_DUPLICADOS_ESPECIFICO.equals(interpretacion.intencion())
        && normalizar(interpretacion.dni()) == null
        && normalizar(interpretacion.nombre()) == null;
  }

  private String normalizarDni(String dni) {
    return normalizar(dni);
  }

  private String normalizar(String valor) {
    return valor == null || valor.isBlank() ? null : valor.trim();
  }

  private String normalizarNombreCompleto(PacienteResponse paciente) {
    return normalizarNombre(String.join(
        " ",
        paciente.getNombres() == null ? "" : paciente.getNombres(),
        paciente.getApellidos() == null ? "" : paciente.getApellidos()
    ));
  }

  private String normalizarNombre(String nombre) {
    return Normalizer.normalize(nombre, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z ]", " ")
        .replaceAll("\\s+", " ")
        .trim();
  }
}
