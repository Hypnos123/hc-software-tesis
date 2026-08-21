package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.services.ConsultaService;
import com.krivi.apihistorialmedico.model.api.*;
import com.krivi.apihistorialmedico.model.entity.*;
import com.krivi.apihistorialmedico.repository.*;
import com.krivi.apihistorialmedico.util.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static com.krivi.apihistorialmedico.util.Constant.MENSAJE_GUARDAR_ERROR;
import static com.krivi.apihistorialmedico.util.Constant.MENSAJE_GUARDAR_OK;

@Service
@Slf4j
public class ConsultaServiceImpl implements ConsultaService {
  @Autowired ConsultaRepository consultaRepository;
  @Autowired PacienteRepository pacienteRepository;
  @Autowired TipoEnfermedadRepository tipoEnfermedadRepository;
  @Autowired UsuarioRepository usuarioRepository;
  @Autowired HistoriaClinicaRepository historiaClinicaRepository;
  @Autowired EmpleadoRepository empleadoRepository;
  @Autowired AntecedentesRepository antecedentesRepository;

  @Override
  public ResponseModelGet<ConsultaResponse> getAllActive(Integer idUsuario) {
    Usuario usuario = getUsuarioAutenticado(idUsuario);
    List<ConsultaResponse> data = new ArrayList<>();
    Iterable<Consulta> consultas = esAdministrador(usuario)
        ? consultaRepository.findAll()
        : consultaRepository.findByDoctorResponsableIdEmpleado(usuario.getEmpleado().getIdEmpleado());
    List<Consulta> consultasVisibles = new ArrayList<>();
    consultas.forEach(consultasVisibles::add);
    Map<Integer, Long> atendidasPorPaciente = new HashMap<>();
    List<Integer> idsPaciente = consultasVisibles.stream().map(Consulta::getPaciente).filter(Objects::nonNull)
        .map(Paciente::getIdPaciente).distinct().toList();
    if (!idsPaciente.isEmpty()) {
      consultaRepository.contarAtendidasPorPacientes(idsPaciente).forEach(resumen ->
          atendidasPorPaciente.put(((Number) resumen[0]).intValue(), ((Number) resumen[1]).longValue()));
    }
    consultasVisibles.forEach(consulta -> data.add(toResponse(consulta,
        atendidasPorPaciente.getOrDefault(consulta.getPaciente().getIdPaciente(), 0L))));
    data.sort(Comparator.comparing((ConsultaResponse c) -> !"PENDIENTE".equals(normalizeEstado(c.getEstado())))
        .thenComparing(ConsultaResponse::getFechaCreacion, Comparator.nullsLast(Comparator.reverseOrder())));
    return response(data);
  }

  @Override
  public ResponseModelGet<ConsultaResponse> findById(int idConsulta, Integer idUsuario) {
    Usuario usuario = getUsuarioAutenticado(idUsuario);
    return response(consultaRepository.findById(idConsulta).filter(c -> puedeAcceder(usuario, c)).map(this::toResponse).map(List::of).orElse(List.of()));
  }

  @Override
  public ResponseModelGet<ConsultaResponse> findByHistoriaClinica(int idHistoriaClinica) {
    List<ConsultaResponse> data = new ArrayList<>();
    consultaRepository.findByHistoriaClinicaIdHistoriaClinica(idHistoriaClinica).forEach(c -> data.add(toResponse(c)));
    return response(data);
  }

  @Override
  public ResponseModelSet save(ConsultaRequest request, Integer idUsuario) {
    Usuario usuario = getUsuarioAutenticado(idUsuario);
    String rol = rolUsuario(usuario);
    if (!"ADMINISTRADOR".equals(rol) && !"ENFERMERO".equals(rol)) {
      throw new SecurityException("El rol del usuario no permite crear consultas");
    }
    ResponseModelSet response = new ResponseModelSet();
    try {
      Consulta consulta = new Consulta();
      applyRequest(consulta, request, true);
      consulta.setUsuario(usuario);
      if ("ENFERMERO".equals(rol)) limpiarEvaluacionMedica(consulta);
      Consulta saved = consultaRepository.save(consulta);
      response.setIdGenerado(saved.getIdConsulta());
      response.setMensaje(MENSAJE_GUARDAR_OK);
      return response;
    } catch (Exception e) {
      log.error("save(): {}", e.getMessage());
      response.setMensaje(MENSAJE_GUARDAR_ERROR);
      response.setError(e.getMessage());
      return response;
    }
  }

