package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.exception.ConsultaMedicaIntegracionException;
import com.krivi.apihistorialmedico.business.exception.ResumenConsultasException;
import com.krivi.apihistorialmedico.business.services.ConsultaMedicaIntegracionService;
import com.krivi.apihistorialmedico.model.api.*;
import com.krivi.apihistorialmedico.model.entity.Consulta;
import com.krivi.apihistorialmedico.model.entity.Empleado;
import com.krivi.apihistorialmedico.model.entity.Paciente;
import com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente;
import com.krivi.apihistorialmedico.repository.ConsultaRepository;
import com.krivi.apihistorialmedico.repository.HistoriaClinicaRepository;
import com.krivi.apihistorialmedico.repository.PacienteRepository;
import com.krivi.apihistorialmedico.repository.UsuarioRepository;
import com.krivi.apihistorialmedico.repository.AntecedentesRepository;
import com.krivi.apihistorialmedico.model.entity.Antecedentes;
import com.krivi.apihistorialmedico.model.projection.ConsultaResumenRecienteProjection;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.Normalizer;
import java.time.Period;
import java.util.Date;

@Service
public class ConsultaMedicaIntegracionServiceImpl implements ConsultaMedicaIntegracionService {
  private static final String PENDIENTE = "PENDIENTE";
  private static final String ATENDIDO = "ATENDIDO";
  private static final ZoneId ZONA_HORARIA_LIMA = ZoneId.of("America/Lima");
  private static final int LIMITE_CANDIDATOS = 5;
  private static final int LIMITE_CONSULTAS_PACIENTE = 5;
  private static final int LIMITE_POR_DEFECTO = 5;
  private static final Pattern DNI_PATTERN = Pattern.compile("\\d{8}");
  private static final Pattern ID_PATTERN = Pattern.compile("[1-9]\\d{0,6}");
  private static final Pattern SOLO_DIGITOS_PATTERN = Pattern.compile("\\d+");
  private static final Pattern PACIENTE_PATTERN = Pattern.compile("(?i)^paciente\\s*:\\s*(.*)$");

  private final ConsultaRepository consultaRepository;
  private final PacienteRepository pacienteRepository;
  private final HistoriaClinicaRepository historiaClinicaRepository;
  private final UsuarioRepository usuarioRepository;
  private final AntecedentesRepository antecedentesRepository;

  public ConsultaMedicaIntegracionServiceImpl(ConsultaRepository consultaRepository,
      PacienteRepository pacienteRepository, HistoriaClinicaRepository historiaClinicaRepository,
      UsuarioRepository usuarioRepository, AntecedentesRepository antecedentesRepository) {
    this.consultaRepository = consultaRepository;
    this.pacienteRepository = pacienteRepository;
    this.historiaClinicaRepository = historiaClinicaRepository;
    this.usuarioRepository = usuarioRepository;
    this.antecedentesRepository = antecedentesRepository;
  }

