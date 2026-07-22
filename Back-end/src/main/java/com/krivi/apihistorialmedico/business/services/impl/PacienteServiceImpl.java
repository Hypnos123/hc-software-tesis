package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.services.PacienteService;
import com.krivi.apihistorialmedico.business.exception.BusquedaPacienteException;
import com.krivi.apihistorialmedico.model.api.*;
import com.krivi.apihistorialmedico.model.entity.Antecedentes;
import com.krivi.apihistorialmedico.model.entity.Paciente;
import com.krivi.apihistorialmedico.repository.PacienteRepository;
import com.krivi.apihistorialmedico.repository.HistoriaClinicaRepository;
import com.krivi.apihistorialmedico.util.Constant;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Array;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.krivi.apihistorialmedico.util.Constant.*;

@Service
@Slf4j
public class PacienteServiceImpl implements PacienteService {

  @Autowired
  PacienteRepository pacienteRepository;

  @Autowired
  HistoriaClinicaRepository historiaClinicaRepository;

  private static final int LIMITE_BUSQUEDA_INTEGRACION = 10;
  private static final int LIMITE_ULTIMOS_PACIENTES_POR_DEFECTO = 5;
  private static final ZoneId ZONA_HORARIA_LIMA = ZoneId.of("America/Lima");
  private static final Pattern DNI_PATTERN = Pattern.compile("\\d{8}");
  private static final Pattern ID_PATTERN = Pattern.compile("[1-9]\\d{0,6}");
  private static final Pattern SOLO_DIGITOS_PATTERN = Pattern.compile("\\d+");

  @Override
  public ResponseModelGet<PacienteResponse> getAllActive() {
    List<PacienteResponse> pacienteResponseList = new ArrayList<>();
    pacienteRepository.findAll().forEach(paciente -> {

      pacienteResponseList.add(PacienteResponse.builder()
          .idPaciente(paciente.getIdPaciente())
          .nombres(paciente.getNombres())
          .apellidos(paciente.getApellidos())
          .fechaIngreso(paciente.getFechaIngreso())
          .fechaNacimiento(paciente.getFechaNacimiento())
          .edad(calcularEdad(paciente.getFechaNacimiento()))
          .estadoCivil(normalizeEstadoCivil(paciente.getEstadoCivil()))
          .numDocumento(paciente.getNumDocumento())
          .sexo(paciente.getSexo())
          .direccion(paciente.getDireccion())
          .distrito(paciente.getDistrito())
          .traidoPor(paciente.getTraidoPor())
          .build());
    });

    ResponseModelGet<PacienteResponse> responseModelGet = new ResponseModelGet<>();
    responseModelGet.setData(pacienteResponseList);
    responseModelGet.setMensaje(Constant.MENSAJE_CONSULTA_OK);
    return responseModelGet;
  }

  @Override
  public ResponseModelGet<PacienteResponse> findById(int idPaciente) {
    List<PacienteResponse> pacienteResponseList = new ArrayList<>();
    Paciente paciente = pacienteRepository.findById(idPaciente).orElse(null);

      pacienteResponseList.add(PacienteResponse.builder()
          .idPaciente(paciente.getIdPaciente())
          .nombres(paciente.getNombres())
          .apellidos(paciente.getApellidos())
          .fechaIngreso(paciente.getFechaIngreso())
          .fechaNacimiento(paciente.getFechaNacimiento())
          .edad(calcularEdad(paciente.getFechaNacimiento()))
          .estadoCivil(normalizeEstadoCivil(paciente.getEstadoCivil()))
          .numDocumento(paciente.getNumDocumento())
          .sexo(paciente.getSexo())
          .direccion(paciente.getDireccion())
          .distrito(paciente.getDistrito())
          .traidoPor(paciente.getTraidoPor())
          .build());


    ResponseModelGet<PacienteResponse> responseModelGet = new ResponseModelGet<>();
    responseModelGet.setData(pacienteResponseList);
    responseModelGet.setMensaje(Constant.MENSAJE_CONSULTA_OK);
    return responseModelGet;
  }

