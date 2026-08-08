package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.services.AsistenteService;
import com.krivi.apihistorialmedico.business.services.HistoriaClinicaService;
import com.krivi.apihistorialmedico.model.api.AsistenteRequest;
import com.krivi.apihistorialmedico.model.api.AsistenteResponse;
import com.krivi.apihistorialmedico.model.api.DuplicadosHistoriasClinicasResponse;
import com.krivi.apihistorialmedico.model.api.GrupoDuplicadoHistoriaClinicaResponse;
import com.krivi.apihistorialmedico.model.api.HistoriaClinicaIntegracionItemResponse;
import com.krivi.apihistorialmedico.model.entity.Consulta;
import com.krivi.apihistorialmedico.model.entity.HistoriaClinica;
import com.krivi.apihistorialmedico.model.entity.Paciente;
import com.krivi.apihistorialmedico.model.entity.Usuario;
import com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente;
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

  @Autowired PacienteRepository pacienteRepository; @Autowired HistoriaClinicaRepository historiaClinicaRepository; @Autowired ConsultaRepository consultaRepository; @Autowired UsuarioRepository usuarioRepository; @Autowired HistoriaClinicaService historiaClinicaService;
  @Override public AsistenteResponse preguntar(AsistenteRequest request, Integer idUsuario) {
    String q = normalizar(request == null ? null : request.getPregunta());
    if (q.isBlank()) return resp("PREGUNTA_VACIA", "Escribe una pregunta para poder ayudarte.", Map.of());
    try {
      Periodo p = periodo(q); Usuario u = idUsuario == null ? null : usuarioRepository.findById(idUsuario).orElse(null); Integer idEmpleado = u != null && u.getEmpleado() != null ? u.getEmpleado().getIdEmpleado() : null;
      String ayuda = ayuda(q); if (ayuda != null) return resp("AYUDA_USO_SISTEMA", ayuda, Map.of());
      AsistenteResponse consultaGeneral = responderConsultaGeneral(q, p); if (consultaGeneral != null) return consultaGeneral;
      AsistenteResponse consultasPaciente = consultarConsultasMedicasPaciente(q); if (consultasPaciente != null) return consultasPaciente;
      AsistenteResponse consultasGenerales = responderConsultasMedicasGenerales(q, p); if (consultasGenerales != null) return consultasGenerales;
      AsistenteResponse historiaPaciente = verificarHistoriaClinicaPaciente(q); if (historiaPaciente != null) return historiaPaciente;
      AsistenteResponse pacientes = intencionPacientes(q, p); if (pacientes != null) return pacientes;
      if (contiene(q,"doctor autenticado","mis consultas","asignadas al doctor","consultas asignadas")) { if (idEmpleado == null) return sinPermiso(); if (contiene(q,"atendio","atendidas")) return cantidad("CONSULTAS_ATENDIDAS_DOCTOR", consultaRepository.countByDoctorResponsableIdEmpleadoAndEstado(idEmpleado,"ATENDIDO"), "El doctor autenticado atendió %d consultas.", p); return cantidad("CONSULTAS_ASIGNADAS_DOCTOR", consultaRepository.countByDoctorResponsableIdEmpleado(idEmpleado), "El doctor autenticado tiene %d consultas asignadas.", p); }
      if (contiene(q,"paciente") && contiene(q,"sin historia","no tienen historia")) return cantidad("PACIENTES_SIN_HISTORIA_CLINICA", Math.max(0, pacienteRepository.countByEstadoRegistro(EstadoRegistroPaciente.ACTIVO)-historiaClinicaRepository.count()), "Actualmente hay %d pacientes sin historia clínica.", p);
      if (contiene(q,"paciente") && contiene(q,"con historia","tienen historia")) return cantidad("PACIENTES_CON_HISTORIA_CLINICA", historiaClinicaRepository.count(), "Actualmente hay %d pacientes con historia clínica.", p);
      if (contiene(q,"paciente")) { long c=p.total()?pacienteRepository.countByEstadoRegistro(EstadoRegistroPaciente.ACTIVO):pacienteRepository.countByEstadoRegistroAndFechaIngresoGreaterThanEqualAndFechaIngresoLessThan(EstadoRegistroPaciente.ACTIVO, Date.from(p.inicio().atZone(ZoneId.systemDefault()).toInstant()), Date.from(p.fin().atZone(ZoneId.systemDefault()).toInstant())); return cantidad("PACIENTES_REGISTRADOS", c, pref(p)+"hay %d pacientes registrados.", p); }
      if (contiene(q,"historia")) { if (contiene(q,"incompleta")) return cantidad("HISTORIAS_CLINICAS_INCOMPLETAS",0,"No se encontraron historias clínicas incompletas según los campos obligatorios actuales.",p); long c=p.total()?historiaClinicaRepository.count():historiaClinicaRepository.countByFechaCreacionGreaterThanEqualAndFechaCreacionLessThan(p.inicio(),p.fin()); return cantidad("HISTORIAS_CLINICAS",c,pref(p)+"hay %d historias clínicas.",p); }
      if (contiene(q,"especialidad")) return ranking("ESPECIALIDADES_MAS_REQUERIDAS", p.total()?consultaRepository.rankingEspecialidades():consultaRepository.rankingEspecialidades(p.inicio(),p.fin()), "especialidades");
      if (contiene(q,"enfermedad")) return ranking("TIPOS_ENFERMEDAD_MAS_REGISTRADOS", p.total()?consultaRepository.rankingTiposEnfermedad():consultaRepository.rankingTiposEnfermedad(p.inicio(),p.fin()), "tipos de enfermedad");
      if (contiene(q,"doctor") && contiene(q,"mas","cada doctor")) return ranking("DOCTORES_CON_MAS_ATENCIONES", consultaRepository.rankingDoctoresAtenciones(), "doctores");
      if (contiene(q,"consulta medica","consultas medicas","atencion medica","atenciones medicas","consulta","atencion","atenciones")) { if (contiene(q,"incompleta","todavia no","faltan")) return incompletas(); if (contiene(q,"pendiente","por atender","faltan")) return cantidad("CONSULTAS_MEDICAS_PENDIENTES", p.total()?consultaRepository.countByEstado("PENDIENTE"):consultaRepository.countByEstadoAndFechaCreacionGreaterThanEqualAndFechaCreacionLessThan("PENDIENTE",p.inicio(),p.fin()), pref(p)+"hay %d consultas médicas por atender.", p); if (contiene(q,"atendida","atendidas","atendio")) return cantidad("CONSULTAS_MEDICAS_ATENDIDAS", p.total()?consultaRepository.countByEstado("ATENDIDO"):consultaRepository.countByEstadoAndFechaCreacionGreaterThanEqualAndFechaCreacionLessThan("ATENDIDO",p.inicio(),p.fin()), pref(p)+"hay %d consultas médicas atendidas.", p); long c=p.total()?consultaRepository.count():consultaRepository.countByFechaCreacionGreaterThanEqualAndFechaCreacionLessThan(p.inicio(),p.fin()); return cantidad("CONSULTAS_MEDICAS_REGISTRADAS",c,pref(p)+"hay %d consultas médicas registradas.",p); }
    } catch (Exception e) { return resp("ERROR_INTERNO", "No pude obtener la información en este momento. Inténtalo nuevamente.", Map.of()); }
    return resp("NO_RECONOCIDA", mensajeConsultaNoReconocida(), Map.of());
  }

  private AsistenteResponse responderConsultaGeneral(String q, Periodo p) {
    if (esUltimosPacientes(q)) return ultimosPacientes();
    if (esEstadisticaEdad(q)) return estadisticaEdad(q);
    if (esConteoPacientes(q)) {
      long cantidad = p.total()
          ? pacienteRepository.countByEstadoRegistro(EstadoRegistroPaciente.ACTIVO)
          : pacienteRepository.countByEstadoRegistroAndFechaIngresoGreaterThanEqualAndFechaIngresoLessThan(
              EstadoRegistroPaciente.ACTIVO,
              Date.from(p.inicio().atZone(ZoneId.systemDefault()).toInstant()),
              Date.from(p.fin().atZone(ZoneId.systemDefault()).toInstant()));
      return cantidad("PACIENTES_REGISTRADOS", cantidad, pref(p) + "hay %d pacientes registrados.", p);
    }
    AsistenteResponse historiasDuplicadas = responderHistoriasClinicasDuplicadas(q);
    if (historiasDuplicadas != null) return historiasDuplicadas;
    AsistenteResponse pacientesDuplicados = buscarPacienteDuplicado(q);
    if (pacientesDuplicados != null && esAnalisisDuplicados(q)) return pacientesDuplicados;
    if (esConsultaGeneralHistoriasClinicas(q)) {
      long cantidad = p.total()
          ? historiaClinicaRepository.count()
          : historiaClinicaRepository.countByFechaCreacionGreaterThanEqualAndFechaCreacionLessThan(p.inicio(), p.fin());
      return cantidad("HISTORIAS_CLINICAS", cantidad, pref(p) + "hay %d historias clínicas.", p);
    }
    if (esConsultasAtendidasHoy(q)) {
      long cantidad = consultaRepository.countByEstadoAndFechaAtencionGreaterThanEqualAndFechaAtencionLessThan("ATENDIDO", p.inicio(), p.fin());
      String respuesta = cantidad == 0
          ? "Actualmente no hay consultas médicas atendidas el día de hoy."
          : "Actualmente hay " + cantidad + " consultas médicas atendidas el día de hoy.";
      return resp("CONSULTAS_MEDICAS_ATENDIDAS", respuesta, Map.of("cantidad", cantidad, "periodo", p.nombre()));
    }
    return null;
  }

  private AsistenteResponse responderConsultasMedicasGenerales(String q, Periodo p) {
    if (esConsultasPendientesGenerales(q)) {
      long cantidad = p.total()
          ? consultaRepository.countByEstado("PENDIENTE")
          : consultaRepository.countByEstadoAndFechaCreacionGreaterThanEqualAndFechaCreacionLessThan("PENDIENTE", p.inicio(), p.fin());
      return cantidad("CONSULTAS_MEDICAS_PENDIENTES", cantidad, pref(p) + "hay %d consultas médicas por atender.", p);
    }
    if (!esConsultaGeneralConsultasMedicas(q)) return null;
    long cantidad = p.total()
        ? consultaRepository.count()
        : consultaRepository.countByFechaCreacionGreaterThanEqualAndFechaCreacionLessThan(p.inicio(), p.fin());
    return cantidad("CONSULTAS_MEDICAS_REGISTRADAS", cantidad, pref(p) + "hay %d consultas médicas registradas.", p);
  }

  private boolean esConsultaGeneralHistoriasClinicas(String q) {
    boolean mencionaHistorias = contiene(q, "historia clinica", "historias clinicas");
    boolean intencionGeneral = contiene(q, "cuantas", "cuantos", "cantidad", "total", "registradas", "creadas", "creados");
    return mencionaHistorias && intencionGeneral && !referenciaPacienteExplicita(q);
  }

  private boolean esConsultaGeneralConsultasMedicas(String q) {
    boolean mencionaConsultas = mencionaConsultas(q);
    boolean totalRegistradas = contiene(q, "cuantas", "cuantos", "cantidad", "total", "registradas")
        && !contiene(q, "pendiente", "pendientes", "por atender");
    return mencionaConsultas && totalRegistradas && !referenciaPacienteExplicita(q);
  }

  private boolean esConsultasAtendidasHoy(String q) {
    return mencionaConsultas(q)
        && contiene(q, "atendida", "atendidas", "atendio", "atendieron")
        && contiene(q, "hoy", "dia de hoy")
        && !referenciaPacienteExplicita(q);
  }

  private boolean esUltimaConsultaPaciente(String q) {
    return contiene(q, "consulta", "consultas") && contiene(q, "ultima", "ultimo", "reciente");
  }

  private boolean esConsultasPendientesPaciente(String q) {
    if (!contiene(q, "consulta", "consultas") || !contiene(q, "pendiente", "pendientes", "por atender")) return false;
    return referenciaPacienteExplicita(q) || tieneNombrePacienteEnConsulta(q) || contiene(q, "tiene consultas", "consultas del paciente");
  }

  private boolean esConsultasPendientesGenerales(String q) {
    return mencionaConsultas(q)
        && contiene(q, "pendiente", "pendientes", "por atender")
        && !referenciaPacienteExplicita(q)
        && !tieneNombrePacienteEnConsulta(q);
  }

  private boolean mencionaConsultas(String q) {
    return contiene(q, "consulta", "consultas", "consulta medica", "consultas medicas", "atencion medica", "atenciones medicas");
  }

  private boolean tieneNombrePacienteEnConsulta(String q) {
    return terminosNombre(extraerNombrePaciente(q)).size() >= 2;
  }

  private boolean referenciaPacienteExplicita(String q) {
    return contiene(q, "paciente", "dni", " id ")
        || DNI_PATTERN.matcher(q).find()
        || Pattern.compile("\\b(?:id|codigo|cod)\\s*\\d+\\b").matcher(q).find();
  }



  private AsistenteResponse consultarConsultasMedicasPaciente(String q) {
    if (!esConsultaMedicaPaciente(q)) return null;
    boolean ultimaConsulta = esUltimaConsultaPaciente(q);
    boolean consultasPendientes = esConsultasPendientesPaciente(q);
    ResultadoBusquedaHistoria resultado = buscarPacienteParaHistoria(q);
    if (resultado.requiereDatos()) return resp("CONSULTAS_MEDICAS_REQUIERE_PACIENTE", "Escribe el DNI o el nombre y los dos apellidos del paciente para consultar sus consultas médicas.", Map.of());
    if (resultado.paciente().isEmpty()) {
      if (!resultado.similares().isEmpty()) return resp("CONSULTAS_MEDICAS_PACIENTE_AMBIGUO", respuestaPacienteAmbiguoConsultas(resultado.similares()), Map.of("resultados", resultado.similares().stream().map(this::pacienteMap).collect(Collectors.toList())));
      return resp("CONSULTAS_MEDICAS_PACIENTE_NO_ENCONTRADO", "No se encontró un paciente registrado con esos datos.", Map.of());
    }
    Paciente paciente = resultado.paciente().get();
    Optional<HistoriaClinica> historia = historiaClinicaRepository.findByPacienteIdPaciente(paciente.getIdPaciente());
    if (historia.isEmpty()) {
      String respuesta = consultasPendientes
          ? "El paciente está registrado, pero no cuenta con una historia clínica. Por lo tanto, no tiene consultas médicas pendientes."
          : "El paciente está registrado, pero no cuenta con una historia clínica. Por lo tanto, no tiene consultas médicas registradas.";
      return resp("CONSULTAS_MEDICAS_SIN_HISTORIA_CLINICA", respuesta, Map.of("paciente", pacienteMap(paciente)));
    }
    if (ultimaConsulta) {
      return consultaRepository.findUltimaByHistoriaClinica(historia.get().getIdHistoriaClinica())
          .map(consulta -> resp("CONSULTAS_MEDICAS_PACIENTE_ULTIMA", "Última consulta médica del paciente:\n" + detalleConsultaMedica(consulta, paciente), Map.of("resultados", List.of(consultaMedicaMap(consulta, paciente)), "cantidad", 1)))
          .orElse(resp("CONSULTAS_MEDICAS_SIN_REGISTROS", "El paciente no tiene consultas médicas registradas.", Map.of("resultados", List.of(), "paciente", pacienteMap(paciente))));
    }
    if (consultasPendientes) {
      List<Consulta> pendientes = consultaRepository.findByHistoriaClinicaIdHistoriaClinicaAndEstadoIgnoreCaseOrderByFechaCreacionAsc(historia.get().getIdHistoriaClinica(), "PENDIENTE");
      if (pendientes.isEmpty()) return resp("CONSULTAS_MEDICAS_PACIENTE_PENDIENTES", "El paciente no tiene consultas médicas pendientes.", Map.of("resultados", List.of(), "cantidad", 0, "paciente", pacienteMap(paciente)));
      return resp("CONSULTAS_MEDICAS_PACIENTE_PENDIENTES", "Consultas médicas pendientes del paciente:\n" + pendientes.stream().limit(5).map(c -> detalleConsultaMedica(c, paciente)).collect(Collectors.joining("\n\n")), Map.of("resultados", pendientes.stream().limit(5).map(c -> consultaMedicaMap(c, paciente)).collect(Collectors.toList()), "cantidad", pendientes.size()));
    }
    List<Consulta> consultas = new ArrayList<>(consultaRepository.findByHistoriaClinicaIdHistoriaClinica(historia.get().getIdHistoriaClinica()));
    consultas.sort(Comparator.comparing(this::fechaOrdenConsulta, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
    if (consultas.isEmpty()) return resp("CONSULTAS_MEDICAS_SIN_REGISTROS", "La historia clínica del paciente no cuenta con consultas médicas registradas.", Map.of("paciente", pacienteMap(paciente), "historiaClinica", historiaMap(historia.get(), paciente)));
    List<Consulta> filtradas = consultas;
    String intencion = "CONSULTAS_MEDICAS_PACIENTE_LISTADO";
    String titulo = "Consultas médicas recientes del paciente";
    if (contiene(q, "pendiente", "pendientes", "por atender")) { filtradas = filtrarEstado(consultas, "PENDIENTE"); intencion = "CONSULTAS_MEDICAS_PACIENTE_PENDIENTES"; titulo = "Consultas médicas pendientes del paciente"; }
    else if (contiene(q, "atendida", "atendidas", "atendio")) { filtradas = filtrarEstado(consultas, "ATENDIDO"); intencion = "CONSULTAS_MEDICAS_PACIENTE_ATENDIDAS"; titulo = "Consultas médicas atendidas del paciente"; }
    if (contiene(q, "cuantas", "cuantos", "cantidad")) return resp(intencion + "_CANTIDAD", "El paciente " + nombreCompleto(paciente) + " tiene " + filtradas.size() + " consultas médicas" + sufijoEstado(q) + ".", Map.of("cantidad", filtradas.size(), "paciente", pacienteMap(paciente), "historiaClinica", historiaMap(historia.get(), paciente)));
    if (filtradas.isEmpty()) return resp(intencion, "No se encontraron consultas médicas" + sufijoEstado(q) + " para este paciente.", Map.of("resultados", List.of(), "paciente", pacienteMap(paciente)));
    return resp(intencion, titulo + ":\n" + filtradas.stream().limit(5).map(c -> detalleConsultaMedica(c, paciente)).collect(Collectors.joining("\n\n")), Map.of("resultados", filtradas.stream().limit(5).map(c -> consultaMedicaMap(c, paciente)).collect(Collectors.toList()), "cantidad", filtradas.size()));
  }

  private boolean esConsultaMedicaPaciente(String q) {
    if (esUltimaConsultaPaciente(q) || esConsultasPendientesPaciente(q)) return true;
    if (esConsultaGeneralConsultasMedicas(q)) return false;
    boolean mencionaConsultas = contiene(q, "consulta medica", "consultas medicas", "atencion medica", "atenciones medicas");
    boolean identificaPaciente = contiene(q, "paciente", "dni", " id ") || DNI_PATTERN.matcher(q).find() || Pattern.compile("\\b(?:id|codigo|cod)\\s*\\d+\\b").matcher(q).find() || terminosNombre(extraerNombrePaciente(q)).size() >= 2;
    return mencionaConsultas && identificaPaciente;
  }

  private List<Consulta> filtrarEstado(List<Consulta> consultas, String estado) { return consultas.stream().filter(c -> estado.equalsIgnoreCase(Optional.ofNullable(c.getEstado()).orElse(""))).collect(Collectors.toList()); }
  private LocalDateTime fechaOrdenConsulta(Consulta c) { if (c.getFechaConsulta() != null) return c.getFechaConsulta().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(); return c.getFechaCreacion(); }
  private String sufijoEstado(String q) { if (contiene(q, "pendiente", "pendientes", "por atender")) return " pendientes"; if (contiene(q, "atendida", "atendidas", "atendio")) return " atendidas"; return " registradas"; }
  private String respuestaPacienteAmbiguoConsultas(List<Paciente> pacientes) { return "Se encontraron varias coincidencias para el paciente. Ingresa el DNI de 8 dígitos del paciente para realizar una búsqueda exacta:\n" + pacientes.stream().map(p -> "- ID: " + p.getIdPaciente() + ", Paciente: " + nombreCompleto(p) + ", DNI: " + valorSeguro(p.getNumDocumento())).collect(Collectors.joining("\n")); }
  private String detalleConsultaMedica(Consulta c, Paciente p) { return "ID de consulta médica: " + c.getIdConsulta() + "\nID de historia clínica: " + (c.getHistoriaClinica()==null?"Sin historia":c.getHistoriaClinica().getIdHistoriaClinica()) + "\nPaciente: " + nombreCompleto(p) + "\nDNI: " + valorSeguro(p.getNumDocumento()) + "\nFecha: " + fechaConsulta(c) + "\nEspecialidad: " + valorSeguro(c.getEspecialidadRequerida()) + "\nEstado: " + estado(c.getEstado()) + "\nDoctor responsable: " + doctorConsulta(c); }
  private Map<String,Object> consultaMedicaMap(Consulta c, Paciente p) { Map<String,Object> m=new LinkedHashMap<>(); m.put("idConsultaMedica", c.getIdConsulta()); m.put("idHistoriaClinica", c.getHistoriaClinica()==null?null:c.getHistoriaClinica().getIdHistoriaClinica()); m.put("paciente", nombreCompleto(p)); m.put("dni", p.getNumDocumento()); m.put("fecha", c.getFechaConsulta()==null?c.getFechaCreacion():c.getFechaConsulta()); m.put("especialidad", c.getEspecialidadRequerida()); m.put("estado", estado(c.getEstado())); m.put("doctorResponsable", doctorConsulta(c)); return m; }
  private String fechaConsulta(Consulta c) { if (c.getFechaConsulta() != null) return new SimpleDateFormat("dd/MM/yyyy").format(c.getFechaConsulta()); return c.getFechaCreacion()==null?"Sin fecha registrada":c.getFechaCreacion().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")); }
  private String doctorConsulta(Consulta c) { return c.getDoctorResponsable()==null?"Sin doctor asignado":String.join(" ", Optional.ofNullable(c.getDoctorResponsable().getNombres()).orElse(""), Optional.ofNullable(c.getDoctorResponsable().getApellidos()).orElse("")).trim(); }


  private AsistenteResponse verificarHistoriaClinicaPaciente(String q) {
    if (!esConsultaHistoriaClinicaPaciente(q)) return null;
    ResultadoBusquedaHistoria resultado = buscarPacienteParaHistoria(q);
    if (resultado.requiereDatos()) {
      return resp("HISTORIA_CLINICA_REQUIERE_PACIENTE", "Escribe el DNI o el nombre y los dos apellidos del paciente para verificar si tiene historia clínica.", Map.of());
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
    if (esConsultaGeneralHistoriasClinicas(q)) return false;
    boolean mencionaHistoria = contiene(q, "historia clinica", "historias clinicas");
    boolean consultaPaciente = contiene(q, "paciente", "dni", " id ", "para ", "este paciente");
    boolean accion = contiene(q, "tiene", "cuenta", "existe", "consulta", "consultar", "verifica", "verificar", "busca", "buscar", "ya tiene", "ya cuenta");
    boolean conteoGeneral = contiene(q, "cuantas", "cuantos", "cantidad", "total", "creadas", "creados") && !contiene(q, "paciente", "dni", " id ", "para ");
    return mencionaHistoria && consultaPaciente && accion && !conteoGeneral;
  }

  private ResultadoBusquedaHistoria buscarPacienteParaHistoria(String q) {
    Matcher dniMatcher = DNI_PATTERN.matcher(q);
    if (dniMatcher.find()) return resultadoDniActivo(dniMatcher.group());
    Matcher idMatcher = Pattern.compile("\\b(?:id|codigo|cod)\\s*(\\d+)\\b").matcher(q);
    if (idMatcher.find()) return new ResultadoBusquedaHistoria(pacienteRepository.findByIdPacienteAndEstadoRegistro(Integer.valueOf(idMatcher.group(1)), EstadoRegistroPaciente.ACTIVO), List.of(), false);
    Matcher pacienteIdMatcher = Pattern.compile("\\bpaciente\\s+(\\d{1,7})\\b").matcher(q);
    if (pacienteIdMatcher.find()) return new ResultadoBusquedaHistoria(pacienteRepository.findByIdPacienteAndEstadoRegistro(Integer.valueOf(pacienteIdMatcher.group(1)), EstadoRegistroPaciente.ACTIVO), List.of(), false);

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
        + "\n\nIngrese el DNI de 8 dígitos del paciente para realizar una búsqueda exacta.";
  }

  private record ResultadoBusquedaHistoria(Optional<Paciente> paciente, List<Paciente> similares, boolean requiereDatos) {}

  private ResultadoBusquedaHistoria resultadoDniActivo(String dni) {
    List<Paciente> pacientes = pacienteRepository.findByNumDocumentoAndEstadoRegistroOrderByIdPacienteAsc(dni, EstadoRegistroPaciente.ACTIVO);
    return pacientes.size() == 1
        ? new ResultadoBusquedaHistoria(Optional.of(pacientes.getFirst()), List.of(), false)
        : new ResultadoBusquedaHistoria(Optional.empty(), pacientes, false);
  }

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
      long c = p.total() ? pacienteRepository.countByEstadoRegistro(EstadoRegistroPaciente.ACTIVO) : pacienteRepository.countByEstadoRegistroAndFechaIngresoGreaterThanEqualAndFechaIngresoLessThan(EstadoRegistroPaciente.ACTIVO, Date.from(p.inicio().atZone(ZoneId.systemDefault()).toInstant()), Date.from(p.fin().atZone(ZoneId.systemDefault()).toInstant()));
      return cantidad("PACIENTES_REGISTRADOS", c, pref(p) + "hay %d pacientes registrados.", p);
    }
    return null;
  }

  private boolean esConteoPacientes(String q) {
    boolean mencionaPacientes = contiene(q, "paciente");
    boolean solicitaConteo = contiene(q, "cuantos", "cuantas", "cantidad", "total", "actuales", "actualmente", "registrados", "registradas", "hay", "existen");
    boolean mencionaOtraEntidad = contiene(q, "consulta medica", "consultas medicas", "historia clinica", "historias clinicas");
    return mencionaPacientes && solicitaConteo && !mencionaOtraEntidad;
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
      List<Paciente> pacientes = activosPorDni(dni);
      if (pacientes.size() > 1) return resp("BUSQUEDA_PACIENTE_DNI_AMBIGUO", respuestaPacientesSimilares(pacientes), Map.of("tipoBusqueda", "DNI", "resultados", pacientes.stream().map(this::pacienteMap).toList()));
      return pacientes.stream().findFirst()
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
    if (contiene(q, "dni")) return resp("BUSQUEDA_PACIENTE_REQUIERE_DNI", "Ingresa el DNI de 8 dígitos del paciente. Ejemplo: Buscar paciente por DNI (PONER DNI)", Map.of("tipoBusqueda", "DNI"));
    String nombre = extraerNombrePaciente(q);
    if (nombre.length() < 3 || contiene(nombre, "nombre")) return resp("BUSQUEDA_PACIENTE_REQUIERE_NOMBRE", "Ingresa el nombre y los dos apellidos del paciente.", Map.of("tipoBusqueda", "NOMBRE"));
    List<Paciente> coincidencias = pacienteRepository.searchByNombre(nombre, 5);
    if (coincidencias.isEmpty()) coincidencias = buscarPorNombreAproximado(nombre, 5);
    if (coincidencias.isEmpty()) return resp("BUSQUEDA_PACIENTE_SIN_RESULTADOS", "No se encontró un paciente registrado con esos datos.", Map.of("tipoBusqueda", "NOMBRE", "nombre", nombre));
    return resp("BUSQUEDA_PACIENTE_NOMBRE", respuestaPacientesSimilares(coincidencias), Map.of("tipoBusqueda", "NOMBRE", "resultados", coincidencias.stream().map(this::pacienteMap).collect(Collectors.toList())));
  }


  private AsistenteResponse buscarPacientePorId(Integer id) {
    return pacienteRepository.findByIdPacienteAndEstadoRegistro(id, EstadoRegistroPaciente.ACTIVO)
        .map(paciente -> resp("BUSQUEDA_PACIENTE_ID", respuestaPacienteRegistrado(paciente), Map.of("tipoBusqueda", "ID", "paciente", pacienteMap(paciente))))
        .orElse(resp("BUSQUEDA_PACIENTE_SIN_RESULTADOS", "No se encontró un paciente registrado con ese ID.", Map.of("tipoBusqueda", "ID", "idPaciente", id)));
  }

  private AsistenteResponse ultimosPacientes() {
    List<Paciente> pacientes = new ArrayList<>();
    pacienteRepository.findAllByEstadoRegistroOrderByIdPacienteAsc(EstadoRegistroPaciente.ACTIVO).forEach(pacientes::add);
    pacientes.sort(Comparator.comparing(Paciente::getFechaIngreso, Comparator.nullsLast(Date::compareTo)).reversed());
    List<Paciente> ultimos = pacientes.stream().limit(5).collect(Collectors.toList());
    if (ultimos.isEmpty()) return resp("ULTIMOS_PACIENTES", "No se encontraron pacientes registrados.", Map.of("resultados", List.of()));
    return resp("ULTIMOS_PACIENTES", "Últimos pacientes registrados:\n" + ultimos.stream().map(this::detallePaciente).collect(Collectors.joining("\n\n")), Map.of("resultados", ultimos.stream().map(this::pacienteMap).collect(Collectors.toList())));
  }

  private AsistenteResponse estadisticaEdad(String q) {
    List<Paciente> pacientes = new ArrayList<>();
    pacienteRepository.findAllByEstadoRegistroOrderByIdPacienteAsc(EstadoRegistroPaciente.ACTIVO).forEach(pacientes::add);
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
      List<Paciente> pacientes = activosPorDni(dni);
      if (pacientes.size() > 1) return resp("BUSQUEDA_DUPLICADO_DNI_MULTIPLE", respuestaPacientesSimilares(pacientes), Map.of("tipoBusqueda", "DNI", "resultados", pacientes.stream().map(this::pacienteMap).toList()));
      return pacientes.stream().findFirst()
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
    return !esConsultaDuplicidadHistoriasClinicas(q)
        && contiene(q, "duplicado", "duplicados", "duplicada", "duplicadas", "repetido", "repetidos", "duplicidad");
  }

  private AsistenteResponse responderHistoriasClinicasDuplicadas(String q) {
    if (!esConsultaDuplicidadHistoriasClinicas(q)) return null;
    Matcher matcher = DNI_PATTERN.matcher(q);
    String dni = matcher.find() ? matcher.group() : null;
    DuplicadosHistoriasClinicasResponse resultado = historiaClinicaService.obtenerDuplicadosParaIntegracion(dni);
    String detalle = resultado.getDuplicados().stream().map(this::detalleGrupoHistoriasDuplicadas).collect(Collectors.joining("\n\n"));
    String respuesta = detalle.isBlank() ? resultado.getMensaje() : resultado.getMensaje() + "\n\n" + detalle;
    return resp("HISTORIAS_CLINICAS_DUPLICADAS", respuesta, Map.of(
        "hayDuplicados", resultado.isHayDuplicados(),
        "totalGrupos", resultado.getTotalGrupos(),
        "dniConsultado", Optional.ofNullable(resultado.getDniConsultado()).orElse(""),
        "duplicados", resultado.getDuplicados()));
  }

  private boolean esConsultaDuplicidadHistoriasClinicas(String q) {
    boolean duplicidadExplicita = contiene(q, "duplicado", "duplicados", "duplicada", "duplicadas", "repetido", "repetidos", "repetida", "repetidas", "duplicidad", "mas de una historia");
    boolean mencionaHistorias = contiene(q, "historia clinica", "historias clinicas")
        || (contiene(q, "historia", "historias") && duplicidadExplicita);
    boolean verificacionPluralPorDni = contiene(q, "historias clinicas") && contiene(q, "dni") && contiene(q, "verifica", "verificar", "busca", "buscar");
    return mencionaHistorias && (duplicidadExplicita || verificacionPluralPorDni);
  }

  private String detalleGrupoHistoriasDuplicadas(GrupoDuplicadoHistoriaClinicaResponse grupo) {
    String encabezado = "DNI: " + grupo.getHistoriasClinicas().stream().map(HistoriaClinicaIntegracionItemResponse::getDni)
        .filter(Objects::nonNull).findFirst().orElse("Sin DNI") + " (" + grupo.getCantidad() + " historias)";
    String historias = grupo.getHistoriasClinicas().stream().map(this::detalleHistoriaDuplicada).collect(Collectors.joining("\n\n"));
    return encabezado + "\n" + historias + "\n\n" + grupo.getRecomendacion();
  }

  private String detalleHistoriaDuplicada(HistoriaClinicaIntegracionItemResponse historia) {
    return "ID historia clínica: " + historia.getIdHistoriaClinica()
        + "\nID paciente: " + historia.getIdPaciente()
        + "\nPaciente: " + historia.getNombreCompleto()
        + "\nDNI: " + valorSeguro(historia.getDni())
        + "\nFecha de creación: " + fechaHistoria(historia.getFechaCreacion())
        + "\nÚltima actualización: " + fechaHistoria(historia.getUltimaActualizacion())
        + "\nConsultas asociadas: " + historia.getCantidadConsultas()
        + "\nEstado de la historia: " + historia.getEstado();
  }

  private AsistenteResponse analizarDuplicadosPacientes() {
    Map<String, List<Paciente>> grupos = new LinkedHashMap<>();
    pacienteRepository.findAllByEstadoRegistroOrderByIdPacienteAsc(EstadoRegistroPaciente.ACTIVO).forEach(paciente -> {
      String dni = paciente.getNumDocumento() == null ? "" : paciente.getNumDocumento().trim();
      if (!dni.isBlank()) grupos.computeIfAbsent("DNI " + dni, ignored -> new ArrayList<>()).add(paciente);
    });
    List<Paciente> duplicados = grupos.values().stream()
        .filter(lista -> lista.size() > 1)
        .flatMap(List::stream)
        .limit(10)
        .collect(Collectors.toList());
    if (duplicados.isEmpty()) return resp("ANALISIS_DUPLICADOS_SIN_RESULTADOS", "No se encontraron pacientes duplicados activos por DNI.", Map.of("cantidad", 0));
    return resp("ANALISIS_DUPLICADOS_PACIENTES", "Se encontraron posibles pacientes duplicados:\n" + duplicados.stream().map(this::detallePaciente).collect(Collectors.joining("\n\n")) + "\n\nRevise la información antes de crear una nueva historia clínica.", Map.of("cantidad", duplicados.size(), "resultados", duplicados.stream().map(this::pacienteMap).collect(Collectors.toList())));
  }

  private List<Paciente> buscarPorNombreAproximado(String nombre, int limit) {
    String[] tokens = nombre.split(" ");
    List<Paciente> resultados = new ArrayList<>();
    pacienteRepository.findAllByEstadoRegistroOrderByIdPacienteAsc(EstadoRegistroPaciente.ACTIVO).forEach(paciente -> {
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
    return q.replaceAll("\\b(busca|buscar|verifica|verificar|consultar|consulta|consultas|muestrame|mostrar|cuantas|cuantos|cantidad|cual|fue|ultima|ultimo|atencion|atenciones|medica|medicas|pendiente|pendientes|atendida|atendidas|atendio|atendieron|atender|estan|por|nombre|si|existe|existen|ya|esta|registrado|registrada|paciente|pacientes|con|dni|id|codigo|cod|historia|historias|clinica|clinicas|para|de|del|el|la|un|una|por|favor|datos|duplicado|duplicados|duplicada|duplicadas|repetido|repetidos|duplicidad|tiene|cuenta|contiene|asociada|asociado|este|esa|ese)\\b", " ").replaceAll("\\d+", " ").replaceAll("\\s+", " ").trim();
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

  private List<Paciente> activosPorDni(String dni) {
    return pacienteRepository.findByNumDocumentoAndEstadoRegistroOrderByIdPacienteAsc(dni, EstadoRegistroPaciente.ACTIVO);
  }


  private String mensajeAyudaConsultas() {
    return "Puedes realizar estas consultas:\n\n"
        + "PACIENTES\n"
        + "- ¿Cuántos pacientes hay registrados?\n"
        + "- ¿Cuántos pacientes se registraron hoy?\n"
        + "- Muéstrame los últimos pacientes registrados.\n"
        + "- ¿Cuáles son los pacientes más recientes?\n\n"
        + "BÚSQUEDA DE PACIENTES\n"
        + "- Buscar paciente por DNI (PONER DNI).\n"
        + "- Buscar paciente por nombre (AGREGAR NOMBRE Y DOS APELLIDOS).\n"
        + "- Verifica si existe el paciente con DNI (PONER DNI).\n\n"
        + "CONSULTAS MÉDICAS POR PACIENTE\n"
        + "- ¿El paciente con DNI (PONER DNI) tiene consultas médicas?\n"
        + "- ¿El paciente (AGREGAR NOMBRE Y DOS APELLIDOS) tiene consultas médicas?\n"
        + "- ¿Cuál fue la última consulta médica del paciente con DNI (PONER DNI)?\n"
        + "- ¿El paciente (AGREGAR NOMBRE Y DOS APELLIDOS) tiene consultas médicas pendientes?\n\n"
        + "HISTORIAS CLÍNICAS POR PACIENTE\n"
        + "- ¿El paciente con DNI (PONER DNI) tiene historia clínica?\n"
        + "- ¿El paciente (AGREGAR NOMBRE Y DOS APELLIDOS) tiene historia clínica?\n"
        + "- Busca la historia clínica del paciente con DNI (PONER DNI).\n\n"
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
  private String ayuda(String q) {
    if (contiene(q,"ayuda","que preguntas puedo hacer","que puedo preguntarte","como puedo usar el asistente","como usar el asistente","que puedes consultar","que consultas soportas","mostrar opciones","mostrar preguntas disponibles","mostrar consultas disponibles","preguntas disponibles","muestrame las opciones","comandos disponibles")) return mensajeAyudaConsultas();
    if (contiene(q,"como registro un paciente")) return "Ingresa a la sección Pacientes y haz clic en el botón Agregar Pacientes. Completa los datos personales y los antecedentes del paciente. Finalmente, haz clic en Guardar para registrar la información.";
    if (contiene(q,"como edito los datos de un paciente","como edito un paciente")) return "Ingresa a la sección Pacientes, busca al paciente que deseas modificar y haz clic en el ícono del lápiz ubicado en la columna Opciones. Actualiza los datos necesarios y guarda los cambios.";
    if (contiene(q,"como visualizo los datos de un paciente","como visualizo un paciente")) return "Ingresa a la sección Pacientes, busca al paciente que deseas consultar y haz clic en el ícono del ojo ubicado en la columna Opciones. El sistema mostrará todos los datos registrados del paciente.";
    if (contiene(q,"como creo una historia clinica","como crear una historia clinica")) return "Ingresa a la sección Historia Clínica y haz clic en el botón Agregar HC. Selecciona al paciente previamente registrado, completa los datos de la historia clínica y sus antecedentes patológicos. Finalmente, haz clic en Guardar. Recuerda que cada historia clínica debe estar asociada a un paciente registrado en el sistema.";
    if (contiene(q,"como edito una historia clinica")) return "Ingresa a la sección Historia Clínica, busca la historia que deseas modificar y haz clic en el ícono del lápiz ubicado en la columna Opciones. Actualiza los datos necesarios y guarda los cambios.";
    if (contiene(q,"como visualizo una historia clinica")) return "Ingresa a la sección Historia Clínica, busca la historia que deseas consultar y haz clic en el ícono del ojo ubicado en la columna Opciones. El sistema mostrará toda la información registrada.";
    if (contiene(q,"como agrego una consulta medica","como agregar una consulta medica")) return "Ingresa a la sección Historia Clínica y busca la historia clínica del paciente. En la columna Opciones, haz clic en el ícono de documento para acceder a sus consultas médicas. Luego, selecciona Agregar consulta, completa los campos requeridos y guarda la información.";
    if (contiene(q,"como comienzo la atencion de una consulta medica","como comenzar la atencion de una consulta medica")) return "Ingresa a la sección Consultas y busca una consulta que se encuentre en estado Por atender. En la columna Opciones, haz clic en Comenzar atención, completa la evaluación médica y guarda la información para finalizar la atención.";
    if (contiene(q,"como visualizo una consulta medica antes de atenderla")) return "Ingresa a la sección Consultas, busca la consulta que deseas revisar y haz clic en el ícono del ojo ubicado en la columna Opciones. El sistema mostrará los datos de la consulta sin necesidad de comenzar la atención.";
    return null;
  }
}