  @Override
  public ResponseModelSet update(ConsultaRequest request) {
    ResponseModelSet response = new ResponseModelSet();
    try {
      Consulta consulta = consultaRepository.findById(request.getIdConsulta())
          .orElseThrow(() -> new IllegalArgumentException("Consulta no encontrada"));
      applyRequest(consulta, request, false);
      Consulta saved = consultaRepository.save(consulta);
      response.setIdGenerado(saved.getIdConsulta());
      response.setMensaje(MENSAJE_GUARDAR_OK);
      return response;
    } catch (Exception e) {
      log.error("update(): {}", e.getMessage());
      response.setMensaje(MENSAJE_GUARDAR_ERROR);
      response.setError(e.getMessage());
      return response;
    }
  }

  @Override
  public ResponseModelSet finalizarAtencion(int idConsulta, ConsultaRequest request, Integer idUsuario) {
    ResponseModelSet response = new ResponseModelSet();
    try {
      Usuario usuario = getUsuarioAutenticado(idUsuario);
      Consulta consulta = consultaRepository.findById(idConsulta)
          .orElseThrow(() -> new IllegalArgumentException("Consulta no encontrada"));
      if (!puedeAcceder(usuario, consulta)) throw new SecurityException("No tiene permiso para atender esta consulta");
      if (!"PENDIENTE".equals(normalizeEstado(consulta.getEstado()))) throw new IllegalArgumentException("La consulta ya fue atendida o no está pendiente");
      if (!hasText(request.getDiagnostico()) || !hasText(request.getTratamiento())) throw new IllegalArgumentException("Debe completar diagnóstico y tratamiento");
      consulta.setDiagnostico(request.getDiagnostico());
      consulta.setExamenesRecetados(request.getExamenesRecetados());
      consulta.setReceta(request.getReceta());
      consulta.setTratamiento(request.getTratamiento());
      consulta.setProximaCita(request.getProximaCita());
      consulta.setEstado("ATENDIDO");
      if (consulta.getFechaAtencion() == null) consulta.setFechaAtencion(LocalDateTime.now(ZoneId.of("America/Lima")));
      consulta.setUsuario(usuario);
      Consulta saved = consultaRepository.save(consulta);
      response.setIdGenerado(saved.getIdConsulta());
      response.setMensaje(MENSAJE_GUARDAR_OK);
      return response;
    } catch (Exception e) {
      log.error("finalizarAtencion(): {}", e.getMessage());
      response.setMensaje(MENSAJE_GUARDAR_ERROR);
      response.setError(e.getMessage());
      return response;
    }
  }

