package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.exception.ConsultaFusionAuditoriaException;
import com.krivi.apihistorialmedico.business.services.AuditoriaFusionHistoriaClinicaAdminService;
import com.krivi.apihistorialmedico.business.services.ReautenticacionLocalService;
import com.krivi.apihistorialmedico.model.api.*;
import com.krivi.apihistorialmedico.model.entity.*;
import com.krivi.apihistorialmedico.repository.AuditoriaFusionHistoriaClinicaRepository;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AuditoriaFusionHistoriaClinicaAdminServiceImpl implements AuditoriaFusionHistoriaClinicaAdminService {
  private static final int TAMANO_MAXIMO = 100;
  private static final Set<String> CAMPOS_ORDENABLES = Set.of("fecha", "idAuditoria", "idHistoriaEliminada", "resultado");

  private final ReautenticacionLocalService reautenticacion;
  private final AuditoriaFusionHistoriaClinicaRepository repository;

  public AuditoriaFusionHistoriaClinicaAdminServiceImpl(ReautenticacionLocalService reautenticacion,
      AuditoriaFusionHistoriaClinicaRepository repository) {
    this.reautenticacion = reautenticacion;
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public PaginaResponse<FusionHistoriaClinicaAuditoriaResumenResponse> listar(Integer idUsuarioActual, int page, int size,
      String sort, String search, String dni, Integer idPaciente, Integer idHistoriaPrincipal,
      Integer idHistoriaEliminada, Integer idUsuario, String resultado, LocalDateTime desde, LocalDateTime hasta) {
    reautenticacion.validarAdministrador(idUsuarioActual);
    validarFiltros(page, size, idPaciente, idHistoriaPrincipal, idHistoriaEliminada, idUsuario, desde, hasta);
    Page<AuditoriaFusionHistoriaClinica> auditorias = repository.buscar(limpiar(search), limpiar(dni), idPaciente,
        idHistoriaPrincipal, idHistoriaEliminada, idUsuario, limpiar(resultado), desde, hasta,
        PageRequest.of(page, size, resolverOrden(sort)));
    return PaginaResponse.<FusionHistoriaClinicaAuditoriaResumenResponse>builder()
        .content(auditorias.getContent().stream().map(this::resumen).toList()).page(auditorias.getNumber())
        .size(auditorias.getSize()).totalElements(auditorias.getTotalElements()).totalPages(auditorias.getTotalPages()).build();
  }

  @Override
  @Transactional(readOnly = true)
  public FusionHistoriaClinicaAuditoriaDetalleResponse obtenerDetalle(Integer idUsuarioActual, Integer idAuditoria) {
    reautenticacion.validarAdministrador(idUsuarioActual);
    if (idAuditoria == null || idAuditoria < 1) throw error("AUDITORIA_INVALIDA", "El identificador de auditoría no es válido.", HttpStatus.BAD_REQUEST);
    return repository.buscarDetalle(idAuditoria).map(this::detalle)
        .orElseThrow(() -> error("AUDITORIA_FUSION_NO_ENCONTRADA", "La auditoría de fusión no existe.", HttpStatus.NOT_FOUND));
  }

  private FusionHistoriaClinicaAuditoriaResumenResponse resumen(AuditoriaFusionHistoriaClinica a) {
    return FusionHistoriaClinicaAuditoriaResumenResponse.builder().idAuditoria(a.getIdAuditoria())
        .idPaciente(a.getPaciente().getIdPaciente()).nombrePaciente(nombre(a.getPaciente())).dni(a.getPaciente().getNumDocumento())
        .idHistoriaPrincipal(a.getHistoriaPrincipal().getIdHistoriaClinica()).idHistoriaEliminada(a.getIdHistoriaEliminada())
        .consultasTransferidas(a.getConsultasTransferidas()).fecha(a.getFecha())
        .usuarioResponsable(a.getUsuario().getUsuario()).resultado(a.getResultado()).build();
  }

  private FusionHistoriaClinicaAuditoriaDetalleResponse detalle(AuditoriaFusionHistoriaClinica a) {
    Empleado empleado = a.getEmpleado();
    return FusionHistoriaClinicaAuditoriaDetalleResponse.builder().idAuditoria(a.getIdAuditoria()).fecha(a.getFecha())
        .resultado(a.getResultado()).origen(a.getOrigen()).motivo(a.getMotivo()).detalle(a.getDetalle())
        .idPaciente(a.getPaciente().getIdPaciente()).nombrePaciente(nombre(a.getPaciente())).dni(a.getPaciente().getNumDocumento())
        .idHistoriaPrincipal(a.getHistoriaPrincipal().getIdHistoriaClinica()).idHistoriaEliminada(a.getIdHistoriaEliminada())
        .consultasAntesPrincipal(a.getConsultasAntesPrincipal()).consultasAntesSecundaria(a.getConsultasAntesSecundaria())
        .consultasTransferidas(a.getConsultasTransferidas()).consultasDespuesPrincipal(a.getConsultasDespuesPrincipal())
        .idUsuario(a.getUsuario().getIdUsuario()).usuarioResponsable(a.getUsuario().getUsuario())
        .idEmpleado(empleado.getIdEmpleado()).empleadoResponsable(nombre(empleado)).cargo(a.getCargo())
        .explicacion(explicacion(a)).build();
  }

  private String explicacion(AuditoriaFusionHistoriaClinica a) {
    long cantidad = a.getConsultasTransferidas();
    String unidad = cantidad == 1 ? "consulta" : "consultas";
    return "La HC " + a.getIdHistoriaEliminada() + " fue fusionada en la HC "
        + a.getHistoriaPrincipal().getIdHistoriaClinica() + ". Se conservó la HC "
        + a.getHistoriaPrincipal().getIdHistoriaClinica() + " y se transfirió " + cantidad + " " + unidad
        + " desde la HC " + a.getIdHistoriaEliminada() + ".";
  }

  private void validarFiltros(int page, int size, Integer... ids) {
    if (page < 0) throw error("PAGINA_INVALIDA", "La página no puede ser negativa.", HttpStatus.BAD_REQUEST);
    if (size < 1 || size > TAMANO_MAXIMO) throw error("TAMANO_PAGINA_INVALIDO", "El tamaño debe estar entre 1 y 100.", HttpStatus.BAD_REQUEST);
    if (Arrays.stream(ids).filter(Objects::nonNull).anyMatch(id -> id < 1))
      throw error("IDENTIFICADOR_INVALIDO", "Los identificadores de filtro deben ser positivos.", HttpStatus.BAD_REQUEST);
  }

  private void validarFiltros(int page, int size, Integer idPaciente, Integer principal, Integer eliminada,
      Integer idUsuario, LocalDateTime desde, LocalDateTime hasta) {
    validarFiltros(page, size, idPaciente, principal, eliminada, idUsuario);
    if (desde != null && hasta != null && desde.isAfter(hasta))
      throw error("RANGO_FECHAS_INVALIDO", "La fecha inicial no puede ser posterior a la fecha final.", HttpStatus.BAD_REQUEST);
  }

  private Sort resolverOrden(String sort) {
    if (sort == null || sort.isBlank()) return Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("idAuditoria"));
    String[] partes = sort.split(",", 2); String campo = partes[0].trim();
    if (!CAMPOS_ORDENABLES.contains(campo)) throw error("ORDEN_INVALIDO", "El campo de ordenamiento no es válido.", HttpStatus.BAD_REQUEST);
    Sort.Direction direccion;
    try { direccion = partes.length == 2 ? Sort.Direction.fromString(partes[1].trim()) : Sort.Direction.ASC; }
    catch (IllegalArgumentException e) { throw error("ORDEN_INVALIDO", "La dirección de ordenamiento no es válida.", HttpStatus.BAD_REQUEST); }
    Sort orden = Sort.by(new Sort.Order(direccion, campo));
    return "idAuditoria".equals(campo) ? orden : orden.and(Sort.by(Sort.Order.desc("idAuditoria")));
  }

  private String limpiar(String valor) { return valor == null || valor.isBlank() ? null : valor.trim(); }
  private String nombre(Paciente p) { return juntar(p.getNombres(), p.getApellidos()); }
  private String nombre(Empleado e) { return juntar(e.getNombres(), e.getApellidos()); }
  private String juntar(String a, String b) { return (Objects.toString(a, "") + " " + Objects.toString(b, "")).replaceAll("\\s+", " ").trim(); }
  private ConsultaFusionAuditoriaException error(String resultado, String mensaje, HttpStatus status) { return new ConsultaFusionAuditoriaException(resultado, mensaje, status); }
}
