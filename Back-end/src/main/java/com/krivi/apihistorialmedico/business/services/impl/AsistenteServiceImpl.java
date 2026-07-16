package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.services.AsistenteService;
import com.krivi.apihistorialmedico.model.api.AsistenteRequest;
import com.krivi.apihistorialmedico.model.api.AsistenteResponse;
import com.krivi.apihistorialmedico.model.entity.Consulta;
import com.krivi.apihistorialmedico.model.entity.HistoriaClinica;
import com.krivi.apihistorialmedico.model.entity.Paciente;
import com.krivi.apihistorialmedico.model.entity.Usuario;
import com.krivi.apihistorialmedico.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AsistenteServiceImpl implements AsistenteService {
  private static final Pattern DNI_PATTERN = Pattern.compile("\\b\\d{8}\\b");

  @Autowired PacienteRepository pacienteRepository; @Autowired HistoriaClinicaRepository historiaClinicaRepository; @Autowired ConsultaRepository consultaRepository; @Autowired UsuarioRepository usuarioRepository;
  @Override public AsistenteResponse preguntar(AsistenteRequest request, Integer idUsuario) {
    String q = normalizar(request == null ? null : request.getPregunta());
    if (q.isBlank()) return resp("PREGUNTA_VACIA", "Escribe una pregunta para poder ayudarte.", Map.of());
    try {
      Periodo p = periodo(q); Usuario u = idUsuario == null ? null : usuarioRepository.findById(idUsuario).orElse(null); Integer idEmpleado = u != null && u.getEmpleado() != null ? u.getEmpleado().getIdEmpleado() : null;
      String ayuda = ayuda(q); if (ayuda != null) return resp("AYUDA_USO_SISTEMA", ayuda, Map.of());
      AsistenteResponse consultasPaciente = consultarConsultasMedicasPaciente(q); if (consultasPaciente != null) return consultasPaciente;
      AsistenteResponse historiaPaciente = verificarHistoriaClinicaPaciente(q); if (historiaPaciente != null) return historiaPaciente;
      AsistenteResponse pacientes = intencionPacientes(q, p); if (pacientes != null) return pacientes;
      AsistenteResponse duplicado = buscarPacienteDuplicado(q); if (duplicado != null) return duplicado;
      if (contiene(q,"doctor autenticado","mis consultas","asignadas al doctor","consultas asignadas")) { if (idEmpleado == null) return sinPermiso(); if (contiene(q,"atendio","atendidas")) return cantidad("CONSULTAS_ATENDIDAS_DOCTOR", consultaRepository.countByDoctorResponsableIdEmpleadoAndEstado(idEmpleado,"ATENDIDO"), "El doctor autenticado atendió %d consultas.", p); return cantidad("CONSULTAS_ASIGNADAS_DOCTOR", consultaRepository.countByDoctorResponsableIdEmpleado(idEmpleado), "El doctor autenticado tiene %d consultas asignadas.", p); }
      if (contiene(q,"paciente") && contiene(q,"sin historia","no tienen historia")) return cantidad("PACIENTES_SIN_HISTORIA_CLINICA", pacienteRepository.count()-historiaClinicaRepository.count(), "Actualmente hay %d pacientes sin historia clínica.", p);
      if (contiene(q,"paciente") && contiene(q,"con historia","tienen historia")) return cantidad("PACIENTES_CON_HISTORIA_CLINICA", historiaClinicaRepository.count(), "Actualmente hay %d pacientes con historia clínica.", p);
      if (contiene(q,"paciente")) { long c=p.total()?pacienteRepository.count():pacienteRepository.countByFechaIngresoGreaterThanEqualAndFechaIngresoLessThan(Date.from(p.inicio().atZone(ZoneId.systemDefault()).toInstant()), Date.from(p.fin().atZone(ZoneId.systemDefault()).toInstant())); return cantidad("PACIENTES_REGISTRADOS", c, pref(p)+"hay %d pacientes registrados.", p); }
      if (contiene(q,"historia")) { if (contiene(q,"incompleta")) return cantidad("HISTORIAS_CLINICAS_INCOMPLETAS",0,"No se encontraron historias clínicas incompletas según los campos obligatorios actuales.",p); long c=p.total()?historiaClinicaRepository.count():historiaClinicaRepository.countByFechaCreacionGreaterThanEqualAndFechaCreacionLessThan(p.inicio(),p.fin()); return cantidad("HISTORIAS_CLINICAS",c,pref(p)+"hay %d historias clínicas.",p); }
      if (contiene(q,"especialidad")) return ranking("ESPECIALIDADES_MAS_REQUERIDAS", p.total()?consultaRepository.rankingEspecialidades():consultaRepository.rankingEspecialidades(p.inicio(),p.fin()), "especialidades");
      if (contiene(q,"enfermedad")) return ranking("TIPOS_ENFERMEDAD_MAS_REGISTRADOS", p.total()?consultaRepository.rankingTiposEnfermedad():consultaRepository.rankingTiposEnfermedad(p.inicio(),p.fin()), "tipos de enfermedad");
      if (contiene(q,"doctor") && contiene(q,"mas","cada doctor")) return ranking("DOCTORES_CON_MAS_ATENCIONES", consultaRepository.rankingDoctoresAtenciones(), "doctores");
      if (contiene(q,"consulta medica","consultas medicas","atencion medica","atenciones medicas","consulta","atencion","atenciones")) { if (contiene(q,"incompleta","todavia no","faltan")) return incompletas(); if (contiene(q,"pendiente","por atender","faltan")) return cantidad("CONSULTAS_MEDICAS_PENDIENTES", p.total()?consultaRepository.countByEstado("PENDIENTE"):consultaRepository.countByEstadoAndFechaCreacionGreaterThanEqualAndFechaCreacionLessThan("PENDIENTE",p.inicio(),p.fin()), pref(p)+"hay %d consultas médicas por atender.", p); if (contiene(q,"atendida","atendidas","atendio")) return cantidad("CONSULTAS_MEDICAS_ATENDIDAS", p.total()?consultaRepository.countByEstado("ATENDIDO"):consultaRepository.countByEstadoAndFechaCreacionGreaterThanEqualAndFechaCreacionLessThan("ATENDIDO",p.inicio(),p.fin()), pref(p)+"hay %d consultas médicas atendidas.", p); long c=p.total()?consultaRepository.count():consultaRepository.countByFechaCreacionGreaterThanEqualAndFechaCreacionLessThan(p.inicio(),p.fin()); return cantidad("CONSULTAS_MEDICAS_REGISTRADAS",c,pref(p)+"hay %d consultas médicas registradas.",p); }
    } catch (Exception e) { return resp("ERROR_INTERNO", "No pude obtener la información en este momento. Inténtalo nuevamente.", Map.of()); }
    return resp("NO_RECONOCIDA", mensajeConsultaNoReconocida(), Map.of());
  }



  private AsistenteResponse consultarConsultasMedicasPaciente(String q) {
    if (!esConsultaMedicaPaciente(q)) return null;
    ResultadoBusquedaHistoria resultado = buscarPacienteParaHistoria(q);
    if (resultado.requiereDatos()) return resp("CONSULTAS_MEDICAS_REQUIERE_PACIENTE", "Indica el DNI, ID o nombre completo del paciente para consultar sus consultas médicas.", Map.of());
    if (resultado.paciente().isEmpty()) {
      if (!resultado.similares().isEmpty()) return resp("CONSULTAS_MEDICAS_PACIENTE_AMBIGUO", respuestaPacienteAmbiguoConsultas(resultado.similares()), Map.of("resultados", resultado.similares().stream().map(this::pacienteMap).collect(Collectors.toList())));
      return resp("CONSULTAS_MEDICAS_PACIENTE_NO_ENCONTRADO", "No se encontró un paciente registrado con esos datos.", Map.of());
    }
    Paciente paciente = resultado.paciente().get();
    Optional<HistoriaClinica> historia = historiaClinicaRepository.findByPacienteIdPaciente(paciente.getIdPaciente());
    if (historia.isEmpty()) return resp("CONSULTAS_MEDICAS_SIN_HISTORIA_CLINICA", "El paciente está registrado, pero no cuenta con una historia clínica. Por lo tanto, no tiene consultas médicas registradas.", Map.of("paciente", pacienteMap(paciente)));
    List<Consulta> consultas = new ArrayList<>(consultaRepository.findByHistoriaClinicaIdHistoriaClinica(historia.get().getIdHistoriaClinica()));
    consultas.sort(Comparator.comparing(this::fechaOrdenConsulta, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
    if (consultas.isEmpty()) return resp("CONSULTAS_MEDICAS_SIN_REGISTROS", "La historia clínica del paciente no cuenta con consultas médicas registradas.", Map.of("paciente", pacienteMap(paciente), "historiaClinica", historiaMap(historia.get(), paciente)));
    List<Consulta> filtradas = consultas;
    String intencion = "CONSULTAS_MEDICAS_PACIENTE_LISTADO";
    String titulo = "Consultas médicas recientes del paciente";
    if (contiene(q, "pendiente", "pendientes", "por atender")) { filtradas = filtrarEstado(consultas, "PENDIENTE"); intencion = "CONSULTAS_MEDICAS_PACIENTE_PENDIENTES"; titulo = "Consultas médicas pendientes del paciente"; }
    else if (contiene(q, "atendida", "atendidas", "atendio")) { filtradas = filtrarEstado(consultas, "ATENDIDO"); intencion = "CONSULTAS_MEDICAS_PACIENTE_ATENDIDAS"; titulo = "Consultas médicas atendidas del paciente"; }
    else if (contiene(q, "ultima", "ultimo", "reciente")) { filtradas = consultas.stream().limit(1).collect(Collectors.toList()); intencion = "CONSULTAS_MEDICAS_PACIENTE_ULTIMA"; titulo = "Última consulta médica del paciente"; }
    if (contiene(q, "cuantas", "cuantos", "cantidad")) return resp(intencion + "_CANTIDAD", "El paciente " + nombreCompleto(paciente) + " tiene " + filtradas.size() + " consultas médicas" + sufijoEstado(q) + ".", Map.of("cantidad", filtradas.size(), "paciente", pacienteMap(paciente), "historiaClinica", historiaMap(historia.get(), paciente)));
    if (filtradas.isEmpty()) return resp(intencion, "No se encontraron consultas médicas" + sufijoEstado(q) + " para este paciente.", Map.of("resultados", List.of(), "paciente", pacienteMap(paciente)));
    return resp(intencion, titulo + ":\n" + filtradas.stream().limit(5).map(c -> detalleConsultaMedica(c, paciente)).collect(Collectors.joining("\n\n")), Map.of("resultados", filtradas.stream().limit(5).map(c -> consultaMedicaMap(c, paciente)).collect(Collectors.toList()), "cantidad", filtradas.size()));
  }

  private boolean esConsultaMedicaPaciente(String q) {
    boolean mencionaConsultas = contiene(q, "consulta medica", "consultas medicas", "atencion medica", "atenciones medicas");
    boolean identificaPaciente = contiene(q, "paciente", "dni", " id ") || DNI_PATTERN.matcher(q).find() || Pattern.compile("\\b(?:id|codigo|cod)\\s*\\d+\\b").matcher(q).find() || terminosNombre(extraerNombrePaciente(q)).size() >= 2;
    return mencionaConsultas && identificaPaciente;
  }

  private List<Consulta> filtrarEstado(List<Consulta> consultas, String estado) { return consultas.stream().filter(c -> estado.equalsIgnoreCase(Optional.ofNullable(c.getEstado()).orElse(""))).collect(Collectors.toList()); }
  private LocalDateTime fechaOrdenConsulta(Consulta c) { if (c.getFechaConsulta() != null) return c.getFechaConsulta().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(); return c.getFechaCreacion(); }
  private String sufijoEstado(String q) { if (contiene(q, "pendiente", "pendientes", "por atender")) return " pendientes"; if (contiene(q, "atendida", "atendidas", "atendio")) return " atendidas"; return " registradas"; }
  private String respuestaPacienteAmbiguoConsultas(List<Paciente> pacientes) { return "Se encontraron varias coincidencias para el paciente. Indica el DNI o ID para consultar las consultas médicas de forma exacta:\n" + pacientes.stream().map(p -> "- ID: " + p.getIdPaciente() + ", Paciente: " + nombreCompleto(p) + ", DNI: " + valorSeguro(p.getNumDocumento())).collect(Collectors.joining("\n")); }
  private String detalleConsultaMedica(Consulta c, Paciente p) { return "ID de consulta médica: " + c.getIdConsulta() + "\nID de historia clínica: " + (c.getHistoriaClinica()==null?"Sin historia":c.getHistoriaClinica().getIdHistoriaClinica()) + "\nPaciente: " + nombreCompleto(p) + "\nDNI: " + valorSeguro(p.getNumDocumento()) + "\nFecha: " + fechaConsulta(c) + "\nEspecialidad: " + valorSeguro(c.getEspecialidadRequerida()) + "\nEstado: " + estado(c.getEstado()) + "\nDoctor responsable: " + doctorConsulta(c); }
  private Map<String,Object> consultaMedicaMap(Consulta c, Paciente p) { Map<String,Object> m=new LinkedHashMap<>(); m.put("idConsultaMedica", c.getIdConsulta()); m.put("idHistoriaClinica", c.getHistoriaClinica()==null?null:c.getHistoriaClinica().getIdHistoriaClinica()); m.put("paciente", nombreCompleto(p)); m.put("dni", p.getNumDocumento()); m.put("fecha", c.getFechaConsulta()); m.put("especialidad", c.getEspecialidadRequerida()); m.put("estado", estado(c.getEstado())); m.put("doctorResponsable", doctorConsulta(c)); return m; }
  private String fechaConsulta(Consulta c) { if (c.getFechaConsulta() != null) return new SimpleDateFormat("dd/MM/yyyy").format(c.getFechaConsulta()); return c.getFechaCreacion()==null?"Sin fecha registrada":c.getFechaCreacion().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")); }
  private String doctorConsulta(Consulta c) { return c.getDoctorResponsable()==null?"Sin doctor asignado":String.join(" ", Optional.ofNullable(c.getDoctorResponsable().getNombres()).orElse(""), Optional.ofNullable(c.getDoctorResponsable().getApellidos()).orElse("")).trim(); }


  private AsistenteResponse verificarHistoriaClinicaPaciente(String q) {
    if (!esConsultaHistoriaClinicaPaciente(q)) return null;
    ResultadoBusquedaHistoria resultado = buscarPacienteParaHistoria(q);
    if (resultado.requiereDatos()) {
      return resp("HISTORIA_CLINICA_REQUIERE_PACIENTE", "Indica el DNI, ID o nombre completo del paciente para verificar si tiene historia clínica.", Map.of());
    }
    if (resultado.paciente().isEmpty()) {
      if (!resultado.similares().isEmpty()) {
        return resp("HISTORIA_CLINICA_PACIENTE_SIMILARES", respuestaPacienteNoEncontradoConSimilares(resultado.similares()), Map.of("resultados", resultado.similares().stream().map(this::pacienteMap).collect(Collectors.toList())));
      }
      return resp("HISTORIA_CLINICA_PACIENTE_NO_ENCONTRADO", "No se encontró un paciente registrado con esos datos.", Map.of());
    }
    Paciente pacienteEncontrado = resultado.paciente().get();
    return historiaClinicaRepository.findByPacienteIdPaciente(pacienteEncontrado.getIdPaciente())
        .map(historia -> resp("HISTORIA_CLINICA_EXISTENTE", respuestaHistoriaClinicaExistente(historia, pacienteEncontrado), Map.of("historiaClinica", historiaMap(historia, pacienteEncontrado), "paciente", pacienteMap(pacienteEncontrado))))
        .orElse(resp("HISTORIA_CLINICA_NO_EXISTE", "El paciente está registrado, pero no cuenta con una historia clínica. Puede continuar con la creación.", Map.of("paciente", pacienteMap(pacienteEncontrado))));
  }

  private boolean esConsultaHistoriaClinicaPaciente(String q) {
    boolean mencionaHistoria = contiene(q, "historia clinica", "historias clinicas");
    boolean consultaPaciente = contiene(q, "paciente", "dni", " id ", "para ", "este paciente");
    boolean accion = contiene(q, "tiene", "cuenta", "existe", "consulta", "consultar", "verifica", "verificar", "busca", "buscar", "ya tiene", "ya cuenta");
    boolean conteoGeneral = contiene(q, "cuantas", "cuantos", "cantidad", "total", "creadas", "creados") && !contiene(q, "paciente", "dni", " id ", "para ");
    return mencionaHistoria && consultaPaciente && accion && !conteoGeneral;
  }

  private ResultadoBusquedaHistoria buscarPacienteParaHistoria(String q) {
    Matcher dniMatcher = DNI_PATTERN.matcher(q);
    if (dniMatcher.find()) return new ResultadoBusquedaHistoria(pacienteRepository.findByNumDocumento(dniMatcher.group()), List.of(), false);
    Matcher idMatcher = Pattern.compile("\\b(?:id|codigo|cod)\\s*(\\d+)\\b").matcher(q);
    if (idMatcher.find()) return new ResultadoBusquedaHistoria(pacienteRepository.findById(Integer.valueOf(idMatcher.group(1))), List.of(), false);
    Matcher pacienteIdMatcher = Pattern.compile("\\bpaciente\\s+(\\d{1,7})\\b").matcher(q);
    if (pacienteIdMatcher.find()) return new ResultadoBusquedaHistoria(pacienteRepository.findById(Integer.valueOf(pacienteIdMatcher.group(1))), List.of(), false);

    String nombre = extraerNombrePaciente(q);
    List<String> terminos = terminosNombre(nombre);
    if (terminos.size() < 2) {
      List<Paciente> similares = nombre.length() < 3 ? List.of() : buscarPorNombreAproximado(nombre, 5);
      return new ResultadoBusquedaHistoria(Optional.empty(), similares, nombre.length() < 3);
    }

    List<Paciente> candidatos = pacienteRepository.searchByNombre(nombre, 10);
    if (candidatos.isEmpty()) candidatos = buscarPorNombreAproximado(nombre, 10);
    List<Paciente> coincidenciasEstrictas = candidatos.stream()
        .filter(paciente -> nombrePacienteContieneTerminos(paciente, terminos))
        .collect(Collectors.toList());
    if (coincidenciasEstrictas.size() == 1) return new ResultadoBusquedaHistoria(Optional.of(coincidenciasEstrictas.get(0)), List.of(), false);
    if (coincidenciasEstrictas.size() > 1) return new ResultadoBusquedaHistoria(Optional.empty(), coincidenciasEstrictas, false);
    return new ResultadoBusquedaHistoria(Optional.empty(), candidatos.stream().limit(5).collect(Collectors.toList()), false);
  }

  private List<String> terminosNombre(String nombre) {
    if (nombre == null || nombre.isBlank()) return List.of();
    return Arrays.stream(normalizar(nombre).split(" "))
        .filter(termino -> termino.length() >= 2)
        .distinct()
        .collect(Collectors.toList());
  }

  private boolean nombrePacienteContieneTerminos(Paciente paciente, List<String> terminos) {
    String nombrePaciente = normalizar((paciente.getNombres() == null ? "" : paciente.getNombres()) + " " + (paciente.getApellidos() == null ? "" : paciente.getApellidos()));
    return terminos.stream().allMatch(nombrePaciente::contains);
  }

  private String respuestaPacienteNoEncontradoConSimilares(List<Paciente> similares) {
    String resultados = similares.stream()
        .map(paciente -> "- " + nombreCompleto(paciente) + ", DNI: " + valorSeguro(paciente.getNumDocumento()))
        .collect(Collectors.joining("\n"));
    return "No se encontró un paciente registrado con ese nombre.\n"
        + "Se encontraron posibles coincidencias:\n"
        + resultados
        + "\n\nIndique el DNI o ID del paciente para realizar una búsqueda exacta.";
  }

  private record ResultadoBusquedaHistoria(Optional<Paciente> paciente, List<Paciente> similares, boolean requiereDatos) {}

  private String respuestaHistoriaClinicaExistente(HistoriaClinica historia, Paciente paciente) {
    return "Este paciente ya cuenta con una historia clínica registrada. No se recomienda crear una nueva. Abra la historia existente para registrar una nueva consulta.\n\n"
        + "ID historia clínica: " + historia.getIdHistoriaClinica()
        + "\nID paciente: " + paciente.getIdPaciente()
        + "\nPaciente: " + nombreCompleto(paciente)
        + "\nDNI: " + valorSeguro(paciente.getNumDocumento())
        + "\nFecha de creación: " + fechaHistoria(historia.getFechaCreacion());
  }

  private Map<String, Object> historiaMap(HistoriaClinica historia, Paciente paciente) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("idHistoriaClinica", historia.getIdHistoriaClinica());
    map.put("idPaciente", paciente.getIdPaciente());
    map.put("fechaCreacion", historia.getFechaCreacion());
    return map;
  }

  private String fechaHistoria(LocalDateTime fecha) {
    if (fecha == null) return "Sin fecha registrada";
    return fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
  }

  private AsistenteResponse intencionPacientes(String q, Periodo p) {
    if (esAnalisisDuplicados(q)) return null;
    if (esUltimosPacientes(q)) return ultimosPacientes();
    if (esEstadisticaEdad(q)) return estadisticaEdad(q);
    if (esBusquedaAvanzadaPaciente(q)) return busquedaAvanzadaPaciente(q);
    if (esConteoPacientes(q)) {
      long c = p.total() ? pacienteRepository.count() : pacienteRepository.countByFechaIngresoGreaterThanEqualAndFechaIngresoLessThan(Date.from(p.inicio().atZone(ZoneId.systemDefault()).toInstant()), Date.from(p.fin().atZone(ZoneId.systemDefault()).toInstant()));
      return cantidad("PACIENTES_REGISTRADOS", c, pref(p) + "hay %d pacientes registrados.", p);
    }
    return null;
  }

  private boolean esConteoPacientes(String q) {
    return contiene(q, "paciente") && contiene(q, "cuantos", "cuantas", "cantidad", "total", "actuales", "actualmente", "registrados", "registradas", "hay", "existen");
  }

  private boolean esUltimosPacientes(String q) {
    return contiene(q, "paciente", "registro", "registros") && contiene(q, "ultimos", "ultimas", "recientes", "recientemente", "nuevos", "nuevas");
  }

  private boolean esBusquedaAvanzadaPaciente(String q) {
    return contiene(q, "buscar", "busca", "consultar", "consulta", "mostrar", "muestrame", "datos") && contiene(q, "dni", "nombre", "id", "paciente");
  }

  private boolean esEstadisticaEdad(String q) {
    return contiene(q, "edad", "edades", "mayores", "menores") && contiene(q, "paciente", "pacientes", "promedio", "mayores", "menores");
  }

  private AsistenteResponse busquedaAvanzadaPaciente(String q) {
    Matcher dniMatcher = DNI_PATTERN.matcher(q);
    if (dniMatcher.find()) {
      String dni = dniMatcher.group();
      return pacienteRepository.findByNumDocumento(dni)
          .map(paciente -> resp("BUSQUEDA_PACIENTE_DNI", respuestaPacienteRegistrado(paciente), Map.of("tipoBusqueda", "DNI", "paciente", pacienteMap(paciente))))
          .orElse(resp("BUSQUEDA_PACIENTE_SIN_RESULTADOS", "No se encontró un paciente registrado con esos datos.", Map.of("tipoBusqueda", "DNI", "dni", dni)));
    }
    Matcher idMatcher = Pattern.compile("\\b(?:id|codigo|cod)\\s*(\\d+)\\b").matcher(q);
    if (idMatcher.find()) {
      return buscarPacientePorId(Integer.valueOf(idMatcher.group(1)));
    }
    Matcher pacienteIdMatcher = Pattern.compile("\\bpaciente\\s+(\\d{1,7})\\b").matcher(q);
    if (pacienteIdMatcher.find()) {
      return buscarPacientePorId(Integer.valueOf(pacienteIdMatcher.group(1)));
    }
    if (contiene(q, "dni")) return resp("BUSQUEDA_PACIENTE_REQUIERE_DNI", "Indica el DNI de 8 dígitos para buscar al paciente. Ejemplo: Buscar paciente por DNI 45678912", Map.of("tipoBusqueda", "DNI"));
    String nombre = extraerNombrePaciente(q);
    if (nombre.length() < 3 || contiene(nombre, "nombre")) return resp("BUSQUEDA_PACIENTE_REQUIERE_NOMBRE", "Indica nombres y/o apellidos para buscar posibles coincidencias del paciente.", Map.of("tipoBusqueda", "NOMBRE"));
    List<Paciente> coincidencias = pacienteRepository.searchByNombre(nombre, 5);
    if (coincidencias.isEmpty()) coincidencias = buscarPorNombreAproximado(nombre, 5);
    if (coincidencias.isEmpty()) return resp("BUSQUEDA_PACIENTE_SIN_RESULTADOS", "No se encontró un paciente registrado con esos datos.", Map.of("tipoBusqueda", "NOMBRE", "nombre", nombre));
    return resp("BUSQUEDA_PACIENTE_NOMBRE", respuestaPacientesSimilares(coincidencias), Map.of("tipoBusqueda", "NOMBRE", "resultados", coincidencias.stream().map(this::pacienteMap).collect(Collectors.toList())));
  }


  private AsistenteResponse buscarPacientePorId(Integer id) {
    return pacienteRepository.findById(id)
        .map(paciente -> resp("BUSQUEDA_PACIENTE_ID", respuestaPacienteRegistrado(paciente), Map.of("tipoBusqueda", "ID", "paciente", pacienteMap(paciente))))
        .orElse(resp("BUSQUEDA_PACIENTE_SIN_RESULTADOS", "No se encontró un paciente registrado con ese ID.", Map.of("tipoBusqueda", "ID", "idPaciente", id)));
  }

  private AsistenteResponse ultimosPacientes() {
    List<Paciente> pacientes = new ArrayList<>();
    pacienteRepository.findAll().forEach(pacientes::add);
    pacientes.sort(Comparator.comparing(Paciente::getFechaIngreso, Comparator.nullsLast(Date::compareTo)).reversed());
    List<Paciente> ultimos = pacientes.stream().limit(5).collect(Collectors.toList());
    if (ultimos.isEmpty()) return resp("ULTIMOS_PACIENTES", "No se encontraron pacientes registrados.", Map.of("resultados", List.of()));
    return resp("ULTIMOS_PACIENTES", "Últimos pacientes registrados:\n" + ultimos.stream().map(this::detallePaciente).collect(Collectors.joining("\n\n")), Map.of("resultados", ultimos.stream().map(this::pacienteMap).collect(Collectors.toList())));
  }

  private AsistenteResponse estadisticaEdad(String q) {
    List<Paciente> pacientes = new ArrayList<>();
    pacienteRepository.findAll().forEach(pacientes::add);
    List<Integer> edades = pacientes.stream().map(this::edadPaciente).filter(Objects::nonNull).collect(Collectors.toList());
    if (edades.isEmpty()) return resp("ESTADISTICAS_EDAD_PACIENTES", "No hay fechas de nacimiento suficientes para calcular estadísticas de edad.", Map.of());
    Matcher mayores = Pattern.compile("mayores?\\s+de\\s+(\\d+)").matcher(q);
    if (mayores.find()) {
      int edad = Integer.parseInt(mayores.group(1));
      long cantidad = edades.stream().filter(e -> e > edad).count();
      return resp("PACIENTES_MAYORES_EDAD", "Hay " + cantidad + " pacientes mayores de " + edad + " años.", Map.of("edad", edad, "cantidad", cantidad));
    }
    double promedio = edades.stream().mapToInt(Integer::intValue).average().orElse(0);
    if (contiene(q, "promedio")) return resp("EDAD_PROMEDIO_PACIENTES", String.format("La edad promedio de los pacientes es %.1f años.", promedio), Map.of("edadPromedio", promedio));
    Map<String, Long> rangos = new LinkedHashMap<>();
    rangos.put("0-17", edades.stream().filter(e -> e <= 17).count());
    rangos.put("18-29", edades.stream().filter(e -> e >= 18 && e <= 29).count());
    rangos.put("30-59", edades.stream().filter(e -> e >= 30 && e <= 59).count());
    rangos.put("60+", edades.stream().filter(e -> e >= 60).count());
    return resp("PACIENTES_POR_EDAD", "Pacientes por edad:\n" + rangos.entrySet().stream().map(e -> e.getKey() + " años: " + e.getValue()).collect(Collectors.joining("\n")), Map.of("rangos", rangos, "edadPromedio", promedio));
  }

  private Integer edadPaciente(Paciente paciente) {
    if (paciente.getFechaNacimiento() == null) return null;
    return Period.between(paciente.getFechaNacimiento().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(), LocalDate.now()).getYears();
  }

  private AsistenteResponse buscarPacienteDuplicado(String q) {
    Matcher matcher = DNI_PATTERN.matcher(q);
    if (esAnalisisDuplicados(q) && !matcher.find()) return analizarDuplicadosPacientes();
    matcher.reset();
    if (!esConsultaDuplicadoPaciente(q)) return null;
    if (matcher.find()) {
      String dni = matcher.group();
      return pacienteRepository.findByNumDocumento(dni)
          .map(paciente -> resp("BUSQUEDA_DUPLICADO_DNI", respuestaPacienteRegistrado(paciente), Map.of("tipoBusqueda", "DNI", "paciente", pacienteMap(paciente))))
          .orElse(resp("BUSQUEDA_DUPLICADO_SIN_RESULTADOS", "No se encontró un paciente registrado con esos datos. Puede continuar con el registro.", Map.of("tipoBusqueda", "DNI", "dni", dni)));
    }
    String nombre = extraerNombrePaciente(q);
    if (nombre.length() < 3) return null;
    List<Paciente> coincidencias = pacienteRepository.searchByNombre(nombre, 5);
    if (coincidencias.isEmpty()) coincidencias = buscarPorNombreAproximado(nombre, 5);
    if (coincidencias.isEmpty()) return resp("BUSQUEDA_DUPLICADO_SIN_RESULTADOS", "No se encontró un paciente registrado con esos datos. Puede continuar con el registro.", Map.of("tipoBusqueda", "NOMBRE", "nombre", nombre));
    List<Map<String, Object>> resultados = coincidencias.stream().map(this::pacienteMap).collect(Collectors.toList());
    return resp("BUSQUEDA_DUPLICADO_NOMBRE", respuestaPacientesSimilares(coincidencias), Map.of("tipoBusqueda", "NOMBRE", "resultados", resultados));
  }



  private boolean esAnalisisDuplicados(String q) {
    return contiene(q, "duplicado", "duplicados", "duplicada", "duplicadas", "repetido", "repetidos", "duplicidad");
  }

  private AsistenteResponse analizarDuplicadosPacientes() {
    Map<String, List<Paciente>> grupos = new LinkedHashMap<>();
    pacienteRepository.findAll().forEach(paciente -> {
      String dni = paciente.getNumDocumento() == null ? "" : paciente.getNumDocumento().trim();
      String nombre = normalizar(nombreCompleto(paciente));
      String key = !dni.isBlank() ? "DNI " + dni : nombre.isBlank() ? "" : "NOMBRE " + nombre;
      if (!key.isBlank()) grupos.computeIfAbsent(key, ignored -> new ArrayList<>()).add(paciente);
    });
    List<Paciente> duplicados = grupos.values().stream()
        .filter(lista -> lista.size() > 1)
        .flatMap(List::stream)
        .limit(10)
        .collect(Collectors.toList());
    if (duplicados.isEmpty()) return resp("ANALISIS_DUPLICADOS_SIN_RESULTADOS", "No se encontraron pacientes duplicados evidentes por DNI o nombre completo.", Map.of("cantidad", 0));
    return resp("ANALISIS_DUPLICADOS_PACIENTES", "Se encontraron posibles pacientes duplicados:\n" + duplicados.stream().map(this::detallePaciente).collect(Collectors.joining("\n\n")) + "\n\nRevise la información antes de crear una nueva historia clínica.", Map.of("cantidad", duplicados.size(), "resultados", duplicados.stream().map(this::pacienteMap).collect(Collectors.toList())));
  }

  private List<Paciente> buscarPorNombreAproximado(String nombre, int limit) {
    String[] tokens = nombre.split(" ");
    List<Paciente> resultados = new ArrayList<>();
    pacienteRepository.findAll().forEach(paciente -> {
      String nombrePaciente = normalizar((paciente.getNombres() == null ? "" : paciente.getNombres()) + " " + (paciente.getApellidos() == null ? "" : paciente.getApellidos()));
      long coincidencias = Arrays.stream(tokens).filter(token -> token.length() >= 2 && nombrePaciente.contains(token)).count();
      if (coincidencias > 0) resultados.add(paciente);
    });
    resultados.sort(Comparator.comparingInt((Paciente paciente) -> puntajeNombre(paciente, tokens)).reversed());
    return resultados.stream().limit(limit).collect(Collectors.toList());
  }

  private int puntajeNombre(Paciente paciente, String[] tokens) {
    String nombrePaciente = normalizar((paciente.getNombres() == null ? "" : paciente.getNombres()) + " " + (paciente.getApellidos() == null ? "" : paciente.getApellidos()));
    return (int) Arrays.stream(tokens).filter(token -> token.length() >= 2 && nombrePaciente.contains(token)).count();
  }

  private boolean esConsultaDuplicadoPaciente(String q) {
    boolean existenciaPaciente = contiene(q, "existe un paciente", "existe el paciente", "existe paciente", "verificar si existe", "verifica si existe", "ya esta registrado", "ya esta registrada", "ya esta el paciente", "se encuentra registrado");
    boolean busquedaPaciente = contiene(q, "buscar paciente", "busca paciente", "buscar por dni", "busca por dni", "buscar paciente por dni", "buscar por nombre", "busca por nombre", "buscar paciente por nombre");
    boolean duplicidad = esAnalisisDuplicados(q) || contiene(q, "historia clinica duplicada", "historias clinicas duplicadas");
    boolean tieneEntidadPaciente = contiene(q, "paciente", "pacientes", "dni", "historia clinica", "historias clinicas");
    return (existenciaPaciente || busquedaPaciente || duplicidad) && tieneEntidadPaciente;
  }

  private String extraerNombrePaciente(String q) {
    return q.replaceAll("\\b(busca|buscar|verifica|verificar|consultar|consulta|consultas|muestrame|mostrar|cuantas|cuantos|cantidad|ultima|ultimo|atencion|atenciones|medica|medicas|pendiente|pendientes|atendida|atendidas|atendio|por|nombre|si|existe|existen|ya|esta|registrado|registrada|paciente|pacientes|con|dni|id|codigo|cod|historia|historias|clinica|clinicas|para|el|la|un|una|por|favor|datos|duplicado|duplicados|duplicada|duplicadas|repetido|repetidos|duplicidad|tiene|cuenta|contiene|asociada|asociado|del|este|esa|ese)\\b", " ").replaceAll("\\d+", " ").replaceAll("\\s+", " ").trim();
  }


  private String respuestaPacienteRegistrado(Paciente paciente) {
    return "Se encontró un paciente registrado:\n"
        + detallePaciente(paciente)
        + "\n\nNo se recomienda crear una nueva historia clínica para este paciente.";
  }

  private String respuestaPacientesSimilares(List<Paciente> pacientes) {
    String detalle = pacientes.stream()
        .map(this::detallePaciente)
        .collect(Collectors.joining("\n\n"));
    return "Se encontraron posibles pacientes similares:\n"
        + detalle
        + "\n\nRevise la información antes de crear una nueva historia clínica.";
  }

  private String detallePaciente(Paciente paciente) {
    return "ID: " + paciente.getIdPaciente()
        + "\nPaciente: " + nombreCompleto(paciente)
        + "\nDNI: " + valorSeguro(paciente.getNumDocumento())
        + "\nFecha de registro: " + fechaRegistro(paciente);
  }

  private String fechaRegistro(Paciente paciente) {
    if (paciente.getFechaIngreso() == null) return "Sin fecha registrada";
    return new SimpleDateFormat("dd/MM/yyyy").format(paciente.getFechaIngreso());
  }

  private String valorSeguro(String valor) {
    return valor == null || valor.isBlank() ? "Sin DNI" : valor;
  }

  private String nombreCompleto(Paciente paciente) {
    return String.join(" ", Optional.ofNullable(paciente.getNombres()).orElse(""), Optional.ofNullable(paciente.getApellidos()).orElse("")).trim();
  }

  private Map<String, Object> pacienteMap(Paciente paciente) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("idPaciente", paciente.getIdPaciente());
    map.put("nombres", paciente.getNombres());
    map.put("apellidos", paciente.getApellidos());
    map.put("numDocumento", paciente.getNumDocumento());
    map.put("fechaRegistro", paciente.getFechaIngreso());
    return map;
  }


  private String mensajeAyudaConsultas() {
    return "Puedes realizar estas consultas:\n\n"
        + "PACIENTES\n"
        + "- ¿Cuántos pacientes hay registrados?\n"
        + "- ¿Cuántos pacientes se registraron hoy?\n"
        + "- Muéstrame los últimos pacientes registrados.\n"
        + "- ¿Cuáles son los pacientes más recientes?\n\n"
        + "BÚSQUEDA DE PACIENTES\n"
        + "- Busca un paciente por DNI 72845292.\n"
        + "- Consulta el paciente ID 4.\n"
        + "- Buscar paciente por nombre Rafael Velásquez.\n"
        + "- Verifica si existe un paciente con DNI 72845292.\n\n"
        + "CONSULTAS MÉDICAS POR PACIENTE\n"
        + "- ¿El paciente con DNI 72845292 tiene consultas médicas?\n"
        + "- ¿Cuántas consultas médicas tiene el paciente con DNI 72845292?\n"
        + "- Muéstrame las consultas médicas del paciente ID 4.\n"
        + "- ¿Cuál fue la última consulta médica del paciente con DNI 72845292?\n"
        + "- ¿El paciente ID 4 tiene consultas médicas pendientes?\n"
        + "- ¿Cuántas consultas médicas atendidas tiene Rafael Velásquez?\n\n"
        + "HISTORIAS CLÍNICAS POR PACIENTE\n"
        + "- ¿El paciente con DNI 72845292 ya tiene historia clínica?\n"
        + "- Consulta si el paciente ID 4 tiene historia clínica.\n"
        + "- ¿Existe una historia clínica para Rafael Velásquez Morales?\n"
        + "- Busca la historia clínica del paciente con DNI 72845292.\n\n"
        + "DUPLICADOS\n"
        + "- ¿Existen pacientes duplicados?\n"
        + "- Busca pacientes duplicados.\n"
        + "- Verifica si hay pacientes repetidos.\n"
        + "- Analiza posibles duplicados.\n"
        + "- Revisa la duplicidad de historias clínicas.\n\n"
        + "ESTADÍSTICAS\n"
        + "- ¿Cuál es la edad promedio de los pacientes?\n"
        + "- ¿Cuántos pacientes son mayores de 60 años?\n"
        + "- Muéstrame los pacientes por rango de edad.\n\n"
        + "OTRAS CONSULTAS DEL SISTEMA\n"
        + "- Historias clínicas creadas hoy.\n"
        + "- Consultas médicas pendientes.\n"
        + "- Consultas médicas atendidas hoy.\n"
        + "- Consultas médicas incompletas.\n"
        + "- Especialidad más requerida.\n"
        + "- Enfermedad más registrada.";
  }

  private String mensajeConsultaNoReconocida() {
    return "No entiendo tu pregunta.\nEscribe ‘¿Qué preguntas puedo hacer?’ para ver las consultas disponibles.";
  }

  private AsistenteResponse incompletas(){List<Map<String,Object>> items=consultaRepository.findIncompletas().stream().limit(5).map(c->{Map<String,Object>m=new LinkedHashMap<>();m.put("idConsulta",c.getIdConsulta());m.put("estado",estado(c.getEstado()));m.put("especialidad",c.getEspecialidadRequerida());m.put("fecha",c.getFechaCreacion());m.put("doctor",c.getDoctorResponsable()==null?null:c.getDoctorResponsable().getNombres()+" "+c.getDoctorResponsable().getApellidos());return m;}).collect(Collectors.toList());return resp("CONSULTAS_INCOMPLETAS",items.isEmpty()?"No se encontraron registros para el periodo solicitado.":"Actualmente existen "+consultaRepository.countIncompletas()+" consultas incompletas. Te muestro hasta 5 resultados.",Map.of("resultados",items));}
  private AsistenteResponse ranking(String i,List<Object[]> rows,String n){List<Map<String,Object>> r=rows.stream().limit(5).map(a->Map.<String,Object>of("nombre",String.valueOf(a[0]),"cantidad",((Number)a[1]).longValue())).collect(Collectors.toList());return resp(i,r.isEmpty()?"No se encontraron registros para el periodo solicitado.":"Estos son los principales "+n+" registrados.",Map.of("resultados",r));}
  private AsistenteResponse cantidad(String i,long c,String f,Periodo p){return resp(i,String.format(f,c),Map.of("cantidad",c,"periodo",p.nombre()));} private AsistenteResponse sinPermiso(){return resp("SIN_PERMISOS","No tienes permisos para consultar esa información.",Map.of());} private AsistenteResponse resp(String i,String r,Map<String,Object>d){return AsistenteResponse.builder().intencion(i).respuesta(r).datos(d).build();}
  private boolean contiene(String q,String...xs){return Arrays.stream(xs).anyMatch(q::contains);} private String estado(String e){return "PENDIENTE".equals(e)?"Por atender":"ATENDIDO".equals(e)?"Atendido":e;} private String normalizar(String s){if(s==null)return"";return Normalizer.normalize(s.toLowerCase().trim(),Normalizer.Form.NFD).replaceAll("\\p{M}","").replaceAll("[¿?¡!.,;:]+"," ").replaceAll("\\s+"," ");} private String pref(Periodo p){return p.total()?"Actualmente ":p.nombre().equals("HOY")?"Hoy ":p.nombre().equals("SEMANA_ACTUAL")?"Esta semana ":"Este mes ";}
  private Periodo periodo(String q){LocalDate n=LocalDate.now();if(q.contains("hoy"))return new Periodo("HOY",n.atStartOfDay(),n.plusDays(1).atStartOfDay(),false);if(q.contains("semana")){LocalDate i=n.with(DayOfWeek.MONDAY);return new Periodo("SEMANA_ACTUAL",i.atStartOfDay(),i.plusWeeks(1).atStartOfDay(),false);}if(q.contains("mes")){LocalDate i=n.withDayOfMonth(1);return new Periodo("MES_ACTUAL",i.atStartOfDay(),i.plusMonths(1).atStartOfDay(),false);}return new Periodo("TOTAL",null,null,true);} record Periodo(String nombre,LocalDateTime inicio,LocalDateTime fin,boolean total){}
  private String ayuda(String q){if(contiene(q,"ayuda","que preguntas puedo hacer","que puedo preguntarte","como puedo usar el asistente","que puedes consultar","que consultas soportas","mostrar opciones","mostrar preguntas disponibles","preguntas disponibles","muestrame las opciones","comandos disponibles"))return mensajeAyudaConsultas();if(contiene(q,"como registro un paciente"))return"Para registrar un paciente ingresa a Pacientes, selecciona Agregar Pacientes, completa los datos obligatorios y guarda.";if(contiene(q,"como edito un paciente"))return"Para editar un paciente ingresa a Pacientes, busca el registro y usa la acción Editar.";if(contiene(q,"historia clinica")&&contiene(q,"como creo","crear"))return"Para crear una historia clínica selecciona el paciente y registra su historia desde el módulo Historia Clínica.";if(contiene(q,"consulta")&&contiene(q,"agrego","agregar","crear"))return"Para agregar una consulta médica abre la historia clínica del paciente y usa la opción para registrar una nueva consulta.";if(contiene(q,"atiendo una consulta","atender una consulta"))return"Para atender una consulta entra a Consultas, abre el detalle pendiente, completa diagnóstico y tratamiento, y finaliza la atención.";if(contiene(q,"empleado"))return"Para registrar o activar/desactivar empleados ingresa al módulo Empleados y usa las acciones disponibles.";if(contiene(q,"usuario","permisos"))return"Para crear usuarios o asignar permisos ingresa al módulo Usuarios y configura sus permisos por menú.";return null;}
}
