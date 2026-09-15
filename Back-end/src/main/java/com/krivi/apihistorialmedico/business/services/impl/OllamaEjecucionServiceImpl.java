package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.exception.OllamaEjecucionException;
import com.krivi.apihistorialmedico.business.services.OllamaEjecucionService;
import com.krivi.apihistorialmedico.business.services.OllamaService;
import com.krivi.apihistorialmedico.business.services.HistoriaClinicaService;
import com.krivi.apihistorialmedico.business.services.PacienteDuplicadoService;
import com.krivi.apihistorialmedico.business.services.PacienteService;
import com.krivi.apihistorialmedico.model.api.DuplicadosPacientesResponse;
import com.krivi.apihistorialmedico.model.api.BusquedaHistoriasClinicasResponse;
import com.krivi.apihistorialmedico.model.api.DuplicadosHistoriasClinicasResponse;
import com.krivi.apihistorialmedico.model.api.HistoriasClinicasFaltantesPreviewResponse;
import com.krivi.apihistorialmedico.model.api.HistoriaClinicaIntegracionItemResponse;
import com.krivi.apihistorialmedico.model.api.GrupoDuplicadoHistoriaClinicaResponse;
import com.krivi.apihistorialmedico.model.api.OllamaEjecucionResponse;
import com.krivi.apihistorialmedico.model.api.OllamaInterpretacionResponse;
import com.krivi.apihistorialmedico.model.api.PacienteResponse;
import com.krivi.apihistorialmedico.model.api.PacienteDuplicadoComparacionResponse;
import com.krivi.apihistorialmedico.model.api.ResponseModelGet;
import com.krivi.apihistorialmedico.model.api.UltimosPacientesResponse;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class OllamaEjecucionServiceImpl implements OllamaEjecucionService {
  private static final String CATEGORIA_PACIENTES = "PACIENTES";
  private static final String CATEGORIA_HISTORIAS = "HISTORIAS_CLINICAS";
  private static final String INTENCION_VERIFICAR_EXISTENCIA = "VERIFICAR_EXISTENCIA";
  private static final String INTENCION_BUSCAR_PACIENTE = "BUSCAR_PACIENTE";
  private static final String INTENCION_PACIENTES_DUPLICADOS = "PACIENTES_DUPLICADOS";
  private static final String INTENCION_ULTIMOS_PACIENTES = "ULTIMOS_PACIENTES";
  private static final String INTENCION_CONSULTAR_HISTORIAS = "CONSULTAR_HISTORIAS";
  private static final String INTENCION_HISTORIAS_DUPLICADAS = "HISTORIAS_DUPLICADAS";
  private static final String INTENCION_ULTIMAS_HISTORIAS = "ULTIMAS_HISTORIAS";
  private static final String INTENCION_PACIENTES_SIN_HISTORIA = "PACIENTES_SIN_HISTORIA";
  private static final int LIMITE_ULTIMOS_POR_DEFECTO = 3;
  private static final int LIMITE_ULTIMOS_MAXIMO = 6;
  private static final Pattern DNI_PATTERN = Pattern.compile("\\d{8}");

  private final OllamaService ollamaService;
  private final PacienteService pacienteService;
  private final PacienteDuplicadoService pacienteDuplicadoService;
  private final HistoriaClinicaService historiaClinicaService;

  public OllamaEjecucionServiceImpl(
      OllamaService ollamaService,
      PacienteService pacienteService,
      PacienteDuplicadoService pacienteDuplicadoService,
      HistoriaClinicaService historiaClinicaService
  ) {
    this.ollamaService = ollamaService;
    this.pacienteService = pacienteService;
    this.pacienteDuplicadoService = pacienteDuplicadoService;
    this.historiaClinicaService = historiaClinicaService;
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
    if (esConsultaPacientesDuplicados(interpretacion)) {
      return consultarPacientesDuplicados(interpretacion);
    }
    if (esConsultaUltimosPacientes(interpretacion)) {
      return consultarUltimosPacientes(interpretacion);
    }
    if (esIntencion(interpretacion, CATEGORIA_HISTORIAS, INTENCION_CONSULTAR_HISTORIAS)) {
      return consultarHistorias(interpretacion);
    }
    if (esIntencion(interpretacion, CATEGORIA_HISTORIAS, INTENCION_HISTORIAS_DUPLICADAS)) {
      return consultarHistoriasDuplicadas(interpretacion);
    }
    if (esIntencion(interpretacion, CATEGORIA_HISTORIAS, INTENCION_ULTIMAS_HISTORIAS)) {
      return consultarUltimasHistorias(interpretacion);
    }
    if (esIntencion(interpretacion, CATEGORIA_PACIENTES, INTENCION_PACIENTES_SIN_HISTORIA)) {
      return consultarPacientesSinHistoria();
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
          interpretacion.nombre(),
          interpretacion.limite()
      );
    }
    if (esBusquedaDePaciente(interpretacion) || esVerificacionDePaciente(interpretacion)
        || esIntencion(interpretacion, CATEGORIA_HISTORIAS, INTENCION_CONSULTAR_HISTORIAS)
        || esIntencion(interpretacion, CATEGORIA_HISTORIAS, INTENCION_HISTORIAS_DUPLICADAS)) {
      String dni = normalizar(interpretacion.dni());
      String nombre = normalizar(interpretacion.nombre());
      if (dni != null && !dni.chars().allMatch(Character::isDigit) && nombre == null) {
        return new OllamaInterpretacionResponse(
            interpretacion.categoria(), interpretacion.intencion(), null, dni,
            interpretacion.limite()
        );
      }
      if (nombre != null && nombre.chars().allMatch(Character::isDigit) && dni == null) {
        return new OllamaInterpretacionResponse(
            interpretacion.categoria(), interpretacion.intencion(), nombre, null,
            interpretacion.limite()
        );
      }
    }
    return interpretacion;
  }

  private OllamaEjecucionResponse verificarExistencia(
      OllamaInterpretacionResponse interpretacion
  ) {
    return consultarPaciente(interpretacion, INTENCION_VERIFICAR_EXISTENCIA);
  }

  private OllamaEjecucionResponse buscarPaciente(OllamaInterpretacionResponse interpretacion) {
    return consultarPaciente(interpretacion, INTENCION_BUSCAR_PACIENTE);
  }

  private OllamaEjecucionResponse consultarPaciente(
      OllamaInterpretacionResponse interpretacion,
      String intencion
  ) {
    String dni = normalizar(interpretacion.dni());
    String nombre = normalizar(interpretacion.nombre());
    if (dni == null && nombre == null) {
      return new OllamaEjecucionResponse(
          CATEGORIA_PACIENTES,
          intencion,
          null,
          null,
          null,
          null,
          "Indica el DNI o el nombre del paciente que deseas consultar."
      );
    }
    if (dni != null && !DNI_PATTERN.matcher(dni).matches()) {
      return new OllamaEjecucionResponse(
          CATEGORIA_PACIENTES,
          intencion,
          null,
          dni,
          nombre,
          null,
          "El DNI debe contener exactamente 8 dígitos."
      );
    }

    try {
      ResponseModelGet<PacienteResponse> resultado = pacienteService.search(nombre, dni, 25);
      var pacientes = resultado.getData() == null ? java.util.List.<PacienteResponse>of()
          : resultado.getData();
      boolean encontrado = !pacientes.isEmpty();
      return new OllamaEjecucionResponse(
          CATEGORIA_PACIENTES,
          intencion,
          encontrado,
          dni,
          nombre,
          pacientes,
          mensajeResultadoPaciente(intencion, encontrado, dni)
      );
    } catch (DataAccessException exception) {
      throw new OllamaEjecucionException(
          "No se pudo consultar la información de pacientes en este momento.",
          exception
      );
    }
  }

  private String mensajeResultadoPaciente(String intencion, boolean encontrado, String dni) {
    if (INTENCION_VERIFICAR_EXISTENCIA.equals(intencion)) {
      if (encontrado) {
        return "El paciente se encuentra registrado.";
      }
      return dni == null
          ? "No se encontró ningún paciente con ese nombre."
          : "No se encontró un paciente con ese DNI.";
    }
    return encontrado
        ? "Se encontraron pacientes que coinciden con la búsqueda."
        : "No se encontraron pacientes con el criterio indicado.";
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

  private OllamaEjecucionResponse consultarUltimosPacientes(
      OllamaInterpretacionResponse interpretacion
  ) {
    int limite = normalizarLimiteUltimos(interpretacion.limite());
    try {
      UltimosPacientesResponse resultado = pacienteService.obtenerUltimosParaIntegracion(limite);
      List<PacienteResponse> pacientes = resultado.getPacientes() == null
          ? List.of()
          : resultado.getPacientes().stream()
              .map(paciente -> PacienteResponse.builder()
                  .idPaciente(paciente.getIdPaciente())
                  .numDocumento(paciente.getDni())
                  .nombreCompleto(paciente.getNombreCompleto())
                  .fechaCreacion(paciente.getFechaCreacion())
                  .build())
              .toList();
      String mensaje;
      if (pacientes.isEmpty()) {
        mensaje = "No se encontraron pacientes activos registrados.";
      } else if (interpretacion.limite() == null) {
        mensaje = "Estos son los " + pacientes.size()
            + " pacientes registrados más recientemente.";
      } else {
        mensaje = "Se encontraron los " + pacientes.size()
            + " pacientes registrados más recientemente.";
      }
      return new OllamaEjecucionResponse(
          CATEGORIA_PACIENTES,
          INTENCION_ULTIMOS_PACIENTES,
          !pacientes.isEmpty(),
          null,
          null,
          pacientes,
          mensaje
      );
    } catch (DataAccessException exception) {
      throw new OllamaEjecucionException(
          "No se pudo consultar la información de pacientes en este momento.",
          exception
      );
    }
  }

  int normalizarLimiteUltimos(Integer limite) {
    if (limite == null || limite <= 0) {
      return LIMITE_ULTIMOS_POR_DEFECTO;
    }
    return Math.max(LIMITE_ULTIMOS_POR_DEFECTO, Math.min(limite, LIMITE_ULTIMOS_MAXIMO));
  }

  private OllamaEjecucionResponse consultarHistorias(
      OllamaInterpretacionResponse interpretacion
  ) {
    String dni = normalizar(interpretacion.dni());
    String nombre = normalizar(interpretacion.nombre());
    OllamaEjecucionResponse error = validarCriterioHistorias(
        interpretacion.intencion(), dni, nombre);
    if (error != null) {
      return error;
    }
    try {
      ResponseModelGet<PacienteResponse> pacientesEncontrados = pacienteService.search(
          nombre, dni, 25);
      List<PacienteResponse> pacientes = filtrarPacientesPorNombre(
          datos(pacientesEncontrados), nombre);
      if (pacientes.isEmpty()) {
        return respuestaHistorias(interpretacion.intencion(), false, dni, nombre, List.of(),
            "No se encontró ningún paciente activo con ese "
                + (dni == null ? "nombre." : "DNI."));
      }
      List<HistoriaClinicaIntegracionItemResponse> historias = dni == null
          ? buscarHistoriasDePacientes(pacientes)
          : historias(historiaClinicaService.buscarParaIntegracion(dni));
      if (historias.isEmpty()) {
        return respuestaHistorias(interpretacion.intencion(), false, dni, nombre, List.of(),
            "El paciente se encuentra registrado, pero no tiene historias clínicas asociadas.");
      }
      int cantidad = historias.size();
      String referencia = nombre != null ? nombre : "el paciente con DNI " + dni;
      return respuestaHistorias(interpretacion.intencion(), true, dni, nombre,
          historias, "Se encontraron " + cantidad
              + " historias clínicas para " + referencia + ".");
    } catch (DataAccessException exception) {
      throw errorConsultaHistorias(exception);
    }
  }

  private OllamaEjecucionResponse consultarHistoriasDuplicadas(
      OllamaInterpretacionResponse interpretacion
  ) {
    String dni = normalizar(interpretacion.dni());
    String nombre = normalizar(interpretacion.nombre());
    if (dni != null && !DNI_PATTERN.matcher(dni).matches()) {
      return respuestaHistoriasDuplicadas(dni, nombre, null,
          "El DNI debe contener exactamente 8 dígitos.");
    }
    try {
      if (dni != null || nombre != null) {
        List<PacienteResponse> pacientes = filtrarPacientesPorNombre(
            datos(pacienteService.search(nombre, dni, 25)), nombre);
        if (pacientes.isEmpty()) {
          return respuestaHistoriasDuplicadas(dni, nombre, null,
              "No se encontró ningún paciente activo con ese "
                  + (dni == null ? "nombre." : "DNI."));
        }
      }
      DuplicadosHistoriasClinicasResponse grupos = nombre != null
          ? historiaClinicaService.obtenerDuplicadosPorNombreParaIntegracion(nombre)
          : historiaClinicaService.obtenerDuplicadosParaIntegracion(dni);
      grupos = sinRecomendaciones(grupos);
      String mensaje = grupos.isHayDuplicados()
          ? grupos.getMensaje()
          : (dni != null || nombre != null
              ? "No se encontraron historias clínicas duplicadas para este paciente."
              : "No se encontraron historias clínicas duplicadas.");
      return respuestaHistoriasDuplicadas(dni, nombre, grupos, mensaje);
    } catch (DataAccessException exception) {
      throw errorConsultaHistorias(exception);
    }
  }

  private OllamaEjecucionResponse consultarUltimasHistorias(
      OllamaInterpretacionResponse interpretacion
  ) {
    int limite = normalizarLimiteUltimos(interpretacion.limite());
    try {
      BusquedaHistoriasClinicasResponse resultado = historiaClinicaService
          .obtenerUltimasParaIntegracion(limite);
      List<HistoriaClinicaIntegracionItemResponse> historias = resultado.getHistoriasClinicas() == null
          ? List.of()
          : resultado.getHistoriasClinicas();
      String mensaje = historias.isEmpty()
          ? "No se encontraron historias clínicas registradas para pacientes activos."
          : "Últimas " + historias.size() + " historias clínicas registradas.";
      return respuestaHistorias(INTENCION_ULTIMAS_HISTORIAS, !historias.isEmpty(), null, null,
          historias, mensaje);
    } catch (DataAccessException exception) {
      throw errorConsultaHistorias(exception);
    }
  }

  private OllamaEjecucionResponse consultarPacientesSinHistoria() {
    try {
      HistoriasClinicasFaltantesPreviewResponse resultado = historiaClinicaService
          .obtenerHistoriasClinicasFaltantes();
      List<PacienteResponse> pacientes = resultado.getPacientes() == null
          ? List.of()
          : resultado.getPacientes().stream().limit(6)
              .map(paciente -> PacienteResponse.builder()
                  .idPaciente(paciente.getIdPaciente())
                  .nombreCompleto(paciente.getNombreCompleto())
                  .numDocumento(paciente.getDni())
                  .build())
              .toList();
      String mensaje = resultado.getCantidad() == 0
          ? "Todos los pacientes activos cuentan con una historia clínica asociada."
          : "Se encontraron " + resultado.getCantidad()
              + " pacientes activos sin historia clínica.";
      return new OllamaEjecucionResponse(CATEGORIA_PACIENTES,
          INTENCION_PACIENTES_SIN_HISTORIA, !pacientes.isEmpty(), null, null, pacientes,
          null, null, null, null, mensaje);
    } catch (DataAccessException exception) {
      throw errorConsultaHistorias(exception);
    }
  }

  private OllamaEjecucionResponse validarCriterioHistorias(
      String intencion,
      String dni,
      String nombre
  ) {
    if (dni == null && nombre == null) {
      return respuestaHistorias(intencion, null, null, null, null,
          "Indica el DNI o el nombre completo del paciente.");
    }
    if (dni != null && !DNI_PATTERN.matcher(dni).matches()) {
      return respuestaHistorias(intencion, null, dni, nombre, null,
          "El DNI debe contener exactamente 8 dígitos.");
    }
    return null;
  }

  private OllamaEjecucionResponse respuestaHistorias(
      String intencion,
      Boolean encontrado,
      String dni,
      String nombre,
      List<HistoriaClinicaIntegracionItemResponse> historias,
      String mensaje
  ) {
    return new OllamaEjecucionResponse(CATEGORIA_HISTORIAS, intencion, encontrado, dni, nombre,
        null, null, null, historias, null, mensaje);
  }

  private OllamaEjecucionResponse respuestaHistoriasDuplicadas(
      String dni,
      String nombre,
      DuplicadosHistoriasClinicasResponse grupos,
      String mensaje
  ) {
    return new OllamaEjecucionResponse(CATEGORIA_HISTORIAS, INTENCION_HISTORIAS_DUPLICADAS,
        grupos == null ? null : grupos.isHayDuplicados(), dni, nombre, null, null, null, null,
        grupos, mensaje);
  }

  private DuplicadosHistoriasClinicasResponse sinRecomendaciones(
      DuplicadosHistoriasClinicasResponse respuesta
  ) {
    List<GrupoDuplicadoHistoriaClinicaResponse> grupos = respuesta.getDuplicados() == null
        ? List.of()
        : respuesta.getDuplicados().stream()
            .map(grupo -> GrupoDuplicadoHistoriaClinicaResponse.builder()
                .tipo(grupo.getTipo())
                .valorCoincidente(grupo.getValorCoincidente())
                .cantidad(grupo.getCantidad())
                .historiasClinicas(grupo.getHistoriasClinicas())
                .build())
            .toList();
    return DuplicadosHistoriasClinicasResponse.builder()
        .hayDuplicados(respuesta.isHayDuplicados())
        .totalGrupos(respuesta.getTotalGrupos())
        .duplicados(grupos)
        .dniConsultado(respuesta.getDniConsultado())
        .mensaje(respuesta.getMensaje())
        .build();
  }

  private <T> List<T> datos(ResponseModelGet<T> resultado) {
    return resultado.getData() == null ? List.of() : resultado.getData();
  }

  private List<PacienteResponse> filtrarPacientesPorNombre(
      List<PacienteResponse> pacientes,
      String nombre
  ) {
    if (nombre == null) {
      return pacientes;
    }
    List<String> palabrasBuscadas = Arrays.asList(normalizarNombre(nombre).split(" "));
    return pacientes.stream()
        .filter(paciente -> {
          List<String> palabrasPaciente = Arrays.asList(
              normalizarNombreCompleto(paciente).split(" "));
          return palabrasBuscadas.stream().allMatch(palabrasPaciente::contains);
        })
        .toList();
  }

  private List<HistoriaClinicaIntegracionItemResponse> buscarHistoriasDePacientes(
      List<PacienteResponse> pacientes
  ) {
    LinkedHashMap<Integer, HistoriaClinicaIntegracionItemResponse> unicas = new LinkedHashMap<>();
    pacientes.stream()
        .map(PacienteResponse::getIdPaciente)
        .filter(java.util.Objects::nonNull)
        .map(id -> historiaClinicaService.buscarParaIntegracion("paciente:" + id))
        .flatMap(resultado -> historias(resultado).stream())
        .forEach(historia -> unicas.putIfAbsent(historia.getIdHistoriaClinica(), historia));
    return List.copyOf(unicas.values());
  }

  private List<HistoriaClinicaIntegracionItemResponse> historias(
      BusquedaHistoriasClinicasResponse resultado
  ) {
    return resultado.getHistoriasClinicas() == null ? List.of() : resultado.getHistoriasClinicas();
  }

  private OllamaEjecucionException errorConsultaHistorias(DataAccessException exception) {
    return new OllamaEjecucionException(
        "No se pudo consultar la información de historias clínicas en este momento.", exception);
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
        null,
        null,
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

  private boolean esConsultaUltimosPacientes(OllamaInterpretacionResponse interpretacion) {
    return CATEGORIA_PACIENTES.equals(interpretacion.categoria())
        && INTENCION_ULTIMOS_PACIENTES.equals(interpretacion.intencion());
  }

  private boolean esIntencion(
      OllamaInterpretacionResponse interpretacion,
      String categoria,
      String intencion
  ) {
    return categoria.equals(interpretacion.categoria())
        && intencion.equals(interpretacion.intencion());
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
