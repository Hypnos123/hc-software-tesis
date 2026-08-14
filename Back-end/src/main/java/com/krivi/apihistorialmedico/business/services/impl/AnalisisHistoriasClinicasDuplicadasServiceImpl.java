package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.exception.BusquedaHistoriaClinicaException;
import com.krivi.apihistorialmedico.business.services.AnalisisHistoriasClinicasDuplicadasService;
import com.krivi.apihistorialmedico.model.api.AnalisisHistoriasClinicasDuplicadasResponse;
import com.krivi.apihistorialmedico.model.api.ConsultaHistoriaAnalisisResponse;
import com.krivi.apihistorialmedico.model.api.HistoriaClinicaAnalisisDetalladoResponse;
import com.krivi.apihistorialmedico.model.api.PosibleCoincidenciaConsultaResponse;
import com.krivi.apihistorialmedico.model.entity.Consulta;
import com.krivi.apihistorialmedico.model.entity.HistoriaClinica;
import com.krivi.apihistorialmedico.model.entity.Paciente;
import com.krivi.apihistorialmedico.repository.ConsultaRepository;
import com.krivi.apihistorialmedico.repository.HistoriaClinicaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class AnalisisHistoriasClinicasDuplicadasServiceImpl implements AnalisisHistoriasClinicasDuplicadasService {
  private static final ZoneId ZONA_HORARIA_LIMA = ZoneId.of("America/Lima");
  private static final String MISMO_PACIENTE = "MISMO_PACIENTE";
  private static final String MISMO_DNI_DIFERENTE_PACIENTE = "MISMO_DNI_DIFERENTE_PACIENTE";

  private final HistoriaClinicaRepository historiaRepository;
  private final ConsultaRepository consultaRepository;

  public AnalisisHistoriasClinicasDuplicadasServiceImpl(HistoriaClinicaRepository historiaRepository,
                                                         ConsultaRepository consultaRepository) {
    this.historiaRepository = historiaRepository;
    this.consultaRepository = consultaRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public AnalisisHistoriasClinicasDuplicadasResponse analizar(List<Integer> idsHistoriasClinicas) {
    List<Integer> ids = validarIds(idsHistoriasClinicas);
    List<HistoriaClinica> historias = historiaRepository.findForAnalisisByIds(ids).stream()
        .sorted(Comparator.comparing(HistoriaClinica::getIdHistoriaClinica))
        .toList();
    if (historias.size() != ids.size()) {
      throw error("HISTORIA_NO_ENCONTRADA", "Una o más historias clínicas no existen o pertenecen a pacientes inactivos.", HttpStatus.NOT_FOUND);
    }

    String tipoDuplicidad = resolverTipoDuplicidad(historias);
    Map<Integer, List<Consulta>> consultasPorHistoria = cargarConsultas(historias);
    List<String> advertencias = validarIntegridad(historias, consultasPorHistoria);
    List<HistoriaClinicaAnalisisDetalladoResponse> detalles = historias.stream()
        .map(historia -> toDetalle(historia, consultasPorHistoria.get(historia.getIdHistoriaClinica())))
        .sorted(comparadorRecomendacion())
        .toList();
    HistoriaClinicaAnalisisDetalladoResponse recomendada = detalles.getFirst();
    boolean fusionPermitida = MISMO_PACIENTE.equals(tipoDuplicidad) && advertencias.isEmpty();
    String motivoBloqueo = motivoBloqueo(tipoDuplicidad, advertencias);
    List<PosibleCoincidenciaConsultaResponse> coincidencias = detectarCoincidencias(historias, consultasPorHistoria);

    return AnalisisHistoriasClinicasDuplicadasResponse.builder()
        .tipoDuplicidad(tipoDuplicidad)
        .idHistoriaClinicaRecomendada(recomendada.getIdHistoriaClinica())
        .motivosRecomendacion(motivosRecomendacion(recomendada, detalles))
        .resumenComparativo(resumen(detalles, recomendada))
        .historiasComparadas(detalles)
        .posiblesCoincidencias(coincidencias)
        .futuraFusionPermitida(fusionPermitida)
        .motivoBloqueo(motivoBloqueo)
        .advertenciasIntegridad(advertencias)
        .mensaje(mensaje(tipoDuplicidad, recomendada, detalles, coincidencias, advertencias))
        .tokenAnalisis(tokenAnalisis(detalles))
        .build();
  }

  private List<Integer> validarIds(List<Integer> idsSolicitados) {
    if (idsSolicitados == null || idsSolicitados.size() < 2) {
      throw error("HISTORIAS_INSUFICIENTES", "Debe indicar al menos dos historias clínicas.", HttpStatus.BAD_REQUEST);
    }
    if (idsSolicitados.stream().anyMatch(id -> id == null || id < 1)) {
      throw error("ID_HISTORIA_INVALIDO", "Todos los identificadores de historia clínica deben ser positivos.", HttpStatus.BAD_REQUEST);
    }
    List<Integer> ids = new ArrayList<>(new LinkedHashSet<>(idsSolicitados));
    if (ids.size() != idsSolicitados.size()) {
      throw error("HISTORIAS_REPETIDAS", "No debe repetir identificadores de historias clínicas.", HttpStatus.BAD_REQUEST);
    }
    return ids;
  }

  private String resolverTipoDuplicidad(List<HistoriaClinica> historias) {
    long pacientes = historias.stream().map(h -> h.getPaciente().getIdPaciente()).distinct().count();
    if (pacientes == 1) return MISMO_PACIENTE;
    Set<String> dnis = historias.stream().map(h -> dni(h.getPaciente())).collect(java.util.stream.Collectors.toSet());
    if (dnis.size() == 1 && !dnis.contains(null)) return MISMO_DNI_DIFERENTE_PACIENTE;
    throw error("HISTORIAS_NO_DUPLICADAS",
        "Las historias seleccionadas no pertenecen al mismo paciente ni a pacientes con el mismo DNI.", HttpStatus.BAD_REQUEST);
  }

  private Map<Integer, List<Consulta>> cargarConsultas(List<HistoriaClinica> historias) {
    Map<Integer, List<Consulta>> resultado = new HashMap<>();
    historias.forEach(historia -> resultado.put(historia.getIdHistoriaClinica(),
        consultaRepository.findByHistoriaClinicaIdHistoriaClinica(historia.getIdHistoriaClinica())));
    return resultado;
  }

  private List<String> validarIntegridad(List<HistoriaClinica> historias, Map<Integer, List<Consulta>> consultas) {
    List<String> advertencias = new ArrayList<>();
    historias.forEach(historia -> consultas.get(historia.getIdHistoriaClinica()).stream()
        .filter(consulta -> consulta.getPaciente() == null
            || !Objects.equals(consulta.getPaciente().getIdPaciente(), historia.getPaciente().getIdPaciente()))
        .forEach(consulta -> advertencias.add("La consulta ID " + consulta.getIdConsulta()
            + " referencia al paciente " + (consulta.getPaciente() == null ? "no registrado" : consulta.getPaciente().getIdPaciente())
            + ", pero su historia clínica pertenece al paciente " + historia.getPaciente().getIdPaciente() + ".")));
    return advertencias;
  }

  private HistoriaClinicaAnalisisDetalladoResponse toDetalle(HistoriaClinica historia, List<Consulta> consultas) {
    int campos = consultas.stream().mapToInt(this::camposInformados).sum();
    int riqueza = consultas.stream().mapToInt(this::puntajeRiqueza).sum();
    List<ConsultaHistoriaAnalisisResponse> exclusivas = consultas.stream()
        .sorted(Comparator.comparing(Consulta::getIdConsulta))
        .map(consulta -> ConsultaHistoriaAnalisisResponse.builder().idConsulta(consulta.getIdConsulta())
            .estado(consulta.getEstado()).fechaActividad(fechaActividad(consulta))
            .idEmpleado(consulta.getDoctorResponsable() == null ? null : consulta.getDoctorResponsable().getIdEmpleado())
            .medico(consulta.getDoctorResponsable() == null ? null : nombreEmpleado(consulta.getDoctorResponsable()))
            .diagnosticoResumen(resumir(consulta.getDiagnostico()))
            .camposClinicosInformados(camposInformados(consulta)).puntajeRiquezaClinica(puntajeRiqueza(consulta)).build())
        .toList();
    return HistoriaClinicaAnalisisDetalladoResponse.builder()
        .idHistoriaClinica(historia.getIdHistoriaClinica()).idPaciente(historia.getPaciente().getIdPaciente())
        .dni(dni(historia.getPaciente())).nombreCompleto(nombre(historia.getPaciente()))
        .fechaCreacion(historia.getFechaCreacion()).ultimaActualizacion(historia.getUltimaActualizacion())
        .cantidadConsultas(consultas.size())
        .ultimaActividadClinica(consultas.stream().map(this::fechaActividad).filter(Objects::nonNull).max(LocalDateTime::compareTo).orElse(null))
        .cantidadConsultasPendientes(consultas.stream().filter(c -> "PENDIENTE".equalsIgnoreCase(texto(c.getEstado()))).count())
        .cantidadConsultasAtendidas(consultas.stream().filter(c -> "ATENDIDO".equalsIgnoreCase(texto(c.getEstado()))).count())
        .camposClinicosInformados(campos).puntajeRiquezaClinica(riqueza)
        .cantidadConsultasExclusivas(exclusivas.size()).consultasExclusivas(exclusivas).build();
  }

  private Comparator<HistoriaClinicaAnalisisDetalladoResponse> comparadorRecomendacion() {
    return Comparator.comparingLong(HistoriaClinicaAnalisisDetalladoResponse::getCantidadConsultas).reversed()
        .thenComparing(Comparator.comparingInt(HistoriaClinicaAnalisisDetalladoResponse::getPuntajeRiquezaClinica).reversed())
        .thenComparing(HistoriaClinicaAnalisisDetalladoResponse::getUltimaActividadClinica, Comparator.nullsLast(Comparator.reverseOrder()))
        .thenComparing(HistoriaClinicaAnalisisDetalladoResponse::getFechaCreacion, Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(HistoriaClinicaAnalisisDetalladoResponse::getIdHistoriaClinica);
  }

  private int camposInformados(Consulta c) {
    int campos = 0;
    campos += informado(c.getDiagnostico()); campos += informado(c.getTratamiento()); campos += informado(c.getReceta());
    campos += informado(c.getExamenesRecetados()); campos += informado(c.getRelatoPaciente());
    campos += informado(c.getPresionArterial()); campos += informado(c.getFrecuenciaCardiaca());
    campos += informado(c.getFrecuenciaRespiratoria()); campos += informado(c.getTalla()); campos += informado(c.getTemperatura());
    campos += c.getPeso() == null ? 0 : 1; campos += informado(c.getTiempoEnfermedad());
    campos += informado(c.getEspecialidadRequerida()); campos += c.getTipoEnfermedad() == null ? 0 : 1;
    campos += c.getProximaCita() == null ? 0 : 1;
    return campos;
  }

  private int puntajeRiqueza(Consulta c) {
    int puntaje = 3 * (informado(c.getDiagnostico()) + informado(c.getTratamiento()) + informado(c.getReceta())
        + informado(c.getExamenesRecetados()) + informado(c.getRelatoPaciente()));
    return puntaje + camposInformados(c)
        - informado(c.getDiagnostico()) - informado(c.getTratamiento()) - informado(c.getReceta())
        - informado(c.getExamenesRecetados()) - informado(c.getRelatoPaciente());
  }

  private List<PosibleCoincidenciaConsultaResponse> detectarCoincidencias(List<HistoriaClinica> historias,
      Map<Integer, List<Consulta>> consultasPorHistoria) {
    List<PosibleCoincidenciaConsultaResponse> resultado = new ArrayList<>();
    for (int i = 0; i < historias.size(); i++) {
      for (int j = i + 1; j < historias.size(); j++) {
        HistoriaClinica a = historias.get(i); HistoriaClinica b = historias.get(j);
        for (Consulta consultaA : consultasPorHistoria.get(a.getIdHistoriaClinica())) {
          for (Consulta consultaB : consultasPorHistoria.get(b.getIdHistoriaClinica())) {
            List<String> criterios = criteriosCoincidentes(consultaA, consultaB);
            if (criterios.contains("MISMA_FECHA") && criterios.size() >= 3
                && criterios.stream().anyMatch(c -> Set.of("MISMO_DIAGNOSTICO", "MISMO_TRATAMIENTO", "MISMA_RECETA").contains(c))) {
              resultado.add(PosibleCoincidenciaConsultaResponse.builder().clasificacion("POSIBLE_COINCIDENCIA")
                  .idConsultaA(consultaA.getIdConsulta()).idHistoriaClinicaA(a.getIdHistoriaClinica())
                  .idConsultaB(consultaB.getIdConsulta()).idHistoriaClinicaB(b.getIdHistoriaClinica())
                  .criteriosCoincidentes(criterios)
                  .advertencia("Coincidencia informativa: ambas consultas se conservan y requieren revisión humana.").build());
            }
          }
        }
      }
    }
    return resultado;
  }

  private List<String> criteriosCoincidentes(Consulta a, Consulta b) {
    List<String> criterios = new ArrayList<>();
    if (Objects.equals(fechaClinica(a), fechaClinica(b)) && fechaClinica(a) != null) criterios.add("MISMA_FECHA");
    if (mismoId(a.getDoctorResponsable(), b.getDoctorResponsable(), e -> e.getIdEmpleado())) criterios.add("MISMO_MEDICO");
    if (mismoId(a.getTipoEnfermedad(), b.getTipoEnfermedad(), t -> t.getIdTipoEnfermedad())) criterios.add("MISMO_TIPO_ENFERMEDAD");
    if (mismoTexto(a.getDiagnostico(), b.getDiagnostico())) criterios.add("MISMO_DIAGNOSTICO");
    if (mismoTexto(a.getTratamiento(), b.getTratamiento())) criterios.add("MISMO_TRATAMIENTO");
    if (mismoTexto(a.getReceta(), b.getReceta())) criterios.add("MISMA_RECETA");
    return criterios;
  }

  private <T> boolean mismoId(T a, T b, java.util.function.Function<T, Integer> id) {
    return a != null && b != null && id.apply(a) != null && Objects.equals(id.apply(a), id.apply(b));
  }

  private List<String> motivosRecomendacion(HistoriaClinicaAnalisisDetalladoResponse recomendada,
      List<HistoriaClinicaAnalisisDetalladoResponse> historias) {
    HistoriaClinicaAnalisisDetalladoResponse alternativa = historias.get(1);
    if (recomendada.getCantidadConsultas() != alternativa.getCantidadConsultas())
      return List.of("Tiene la mayor cantidad de consultas médicas.");
    if (recomendada.getPuntajeRiquezaClinica() != alternativa.getPuntajeRiquezaClinica())
      return List.of("Con igual cantidad de consultas, posee mayor riqueza de información clínica.");
    if (compararActividad(recomendada.getUltimaActividadClinica(), alternativa.getUltimaActividadClinica()) != 0)
      return List.of("Con igual cantidad y riqueza clínica, registra la actividad clínica más reciente.");
    if (!Objects.equals(recomendada.getFechaCreacion(), alternativa.getFechaCreacion()))
      return List.of("Los criterios clínicos están empatados y es la historia clínica más antigua.");
    return List.of("Todos los criterios están empatados y tiene el ID menor.");
  }

  private int compararActividad(LocalDateTime a, LocalDateTime b) {
    if (a == null && b == null) return 0;
    if (a == null) return -1;
    if (b == null) return 1;
    return a.compareTo(b);
  }

  private String resumen(List<HistoriaClinicaAnalisisDetalladoResponse> historias,
      HistoriaClinicaAnalisisDetalladoResponse recomendada) {
    long consultas = historias.stream().mapToLong(HistoriaClinicaAnalisisDetalladoResponse::getCantidadConsultas).sum();
    return "Se compararon " + historias.size() + " historias con " + consultas
        + " consultas en total. Se recomienda conservar la historia clínica ID " + recomendada.getIdHistoriaClinica() + ".";
  }

  private String motivoBloqueo(String tipo, List<String> advertencias) {
    if (!advertencias.isEmpty()) return "Se detectaron inconsistencias entre el paciente de una consulta y el paciente de su historia clínica.";
    if (MISMO_DNI_DIFERENTE_PACIENTE.equals(tipo))
      return "Las historias comparten DNI, pero pertenecen a pacientes diferentes; primero deben gestionarse los pacientes duplicados.";
    return null;
  }

  private String mensaje(String tipo, HistoriaClinicaAnalisisDetalladoResponse recomendada,
      List<HistoriaClinicaAnalisisDetalladoResponse> historias, List<PosibleCoincidenciaConsultaResponse> coincidencias,
      List<String> advertencias) {
    if (!advertencias.isEmpty()) return "El análisis terminó con advertencias de integridad y no permite una futura fusión automática.";
    if (MISMO_DNI_DIFERENTE_PACIENTE.equals(tipo))
      return "Estas historias comparten DNI, pero pertenecen a registros de paciente diferentes. No es seguro fusionarlas automáticamente.";
    long transferibles = historias.stream().filter(h -> h != recomendada)
        .mapToLong(HistoriaClinicaAnalisisDetalladoResponse::getCantidadConsultasExclusivas).sum();
    String consultas = transferibles == 0 ? "No existen consultas para transferir."
        : "Las historias secundarias contienen " + transferibles + " consultas exclusivas que podrían transferirse en una futura fusión.";
    String similares = coincidencias.isEmpty() ? "" : " Se detectaron " + coincidencias.size() + " posibles coincidencias que requieren revisión.";
    return "Se recomienda conservar la historia clínica ID " + recomendada.getIdHistoriaClinica() + ". " + consultas + similares;
  }

  private LocalDate fechaClinica(Consulta c) {
    if (c.getFechaAtencion() != null) return c.getFechaAtencion().toLocalDate();
    if (c.getFechaConsulta() != null) return aLocalDate(c.getFechaConsulta());
    return c.getFechaCreacion() == null ? null : c.getFechaCreacion().toLocalDate();
  }

  private LocalDateTime fechaActividad(Consulta c) {
    if (c.getFechaAtencion() != null) return c.getFechaAtencion();
    if (c.getFechaConsulta() != null) return aLocalDate(c.getFechaConsulta()).atStartOfDay();
    return c.getFechaCreacion();
  }

  private LocalDate aLocalDate(Date fecha) {
    return fecha instanceof java.sql.Date sql ? sql.toLocalDate() : fecha.toInstant().atZone(ZONA_HORARIA_LIMA).toLocalDate();
  }

  private int informado(String valor) { return valor == null || valor.isBlank() ? 0 : 1; }
  private String texto(String valor) { return valor == null ? "" : valor.trim(); }
  private boolean mismoTexto(String a, String b) { return !normalizar(a).isEmpty() && normalizar(a).equals(normalizar(b)); }
  private String normalizar(String valor) {
    if (valor == null) return "";
    return Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
  }
  private String dni(Paciente paciente) { return paciente.getNumDocumento() == null ? null : paciente.getNumDocumento().trim(); }
  private String nombre(Paciente paciente) {
    return (Objects.toString(paciente.getNombres(), "") + " " + Objects.toString(paciente.getApellidos(), ""))
        .replaceAll("\\s+", " ").trim();
  }
  private String nombreEmpleado(Empleado empleado) {
    return (Objects.toString(empleado.getNombres(), "") + " " + Objects.toString(empleado.getApellidos(), "")).replaceAll("\\s+", " ").trim();
  }
  private String resumir(String valor) {
    if (valor == null || valor.isBlank()) return null;
    String limpio = valor.trim(); return limpio.length() <= 100 ? limpio : limpio.substring(0, 97) + "...";
  }
  private String tokenAnalisis(List<HistoriaClinicaAnalisisDetalladoResponse> historias) {
    String contenido = historias.stream().sorted(Comparator.comparing(HistoriaClinicaAnalisisDetalladoResponse::getIdHistoriaClinica))
        .map(h -> h.getIdHistoriaClinica() + ":" + h.getIdPaciente() + ":" + h.getCantidadConsultas() + ":"
            + h.getConsultasExclusivas().stream().map(c -> c.getIdConsulta() + "|" + c.getEstado() + "|" + c.getFechaActividad()
                + "|" + c.getIdEmpleado() + "|" + c.getDiagnosticoResumen() + "|" + c.getPuntajeRiquezaClinica()).toList())
        .collect(java.util.stream.Collectors.joining(";"));
    return Integer.toUnsignedString(contenido.hashCode(), 16);
  }
  private BusquedaHistoriaClinicaException error(String codigo, String mensaje, HttpStatus status) {
    return new BusquedaHistoriaClinicaException(codigo, mensaje, status);
  }
}