  private void applyRequest(Consulta consulta, ConsultaRequest request, boolean nuevo) {
    Integer idHistoriaClinica = request.getIdHistoriaClinica() != null ? request.getIdHistoriaClinica() : consulta.getHistoriaClinica().getIdHistoriaClinica();
    HistoriaClinica historia = historiaClinicaRepository.findByIdHistoriaClinicaAndPacienteEstadoRegistro(
            idHistoriaClinica, com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente.ACTIVO)
        .orElseThrow(() -> new IllegalArgumentException("Historia clínica no encontrada"));
    Empleado doctor = empleadoRepository.findById(request.getIdEmpleadoDoctor())
        .filter(this::isDoctorActivo)
        .orElseThrow(() -> new IllegalArgumentException("Debe seleccionar un doctor activo"));

    consulta.setHistoriaClinica(historia);
    consulta.setPaciente(historia.getPaciente());
    consulta.setDoctorResponsable(doctor);
    consulta.setPresionArterial(request.getPresionArterial());
    consulta.setFrecuenciaCardiaca(request.getFrecuenciaCardiaca());
    consulta.setFrecuenciaRespiratoria(request.getFrecuenciaRespiratoria());
    consulta.setTalla(request.getTalla());
    consulta.setTemperatura(request.getTemperatura());
    consulta.setPeso(request.getPeso());
    consulta.setFechaConsulta(request.getFechaConsulta());
    consulta.setTiempoEnfermedad(request.getTiempoEnfermedad());
    consulta.setRelatoPaciente(request.getRelatoPaciente());
    consulta.setEspecialidadRequerida(normalizeEspecialidad(request.getEspecialidadRequerida()));
    consulta.setDiagnostico(request.getDiagnostico());
    consulta.setExamenesRecetados(request.getExamenesRecetados());
    consulta.setReceta(request.getReceta());
    consulta.setTratamiento(request.getTratamiento());
    consulta.setProximaCita(request.getProximaCita());
    consulta.setTipoEnfermedad(resolveTipoEnfermedad(request));
    if (request.getIdUsuario() != null) usuarioRepository.findById(request.getIdUsuario()).ifPresent(consulta::setUsuario);
    if (!nuevo && tieneEvaluacionMedica(request)) {
      if (!"ATENDIDO".equals(normalizeEstado(consulta.getEstado()))) {
        consulta.setEstado("ATENDIDO");
        if (consulta.getFechaAtencion() == null) consulta.setFechaAtencion(LocalDateTime.now(ZoneId.of("America/Lima")));
      }
    }
  }


  private Usuario getUsuarioAutenticado(Integer idUsuario) {
    if (idUsuario == null) throw new SecurityException("Usuario autenticado requerido");
    return usuarioRepository.findById(idUsuario).filter(u -> Boolean.TRUE.equals(u.getEstado()))
        .orElseThrow(() -> new SecurityException("Usuario autenticado inválido"));
  }

  private boolean esAdministrador(Usuario usuario) { return "ADMINISTRADOR".equals(normalize(usuario.getTipoUsuario())); }

  private String rolUsuario(Usuario usuario) {
    String tipo = normalize(usuario.getTipoUsuario());
    String cargo = usuario.getEmpleado() == null ? null : normalize(usuario.getEmpleado().getCargo());
    if (cargo != null && Set.of("ENFERMERA", "ENFERMERA(O)", "ENFERMERO(A)", "ENFERMERIA").contains(cargo)) return "ENFERMERO";
    return cargo != null ? cargo : tipo;
  }

  private void limpiarEvaluacionMedica(Consulta consulta) {
    consulta.setDiagnostico(null);
    consulta.setExamenesRecetados(null);
    consulta.setReceta(null);
    consulta.setTratamiento(null);
    consulta.setProximaCita(null);
  }

  private boolean puedeAcceder(Usuario usuario, Consulta consulta) {
    return esAdministrador(usuario) || (consulta.getDoctorResponsable() != null && usuario.getEmpleado() != null
        && Objects.equals(consulta.getDoctorResponsable().getIdEmpleado(), usuario.getEmpleado().getIdEmpleado()));
  }

  private String normalizeEstado(String v) { return normalize(v); }

  private TipoEnfermedad resolveTipoEnfermedad(ConsultaRequest request) {
    if (request.getIdTipoEnfermedad() != null) return tipoEnfermedadRepository.findById(request.getIdTipoEnfermedad()).orElseThrow();
    String descripcion = normalizeTipoEnfermedad(request.getTipoEnfermedad());
    return tipoEnfermedadRepository.findByDescripcionIgnoreCase(descripcion).orElseGet(() -> {
      TipoEnfermedad tipo = new TipoEnfermedad();
      tipo.setDescripcion(descripcion);
      return tipoEnfermedadRepository.save(tipo);
    });
  }