  @Override
  public ResponseModelGet<PacienteResponse> search(String nombre, String dni, Integer limit) {
    int safeLimit = limit == null || limit < 1 || limit > 25 ? 10 : limit;
    List<PacienteResponse> pacienteResponseList = new ArrayList<>();
    Iterable<Paciente> pacientes;
    if (dni != null && !dni.trim().isEmpty()) {
      pacientes = pacienteRepository.searchByDni(dni.trim(), safeLimit);
    } else if (nombre != null && nombre.trim().length() >= 2) {
      List<Paciente> resultados = pacienteRepository.searchByNombre(nombre.trim(), safeLimit);
      pacientes = resultados.isEmpty() ? searchByApproximateName(nombre.trim(), safeLimit) : resultados;
    } else {
      pacientes = new ArrayList<>();
    }
    pacientes.forEach(paciente -> pacienteResponseList.add(toResponse(paciente)));
    ResponseModelGet<PacienteResponse> responseModelGet = new ResponseModelGet<>();
    responseModelGet.setData(pacienteResponseList);
    responseModelGet.setMensaje(Constant.MENSAJE_CONSULTA_OK);
    return responseModelGet;
  }

  @Override
  public BusquedaPacienteResponse buscarParaIntegracion(String criterio) {
    String criterioNormalizado = validarCriterioBusqueda(criterio);
    List<Paciente> pacientes = buscarPacientes(criterioNormalizado);
    List<PacienteBusquedaItemResponse> resultados = pacientes.stream()
        .map(this::toBusquedaResponse)
        .toList();

    if (resultados.isEmpty()) {
      return BusquedaPacienteResponse.builder()
          .encontrado(false)
          .tipoResultado("sin_resultados")
          .pacientes(resultados)
          .mensaje("No se encontró ningún paciente con el criterio indicado.")
          .build();
    }

    return BusquedaPacienteResponse.builder()
        .encontrado(true)
        .tipoResultado(resultados.size() == 1 ? "unico" : "multiple")
        .pacientes(resultados)
        .build();
  }

  @Override
  public EstadisticasPacientesResponse obtenerEstadisticasParaIntegracion() {
    LocalDateTime inicioHoy = LocalDate.now(ZONA_HORARIA_LIMA).atStartOfDay();
    LocalDateTime inicioManana = inicioHoy.plusDays(1);
    return EstadisticasPacientesResponse.builder()
        .totalPacientes(pacienteRepository.count())
        .registradosHoy(pacienteRepository.countByFechaCreacionGreaterThanEqualAndFechaCreacionLessThan(inicioHoy, inicioManana))
        .build();
  }

  @Override
  public UltimosPacientesResponse obtenerUltimosParaIntegracion(Integer limite) {
    int limiteValidado = validarLimiteUltimos(limite);
    List<PacienteRegistroResponse> pacientes = pacienteRepository.findTop10ByOrderByFechaCreacionDesc().stream()
        .limit(limiteValidado)
        .map(this::toPacienteRegistroResponse)
        .toList();
    return UltimosPacientesResponse.builder().cantidad(pacientes.size()).pacientes(pacientes).build();
  }

  @Override
  public PacientesRegistradosHoyResponse obtenerRegistradosHoyParaIntegracion() {
    LocalDate fechaHoy = LocalDate.now(ZONA_HORARIA_LIMA);
    LocalDateTime inicioHoy = fechaHoy.atStartOfDay();
    List<PacienteRegistroResponse> pacientes = pacienteRepository
        .findByFechaCreacionGreaterThanEqualAndFechaCreacionLessThanOrderByFechaCreacionDesc(inicioHoy, inicioHoy.plusDays(1))
        .stream()
        .map(this::toPacienteRegistroResponse)
        .toList();
    return PacientesRegistradosHoyResponse.builder()
        .fecha(fechaHoy)
        .cantidad(pacientes.size())
        .pacientes(pacientes)
        .build();
  }

  @Override
  public DuplicadosPacientesResponse obtenerDuplicadosParaIntegracion() {
    List<GrupoDuplicadoDniResponse> duplicados = pacienteRepository.findDnisDuplicados().stream()
        .map(this::toGrupoDuplicadoDniResponse)
        .toList();
    return DuplicadosPacientesResponse.builder()
        .hayDuplicados(!duplicados.isEmpty())
        .totalGrupos(duplicados.size())
        .duplicados(duplicados)
        .build();
  }

  private int validarLimiteUltimos(Integer limite) {
    if (limite == null) return LIMITE_ULTIMOS_PACIENTES_POR_DEFECTO;
    if (limite < 1 || limite > LIMITE_BUSQUEDA_INTEGRACION) {
      throw new BusquedaPacienteException("LIMITE_INVALIDO", "El límite debe estar entre 1 y 10.", HttpStatus.BAD_REQUEST);
    }
    return limite;
  }

  private PacienteRegistroResponse toPacienteRegistroResponse(Paciente paciente) {
    return PacienteRegistroResponse.builder()
        .idPaciente(paciente.getIdPaciente())
        .dni(paciente.getNumDocumento())
        .nombreCompleto(nombreCompleto(paciente))
        .fechaCreacion(paciente.getFechaCreacion())
        .build();
  }

