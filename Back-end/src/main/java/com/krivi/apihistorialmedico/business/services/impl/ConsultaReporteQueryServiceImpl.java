package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.exception.ConsultaReporteException;
import com.krivi.apihistorialmedico.business.services.ConsultaReporteQueryService;
import com.krivi.apihistorialmedico.model.api.*;
import com.krivi.apihistorialmedico.model.entity.Consulta;
import com.krivi.apihistorialmedico.model.entity.Empleado;
import com.krivi.apihistorialmedico.model.entity.Paciente;
import com.krivi.apihistorialmedico.repository.ConsultaRepository;
import com.krivi.apihistorialmedico.repository.PacienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class ConsultaReporteQueryServiceImpl implements ConsultaReporteQueryService {
  static final ZoneId ZONA_HORARIA_LIMA = ZoneId.of("America/Lima");
  private static final String ATENDIDO = "ATENDIDO";

  private static final Comparator<Consulta> ORDEN_CRONOLOGICO = Comparator
      .comparing(ConsultaReporteQueryServiceImpl::resolverFechaEfectiva,
          Comparator.nullsLast(Comparator.naturalOrder()))
      .thenComparing(Consulta::getIdConsulta, Comparator.nullsLast(Comparator.naturalOrder()));
  private static final Comparator<Consulta> ORDEN_ULTIMA = Comparator
      .comparing(ConsultaReporteQueryServiceImpl::resolverFechaEfectiva,
          Comparator.nullsFirst(Comparator.naturalOrder()))
      .thenComparing(Consulta::getIdConsulta, Comparator.nullsFirst(Comparator.naturalOrder()));

  private final ConsultaRepository consultaRepository;
  private final PacienteRepository pacienteRepository;

  public ConsultaReporteQueryServiceImpl(ConsultaRepository consultaRepository,
      PacienteRepository pacienteRepository) {
    this.consultaRepository = consultaRepository;
    this.pacienteRepository = pacienteRepository;
  }

  @Override
  public ReporteMedicoDocumento seleccionarConsultaIndividual(Integer idConsulta) {
    if (idConsulta == null || idConsulta < 1) {
      throw error("ID_CONSULTA_INVALIDO", "El identificador de la consulta debe ser un entero positivo.");
    }
    Consulta consulta = consultaRepository.findByIdForReporte(idConsulta)
        .orElseThrow(() -> error("CONSULTA_INEXISTENTE", "La consulta indicada no existe."));
    if (!esAtendida(consulta)) {
      throw error("CONSULTA_NO_ATENDIDA", "Solo se pueden generar reportes de consultas atendidas.");
    }
    return construirDocumento(consulta.getPaciente(), ReporteConsultaAlcance.ULTIMA, null, null, null,
        1, List.of(consulta));
  }

  @Override
  public ReporteMedicoDocumento seleccionarConsultasPaciente(Integer idPaciente,
      ReporteConsultaFiltroRequest filtro) {
    if (idPaciente == null || idPaciente < 1) {
      throw error("ID_PACIENTE_INVALIDO", "El identificador del paciente debe ser un entero positivo.");
    }
    if (filtro == null || filtro.getAlcance() == null) {
      throw error("ALCANCE_REQUERIDO", "Debe indicar el alcance del reporte.");
    }
    validarFiltro(filtro);
    Paciente paciente = pacienteRepository.findById(idPaciente)
        .orElseThrow(() -> error("PACIENTE_INEXISTENTE", "El paciente indicado no existe."));
    List<Consulta> todas = consultaRepository.findByPacienteIdForReporte(idPaciente);

    if (filtro.getAlcance() == ReporteConsultaAlcance.ULTIMA) {
      List<Consulta> ultima = todas.stream().filter(this::esAtendida).max(ORDEN_ULTIMA)
          .map(List::of).orElseGet(List::of);
      return construirDocumento(paciente, filtro.getAlcance(), null, null, null,
          ultima.isEmpty() ? 0 : 1, ultima);
    }

    List<Consulta> encontradas = todas.stream().filter(consulta -> coincidePeriodo(consulta, filtro)).toList();
    List<Consulta> incluidas = encontradas.stream().filter(this::esAtendida).sorted(ORDEN_CRONOLOGICO).toList();
    return construirDocumento(paciente, filtro.getAlcance(), filtro.getFecha(), filtro.getFechaDesde(),
        filtro.getFechaHasta(), encontradas.size(), incluidas);
  }

  private ReporteMedicoDocumento construirDocumento(Paciente paciente, ReporteConsultaAlcance alcance,
      LocalDate fecha, LocalDate fechaDesde, LocalDate fechaHasta, long total, List<Consulta> consultas) {
    List<ReporteMedicoConsulta> detalles = consultas.stream().sorted(ORDEN_CRONOLOGICO)
        .map(this::mapearConsulta).toList();
    List<Integer> idsHistorias = detalles.stream().map(ReporteMedicoConsulta::getIdHistoriaClinica)
        .filter(Objects::nonNull).distinct().sorted().toList();
    long incluidas = detalles.size();
    return ReporteMedicoDocumento.builder()
        .alcance(alcance).fecha(fecha).fechaDesde(fechaDesde).fechaHasta(fechaHasta)
        .totalConsultasEncontradas(total).consultasAtendidasIncluidas(incluidas)
        .consultasNoAtendidasExcluidas(total - incluidas)
        .paciente(mapearPaciente(paciente)).idsHistoriasClinicasIncluidas(idsHistorias)
        .consultas(detalles).build();
  }

  private ReporteMedicoPaciente mapearPaciente(Paciente paciente) {
    return ReporteMedicoPaciente.builder().idPaciente(paciente.getIdPaciente())
        .nombreCompleto(unirNombre(paciente.getNombres(), paciente.getApellidos()))
        .dni(limpiar(paciente.getNumDocumento())).fechaNacimiento(aLocalDate(paciente.getFechaNacimiento())).build();
  }

  private ReporteMedicoConsulta mapearConsulta(Consulta consulta) {
    LocalDateTime fechaEfectiva = resolverFechaEfectiva(consulta);
    Empleado doctor = consulta.getDoctorResponsable();
    return ReporteMedicoConsulta.builder().idConsulta(consulta.getIdConsulta())
        .idHistoriaClinica(consulta.getHistoriaClinica() == null ? null
            : consulta.getHistoriaClinica().getIdHistoriaClinica())
        .fechaEfectiva(fechaEfectiva).origenFechaEfectiva(resolverOrigenFechaEfectiva(consulta))
        .edadPaciente(calcularEdad(consulta.getPaciente().getFechaNacimiento(), fechaEfectiva))
        .especialidad(limpiar(consulta.getEspecialidadRequerida()))
        .medicoResponsable(doctor == null ? null : unirNombre(doctor.getNombres(), doctor.getApellidos()))
        .diagnostico(consulta.getDiagnostico()).examenesRecetados(consulta.getExamenesRecetados())
        .receta(consulta.getReceta()).tratamiento(consulta.getTratamiento())
        .proximaCita(aLocalDate(consulta.getProximaCita())).build();
  }

  private void validarFiltro(ReporteConsultaFiltroRequest filtro) {
    if (filtro.getAlcance() == ReporteConsultaAlcance.FECHA && filtro.getFecha() == null) {
      throw error("FECHA_REQUERIDA", "Debe indicar la fecha del reporte.");
    }
    if (filtro.getAlcance() == ReporteConsultaAlcance.RANGO_FECHAS) {
      if (filtro.getFechaDesde() == null || filtro.getFechaHasta() == null) {
        throw error("RANGO_REQUERIDO", "Debe indicar la fecha inicial y final del reporte.");
      }
      if (filtro.getFechaDesde().isAfter(filtro.getFechaHasta())) {
        throw error("RANGO_INVALIDO", "La fecha inicial no puede ser posterior a la fecha final.");
      }
    }
  }

  private boolean coincidePeriodo(Consulta consulta, ReporteConsultaFiltroRequest filtro) {
    if (filtro.getAlcance() == ReporteConsultaAlcance.TODAS) return true;
    LocalDateTime efectiva = resolverFechaEfectiva(consulta);
    if (efectiva == null) return false;
    LocalDate dia = efectiva.toLocalDate();
    if (filtro.getAlcance() == ReporteConsultaAlcance.FECHA) return dia.equals(filtro.getFecha());
    return !dia.isBefore(filtro.getFechaDesde()) && !dia.isAfter(filtro.getFechaHasta());
  }

  private boolean esAtendida(Consulta consulta) {
    return consulta.getEstado() != null
        && ATENDIDO.equals(consulta.getEstado().trim().toUpperCase(Locale.ROOT));
  }

  static LocalDateTime resolverFechaEfectiva(Consulta consulta) {
    if (consulta.getFechaAtencion() != null) return consulta.getFechaAtencion();
    LocalDate fechaConsulta = aLocalDate(consulta.getFechaConsulta());
    if (fechaConsulta != null) return fechaConsulta.atStartOfDay();
    return consulta.getFechaCreacion();
  }

  private OrigenFechaConsultaReporte resolverOrigenFechaEfectiva(Consulta consulta) {
    if (consulta.getFechaAtencion() != null) return OrigenFechaConsultaReporte.FECHA_ATENCION;
    if (consulta.getFechaConsulta() != null) return OrigenFechaConsultaReporte.FECHA_CONSULTA;
    return consulta.getFechaCreacion() == null ? null : OrigenFechaConsultaReporte.FECHA_CREACION;
  }

  private static Integer calcularEdad(Date nacimiento, LocalDateTime fechaEfectiva) {
    LocalDate fechaNacimiento = aLocalDate(nacimiento);
    if (fechaNacimiento == null || fechaEfectiva == null || fechaNacimiento.isAfter(fechaEfectiva.toLocalDate())) {
      return null;
    }
    return Period.between(fechaNacimiento, fechaEfectiva.toLocalDate()).getYears();
  }

  private static LocalDate aLocalDate(Date fecha) {
    if (fecha == null) return null;
    if (fecha instanceof java.sql.Date fechaSql) return fechaSql.toLocalDate();
    return fecha.toInstant().atZone(ZONA_HORARIA_LIMA).toLocalDate();
  }

  private String unirNombre(String nombres, String apellidos) {
    return Stream.of(limpiar(nombres), limpiar(apellidos)).filter(Objects::nonNull)
        .reduce((primero, segundo) -> primero + " " + segundo).orElse(null);
  }

  private String limpiar(String valor) {
    if (valor == null) return null;
    String limpio = valor.trim().replaceAll("\\s+", " ");
    return limpio.isEmpty() ? null : limpio;
  }

  private ConsultaReporteException error(String codigo, String mensaje) {
    return new ConsultaReporteException(codigo, mensaje);
  }
}
