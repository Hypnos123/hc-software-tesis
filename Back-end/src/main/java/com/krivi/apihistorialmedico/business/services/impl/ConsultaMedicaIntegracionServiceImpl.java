package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.exception.ConsultaMedicaIntegracionException;
import com.krivi.apihistorialmedico.business.services.ConsultaMedicaIntegracionService;
import com.krivi.apihistorialmedico.model.api.*;
import com.krivi.apihistorialmedico.model.entity.Consulta;
import com.krivi.apihistorialmedico.model.entity.Empleado;
import com.krivi.apihistorialmedico.model.entity.Paciente;
import com.krivi.apihistorialmedico.repository.ConsultaRepository;
import com.krivi.apihistorialmedico.repository.HistoriaClinicaRepository;
import com.krivi.apihistorialmedico.repository.PacienteRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

  public ConsultaMedicaIntegracionServiceImpl(ConsultaRepository consultaRepository,
      PacienteRepository pacienteRepository, HistoriaClinicaRepository historiaClinicaRepository) {
    this.consultaRepository = consultaRepository;
    this.pacienteRepository = pacienteRepository;
    this.historiaClinicaRepository = historiaClinicaRepository;
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
      return pacienteRepository.findById(Integer.parseInt(id)).map(List::of).orElse(List.of());
    }
    if (DNI_PATTERN.matcher(valor).matches()) return pacienteRepository.findByNumDocumento(valor).map(List::of).orElse(List.of());
    if (ID_PATTERN.matcher(valor).matches()) return pacienteRepository.findById(Integer.parseInt(valor)).map(List::of).orElse(List.of());
    if (SOLO_DIGITOS_PATTERN.matcher(valor).matches()) throw error("CRITERIO_INVALIDO", "El criterio numérico debe ser un DNI de 8 dígitos o un ID de paciente positivo de hasta 7 dígitos.");
    return pacienteRepository.searchByNombre(valor, LIMITE_CANDIDATOS);
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