  private GrupoDuplicadoDniResponse toGrupoDuplicadoDniResponse(String dni) {
    List<PacienteDuplicadoItemResponse> pacientes = pacienteRepository.findByNumDocumentoOrderByIdPacienteAsc(dni).stream()
        .map(paciente -> PacienteDuplicadoItemResponse.builder()
            .idPaciente(paciente.getIdPaciente())
            .dni(paciente.getNumDocumento())
            .nombreCompleto(nombreCompleto(paciente))
            .build())
        .toList();
    return GrupoDuplicadoDniResponse.builder()
        .tipo("dni")
        .valorCoincidente(dni)
        .cantidad(pacientes.size())
        .pacientes(pacientes)
        .build();
  }

  private String validarCriterioBusqueda(String criterio) {
    if (criterio == null || criterio.trim().isEmpty()) {
      throw new BusquedaPacienteException("CRITERIO_VACIO", "El criterio de búsqueda es obligatorio.", HttpStatus.BAD_REQUEST);
    }

    String valor = criterio.trim();
    if (SOLO_DIGITOS_PATTERN.matcher(valor).matches()
        && !DNI_PATTERN.matcher(valor).matches()
        && !ID_PATTERN.matcher(valor).matches()) {
      throw new BusquedaPacienteException("CRITERIO_INVALIDO", "El criterio numérico debe ser un DNI de 8 dígitos o un ID de paciente positivo de menos de 8 dígitos.", HttpStatus.BAD_REQUEST);
    }
    return valor;
  }

  private List<Paciente> buscarPacientes(String criterio) {
    if (DNI_PATTERN.matcher(criterio).matches()) {
      return pacienteRepository.findByNumDocumento(criterio).map(List::of).orElse(List.of());
    }
    if (ID_PATTERN.matcher(criterio).matches()) {
      return pacienteRepository.findById(Integer.parseInt(criterio)).map(List::of).orElse(List.of());
    }

    List<Paciente> resultados = pacienteRepository.searchByNombre(criterio, LIMITE_BUSQUEDA_INTEGRACION);
    return resultados.isEmpty() ? searchByApproximateName(criterio, LIMITE_BUSQUEDA_INTEGRACION) : resultados;
  }

  private PacienteBusquedaItemResponse toBusquedaResponse(Paciente paciente) {
    return PacienteBusquedaItemResponse.builder()
        .idPaciente(paciente.getIdPaciente())
        .dni(paciente.getNumDocumento())
        .nombreCompleto(nombreCompleto(paciente))
        .tieneHistoriaClinica(historiaClinicaRepository.existsByPacienteIdPaciente(paciente.getIdPaciente()))
        .build();
  }

  private String nombreCompleto(Paciente paciente) {
    return String.join(" ",
        Optional.ofNullable(paciente.getNombres()).orElse(""),
        Optional.ofNullable(paciente.getApellidos()).orElse("")).trim();
  }


  private List<Paciente> searchByApproximateName(String nombre, int limit) {
    String[] tokens = normalize(nombre).split(" ");
    List<Paciente> resultados = new ArrayList<>();
    pacienteRepository.findAll().forEach(paciente -> {
      String nombrePaciente = normalize((paciente.getNombres() == null ? "" : paciente.getNombres()) + " " + (paciente.getApellidos() == null ? "" : paciente.getApellidos()));
      long coincidencias = java.util.Arrays.stream(tokens)
          .filter(token -> token.length() >= 2 && nombrePaciente.contains(token))
          .count();
      if (coincidencias > 0) {
        resultados.add(paciente);
      }
    });
    resultados.sort(Comparator.comparingInt((Paciente paciente) -> approximateScore(paciente, tokens)).reversed());
    return resultados.stream().limit(limit).toList();
  }

  private int approximateScore(Paciente paciente, String[] tokens) {
    String nombrePaciente = normalize((paciente.getNombres() == null ? "" : paciente.getNombres()) + " " + (paciente.getApellidos() == null ? "" : paciente.getApellidos()));
    return (int) java.util.Arrays.stream(tokens)
        .filter(token -> token.length() >= 2 && nombrePaciente.contains(token))
        .count();
  }

