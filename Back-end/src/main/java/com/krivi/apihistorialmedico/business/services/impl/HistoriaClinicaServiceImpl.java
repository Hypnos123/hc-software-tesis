package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.services.HistoriaClinicaService;
import com.krivi.apihistorialmedico.business.exception.BusquedaHistoriaClinicaException;
import com.krivi.apihistorialmedico.business.exception.CreacionHistoriaClinicaException;
import com.krivi.apihistorialmedico.model.api.*;
import com.krivi.apihistorialmedico.model.entity.*;
import com.krivi.apihistorialmedico.repository.*;
import com.krivi.apihistorialmedico.util.Constant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HistoriaClinicaServiceImpl implements HistoriaClinicaService {
  private static final ZoneId ZONA_HORARIA_LIMA = ZoneId.of("America/Lima");
  private static final int LIMITE_BUSQUEDA_INTEGRACION = 10;
  private static final Pattern DNI_PATTERN = Pattern.compile("\\d{8}");
  private static final Pattern ID_PATTERN = Pattern.compile("[1-9]\\d{0,6}");
  private static final Pattern SOLO_DIGITOS_PATTERN = Pattern.compile("\\d+");
  private static final Pattern CRITERIO_EXPLICITO_PATTERN = Pattern.compile("(?i)^(historia|paciente|dni)\\s*:\\s*(.*)$");
  @Autowired HistoriaClinicaRepository historiaClinicaRepository;
  @Autowired PacienteRepository pacienteRepository;
  @Autowired AntecedentesRepository antecedentesRepository;
  @Autowired ConsultaRepository consultaRepository;

  public ResponseModelGet<HistoriaClinicaResponse> getAll() {
    List<HistoriaClinicaResponse> data = new ArrayList<>();
    historiaClinicaRepository.findAllByPacienteEstadoRegistroOrderByIdHistoriaClinicaAsc(EstadoRegistroPaciente.ACTIVO).forEach(h -> data.add(toResponse(h)));
    return response(data);
  }

  public ResponseModelGet<HistoriaClinicaResponse> findById(int id) {
    return response(historiaClinicaRepository.findByIdHistoriaClinicaAndPacienteEstadoRegistro(id, EstadoRegistroPaciente.ACTIVO).map(this::toResponse).map(List::of).orElse(List.of()));
  }

  public ResponseModelGet<HistoriaClinicaResponse> findByPaciente(int idPaciente) {
    return response(historiaClinicaRepository.findAllByPacienteIdPacienteAndPacienteEstadoRegistroOrderByIdHistoriaClinicaAsc(idPaciente, EstadoRegistroPaciente.ACTIVO)
        .stream().map(this::toResponse).toList());
  }

  @Override
  @Transactional(readOnly = true)
  public HistoriasClinicasFaltantesPreviewResponse obtenerHistoriasClinicasFaltantes() {
    List<PacienteSinHistoriaClinicaResponse> pacientes = pacienteRepository
        .findByEstadoRegistroAndSinHistoriaClinica(EstadoRegistroPaciente.ACTIVO)
        .stream()
        .map(this::toPacienteSinHistoriaResponse)
        .toList();

    return HistoriasClinicasFaltantesPreviewResponse.builder()
        .cantidad(pacientes.size())
        .pacientes(pacientes)
        .build();
  }

  private PacienteSinHistoriaClinicaResponse toPacienteSinHistoriaResponse(Paciente paciente) {
    return PacienteSinHistoriaClinicaResponse.builder()
        .idPaciente(paciente.getIdPaciente())
        .nombreCompleto(nombreCompletoSeguro(paciente))
        .dniEnmascarado(enmascararDni(paciente.getNumDocumento()))
        .build();
  }

  private String nombreCompletoSeguro(Paciente paciente) {
    String nombreCompleto = java.util.stream.Stream.of(paciente.getNombres(), paciente.getApellidos())
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(valor -> !valor.isEmpty())
        .collect(java.util.stream.Collectors.joining(" "));
    return nombreCompleto.isEmpty() ? "Nombre no registrado" : nombreCompleto;
  }

  private String enmascararDni(String dni) {
    if (dni == null) return "No registrado";
    String dniNormalizado = dni.trim();
    if (!DNI_PATTERN.matcher(dniNormalizado).matches()) return "No registrado";
    return "******" + dniNormalizado.substring(dniNormalizado.length() - 2);
  }

  @Transactional
  public ResponseModelSet save(HistoriaClinicaRequest request) {
    validarRequestCreacion(request);
    String dni = request.getDni().trim();
    List<Paciente> coincidencias = pacienteRepository.findByDniNormalizado(dni);
    if (coincidencias.isEmpty()) {
      throw errorCreacion("PACIENTE_NO_ENCONTRADO", "No existe un paciente registrado con el DNI ingresado.", HttpStatus.NOT_FOUND);
    }
    if (coincidencias.size() > 1) {
      throw errorCreacion("DNI_AMBIGUO", "El DNI está asociado a varios pacientes y no se puede resolver automáticamente.", HttpStatus.CONFLICT);
    }

    Paciente paciente = coincidencias.getFirst();
    actualizarPaciente(paciente, request, dni);
    pacienteRepository.save(paciente);
    actualizarOCrearAntecedentes(paciente, request);

    HistoriaClinica historia = new HistoriaClinica();
    historia.setPaciente(paciente);
    HistoriaClinica saved = historiaClinicaRepository.save(historia);
    ResponseModelSet r = new ResponseModelSet();
    r.setIdGenerado(saved.getIdHistoriaClinica());
    r.setMensaje(Constant.MENSAJE_GUARDAR_OK);
    return r;
  }

  private void validarRequestCreacion(HistoriaClinicaRequest request) {
    if (request == null || request.getDni() == null || request.getDni().trim().isEmpty()) {
      throw errorCreacion("DNI_REQUERIDO", "El DNI es obligatorio.", HttpStatus.BAD_REQUEST);
    }
    if (!DNI_PATTERN.matcher(request.getDni().trim()).matches()) {
      throw errorCreacion("DNI_INVALIDO", "El DNI debe contener exactamente ocho dígitos.", HttpStatus.BAD_REQUEST);
    }
    validarTextoRequerido(request.getNombres(), "nombres", 120);
    validarTextoRequerido(request.getApellidos(), "apellidos", 120);
    validarTextoRequerido(request.getEstadoCivil(), "estado civil", 45);
    if (request.getFechaIngreso() == null) {
      throw errorCreacion("DATOS_INVALIDOS", "La fecha de ingreso es obligatoria.", HttpStatus.BAD_REQUEST);
    }
    validarFechaNacimiento(request.getFechaNacimiento());
    validarTextoOpcional(request.getEnfermedadesPrevias(), "enfermedades previas", 120);
    validarTextoOpcional(request.getCirugiasPrevias(), "cirugías previas", 120);
    validarTextoOpcional(request.getAlergiaMedicamentos(), "alergias a medicamentos", 120);
  }

  private void validarFechaNacimiento(LocalDate nacimiento) {
    if (nacimiento == null) {
      throw errorCreacion("FECHA_NACIMIENTO_INVALIDA", "La fecha de nacimiento es obligatoria.", HttpStatus.BAD_REQUEST);
    }
    LocalDate hoy = LocalDate.now(ZONA_HORARIA_LIMA);
    if (nacimiento.isAfter(hoy)) {
      throw errorCreacion("FECHA_NACIMIENTO_INVALIDA", "La fecha de nacimiento no puede ser futura.", HttpStatus.BAD_REQUEST);
    }
  }

  private void validarTextoRequerido(String valor, String campo, int longitudMaxima) {
    if (valor == null || valor.trim().isEmpty()) {
      throw errorCreacion("DATOS_INVALIDOS", "El campo " + campo + " es obligatorio.", HttpStatus.BAD_REQUEST);
    }
    validarLongitud(valor.trim(), campo, longitudMaxima);
  }

  private void validarTextoOpcional(String valor, String campo, int longitudMaxima) {
    if (valor != null) validarLongitud(valor.trim(), campo, longitudMaxima);
  }

  private void validarLongitud(String valor, String campo, int longitudMaxima) {
    if (valor.length() > longitudMaxima) {
      throw errorCreacion("DATOS_INVALIDOS", "El campo " + campo + " admite hasta " + longitudMaxima + " caracteres.", HttpStatus.BAD_REQUEST);
    }
  }

  private void actualizarPaciente(Paciente paciente, HistoriaClinicaRequest request, String dni) {
    paciente.setFechaIngreso(java.sql.Date.valueOf(request.getFechaIngreso()));
    paciente.setFechaNacimiento(java.sql.Date.valueOf(request.getFechaNacimiento()));
    paciente.setApellidos(request.getApellidos().trim());
    paciente.setNombres(request.getNombres().trim());
    paciente.setEstadoCivil(request.getEstadoCivil().trim());
    paciente.setNumDocumento(dni);
  }

  private void actualizarOCrearAntecedentes(Paciente paciente, HistoriaClinicaRequest request) {
    Antecedentes antecedentes = antecedentesRepository.findByPacienteIdPaciente(paciente.getIdPaciente())
        .stream().findFirst().orElseGet(Antecedentes::new);
    antecedentes.setPaciente(paciente);
    antecedentes.setEnfermedadesPrevias(normalizarOpcional(request.getEnfermedadesPrevias()));
    antecedentes.setCirugiasPrevias(normalizarOpcional(request.getCirugiasPrevias()));
    antecedentes.setAlergiaMedicamentos(normalizarOpcional(request.getAlergiaMedicamentos()));
    antecedentesRepository.save(antecedentes);
  }

  private String normalizarOpcional(String valor) {
    if (valor == null || valor.trim().isEmpty()) return null;
    return valor.trim();
  }

  private CreacionHistoriaClinicaException errorCreacion(String codigo, String mensaje, HttpStatus status) {
    return new CreacionHistoriaClinicaException(codigo, mensaje, status);
  }

  @Transactional
  public ResponseModelSet update(int idHistoriaClinica, HistoriaClinicaUpdateRequest request) {
    HistoriaClinica historia = historiaClinicaRepository.findByIdHistoriaClinicaAndPacienteEstadoRegistro(idHistoriaClinica, EstadoRegistroPaciente.ACTIVO)
        .orElseThrow(() -> errorCreacion("HISTORIA_NO_ENCONTRADA", "La historia clínica no existe.", HttpStatus.NOT_FOUND));
    validarRequestActualizacion(request);
    Paciente paciente = historia.getPaciente();
    actualizarPaciente(paciente, request);
    pacienteRepository.save(paciente);
    actualizarOCrearAntecedentes(paciente, request);
    HistoriaClinica actualizada = historiaClinicaRepository.save(historia);

    ResponseModelSet r = new ResponseModelSet();
    r.setMensaje(Constant.MENSAJE_EDITAR_OK);
    r.setIdGenerado(actualizada.getIdHistoriaClinica());
    return r;
  }

  private void validarRequestActualizacion(HistoriaClinicaUpdateRequest request) {
    if (request == null) {
      throw errorCreacion("DATOS_INVALIDOS", "Los datos de actualización son obligatorios.", HttpStatus.BAD_REQUEST);
    }
    validarTextoRequerido(request.getNombres(), "nombres", 120);
    validarTextoRequerido(request.getApellidos(), "apellidos", 120);
    validarTextoRequerido(request.getEstadoCivil(), "estado civil", 45);
    if (request.getFechaIngreso() == null) {
      throw errorCreacion("DATOS_INVALIDOS", "La fecha de ingreso es obligatoria.", HttpStatus.BAD_REQUEST);
    }
    validarFechaNacimiento(request.getFechaNacimiento());
    validarTextoOpcional(request.getEnfermedadesPrevias(), "enfermedades previas", 120);
    validarTextoOpcional(request.getCirugiasPrevias(), "cirugías previas", 120);
    validarTextoOpcional(request.getAlergiaMedicamentos(), "alergias a medicamentos", 120);
  }

  private void actualizarPaciente(Paciente paciente, HistoriaClinicaUpdateRequest request) {
    paciente.setFechaIngreso(java.sql.Date.valueOf(request.getFechaIngreso()));
    paciente.setFechaNacimiento(java.sql.Date.valueOf(request.getFechaNacimiento()));
    paciente.setApellidos(request.getApellidos().trim());
    paciente.setNombres(request.getNombres().trim());
    paciente.setEstadoCivil(request.getEstadoCivil().trim());
  }

  private void actualizarOCrearAntecedentes(Paciente paciente, HistoriaClinicaUpdateRequest request) {
    Antecedentes antecedentes = antecedentesRepository.findByPacienteIdPaciente(paciente.getIdPaciente())
        .stream().findFirst().orElseGet(Antecedentes::new);
    antecedentes.setPaciente(paciente);
    antecedentes.setEnfermedadesPrevias(normalizarOpcional(request.getEnfermedadesPrevias()));
    antecedentes.setCirugiasPrevias(normalizarOpcional(request.getCirugiasPrevias()));
    antecedentes.setAlergiaMedicamentos(normalizarOpcional(request.getAlergiaMedicamentos()));
    antecedentesRepository.save(antecedentes);
  }

  @Override
  public BusquedaHistoriasClinicasResponse buscarParaIntegracion(String criterio) {
    List<HistoriaClinica> historias = buscarHistorias(validarCriterioBusqueda(criterio));
    List<HistoriaClinicaIntegracionItemResponse> resultados = historias.stream()
        .collect(java.util.stream.Collectors.toMap(HistoriaClinica::getIdHistoriaClinica, this::toIntegracionResponse, (primera, segunda) -> primera, LinkedHashMap::new))
        .values().stream().limit(LIMITE_BUSQUEDA_INTEGRACION).toList();

    if (resultados.isEmpty()) {
      return BusquedaHistoriasClinicasResponse.builder().encontrado(false).tipoResultado("sin_resultados")
          .historiasClinicas(resultados).mensaje("No se encontró ninguna historia clínica con el criterio indicado.").build();
    }
    return BusquedaHistoriasClinicasResponse.builder().encontrado(true)
        .tipoResultado(resultados.size() == 1 ? "unico" : "multiple").historiasClinicas(resultados).build();
  }

  @Override
  public EstadisticasHistoriasClinicasResponse obtenerEstadisticasParaIntegracion() {
    LocalDateTime inicioHoy = LocalDate.now(ZONA_HORARIA_LIMA).atStartOfDay();
    return EstadisticasHistoriasClinicasResponse.builder().totalHistoriasClinicas(historiaClinicaRepository.count())
        .creadasHoy(historiaClinicaRepository.countByFechaCreacionGreaterThanEqualAndFechaCreacionLessThan(inicioHoy, inicioHoy.plusDays(1))).build();
  }

  @Override
  public DuplicadosHistoriasClinicasResponse obtenerDuplicadosParaIntegracion() {
    return obtenerDuplicadosParaIntegracion(null);
  }

  @Override
  public DuplicadosHistoriasClinicasResponse obtenerDuplicadosParaIntegracion(String dni) {
    String dniNormalizado = dni == null ? null : dni.trim();
    if (dniNormalizado != null && !DNI_PATTERN.matcher(dniNormalizado).matches()) {
      throw new BusquedaHistoriaClinicaException("DNI_INVALIDO", "El DNI debe contener exactamente ocho dígitos.", HttpStatus.BAD_REQUEST);
    }

    List<HistoriaClinica> historias;
    if (dniNormalizado == null) {
      historias = historiaClinicaRepository.findAllForIntegracion();
    } else {
      if (pacienteRepository.findByDniNormalizado(dniNormalizado).isEmpty()) {
        return respuestaDuplicados(false, dniNormalizado, List.of(), "No se encontró un paciente activo con el DNI ingresado.");
      }
      historias = historiaClinicaRepository.findForIntegracionByDni(dniNormalizado);
    }

    Map<String, List<HistoriaClinica>> candidatos = new LinkedHashMap<>();
    historias.forEach(historia -> {
      String historiaDni = normalizarDni(historia.getPaciente().getNumDocumento());
      String clave = historiaDni == null ? "PACIENTE:" + historia.getPaciente().getIdPaciente() : "DNI:" + historiaDni;
      candidatos.computeIfAbsent(clave, ignored -> new ArrayList<>()).add(historia);
    });

    List<GrupoDuplicadoHistoriaClinicaResponse> grupos = candidatos.entrySet().stream()
        .filter(entry -> entry.getValue().size() >= 2)
        .map(entry -> toGrupoDuplicado(entry.getKey().startsWith("DNI:") ? "dni" : "idPaciente",
            entry.getKey().substring(entry.getKey().indexOf(':') + 1), entry.getValue()))
        .toList();

    if (!grupos.isEmpty()) {
      int cantidad = grupos.stream().mapToInt(GrupoDuplicadoHistoriaClinicaResponse::getCantidad).sum();
      String mensaje = dniNormalizado == null
          ? "Se encontraron " + grupos.size() + " grupos de posibles historias clínicas duplicadas."
          : "Se encontraron " + cantidad + " posibles historias clínicas duplicadas para el DNI " + dniNormalizado + ".";
      return respuestaDuplicados(true, dniNormalizado, grupos, mensaje);
    }
    String mensaje = dniNormalizado == null
        ? "No se encontraron historias clínicas duplicadas. Cada paciente activo tiene una sola historia clínica."
        : "El paciente con DNI " + dniNormalizado + " tiene una sola historia clínica. No se detectó duplicidad.";
    return respuestaDuplicados(false, dniNormalizado, List.of(), mensaje);
  }

  private DuplicadosHistoriasClinicasResponse respuestaDuplicados(boolean hayDuplicados, String dni,
      List<GrupoDuplicadoHistoriaClinicaResponse> grupos, String mensaje) {
    return DuplicadosHistoriasClinicasResponse.builder().hayDuplicados(hayDuplicados).totalGrupos(grupos.size())
        .duplicados(grupos).dniConsultado(dni).mensaje(mensaje).build();
  }

  private List<HistoriaClinica> buscarHistorias(CriterioBusqueda criterio) {
    return switch (criterio.tipo()) {
      case HISTORIA -> historiaClinicaRepository.findForIntegracionByIdHistoriaClinica(Integer.parseInt(criterio.valor()));
      case PACIENTE -> historiaClinicaRepository.findForIntegracionByIdPaciente(Integer.parseInt(criterio.valor()));
      case DNI -> historiaClinicaRepository.findForIntegracionByDni(criterio.valor());
      case NUMERICO_AMBIGUO -> {
        List<HistoriaClinica> historias = new ArrayList<>(historiaClinicaRepository.findForIntegracionByIdHistoriaClinica(Integer.parseInt(criterio.valor())));
        historias.addAll(historiaClinicaRepository.findForIntegracionByIdPaciente(Integer.parseInt(criterio.valor())));
        yield historias;
      }
      case NOMBRE -> buscarPorNombre(criterio.valor());
    };
  }

  private List<HistoriaClinica> buscarPorNombre(String criterio) {
    String[] palabras = normalizarTexto(criterio).split(" ");
    return historiaClinicaRepository.findAllForIntegracion().stream()
        .filter(historia -> contieneTodasLasPalabras(historia.getPaciente(), palabras))
        .toList();
  }

  private boolean contieneTodasLasPalabras(Paciente paciente, String[] palabras) {
    String nombrePaciente = normalizarTexto(String.join(" ",
        Optional.ofNullable(paciente.getNombres()).orElse(""),
        Optional.ofNullable(paciente.getApellidos()).orElse("")));
    return Arrays.stream(palabras).allMatch(nombrePaciente::contains);
  }

  private String normalizarTexto(String valor) {
    return java.text.Normalizer.normalize(valor, java.text.Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", " ")
        .trim();
  }

  private CriterioBusqueda validarCriterioBusqueda(String criterio) {
    if (criterio == null || criterio.trim().isEmpty()) {
      throw new BusquedaHistoriaClinicaException("CRITERIO_VACIO", "El criterio de búsqueda es obligatorio.", HttpStatus.BAD_REQUEST);
    }
    String valor = criterio.trim();
    Matcher explicito = CRITERIO_EXPLICITO_PATTERN.matcher(valor);
    if (explicito.matches()) return validarCriterioExplicito(explicito.group(1).toLowerCase(), explicito.group(2).trim());
    if (DNI_PATTERN.matcher(valor).matches()) return new CriterioBusqueda(TipoCriterio.DNI, valor);
    if (ID_PATTERN.matcher(valor).matches()) return new CriterioBusqueda(TipoCriterio.NUMERICO_AMBIGUO, valor);
    if (SOLO_DIGITOS_PATTERN.matcher(valor).matches()) throw criterioInvalido();
    if (normalizarTexto(valor).isEmpty()) throw criterioInvalido();
    return new CriterioBusqueda(TipoCriterio.NOMBRE, valor);
  }

  private CriterioBusqueda validarCriterioExplicito(String tipo, String valor) {
    if ("dni".equals(tipo) && DNI_PATTERN.matcher(valor).matches()) return new CriterioBusqueda(TipoCriterio.DNI, valor);
    if ("historia".equals(tipo) && ID_PATTERN.matcher(valor).matches()) return new CriterioBusqueda(TipoCriterio.HISTORIA, valor);
    if ("paciente".equals(tipo) && ID_PATTERN.matcher(valor).matches()) return new CriterioBusqueda(TipoCriterio.PACIENTE, valor);
    throw criterioInvalido();
  }

  private BusquedaHistoriaClinicaException criterioInvalido() {
    return new BusquedaHistoriaClinicaException("CRITERIO_INVALIDO", "Use un DNI de 8 dígitos, un ID positivo de hasta 7 dígitos, un prefijo historia:, paciente: o dni:, o un nombre.", HttpStatus.BAD_REQUEST);
  }

  private GrupoDuplicadoHistoriaClinicaResponse toGrupoDuplicado(String tipo, String valor, List<HistoriaClinica> historias) {
    Map<Integer, ResumenConsultasHistoria> resumenes = resumenesConsultas(historias);
    List<HistoriaClinicaIntegracionItemResponse> items = historias.stream()
        .map(historia -> toIntegracionResponse(historia, resumenes.getOrDefault(historia.getIdHistoriaClinica(), ResumenConsultasHistoria.VACIO)))
        .sorted(comparadorRecomendacion()).toList();
    HistoriaClinicaIntegracionItemResponse recomendada = items.getFirst();
    String explicacion = explicarRecomendacion(recomendada, items.size() > 1 ? items.get(1) : null);
    return GrupoDuplicadoHistoriaClinicaResponse.builder().tipo(tipo).valorCoincidente(valor).cantidad(items.size())
        .historiasClinicas(items).idHistoriaClinicaRecomendada(recomendada.getIdHistoriaClinica()).recomendacion(explicacion).build();
  }

  private HistoriaClinicaIntegracionItemResponse toIntegracionResponse(HistoriaClinica historia) {
    return toIntegracionResponse(historia, ResumenConsultasHistoria.VACIO);
  }

  private HistoriaClinicaIntegracionItemResponse toIntegracionResponse(HistoriaClinica historia, ResumenConsultasHistoria resumen) {
    Paciente paciente = historia.getPaciente();
    return HistoriaClinicaIntegracionItemResponse.builder().idHistoriaClinica(historia.getIdHistoriaClinica())
        .idPaciente(paciente.getIdPaciente()).dni(normalizarDni(paciente.getNumDocumento()))
        .nombreCompleto(String.join(" ", Optional.ofNullable(paciente.getNombres()).orElse(""), Optional.ofNullable(paciente.getApellidos()).orElse("")).trim())
        .fechaCreacion(historia.getFechaCreacion()).ultimaActualizacion(historia.getUltimaActualizacion())
        .cantidadConsultas(resumen.cantidad()).ultimaActividadClinica(resumen.ultimaActividad()).estado("ACTIVA").build();
  }

  private Map<Integer, ResumenConsultasHistoria> resumenesConsultas(List<HistoriaClinica> historias) {
    List<Integer> ids = historias.stream().map(HistoriaClinica::getIdHistoriaClinica).toList();
    if (ids.isEmpty()) return Map.of();
    Map<Integer, ResumenConsultasHistoria> resumenes = new HashMap<>();
    consultaRepository.resumirPorHistoriasClinicas(ids).forEach(fila -> resumenes.put(((Number) fila[0]).intValue(),
        new ResumenConsultasHistoria(((Number) fila[1]).longValue(), (LocalDateTime) fila[2])));
    return resumenes;
  }

  private Comparator<HistoriaClinicaIntegracionItemResponse> comparadorRecomendacion() {
    return Comparator.comparingLong(HistoriaClinicaIntegracionItemResponse::getCantidadConsultas).reversed()
        .thenComparing(HistoriaClinicaIntegracionItemResponse::getUltimaActividadClinica, Comparator.nullsLast(Comparator.reverseOrder()))
        .thenComparing(HistoriaClinicaIntegracionItemResponse::getFechaCreacion, Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(HistoriaClinicaIntegracionItemResponse::getIdHistoriaClinica);
  }

  private String explicarRecomendacion(HistoriaClinicaIntegracionItemResponse recomendada,
      HistoriaClinicaIntegracionItemResponse alternativa) {
    String razon;
    if (alternativa == null || recomendada.getCantidadConsultas() != alternativa.getCantidadConsultas()) {
      razon = "contiene " + recomendada.getCantidadConsultas() + " consultas";
    } else if (!Objects.equals(recomendada.getUltimaActividadClinica(), alternativa.getUltimaActividadClinica())) {
      razon = "registra la actividad clínica más reciente";
    } else if (!Objects.equals(recomendada.getFechaCreacion(), alternativa.getFechaCreacion())) {
      razon = "fue creada antes que la historia clínica ID " + alternativa.getIdHistoriaClinica();
    } else {
      razon = "tiene el ID menor como criterio final de desempate";
    }
    return "Se recomienda conservar la historia clínica ID " + recomendada.getIdHistoriaClinica() + " porque " + razon + ".";
  }

  private record ResumenConsultasHistoria(long cantidad, LocalDateTime ultimaActividad) {
    private static final ResumenConsultasHistoria VACIO = new ResumenConsultasHistoria(0, null);
  }

  private String normalizarDni(String dni) {
    if (dni == null) return null;
    String normalizado = dni.trim();
    return DNI_PATTERN.matcher(normalizado).matches() ? normalizado : null;
  }

  private enum TipoCriterio { HISTORIA, PACIENTE, DNI, NUMERICO_AMBIGUO, NOMBRE }
  private record CriterioBusqueda(TipoCriterio tipo, String valor) { }

  private ResponseModelGet<HistoriaClinicaResponse> response(List<HistoriaClinicaResponse> data) { ResponseModelGet<HistoriaClinicaResponse> r = new ResponseModelGet<>(); r.setData(data); r.setMensaje(Constant.MENSAJE_CONSULTA_OK); return r; }

  private HistoriaClinicaResponse toResponse(HistoriaClinica h) {
    Paciente p = h.getPaciente();
    Antecedentes a = antecedentesRepository.findByPacienteIdPaciente(p.getIdPaciente()).stream().findFirst().orElse(null);
    return HistoriaClinicaResponse.builder().idHistoriaClinica(h.getIdHistoriaClinica()).fechaCreacion(h.getFechaCreacion()).ultimaActualizacion(h.getUltimaActualizacion()).idPaciente(p.getIdPaciente()).nombres(p.getNombres()).apellidos(p.getApellidos()).fechaIngreso(p.getFechaIngreso()).fechaNacimiento(p.getFechaNacimiento()).estadoCivil(normalizeEstadoCivil(p.getEstadoCivil())).numDocumento(p.getNumDocumento()).edad(edad(p.getFechaNacimiento())).enfermedadesPrevias(a == null ? null : a.getEnfermedadesPrevias()).cirugiasPrevias(a == null ? null : a.getCirugiasPrevias()).alergiaMedicamentos(a == null ? null : a.getAlergiaMedicamentos()).build();
  }

  private String normalizeEstadoCivil(String estadoCivil) {
    if (estadoCivil == null || estadoCivil.trim().isEmpty()) {
      return estadoCivil;
    }

    String normalized = java.text.Normalizer.normalize(estadoCivil, java.text.Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .trim()
        .toUpperCase();

    if (normalized.startsWith("SOLTER")) return "SOLTERO";
    if (normalized.startsWith("CASAD")) return "CASADO";
    if (normalized.startsWith("DIVORCIAD")) return "DIVORCIADO";
    if (normalized.startsWith("VIUD")) return "VIUDO";

    return normalized;
  }


  private Integer edad(Date fechaNacimiento) {
    if (fechaNacimiento == null) return null;
    LocalDate birth = fechaNacimiento instanceof java.sql.Date fechaSql
        ? fechaSql.toLocalDate()
        : fechaNacimiento.toInstant().atZone(ZONA_HORARIA_LIMA).toLocalDate();
    return Period.between(birth, LocalDate.now(ZONA_HORARIA_LIMA)).getYears();
  }
}