  @Override
  public ResumenConsultasPacienteResponse obtenerResumenPaciente(Integer idPaciente, Integer idUsuario) {
    if (idPaciente == null || idPaciente < 1) {
      throw errorResumen("ID_PACIENTE_INVALIDO", "El identificador del paciente debe ser un entero positivo.", HttpStatus.BAD_REQUEST);
    }
    if (idUsuario == null || idUsuario < 1) {
      throw errorResumen("USUARIO_REQUERIDO", "Debe indicar el usuario autenticado mediante X-Usuario-Id.", HttpStatus.UNAUTHORIZED);
    }
    var usuario = usuarioRepository.findById(idUsuario)
        .filter(u -> Boolean.TRUE.equals(u.getEstado()))
        .orElseThrow(() -> errorResumen("USUARIO_INEXISTENTE", "El usuario indicado no existe o está inactivo.", HttpStatus.UNAUTHORIZED));
    String rol = normalizarRol(usuario.getTipoUsuario());
    if (!"ADMINISTRADOR".equals(rol) && !"DOCTOR".equals(rol)) {
      throw errorResumen("ROL_SIN_PERMISO", "El usuario no tiene permiso para consultar el resumen clínico.", HttpStatus.FORBIDDEN);
    }

    Paciente paciente = pacienteRepository.findById(idPaciente)
        .orElseThrow(() -> errorResumen("PACIENTE_INEXISTENTE", "El paciente indicado no existe.", HttpStatus.NOT_FOUND));
    if (paciente.getEstadoRegistro() == EstadoRegistroPaciente.ARCHIVADO) {
      throw errorResumen("PACIENTE_ARCHIVADO", "El paciente indicado está archivado; no se combinará con el paciente principal.", HttpStatus.CONFLICT);
    }

    List<Integer> idsHistorias = historiaClinicaRepository.findIdsByPacienteId(idPaciente);
    Object[] cabecera = primeraFila(consultaRepository.resumirAtendidasByPacienteId(idPaciente));
    long totalAtendidas = numero(cabecera, 0);
    List<ConsultaResumenRecienteProjection> recientes = consultaRepository.findRecientesAtendidasByPacienteId(
        idPaciente, PageRequest.of(0, 3));
    ConsultaResumenRecienteProjection ultima = recientes.isEmpty() ? null : recientes.getFirst();
    Antecedentes antecedentes = antecedentesRepository.findByPacienteIdPaciente(idPaciente).stream().findFirst().orElse(null);
    Object[] calidad = primeraFila(consultaRepository.resumirCalidadAtendidasByPacienteId(idPaciente));
    return ResumenConsultasPacienteResponse.builder()
        .paciente(ResumenConsultasPacienteResponse.PacienteResumen.builder()
            .idPaciente(paciente.getIdPaciente()).nombreCompleto(nombreCompleto(paciente))
            .dni(paciente.getNumDocumento()).fechaNacimiento(paciente.getFechaNacimiento())
            .edad(calcularEdad(paciente.getFechaNacimiento())).estado(paciente.getEstadoRegistro().name())
            .cantidadHistoriasClinicas((long) idsHistorias.size()).idsHistoriasClinicas(idsHistorias).build())
        .antecedentes(ResumenConsultasPacienteResponse.AntecedentesResumen.builder()
            .enfermedadesPrevias(antecedentes == null ? null : antecedentes.getEnfermedadesPrevias())
            .cirugiasPrevias(antecedentes == null ? null : antecedentes.getCirugiasPrevias())
            .alergiaMedicamentos(antecedentes == null ? null : antecedentes.getAlergiaMedicamentos()).build())
        .resumenAtencion(ResumenConsultasPacienteResponse.ResumenAtencion.builder()
            .totalConsultasAtendidas(totalAtendidas)
            .fechaPrimeraConsulta(fechaHora(cabecera, 1)).fechaUltimaConsulta(fechaHora(cabecera, 2))
            .ultimaEspecialidad(ultima == null ? null : ultima.getEspecialidad())
            .ultimoDoctor(ultima == null ? null : textoLimpio(ultima.getDoctor()))
            .proximasCitas(consultaRepository.findProximasCitasAtendidasByPacienteId(idPaciente,
                java.sql.Date.valueOf(LocalDate.now(ZONA_HORARIA_LIMA)), PageRequest.of(0, 3))).build())
        .tiposEnfermedad(mapearTipos(consultaRepository.contarTiposAtendidosByPacienteId(idPaciente), totalAtendidas))
        .especialidades(mapearEspecialidades(consultaRepository.contarEspecialidadesAtendidasByPacienteId(idPaciente), totalAtendidas))
        .funcionesVitales(ResumenConsultasPacienteResponse.FuncionesVitalesResumen.builder().build())
        .evaluacionesRecientes(recientes.stream().map(this::mapearEvaluacion).toList())
        .consultasRecientes(recientes.stream().map(this::mapearConsulta).toList())
        .calidadDatos(ResumenConsultasPacienteResponse.CalidadDatosResumen.builder()
            .consultasSinFecha(numero(calidad, 0)).consultasSinTipoEnfermedad(numero(calidad, 1))
            .consultasSinEspecialidad(numero(calidad, 2)).consultasConRelacionInconsistente(numero(calidad, 3))
            .valoresVitalesDescartados(null).build())
        .build();
  }