  private boolean isDoctorActivo(Empleado empleado) {
    String cargo = normalize(empleado.getCargo());
    return Boolean.TRUE.equals(empleado.getEstado()) && ("DOCTOR".equals(cargo) || "MEDICO".equals(cargo));
  }

  private boolean tieneEvaluacionMedica(ConsultaRequest request) {
    return hasText(request.getDiagnostico()) || hasText(request.getExamenesRecetados()) || hasText(request.getReceta()) || hasText(request.getTratamiento()) || request.getProximaCita() != null;
  }

  private ConsultaResponse toResponse(Consulta c) {
    return toResponse(c, null);
  }

  private ConsultaResponse toResponse(Consulta c, Long consultasAtendidas) {
    Paciente p = c.getPaciente();
    Antecedentes a = antecedentesRepository.findByPacienteIdPaciente(p.getIdPaciente()).stream().findFirst().orElse(null);
    Empleado d = c.getDoctorResponsable();
    return ConsultaResponse.builder()
        .idConsulta(c.getIdConsulta()).idHistoriaClinica(c.getHistoriaClinica() == null ? null : c.getHistoriaClinica().getIdHistoriaClinica())
        .fechaCreacion(c.getFechaCreacion()).estado(c.getEstado()).presionArterial(c.getPresionArterial()).frecuenciaCardiaca(c.getFrecuenciaCardiaca())
        .frecuenciaRespiratoria(c.getFrecuenciaRespiratoria()).talla(c.getTalla()).temperatura(c.getTemperatura()).peso(c.getPeso()).fechaConsulta(c.getFechaConsulta())
        .tiempoEnfermedad(c.getTiempoEnfermedad()).tipoEnfermedad(c.getTipoEnfermedad() == null ? null : c.getTipoEnfermedad().getDescripcion())
        .idTipoEnfermedad(c.getTipoEnfermedad() == null ? null : c.getTipoEnfermedad().getIdTipoEnfermedad()).especialidadRequerida(c.getEspecialidadRequerida())
        .idEmpleadoDoctor(d == null ? null : d.getIdEmpleado()).doctorResponsable(d == null ? null : d.getApellidos() + " " + d.getNombres())
        .relatoPaciente(c.getRelatoPaciente()).diagnostico(c.getDiagnostico()).examenesRecetados(c.getExamenesRecetados()).receta(c.getReceta()).tratamiento(c.getTratamiento()).proximaCita(c.getProximaCita())
        .idPaciente(p.getIdPaciente()).nombres(p.getNombres()).apellidos(p.getApellidos()).numDocumento(p.getNumDocumento()).edad(edad(p.getFechaNacimiento())).consultasAtendidas(consultasAtendidas)
        .enfermedadesPrevias(a == null ? null : a.getEnfermedadesPrevias()).cirugiasPrevias(a == null ? null : a.getCirugiasPrevias()).alergiaMedicamentos(a == null ? null : a.getAlergiaMedicamentos())
        .idUsuario(c.getUsuario() == null ? null : c.getUsuario().getIdUsuario()).build();
  }

  private ResponseModelGet<ConsultaResponse> response(List<ConsultaResponse> data) { ResponseModelGet<ConsultaResponse> r = new ResponseModelGet<>(); r.setData(data); r.setMensaje(Constant.MENSAJE_CONSULTA_OK); return r; }
  private Integer edad(Date f) { if (f == null) return null; LocalDate b = f.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(); LocalDate n = LocalDate.now(); int e = n.getYear() - b.getYear(); if (n.getDayOfYear() < b.getDayOfYear()) e--; return e; }
  private String normalizeTipoEnfermedad(String v) { return normalize(v); }
  private String normalizeEspecialidad(String v) { return normalize(v); }
  private String normalize(String v) { if (v == null) return null; return Normalizer.normalize(v, Normalizer.Form.NFD).replaceAll("\\p{M}", "").trim().toUpperCase().replace(' ', '_'); }
  private boolean hasText(String v) { return v != null && !v.trim().isEmpty(); }
}
