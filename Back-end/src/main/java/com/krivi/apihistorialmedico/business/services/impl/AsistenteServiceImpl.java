package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.services.AsistenteService;
import com.krivi.apihistorialmedico.model.api.AsistenteRequest;
import com.krivi.apihistorialmedico.model.api.AsistenteResponse;
import com.krivi.apihistorialmedico.model.entity.Paciente;
import com.krivi.apihistorialmedico.model.entity.Usuario;
import com.krivi.apihistorialmedico.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.time.*;
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
      AsistenteResponse duplicado = buscarPacienteDuplicado(q); if (duplicado != null) return duplicado;
      if (contiene(q,"doctor autenticado","mis consultas","asignadas al doctor","consultas asignadas")) { if (idEmpleado == null) return sinPermiso(); if (contiene(q,"atendio","atendidas")) return cantidad("CONSULTAS_ATENDIDAS_DOCTOR", consultaRepository.countByDoctorResponsableIdEmpleadoAndEstado(idEmpleado,"ATENDIDO"), "El doctor autenticado atendió %d consultas.", p); return cantidad("CONSULTAS_ASIGNADAS_DOCTOR", consultaRepository.countByDoctorResponsableIdEmpleado(idEmpleado), "El doctor autenticado tiene %d consultas asignadas.", p); }
      if (contiene(q,"paciente") && contiene(q,"sin historia","no tienen historia")) return cantidad("PACIENTES_SIN_HISTORIA_CLINICA", pacienteRepository.count()-historiaClinicaRepository.count(), "Actualmente hay %d pacientes sin historia clínica.", p);
      if (contiene(q,"paciente") && contiene(q,"con historia","tienen historia")) return cantidad("PACIENTES_CON_HISTORIA_CLINICA", historiaClinicaRepository.count(), "Actualmente hay %d pacientes con historia clínica.", p);
      if (contiene(q,"paciente")) { long c=p.total()?pacienteRepository.count():pacienteRepository.countByFechaIngresoGreaterThanEqualAndFechaIngresoLessThan(Date.from(p.inicio().atZone(ZoneId.systemDefault()).toInstant()), Date.from(p.fin().atZone(ZoneId.systemDefault()).toInstant())); return cantidad("PACIENTES_REGISTRADOS", c, pref(p)+"hay %d pacientes registrados.", p); }
      if (contiene(q,"historia")) { if (contiene(q,"incompleta")) return cantidad("HISTORIAS_CLINICAS_INCOMPLETAS",0,"No se encontraron historias clínicas incompletas según los campos obligatorios actuales.",p); long c=p.total()?historiaClinicaRepository.count():historiaClinicaRepository.countByFechaCreacionGreaterThanEqualAndFechaCreacionLessThan(p.inicio(),p.fin()); return cantidad("HISTORIAS_CLINICAS",c,pref(p)+"hay %d historias clínicas.",p); }
      if (contiene(q,"especialidad")) return ranking("ESPECIALIDADES_MAS_REQUERIDAS", p.total()?consultaRepository.rankingEspecialidades():consultaRepository.rankingEspecialidades(p.inicio(),p.fin()), "especialidades");
      if (contiene(q,"enfermedad")) return ranking("TIPOS_ENFERMEDAD_MAS_REGISTRADOS", p.total()?consultaRepository.rankingTiposEnfermedad():consultaRepository.rankingTiposEnfermedad(p.inicio(),p.fin()), "tipos de enfermedad");
      if (contiene(q,"doctor") && contiene(q,"mas","cada doctor")) return ranking("DOCTORES_CON_MAS_ATENCIONES", consultaRepository.rankingDoctoresAtenciones(), "doctores");
      if (contiene(q,"consulta","atencion","atenciones")) { if (contiene(q,"incompleta","todavia no","faltan")) return incompletas(); if (contiene(q,"pendiente","por atender","faltan")) return cantidad("CONSULTAS_PENDIENTES", p.total()?consultaRepository.countByEstado("PENDIENTE"):consultaRepository.countByEstadoAndFechaCreacionGreaterThanEqualAndFechaCreacionLessThan("PENDIENTE",p.inicio(),p.fin()), pref(p)+"hay %d consultas por atender.", p); if (contiene(q,"atendida","atendidas","atendio")) return cantidad("CONSULTAS_ATENDIDAS", p.total()?consultaRepository.countByEstado("ATENDIDO"):consultaRepository.countByEstadoAndFechaCreacionGreaterThanEqualAndFechaCreacionLessThan("ATENDIDO",p.inicio(),p.fin()), pref(p)+"hay %d consultas atendidas.", p); long c=p.total()?consultaRepository.count():consultaRepository.countByFechaCreacionGreaterThanEqualAndFechaCreacionLessThan(p.inicio(),p.fin()); return cantidad("CONSULTAS_REGISTRADAS",c,pref(p)+"hay %d consultas médicas registradas.",p); }
    } catch (Exception e) { return resp("ERROR_INTERNO", "No pude obtener la información en este momento. Inténtalo nuevamente.", Map.of()); }
    return resp("NO_RECONOCIDA", "No pude identificar la consulta. Puedes preguntarme sobre pacientes, historias clínicas, consultas médicas, especialidades o tipos de enfermedad.", Map.of());
  }

  private AsistenteResponse buscarPacienteDuplicado(String q) {
    if (!esConsultaDuplicadoPaciente(q)) return null;
    Matcher matcher = DNI_PATTERN.matcher(q);
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
    return contiene(q, "existe", "registrado", "registrada", "busca", "buscar", "verifica", "verificar", "encuentra", "historia clinica") && contiene(q, "paciente", "dni", "historia clinica", "registrado", "registrada");
  }

  private String extraerNombrePaciente(String q) {
    return q.replaceAll("\\b(busca|buscar|verifica|verificar|si|existe|ya|esta|registrado|registrada|paciente|con|dni|historia|clinica|para|el|la|un|una|por|favor|datos)\\b", " ").replaceAll("\\d+", " ").replaceAll("\\s+", " ").trim();
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

  private AsistenteResponse incompletas(){List<Map<String,Object>> items=consultaRepository.findIncompletas().stream().limit(5).map(c->{Map<String,Object>m=new LinkedHashMap<>();m.put("idConsulta",c.getIdConsulta());m.put("estado",estado(c.getEstado()));m.put("especialidad",c.getEspecialidadRequerida());m.put("fecha",c.getFechaCreacion());m.put("doctor",c.getDoctorResponsable()==null?null:c.getDoctorResponsable().getNombres()+" "+c.getDoctorResponsable().getApellidos());return m;}).collect(Collectors.toList());return resp("CONSULTAS_INCOMPLETAS",items.isEmpty()?"No se encontraron registros para el periodo solicitado.":"Actualmente existen "+consultaRepository.countIncompletas()+" consultas incompletas. Te muestro hasta 5 resultados.",Map.of("resultados",items));}
  private AsistenteResponse ranking(String i,List<Object[]> rows,String n){List<Map<String,Object>> r=rows.stream().limit(5).map(a->Map.<String,Object>of("nombre",String.valueOf(a[0]),"cantidad",((Number)a[1]).longValue())).collect(Collectors.toList());return resp(i,r.isEmpty()?"No se encontraron registros para el periodo solicitado.":"Estos son los principales "+n+" registrados.",Map.of("resultados",r));}
  private AsistenteResponse cantidad(String i,long c,String f,Periodo p){return resp(i,String.format(f,c),Map.of("cantidad",c,"periodo",p.nombre()));} private AsistenteResponse sinPermiso(){return resp("SIN_PERMISOS","No tienes permisos para consultar esa información.",Map.of());} private AsistenteResponse resp(String i,String r,Map<String,Object>d){return AsistenteResponse.builder().intencion(i).respuesta(r).datos(d).build();}
  private boolean contiene(String q,String...xs){return Arrays.stream(xs).anyMatch(q::contains);} private String estado(String e){return "PENDIENTE".equals(e)?"Por atender":"ATENDIDO".equals(e)?"Atendido":e;} private String normalizar(String s){if(s==null)return"";return Normalizer.normalize(s.toLowerCase().trim(),Normalizer.Form.NFD).replaceAll("\\p{M}","").replaceAll("[¿?¡!.,;:]+"," ").replaceAll("\\s+"," ");} private String pref(Periodo p){return p.total()?"Actualmente ":p.nombre().equals("HOY")?"Hoy ":p.nombre().equals("SEMANA_ACTUAL")?"Esta semana ":"Este mes ";}
  private Periodo periodo(String q){LocalDate n=LocalDate.now();if(q.contains("hoy"))return new Periodo("HOY",n.atStartOfDay(),n.plusDays(1).atStartOfDay(),false);if(q.contains("semana")){LocalDate i=n.with(DayOfWeek.MONDAY);return new Periodo("SEMANA_ACTUAL",i.atStartOfDay(),i.plusWeeks(1).atStartOfDay(),false);}if(q.contains("mes")){LocalDate i=n.withDayOfMonth(1);return new Periodo("MES_ACTUAL",i.atStartOfDay(),i.plusMonths(1).atStartOfDay(),false);}return new Periodo("TOTAL",null,null,true);} record Periodo(String nombre,LocalDateTime inicio,LocalDateTime fin,boolean total){}
  private String ayuda(String q){if(contiene(q,"como registro un paciente"))return"Para registrar un paciente ingresa a Pacientes, selecciona Agregar Pacientes, completa los datos obligatorios y guarda.";if(contiene(q,"como edito un paciente"))return"Para editar un paciente ingresa a Pacientes, busca el registro y usa la acción Editar.";if(contiene(q,"historia clinica")&&contiene(q,"como creo","crear"))return"Para crear una historia clínica selecciona el paciente y registra su historia desde el módulo Historia Clínica.";if(contiene(q,"consulta")&&contiene(q,"agrego","agregar","crear"))return"Para agregar una consulta médica abre la historia clínica del paciente y usa la opción para registrar una nueva consulta.";if(contiene(q,"atiendo una consulta","atender una consulta"))return"Para atender una consulta entra a Consultas, abre el detalle pendiente, completa diagnóstico y tratamiento, y finaliza la atención.";if(contiene(q,"empleado"))return"Para registrar o activar/desactivar empleados ingresa al módulo Empleados y usa las acciones disponibles.";if(contiene(q,"usuario","permisos"))return"Para crear usuarios o asignar permisos ingresa al módulo Usuarios y configura sus permisos por menú.";return null;}
}