  private List<ResumenConsultasPacienteResponse.CategoriaResumen> mapearTipos(List<Object[]> filas, long total) {
    return filas.stream().map(fila -> ResumenConsultasPacienteResponse.CategoriaResumen.builder()
        .id(((Number) fila[0]).intValue()).nombre((String) fila[1]).cantidad(((Number) fila[2]).longValue())
        .porcentaje(porcentaje(((Number) fila[2]).longValue(), total)).build()).toList();
  }

  private List<ResumenConsultasPacienteResponse.CategoriaResumen> mapearEspecialidades(List<Object[]> filas, long total) {
    return filas.stream().map(fila -> ResumenConsultasPacienteResponse.CategoriaResumen.builder()
        .nombre((String) fila[0]).cantidad(((Number) fila[1]).longValue())
        .porcentaje(porcentaje(((Number) fila[1]).longValue(), total)).build()).toList();
  }

  private double porcentaje(long cantidad, long total) {
    if (total == 0) return 0D;
    return java.math.BigDecimal.valueOf(cantidad * 100D / total).setScale(1,
        java.math.RoundingMode.HALF_UP).doubleValue();
  }

  private ResumenConsultasPacienteResponse.EvaluacionRecienteResumen mapearEvaluacion(
      ConsultaResumenRecienteProjection consulta) {
    return ResumenConsultasPacienteResponse.EvaluacionRecienteResumen.builder()
        .idConsulta(consulta.getIdConsulta()).diagnostico(consulta.getDiagnostico())
        .examenesRecetados(consulta.getExamenesRecetados()).receta(consulta.getReceta())
        .tratamiento(consulta.getTratamiento()).proximaCita(consulta.getProximaCita()).build();
  }

  private ResumenConsultasPacienteResponse.ConsultaRecienteResumen mapearConsulta(
      ConsultaResumenRecienteProjection consulta) {
    return ResumenConsultasPacienteResponse.ConsultaRecienteResumen.builder()
        .idConsulta(consulta.getIdConsulta()).idHistoriaClinica(consulta.getIdHistoriaClinica())
        .fecha(consulta.getFechaAtencion()).especialidad(consulta.getEspecialidad())
        .doctor(textoLimpio(consulta.getDoctor())).relatoPaciente(consulta.getRelatoPaciente())
        .diagnostico(consulta.getDiagnostico()).tratamiento(consulta.getTratamiento()).build();
  }

  private Object[] primeraFila(List<Object[]> filas) {
    return filas == null || filas.isEmpty() ? new Object[0] : filas.getFirst();
  }

  private long numero(Object[] fila, int indice) {
    return fila.length > indice && fila[indice] instanceof Number numero ? numero.longValue() : 0L;
  }

  private LocalDateTime fechaHora(Object[] fila, int indice) {
    return fila.length > indice && fila[indice] instanceof LocalDateTime fecha ? fecha : null;
  }

  private String textoLimpio(String texto) {
    if (texto == null) return null;
    String limpio = texto.trim().replaceAll("\\s+", " ");
    return limpio.isEmpty() ? null : limpio;
  }

  private Integer calcularEdad(Date fechaNacimiento) {
    if (fechaNacimiento == null) return null;
    LocalDate nacimiento = fechaNacimiento instanceof java.sql.Date fechaSql
        ? fechaSql.toLocalDate()
        : fechaNacimiento.toInstant().atZone(ZONA_HORARIA_LIMA).toLocalDate();
    return Period.between(nacimiento, LocalDate.now(ZONA_HORARIA_LIMA)).getYears();
  }

  private String normalizarRol(String rol) {
    if (rol == null) return "";
    return Normalizer.normalize(rol.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT);
  }

  private ResumenConsultasException errorResumen(String resultado, String mensaje, HttpStatus status) {
    return new ResumenConsultasException(resultado, mensaje, status);
  }

