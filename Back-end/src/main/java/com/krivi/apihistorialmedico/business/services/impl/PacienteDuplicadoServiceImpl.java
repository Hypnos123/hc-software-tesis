package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.exception.PacienteDuplicadoException;
import com.krivi.apihistorialmedico.business.services.PacienteDuplicadoService;
import com.krivi.apihistorialmedico.model.api.PacienteDuplicadoComparacionResponse;
import com.krivi.apihistorialmedico.model.api.PacienteDuplicadoDetalleResponse;
import com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente;
import com.krivi.apihistorialmedico.model.entity.Paciente;
import com.krivi.apihistorialmedico.repository.AntecedentesRepository;
import com.krivi.apihistorialmedico.repository.ConsultaRepository;
import com.krivi.apihistorialmedico.repository.HistoriaClinicaRepository;
import com.krivi.apihistorialmedico.repository.PacienteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class PacienteDuplicadoServiceImpl implements PacienteDuplicadoService {
  private static final Pattern DNI_PATTERN = Pattern.compile("\\d{8}");
  private static final String TIPO_DOCUMENTO_DNI = "DNI";
  private static final String ADVERTENCIA_REVISION =
      "Dos o más registros contienen información clínica. Deben revisarse antes de archivar alguno.";

  private final PacienteRepository pacienteRepository;
  private final HistoriaClinicaRepository historiaClinicaRepository;
  private final ConsultaRepository consultaRepository;
  private final AntecedentesRepository antecedentesRepository;

  public PacienteDuplicadoServiceImpl(
      PacienteRepository pacienteRepository,
      HistoriaClinicaRepository historiaClinicaRepository,
      ConsultaRepository consultaRepository,
      AntecedentesRepository antecedentesRepository
  ) {
    this.pacienteRepository = pacienteRepository;
    this.historiaClinicaRepository = historiaClinicaRepository;
    this.consultaRepository = consultaRepository;
    this.antecedentesRepository = antecedentesRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public PacienteDuplicadoComparacionResponse compararPorDni(String dni) {
    validarDni(dni);
    List<Paciente> pacientes = pacienteRepository
        .findByNumDocumentoAndEstadoRegistroOrderByIdPacienteAsc(dni, EstadoRegistroPaciente.ACTIVO);
    if (pacientes.isEmpty()) return respuestaSinPacientes(dni);

    List<Integer> ids = pacientes.stream().map(Paciente::getIdPaciente).toList();
    Map<Integer, ResumenActividad> historias = resumenActividad(historiaClinicaRepository.resumirPorPacientes(ids));
    Map<Integer, ResumenActividad> consultas = resumenActividad(consultaRepository.resumirPorPacientes(ids));
    Map<Integer, ResumenAntecedentes> antecedentes = resumenAntecedentes(antecedentesRepository.resumirPorPacientes(ids));

    List<PacienteDuplicadoDetalleResponse> detalles = pacientes.stream()
        .map(paciente -> detalle(paciente, historias.get(paciente.getIdPaciente()),
            consultas.get(paciente.getIdPaciente()), antecedentes.get(paciente.getIdPaciente())))
        .toList();

    if (detalles.size() == 1) return respuestaSinDuplicados(dni, detalles);

    PacienteDuplicadoDetalleResponse recomendado = detalles.stream().sorted(COMPARADOR_RECOMENDACION).findFirst().orElseThrow();
    long relevantes = detalles.stream().filter(PacienteDuplicadoDetalleResponse::isTieneInformacionClinicaRelevante).count();
    boolean requiereRevision = relevantes > 1;
    return PacienteDuplicadoComparacionResponse.builder()
        .dni(dni)
        .cantidadPacientesActivos(detalles.size())
        .esDuplicado(true)
        .pacientes(detalles)
        .idPacienteRecomendado(recomendado.getIdPaciente())
        .razonesRecomendacion(razones(recomendado, detalles))
        .permitirArchivadoSimple(!requiereRevision)
        .requiereRevision(requiereRevision)
        .resultado(requiereRevision ? "REQUIERE_REVISION_O_FUSION" : "DUPLICADOS_ENCONTRADOS")
        .mensaje("Se encontraron " + detalles.size() + " pacientes activos con el mismo DNI.")
        .advertencia(requiereRevision ? ADVERTENCIA_REVISION : null)
        .build();
  }

  private void validarDni(String dni) {
    if (dni == null || !DNI_PATTERN.matcher(dni).matches()) {
      throw new PacienteDuplicadoException("DNI_INVALIDO",
          "El DNI es obligatorio y debe contener exactamente ocho dígitos, sin letras ni espacios.",
          HttpStatus.BAD_REQUEST);
    }
  }

  private PacienteDuplicadoDetalleResponse detalle(
      Paciente paciente,
      ResumenActividad historia,
      ResumenActividad consulta,
      ResumenAntecedentes antecedente
  ) {
    ResumenActividad resumenHistoria = historia == null ? ResumenActividad.VACIO : historia;
    ResumenActividad resumenConsulta = consulta == null ? ResumenActividad.VACIO : consulta;
    ResumenAntecedentes resumenAntecedente = antecedente == null ? ResumenAntecedentes.VACIO : antecedente;
    boolean relevante = resumenHistoria.cantidad() > 0 || resumenConsulta.cantidad() > 0
        || resumenAntecedente.gruposCompletos() > 0;
    return PacienteDuplicadoDetalleResponse.builder()
        .idPaciente(paciente.getIdPaciente())
        .nombres(paciente.getNombres())
        .apellidos(paciente.getApellidos())
        .nombreCompleto(nombreCompleto(paciente))
        .tipoDocumento(TIPO_DOCUMENTO_DNI)
        .dni(paciente.getNumDocumento())
        .fechaCreacion(paciente.getFechaCreacion())
        .ultimaActualizacion(paciente.getUltimaActualizacion())
        .estadoRegistro(paciente.getEstadoRegistro())
        .cantidadHistoriasClinicas(resumenHistoria.cantidad())
        .cantidadConsultas(resumenConsulta.cantidad())
        .cantidadAntecedentes(resumenAntecedente.cantidad())
        .cantidadCamposPersonalesCompletos(contarCamposPersonales(paciente))
        .cantidadGruposClinicosCompletos(resumenAntecedente.gruposCompletos())
        .ultimaActividadClinica(fechaMasReciente(resumenHistoria.ultimaActividad(), resumenConsulta.ultimaActividad()))
        .tieneInformacionClinicaRelevante(relevante)
        .build();
  }

  private Map<Integer, ResumenActividad> resumenActividad(Collection<Object[]> filas) {
    Map<Integer, ResumenActividad> resumen = new HashMap<>();
    if (filas == null) return resumen;
    for (Object[] fila : filas) {
      resumen.put(numero(fila[0]).intValue(), new ResumenActividad(numero(fila[1]).longValue(), (LocalDateTime) fila[2]));
    }
    return resumen;
  }

  private Map<Integer, ResumenAntecedentes> resumenAntecedentes(Collection<Object[]> filas) {
    Map<Integer, ResumenAntecedentes> resumen = new HashMap<>();
    if (filas == null) return resumen;
    for (Object[] fila : filas) {
      resumen.put(numero(fila[0]).intValue(), new ResumenAntecedentes(numero(fila[1]).longValue(), numero(fila[2]).longValue()));
    }
    return resumen;
  }

  private Number numero(Object valor) {
    return valor instanceof Number numero ? numero : 0;
  }

  private int contarCamposPersonales(Paciente paciente) {
    return (int) Stream.of(
            paciente.getNombres(), paciente.getApellidos(), paciente.getFechaIngreso(), paciente.getFechaNacimiento(),
            paciente.getEstadoCivil(), paciente.getNumDocumento(), paciente.getSexo(), paciente.getDireccion(),
            paciente.getDistrito(), paciente.getTraidoPor())
        .filter(this::estaInformado).count();
  }

  private boolean estaInformado(Object valor) {
    return valor != null && (!(valor instanceof String texto) || !texto.trim().isEmpty());
  }

  private String nombreCompleto(Paciente paciente) {
    return String.join(" ", Objects.toString(paciente.getNombres(), ""), Objects.toString(paciente.getApellidos(), ""))
        .replaceAll("\\s+", " ").trim();
  }

  private LocalDateTime fechaMasReciente(LocalDateTime primera, LocalDateTime segunda) {
    if (primera == null) return segunda;
    if (segunda == null) return primera;
    return primera.isAfter(segunda) ? primera : segunda;
  }

  private List<String> razones(PacienteDuplicadoDetalleResponse recomendado,
                               List<PacienteDuplicadoDetalleResponse> candidatos) {
    List<String> razones = new ArrayList<>();
    if (recomendado.getCantidadConsultas() > 0) razones.add("Tiene " + recomendado.getCantidadConsultas() + " consultas registradas.");
    if (recomendado.getCantidadHistoriasClinicas() > 0) razones.add("Tiene " + recomendado.getCantidadHistoriasClinicas() + " historias clínicas.");
    if (recomendado.getCantidadGruposClinicosCompletos() > 0) razones.add("Posee " + recomendado.getCantidadGruposClinicosCompletos() + " grupos de antecedentes informados.");
    razones.add(razonDeterminante(recomendado, candidatos));
    return razones.stream().distinct().toList();
  }

  private String razonDeterminante(PacienteDuplicadoDetalleResponse recomendado,
                                    List<PacienteDuplicadoDetalleResponse> candidatos) {
    List<PacienteDuplicadoDetalleResponse> otros = candidatos.stream()
        .filter(candidato -> !candidato.getIdPaciente().equals(recomendado.getIdPaciente())).toList();
    if (otros.stream().allMatch(item -> recomendado.getCantidadConsultas() > item.getCantidadConsultas()))
      return "Es el registro con mayor cantidad de consultas.";
    if (otros.stream().allMatch(item -> recomendado.getCantidadHistoriasClinicas() > item.getCantidadHistoriasClinicas()))
      return "Es el registro con mayor cantidad de historias clínicas.";
    if (otros.stream().allMatch(item -> recomendado.getCantidadGruposClinicosCompletos() > item.getCantidadGruposClinicosCompletos()))
      return "Es el registro con mayor información de antecedentes.";
    if (otros.stream().allMatch(item -> recomendado.getCantidadAntecedentes() > item.getCantidadAntecedentes()))
      return "Es el registro con mayor cantidad de antecedentes registrados.";
    if (otros.stream().allMatch(item -> recomendado.getCantidadCamposPersonalesCompletos() > item.getCantidadCamposPersonalesCompletos()))
      return "Es el registro con mayor completitud de datos personales.";
    if (recomendado.getUltimaActividadClinica() != null && otros.stream().allMatch(item ->
        item.getUltimaActividadClinica() == null || recomendado.getUltimaActividadClinica().isAfter(item.getUltimaActividadClinica())))
      return "Presenta la actividad clínica más reciente.";
    if (recomendado.getFechaCreacion() != null && otros.stream().allMatch(item ->
        item.getFechaCreacion() == null || recomendado.getFechaCreacion().isBefore(item.getFechaCreacion())))
      return "Se recomienda por ser el registro más antiguo entre los candidatos equivalentes.";
    return "Se recomienda por su menor ID como desempate técnico entre registros equivalentes.";
  }

  private PacienteDuplicadoComparacionResponse respuestaSinPacientes(String dni) {
    return PacienteDuplicadoComparacionResponse.builder().dni(dni).cantidadPacientesActivos(0).esDuplicado(false)
        .pacientes(List.of()).razonesRecomendacion(List.of()).permitirArchivadoSimple(false).requiereRevision(false)
        .resultado("SIN_PACIENTES").mensaje("No existen pacientes activos registrados con ese DNI.").build();
  }

  private PacienteDuplicadoComparacionResponse respuestaSinDuplicados(
      String dni, List<PacienteDuplicadoDetalleResponse> detalles) {
    return PacienteDuplicadoComparacionResponse.builder().dni(dni).cantidadPacientesActivos(1).esDuplicado(false)
        .pacientes(detalles).razonesRecomendacion(List.of()).permitirArchivadoSimple(false).requiereRevision(false)
        .resultado("SIN_DUPLICADOS").mensaje("El DNI corresponde a un único paciente activo y no presenta duplicados.").build();
  }

  private static final Comparator<PacienteDuplicadoDetalleResponse> COMPARADOR_RECOMENDACION =
      Comparator.comparingLong(PacienteDuplicadoDetalleResponse::getCantidadConsultas).reversed()
          .thenComparing(Comparator.comparingLong(PacienteDuplicadoDetalleResponse::getCantidadHistoriasClinicas).reversed())
          .thenComparing(Comparator.comparingLong(PacienteDuplicadoDetalleResponse::getCantidadGruposClinicosCompletos).reversed())
          .thenComparing(Comparator.comparingLong(PacienteDuplicadoDetalleResponse::getCantidadAntecedentes).reversed())
          .thenComparing(Comparator.comparingInt(PacienteDuplicadoDetalleResponse::getCantidadCamposPersonalesCompletos).reversed())
          .thenComparing(PacienteDuplicadoDetalleResponse::getUltimaActividadClinica,
              Comparator.nullsLast(Comparator.reverseOrder()))
          .thenComparing(PacienteDuplicadoDetalleResponse::getFechaCreacion,
              Comparator.nullsLast(Comparator.naturalOrder()))
          .thenComparing(PacienteDuplicadoDetalleResponse::getIdPaciente);

  private record ResumenActividad(long cantidad, LocalDateTime ultimaActividad) {
    private static final ResumenActividad VACIO = new ResumenActividad(0, null);
  }

  private record ResumenAntecedentes(long cantidad, long gruposCompletos) {
    private static final ResumenAntecedentes VACIO = new ResumenAntecedentes(0, 0);
  }
}
