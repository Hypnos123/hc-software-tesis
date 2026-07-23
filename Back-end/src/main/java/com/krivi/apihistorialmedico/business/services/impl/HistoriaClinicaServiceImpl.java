package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.services.HistoriaClinicaService;
import com.krivi.apihistorialmedico.business.exception.BusquedaHistoriaClinicaException;
import com.krivi.apihistorialmedico.model.api.*;
import com.krivi.apihistorialmedico.model.entity.*;
import com.krivi.apihistorialmedico.repository.*;
import com.krivi.apihistorialmedico.util.Constant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

  public ResponseModelGet<HistoriaClinicaResponse> getAll() {
    List<HistoriaClinicaResponse> data = new ArrayList<>();
    historiaClinicaRepository.findAll().forEach(h -> data.add(toResponse(h)));
    return response(data);
  }

  public ResponseModelGet<HistoriaClinicaResponse> findById(int id) {
    return response(historiaClinicaRepository.findById(id).map(this::toResponse).map(List::of).orElse(List.of()));
  }

  public ResponseModelGet<HistoriaClinicaResponse> findByPaciente(int idPaciente) {
    return response(historiaClinicaRepository.findByPacienteIdPaciente(idPaciente).map(this::toResponse).map(List::of).orElse(List.of()));
  }

  public ResponseModelSet save(HistoriaClinicaRequest request) {
    ResponseModelSet r = new ResponseModelSet();
    if (request.getIdPaciente() == null) { r.setMensaje("Debe seleccionar un paciente registrado."); return r; }
    Optional<Paciente> paciente = pacienteRepository.findById(request.getIdPaciente());
    if (paciente.isEmpty()) { r.setMensaje("El paciente seleccionado no existe."); return r; }
    Optional<HistoriaClinica> existente = historiaClinicaRepository.findByPacienteIdPaciente(request.getIdPaciente());
    if (existente.isPresent()) { r.setMensaje("El paciente seleccionado ya cuenta con una historia clínica."); r.setIdGenerado(existente.get().getIdHistoriaClinica()); return r; }
    HistoriaClinica h = new HistoriaClinica();
    h.setPaciente(paciente.get());
    HistoriaClinica saved = historiaClinicaRepository.save(h);
    r.setIdGenerado(saved.getIdHistoriaClinica());
    r.setMensaje(Constant.MENSAJE_GUARDAR_OK);
    return r;
  }

  public ResponseModelSet update(HistoriaClinicaRequest request) {
    ResponseModelSet r = new ResponseModelSet();
    Optional<HistoriaClinica> historia = historiaClinicaRepository.findById(request.getIdHistoriaClinica());
    if (historia.isEmpty()) { r.setMensaje("Historia clínica no encontrada."); return r; }
    historiaClinicaRepository.save(historia.get());
    r.setMensaje(Constant.MENSAJE_EDITAR_OK);
    r.setIdGenerado(historia.get().getIdHistoriaClinica());
    return r;
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
    List<GrupoDuplicadoHistoriaClinicaResponse> grupos = new ArrayList<>();
    historiaClinicaRepository.findIdsPacienteConHistoriasDuplicadas().forEach(idPaciente ->
        grupos.add(toGrupoDuplicado("idPaciente", String.valueOf(idPaciente), historiaClinicaRepository.findForIntegracionByIdPaciente(idPaciente))));
    historiaClinicaRepository.findDnisNormalizadosConHistoriasDuplicadas().forEach(dni ->
        grupos.add(toGrupoDuplicado("dni", dni, historiaClinicaRepository.findForIntegracionByDni(dni))));
    return DuplicadosHistoriasClinicasResponse.builder().hayDuplicados(!grupos.isEmpty())
        .totalGrupos(grupos.size()).duplicados(grupos).build();
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
    List<HistoriaClinicaIntegracionItemResponse> items = historias.stream().map(this::toIntegracionResponse).toList();
    return GrupoDuplicadoHistoriaClinicaResponse.builder().tipo(tipo).valorCoincidente(valor).cantidad(items.size()).historiasClinicas(items).build();
  }

  private HistoriaClinicaIntegracionItemResponse toIntegracionResponse(HistoriaClinica historia) {
    Paciente paciente = historia.getPaciente();
    return HistoriaClinicaIntegracionItemResponse.builder().idHistoriaClinica(historia.getIdHistoriaClinica())
        .idPaciente(paciente.getIdPaciente()).dni(normalizarDni(paciente.getNumDocumento()))
        .nombreCompleto(String.join(" ", Optional.ofNullable(paciente.getNombres()).orElse(""), Optional.ofNullable(paciente.getApellidos()).orElse("")).trim())
        .fechaCreacion(historia.getFechaCreacion()).build();
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
    LocalDate birth = fechaNacimiento.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    LocalDate now = LocalDate.now();
    int age = now.getYear() - birth.getYear();
    if (now.getDayOfYear() < birth.getDayOfYear()) age--;
    return age;
  }
}