  private String normalize(String value) {
    if (value == null) return "";
    return java.text.Normalizer.normalize(value.toLowerCase().trim(), java.text.Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .replaceAll("[^a-z0-9]+", " ")
        .replaceAll("\\s+", " ")
        .trim();
  }

  private PacienteResponse toResponse(Paciente paciente) {
    return PacienteResponse.builder()
        .idPaciente(paciente.getIdPaciente())
        .nombres(paciente.getNombres())
        .apellidos(paciente.getApellidos())
        .fechaIngreso(paciente.getFechaIngreso())
        .fechaNacimiento(paciente.getFechaNacimiento())
        .edad(calcularEdad(paciente.getFechaNacimiento()))
        .estadoCivil(normalizeEstadoCivil(paciente.getEstadoCivil()))
        .numDocumento(paciente.getNumDocumento())
        .sexo(paciente.getSexo())
        .direccion(paciente.getDireccion())
        .distrito(paciente.getDistrito())
        .traidoPor(paciente.getTraidoPor())
        .build();
  }

  private void validarFechaNacimiento(Date fechaNacimiento) {
    if (fechaNacimiento == null) {
      throw new IllegalArgumentException("La fecha de nacimiento es obligatoria.");
    }

    LocalDate nacimiento = toLocalDate(fechaNacimiento);
    LocalDate hoy = LocalDate.now();

    if (nacimiento.isAfter(hoy)) {
      throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura.");
    }

    int edad = Period.between(nacimiento, hoy).getYears();
    if (edad < 0 || edad > 120) {
      throw new IllegalArgumentException("La edad calculada debe estar entre 0 y 120 años.");
    }
  }

  private Integer calcularEdad(Date fechaNacimiento) {
    if (fechaNacimiento == null) return null;
    return Period.between(toLocalDate(fechaNacimiento), LocalDate.now()).getYears();
  }

  private LocalDate toLocalDate(Date fecha) {
    return fecha.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
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

  @Override
  public ResponseModelSet save(PacienteRequest pacienteRequest) {
    ResponseModelSet responseModelSet = new ResponseModelSet();
    try {
      validarFechaNacimiento(pacienteRequest.getFechaNacimiento());
      Paciente paciente = new Paciente();
      paciente.setNombres(pacienteRequest.getNombres());
      paciente.setApellidos(pacienteRequest.getApellidos());
      paciente.setFechaIngreso(pacienteRequest.getFechaIngreso());
      paciente.setFechaNacimiento(pacienteRequest.getFechaNacimiento());
      paciente.setEstadoCivil(normalizeEstadoCivil(pacienteRequest.getEstadoCivil()));
      paciente.setNumDocumento(pacienteRequest.getNumDocumento());
      paciente.setSexo(pacienteRequest.getSexo());
      paciente.setDireccion(pacienteRequest.getDireccion());
      paciente.setDistrito(pacienteRequest.getDistrito());
      paciente.setTraidoPor(pacienteRequest.getTraidoPor());

      Paciente pacienteResponse = pacienteRepository.save(paciente);
      responseModelSet.setIdGenerado(pacienteResponse.getIdPaciente());
      responseModelSet.setMensaje(MENSAJE_GUARDAR_OK);
      return responseModelSet;

    } catch (Exception e) {
      log.error("save(): {}", e.getMessage());
      responseModelSet.setMensaje(MENSAJE_GUARDAR_ERROR);
      return responseModelSet;
    }
  }

  @Override
  public ResponseModelSet update(PacienteRequest pacienteRequest) {
    ResponseModelSet responseModelSet = new ResponseModelSet();
    try {
      validarFechaNacimiento(pacienteRequest.getFechaNacimiento());
      Paciente paciente = new Paciente();
      paciente.setIdPaciente(pacienteRequest.getIdPaciente());
      paciente.setNombres(pacienteRequest.getNombres());
      paciente.setApellidos(pacienteRequest.getApellidos());
      paciente.setFechaIngreso(pacienteRequest.getFechaIngreso());
      paciente.setFechaNacimiento(pacienteRequest.getFechaNacimiento());
      paciente.setEstadoCivil(normalizeEstadoCivil(pacienteRequest.getEstadoCivil()));
      paciente.setNumDocumento(pacienteRequest.getNumDocumento());
      paciente.setSexo(pacienteRequest.getSexo());
      paciente.setDireccion(pacienteRequest.getDireccion());
      paciente.setDistrito(pacienteRequest.getDistrito());
      paciente.setTraidoPor(pacienteRequest.getTraidoPor());

      pacienteRepository.save(paciente);
      responseModelSet.setMensaje(MENSAJE_EDITAR_OK);
      return responseModelSet;

    } catch (Exception e) {
      log.error("update(): {}", e.getMessage());
      responseModelSet.setMensaje(MENSAJE_GUARDAR_ERROR);
      return responseModelSet;
    }
  }
}