  @Override
  public BusquedaConsultasMedicasResponse buscar(String criterio) {
    List<Paciente> pacientes = resolverPacientes(criterio);
    if (pacientes.isEmpty()) {
      return BusquedaConsultasMedicasResponse.builder().encontrado(false).tipoResultado("sin_resultados")
          .pacientes(List.of()).mensaje("No se encontró ningún paciente con el criterio indicado.").build();
    }
    if (pacientes.size() > 1) {
      return BusquedaConsultasMedicasResponse.builder().encontrado(true).tipoResultado("multiple")
          .pacientes(pacientes.stream().limit(LIMITE_CANDIDATOS).map(this::candidato).toList())
          .mensaje("Se encontraron varios pacientes. Indique el DNI o el ID con el formato paciente:ID.").build();
    }

    Paciente paciente = pacientes.getFirst();
    long total = consultaRepository.countByPacienteIdPaciente(paciente.getIdPaciente());
    long pendientes = consultaRepository.countByPacienteIdPacienteAndEstado(paciente.getIdPaciente(), PENDIENTE);
    long atendidas = consultaRepository.countByPacienteIdPacienteAndEstado(paciente.getIdPaciente(), ATENDIDO);
    List<ConsultaMedicaAdministrativaResponse> consultas = consultaRepository
        .findAdministrativasRecientesByPacienteId(paciente.getIdPaciente(), PageRequest.of(0, LIMITE_CONSULTAS_PACIENTE))
        .stream().map(this::consultaAdministrativa).toList();
    String mensaje = total == 0 ? "El paciente está registrado, pero no tiene consultas médicas registradas." : null;
    return BusquedaConsultasMedicasResponse.builder().encontrado(true).tipoResultado("unico")
        .pacientes(List.of(ConsultaMedicaPacienteResponse.builder().idPaciente(paciente.getIdPaciente())
            .dni(paciente.getNumDocumento()).nombreCompleto(nombreCompleto(paciente))
            .tieneHistoriaClinica(historiaClinicaRepository.existsByPacienteIdPaciente(paciente.getIdPaciente()))
            .totalConsultas(total).consultasPendientes(pendientes).consultasAtendidas(atendidas).consultas(consultas).build()))
        .mensaje(mensaje).build();
  }

  @Override
  public EstadisticasConsultasMedicasResponse obtenerEstadisticas() {
    LocalDate fecha = LocalDate.now(ZONA_HORARIA_LIMA);
    LocalDateTime inicio = fecha.atStartOfDay();
    LocalDateTime fin = inicio.plusDays(1);
    return EstadisticasConsultasMedicasResponse.builder().fecha(fecha).totalConsultas(consultaRepository.count())
        .creadasHoy(consultaRepository.countByFechaCreacionGreaterThanEqualAndFechaCreacionLessThan(inicio, fin))
        .atendidasHoy(consultaRepository.countByFechaAtencionGreaterThanEqualAndFechaAtencionLessThan(inicio, fin))
        .totalPendientes(consultaRepository.countByEstado(PENDIENTE)).totalAtendidas(consultaRepository.countByEstado(ATENDIDO)).build();
  }

  @Override
  public ListadoConsultasMedicasResponse obtenerPendientes() {
    List<ConsultaMedicaAdministrativaResponse> consultas = consultaRepository.findPendientesAdministrativas()
        .stream().map(this::consultaAdministrativa).toList();
    return ListadoConsultasMedicasResponse.builder().cantidad(consultas.size()).consultas(consultas).build();
  }

  @Override
  public ListadoConsultasMedicasResponse obtenerUltimas(Integer limite) {
    int limiteValidado = validarLimite(limite);
    List<ConsultaMedicaAdministrativaResponse> consultas = consultaRepository.findUltimasAdministrativas(PageRequest.of(0, limiteValidado))
        .stream().map(this::consultaAdministrativa).toList();
    return ListadoConsultasMedicasResponse.builder().cantidad(consultas.size()).consultas(consultas).build();
  }

