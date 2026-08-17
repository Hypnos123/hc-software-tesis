package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.exception.ConsultaPacienteArchivadoException;
import com.krivi.apihistorialmedico.business.services.PacienteArchivadoAdminService;
import com.krivi.apihistorialmedico.business.services.ReautenticacionLocalService;
import com.krivi.apihistorialmedico.model.api.*;
import com.krivi.apihistorialmedico.model.entity.*;
import com.krivi.apihistorialmedico.repository.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PacienteArchivadoAdminServiceImpl implements PacienteArchivadoAdminService {
  private static final int TAMANO_MAXIMO = 100;
  private static final Set<String> CAMPOS_ORDENABLES = Set.of("fechaArchivado", "idPaciente", "nombres", "numDocumento");

  private final ReautenticacionLocalService reautenticacion;
  private final PacienteRepository pacienteRepository;
  private final AuditoriaArchivadoPacienteRepository auditoriaRepository;
  private final HistoriaClinicaRepository historiaRepository;
  private final ConsultaRepository consultaRepository;
  private final AntecedentesRepository antecedentesRepository;

  public PacienteArchivadoAdminServiceImpl(ReautenticacionLocalService reautenticacion,
      PacienteRepository pacienteRepository, AuditoriaArchivadoPacienteRepository auditoriaRepository,
      HistoriaClinicaRepository historiaRepository, ConsultaRepository consultaRepository,
      AntecedentesRepository antecedentesRepository) {
    this.reautenticacion = reautenticacion;
    this.pacienteRepository = pacienteRepository;
    this.auditoriaRepository = auditoriaRepository;
    this.historiaRepository = historiaRepository;
    this.consultaRepository = consultaRepository;
    this.antecedentesRepository = antecedentesRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public PaginaResponse<PacienteArchivadoResumenResponse> listar(Integer idUsuario, int page, int size, String sort,
      String search, String dni, Integer idPaciente, LocalDateTime desde, LocalDateTime hasta) {
    reautenticacion.validarAdministrador(idUsuario);
    validarFiltros(page, size, idPaciente, desde, hasta);
    Pageable pageable = PageRequest.of(page, size, resolverOrden(sort));
    Page<Paciente> pacientes = pacienteRepository.buscarArchivados(limpiar(search), limpiar(dni), idPaciente, desde, hasta, pageable);
    List<Integer> ids = pacientes.getContent().stream().map(Paciente::getIdPaciente).toList();
    Map<Integer, AuditoriaArchivadoPaciente> auditorias = ids.isEmpty() ? Map.of()
        : auditoriaRepository.buscarUltimasPorPacientes(ids).stream().collect(Collectors.toMap(
            a -> a.getPacienteArchivado().getIdPaciente(), Function.identity()));

    List<PacienteArchivadoResumenResponse> content = pacientes.getContent().stream()
        .map(p -> resumen(p, auditorias.get(p.getIdPaciente())))
        .toList();
    return PaginaResponse.<PacienteArchivadoResumenResponse>builder().content(content).page(pacientes.getNumber())
        .size(pacientes.getSize()).totalElements(pacientes.getTotalElements()).totalPages(pacientes.getTotalPages()).build();
  }

  @Override
  @Transactional(readOnly = true)
  public PacienteArchivadoDetalleResponse obtenerDetalle(Integer idUsuario, Integer idPaciente) {
    reautenticacion.validarAdministrador(idUsuario);
    if (idPaciente == null || idPaciente < 1) throw error("PACIENTE_INVALIDO", "El identificador del paciente no es válido.", HttpStatus.BAD_REQUEST);
    Paciente paciente = pacienteRepository.findByIdPacienteAndEstadoRegistro(idPaciente, EstadoRegistroPaciente.ARCHIVADO)
        .orElseThrow(() -> error("PACIENTE_ARCHIVADO_NO_ENCONTRADO", "El paciente archivado no existe.", HttpStatus.NOT_FOUND));
    AuditoriaArchivadoPaciente auditoria = auditoriaRepository
        .buscarPorPacienteMasReciente(idPaciente, PageRequest.of(0, 1)).stream().findFirst().orElse(null);
    return detalle(paciente, auditoria);
  }

  private PacienteArchivadoResumenResponse resumen(Paciente p, AuditoriaArchivadoPaciente a) {
    Paciente principal = p.getPacientePrincipal();
    return PacienteArchivadoResumenResponse.builder().idPaciente(p.getIdPaciente()).nombreCompleto(nombre(p))
        .dni(p.getNumDocumento()).fechaArchivado(p.getFechaArchivado())
        .usuarioResponsable(a != null ? a.getUsuarioResponsable() : usuarioArchivado(p))
        .motivoArchivado(p.getMotivoArchivado()).estadoRegistro(p.getEstadoRegistro().name())
        .idPacientePrincipal(principal == null ? null : principal.getIdPaciente())
        .nombrePacientePrincipal(a != null ? a.getNombrePacientePrincipal() : nombre(principal))
        .idAuditoria(a == null ? null : a.getIdAuditoria()).build();
  }

  private PacienteArchivadoDetalleResponse detalle(Paciente p, AuditoriaArchivadoPaciente a) {
    Paciente principal = p.getPacientePrincipal();
    Empleado empleado = a == null ? null : a.getEmpleado();
    return PacienteArchivadoDetalleResponse.builder().idPaciente(p.getIdPaciente()).nombres(p.getNombres())
        .apellidos(p.getApellidos()).dni(p.getNumDocumento()).estadoRegistro(p.getEstadoRegistro().name())
        .fechaArchivado(p.getFechaArchivado()).motivoArchivado(p.getMotivoArchivado())
        .detalleMotivoArchivado(p.getDetalleMotivoArchivado()).idAuditoria(a == null ? null : a.getIdAuditoria())
        .usuarioResponsable(a != null ? a.getUsuarioResponsable() : usuarioArchivado(p))
        .idEmpleado(empleado == null ? null : empleado.getIdEmpleado()).empleadoResponsable(nombre(empleado))
        .cargo(a == null ? null : a.getCargo()).origen(a == null ? null : a.getOrigen())
        .fechaAuditoria(a == null ? null : a.getFecha()).estadoAnterior(a == null ? null : a.getEstadoAnterior())
        .estadoNuevo(a == null ? null : a.getEstadoNuevo()).requirioRevisionClinica(a != null && a.isRequirioRevisionClinica())
        .confirmoRevisionClinica(a != null && a.isConfirmoRevisionClinica()).pacientePrincipal(principal(principal))
        .cantidadHistoriasClinicas(historiaRepository.countByPacienteIdPaciente(p.getIdPaciente()))
        .cantidadConsultas(consultaRepository.countByPacienteIdPaciente(p.getIdPaciente()))
        .cantidadAntecedentes(antecedentesRepository.countByPacienteIdPaciente(p.getIdPaciente())).build();
  }

  private PacientePrincipalArchivadoResponse principal(Paciente p) {
    return p == null ? null : PacientePrincipalArchivadoResponse.builder().idPaciente(p.getIdPaciente())
        .nombreCompleto(nombre(p)).dni(p.getNumDocumento()).estadoRegistro(p.getEstadoRegistro().name()).build();
  }

  private void validarFiltros(int page, int size, Integer idPaciente, LocalDateTime desde, LocalDateTime hasta) {
    if (page < 0) throw error("PAGINA_INVALIDA", "La página no puede ser negativa.", HttpStatus.BAD_REQUEST);
    if (size < 1 || size > TAMANO_MAXIMO) throw error("TAMANO_PAGINA_INVALIDO", "El tamaño debe estar entre 1 y 100.", HttpStatus.BAD_REQUEST);
    if (idPaciente != null && idPaciente < 1) throw error("PACIENTE_INVALIDO", "El identificador del paciente no es válido.", HttpStatus.BAD_REQUEST);
    if (desde != null && hasta != null && desde.isAfter(hasta)) throw error("RANGO_FECHAS_INVALIDO", "La fecha inicial no puede ser posterior a la fecha final.", HttpStatus.BAD_REQUEST);
  }

  private Sort resolverOrden(String sort) {
    if (sort == null || sort.isBlank()) return Sort.by(Sort.Order.desc("fechaArchivado"), Sort.Order.desc("idPaciente"));
    String[] partes = sort.split(",", 2);
    String campo = partes[0].trim();
    if (!CAMPOS_ORDENABLES.contains(campo)) throw error("ORDEN_INVALIDO", "El campo de ordenamiento no es válido.", HttpStatus.BAD_REQUEST);
    Sort.Direction direccion;
    try { direccion = partes.length == 2 ? Sort.Direction.fromString(partes[1].trim()) : Sort.Direction.ASC; }
    catch (IllegalArgumentException ex) { throw error("ORDEN_INVALIDO", "La dirección de ordenamiento no es válida.", HttpStatus.BAD_REQUEST); }
    Sort orden = Sort.by(new Sort.Order(direccion, campo));
    return "idPaciente".equals(campo) ? orden : orden.and(Sort.by(Sort.Order.desc("idPaciente")));
  }

  private String limpiar(String valor) { return valor == null || valor.isBlank() ? null : valor.trim(); }
  private String usuarioArchivado(Paciente p) { return p.getArchivadoPor() == null ? null : p.getArchivadoPor().getUsuario(); }
  private String nombre(Paciente p) { return p == null ? null : juntar(p.getNombres(), p.getApellidos()); }
  private String nombre(Empleado e) { return e == null ? null : juntar(e.getNombres(), e.getApellidos()); }
  private String juntar(String a, String b) { return (Objects.toString(a, "") + " " + Objects.toString(b, "")).replaceAll("\\s+", " ").trim(); }
  private ConsultaPacienteArchivadoException error(String resultado, String mensaje, HttpStatus status) { return new ConsultaPacienteArchivadoException(resultado, mensaje, status); }
}