  private List<Paciente> resolverPacientes(String criterio) {
    if (criterio == null || criterio.trim().isEmpty()) throw error("CRITERIO_VACIO", "El criterio de búsqueda es obligatorio.");
    String valor = criterio.trim();
    Matcher paciente = PACIENTE_PATTERN.matcher(valor);
    if (paciente.matches()) {
      String id = paciente.group(1).trim();
      if (!ID_PATTERN.matcher(id).matches()) throw error("CRITERIO_INVALIDO", "El ID de paciente debe ser un entero positivo de hasta 7 dígitos.");
      return pacienteRepository.findByIdPacienteAndEstadoRegistro(Integer.parseInt(id), EstadoRegistroPaciente.ACTIVO).map(List::of).orElse(List.of());
    }
    if (DNI_PATTERN.matcher(valor).matches()) return pacienteRepository.findByNumDocumentoAndEstadoRegistroOrderByIdPacienteAsc(valor, EstadoRegistroPaciente.ACTIVO);
    if (ID_PATTERN.matcher(valor).matches()) return pacienteRepository.findByIdPacienteAndEstadoRegistro(Integer.parseInt(valor), EstadoRegistroPaciente.ACTIVO).map(List::of).orElse(List.of());
    if (SOLO_DIGITOS_PATTERN.matcher(valor).matches()) throw error("CRITERIO_INVALIDO", "El criterio numérico debe ser un DNI de 8 dígitos o un ID de paciente positivo de hasta 7 dígitos.");
    return buscarPorNombre(valor);
  }

  private List<Paciente> buscarPorNombre(String criterio) {
    List<String> terminos = List.of(criterio.trim().toLowerCase(Locale.ROOT).split("\\s+"));
    return pacienteRepository.searchByNombreToken(terminos.getFirst()).stream()
        .filter(paciente -> {
          String nombre = nombreCompleto(paciente).toLowerCase(Locale.ROOT);
          return terminos.stream().allMatch(nombre::contains);
        })
        .limit(LIMITE_CANDIDATOS)
        .toList();
  }

  private int validarLimite(Integer limite) {
    if (limite == null) return LIMITE_POR_DEFECTO;
    if (limite < 1 || limite > 10) throw error("LIMITE_INVALIDO", "El límite debe estar entre 1 y 10.");
    return limite;
  }

  private ConsultaMedicaPacienteResponse candidato(Paciente paciente) {
    return ConsultaMedicaPacienteResponse.builder().idPaciente(paciente.getIdPaciente()).dni(paciente.getNumDocumento())
        .nombreCompleto(nombreCompleto(paciente)).build();
  }

  private ConsultaMedicaAdministrativaResponse consultaAdministrativa(Consulta consulta) {
    Paciente paciente = consulta.getPaciente();
    Empleado medico = consulta.getDoctorResponsable();
    return ConsultaMedicaAdministrativaResponse.builder().idConsulta(consulta.getIdConsulta())
        .idPaciente(paciente == null ? null : paciente.getIdPaciente())
        .idHistoriaClinica(consulta.getHistoriaClinica() == null ? null : consulta.getHistoriaClinica().getIdHistoriaClinica())
        .dni(paciente == null ? null : paciente.getNumDocumento()).nombreCompleto(paciente == null ? null : nombreCompleto(paciente))
        .idEmpleado(medico == null ? null : medico.getIdEmpleado()).nombreMedico(medico == null ? null : nombreCompleto(medico.getNombres(), medico.getApellidos()))
        .especialidad(consulta.getEspecialidadRequerida()).fechaCreacion(consulta.getFechaCreacion())
        .fechaAtencion(consulta.getFechaAtencion()).estado(consulta.getEstado()).build();
  }

  private String nombreCompleto(Paciente paciente) { return nombreCompleto(paciente.getNombres(), paciente.getApellidos()); }
  private String nombreCompleto(String nombres, String apellidos) { return String.join(" ", Optional.ofNullable(nombres).orElse(""), Optional.ofNullable(apellidos).orElse("")).trim(); }
  private ConsultaMedicaIntegracionException error(String codigo, String mensaje) { return new ConsultaMedicaIntegracionException(codigo, mensaje, HttpStatus.BAD_REQUEST); }
}
