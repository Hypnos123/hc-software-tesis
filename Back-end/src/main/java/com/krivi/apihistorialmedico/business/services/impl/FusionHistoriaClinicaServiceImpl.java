package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.exception.FusionHistoriaClinicaException;
import com.krivi.apihistorialmedico.business.services.AnalisisHistoriasClinicasDuplicadasService;
import com.krivi.apihistorialmedico.business.services.FusionHistoriaClinicaService;
import com.krivi.apihistorialmedico.business.services.ReautenticacionLocalService;
import com.krivi.apihistorialmedico.model.api.*;
import com.krivi.apihistorialmedico.model.entity.*;
import com.krivi.apihistorialmedico.repository.*;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class FusionHistoriaClinicaServiceImpl implements FusionHistoriaClinicaService {
  private static final Set<String> ORIGENES = Set.of("CHATBOT", "SWAGGER", "API_LOCAL");
  private final ReautenticacionLocalService reautenticacion;
  private final AnalisisHistoriasClinicasDuplicadasService analisisService;
  private final HistoriaClinicaRepository historiaRepository;
  private final ConsultaRepository consultaRepository;
  private final UsuarioRepository usuarioRepository;
  private final AuditoriaFusionHistoriaClinicaRepository auditoriaRepository;
  private final EntityManager entityManager;

  public FusionHistoriaClinicaServiceImpl(ReautenticacionLocalService reautenticacion,
      AnalisisHistoriasClinicasDuplicadasService analisisService, HistoriaClinicaRepository historiaRepository,
      ConsultaRepository consultaRepository, UsuarioRepository usuarioRepository,
      AuditoriaFusionHistoriaClinicaRepository auditoriaRepository, EntityManager entityManager) {
    this.reautenticacion = reautenticacion; this.analisisService = analisisService; this.historiaRepository = historiaRepository;
    this.consultaRepository = consultaRepository; this.usuarioRepository = usuarioRepository;
    this.auditoriaRepository = auditoriaRepository; this.entityManager = entityManager;
  }

  @Override
  @Transactional
  public FusionarHistoriasClinicasResponse fusionar(Integer idUsuario, Integer idSecundaria, FusionarHistoriasClinicasRequest request) {
    validarSolicitud(idSecundaria, request);
    ReautenticacionRequest credenciales = new ReautenticacionRequest();
    credenciales.setContrasena(request.getContrasena());
    ReautenticacionResponse autorizacion = reautenticacion.reautenticar(idUsuario, credenciales);
    Usuario usuario = usuarioRepository.findById(idUsuario)
        .orElseThrow(() -> error("USUARIO_NO_ENCONTRADO", "El usuario actual no existe.", HttpStatus.NOT_FOUND));

    int idPrincipal = request.getIdHistoriaPrincipal();
    List<Integer> idsOrdenados = new ArrayList<>(List.of(idPrincipal, idSecundaria));
    Collections.sort(idsOrdenados);
    Map<Integer, HistoriaClinica> bloqueadas = new HashMap<>();
    idsOrdenados.forEach(id -> bloqueadas.put(id, historiaRepository.findForFusionById(id)
        .orElseThrow(() -> error("HISTORIA_NO_ENCONTRADA", "Una de las historias clínicas ya no existe.", HttpStatus.NOT_FOUND))));
    HistoriaClinica principal = bloqueadas.get(idPrincipal);
    HistoriaClinica secundaria = bloqueadas.get(idSecundaria);
    validarHistorias(principal, secundaria);

    List<Consulta> consultasPrincipal = consultaRepository.findForFusionByHistoriaId(idPrincipal);
    List<Consulta> consultasSecundaria = consultaRepository.findForFusionByHistoriaId(idSecundaria);
    validarIntegridad(principal, consultasPrincipal);
    validarIntegridad(secundaria, consultasSecundaria);
    validarSnapshot(request, consultasPrincipal, consultasSecundaria);
    AnalisisHistoriasClinicasDuplicadasResponse analisisActual = analisisService.analizar(List.of(idPrincipal, idSecundaria));
    if (request.getTokenAnalisis() == null || !request.getTokenAnalisis().equals(analisisActual.getTokenAnalisis()))
      throw error("ANALISIS_DESACTUALIZADO", "El contenido clínico cambió desde el análisis. Vuelva a analizar.", HttpStatus.CONFLICT);
    int posiblesCoincidencias = analisisActual.getPosiblesCoincidencias().size();

    try {
      consultasSecundaria.forEach(consulta -> consulta.setHistoriaClinica(principal));
      consultaRepository.saveAll(consultasSecundaria);
      entityManager.flush();
      long remanentes = consultaRepository.countByHistoriaClinicaIdHistoriaClinica(idSecundaria);
      if (remanentes != 0) throw error("HISTORIA_CONSULTAS_REMANENTES", "La historia secundaria todavía tiene consultas asociadas.", HttpStatus.CONFLICT);
      long finalPrincipal = consultaRepository.countByHistoriaClinicaIdHistoriaClinica(idPrincipal);
      if (finalPrincipal != consultasPrincipal.size() + consultasSecundaria.size())
        throw error("ANALISIS_DESACTUALIZADO", "Las consultas cambiaron durante la operación. Vuelva a analizar.", HttpStatus.CONFLICT);
      historiaRepository.delete(secundaria);
      entityManager.flush();
      AuditoriaFusionHistoriaClinica auditoria = auditoria(principal, idSecundaria, usuario, autorizacion.getCargo(), request,
          consultasPrincipal.size(), consultasSecundaria.size(), finalPrincipal);
      auditoria = auditoriaRepository.saveAndFlush(auditoria);
      return FusionarHistoriasClinicasResponse.builder().fusionada(true).idHistoriaPrincipal(idPrincipal)
          .idHistoriaEliminada(idSecundaria).idPaciente(principal.getPaciente().getIdPaciente())
          .cantidadConsultasAntesPrincipal(consultasPrincipal.size()).cantidadConsultasAntesSecundaria(consultasSecundaria.size())
          .cantidadConsultasTransferidas(consultasSecundaria.size()).cantidadConsultasFinalPrincipal(finalPrincipal)
          .posiblesCoincidencias(posiblesCoincidencias).idAuditoria(auditoria.getIdAuditoria()).resultado("HISTORIAS_FUSIONADAS")
          .mensaje("Se fusionaron correctamente las historias clínicas. Se conservó la HC " + idPrincipal
              + ", se transfirieron " + consultasSecundaria.size() + " consultas y se eliminó la HC " + idSecundaria + ".").build();
    } catch (FusionHistoriaClinicaException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      throw new FusionHistoriaClinicaException("ERROR_FUSION", "No fue posible fusionar las historias clínicas; no se realizaron cambios.", HttpStatus.CONFLICT, exception);
    }
  }

  private void validarSolicitud(Integer idSecundaria, FusionarHistoriasClinicasRequest request) {
    if (idSecundaria == null || idSecundaria < 1 || request == null || request.getIdHistoriaPrincipal() == null || request.getIdHistoriaPrincipal() < 1)
      throw error("HISTORIA_NO_ENCONTRADA", "Debe indicar historias clínicas válidas.", HttpStatus.BAD_REQUEST);
    if (Objects.equals(idSecundaria, request.getIdHistoriaPrincipal())) throw error("MISMO_ID_HISTORIA", "La historia principal y secundaria deben ser diferentes.", HttpStatus.BAD_REQUEST);
    if (!Boolean.TRUE.equals(request.getConfirmacion())) throw error("CONFIRMACION_REQUERIDA", "Debe confirmar explícitamente la fusión.", HttpStatus.BAD_REQUEST);
    if (request.getMotivo() == null || request.getMotivo().isBlank()) throw error("MOTIVO_REQUERIDO", "El motivo es obligatorio.", HttpStatus.BAD_REQUEST);
    request.setOrigen(request.getOrigen() == null ? "API_LOCAL" : request.getOrigen().trim().toUpperCase(Locale.ROOT));
    if (!ORIGENES.contains(request.getOrigen())) throw error("ORIGEN_INVALIDO", "El origen no es válido.", HttpStatus.BAD_REQUEST);
  }

  private void validarHistorias(HistoriaClinica principal, HistoriaClinica secundaria) {
    if (!Objects.equals(principal.getPaciente().getIdPaciente(), secundaria.getPaciente().getIdPaciente()))
      throw error("HISTORIAS_DE_PACIENTES_DIFERENTES", "Las historias pertenecen a pacientes diferentes; primero debe resolver el paciente duplicado.", HttpStatus.CONFLICT);
    if (principal.getPaciente().getEstadoRegistro() != EstadoRegistroPaciente.ACTIVO)
      throw error("PACIENTE_INACTIVO", "El paciente asociado no está activo.", HttpStatus.CONFLICT);
  }

  private void validarIntegridad(HistoriaClinica historia, List<Consulta> consultas) {
    if (consultas.stream().anyMatch(c -> c.getPaciente() == null || !Objects.equals(c.getPaciente().getIdPaciente(), historia.getPaciente().getIdPaciente())))
      throw error("CONSULTA_INCONSISTENTE", "Una consulta no corresponde al paciente de su historia clínica.", HttpStatus.CONFLICT);
  }

  private void validarSnapshot(FusionarHistoriasClinicasRequest request, List<Consulta> principal, List<Consulta> secundaria) {
    List<Integer> idsPrincipal = principal.stream().map(Consulta::getIdConsulta).sorted().toList();
    List<Integer> idsSecundaria = secundaria.stream().map(Consulta::getIdConsulta).sorted().toList();
    if (!Objects.equals(request.getCantidadEsperadaPrincipal(), (long) principal.size())
        || !Objects.equals(request.getCantidadEsperadaSecundaria(), (long) secundaria.size())
        || !Objects.equals(ordenar(request.getIdsConsultasEsperadasPrincipal()), idsPrincipal)
        || !Objects.equals(ordenar(request.getIdsConsultasEsperadasSecundaria()), idsSecundaria))
      throw error("ANALISIS_DESACTUALIZADO", "Las consultas cambiaron desde el análisis. Vuelva a analizar antes de fusionar.", HttpStatus.CONFLICT);
  }
  private List<Integer> ordenar(List<Integer> ids) { return ids == null ? null : ids.stream().sorted().toList(); }

  private AuditoriaFusionHistoriaClinica auditoria(HistoriaClinica principal, Integer eliminada, Usuario usuario, String cargo,
      FusionarHistoriasClinicasRequest request, long antesPrincipal, long antesSecundaria, long despues) {
    AuditoriaFusionHistoriaClinica a = new AuditoriaFusionHistoriaClinica(); a.setHistoriaPrincipal(principal); a.setIdHistoriaEliminada(eliminada);
    a.setPaciente(principal.getPaciente()); a.setUsuario(usuario); a.setEmpleado(usuario.getEmpleado()); a.setCargo(cargo);
    a.setOrigen(request.getOrigen()); a.setMotivo(request.getMotivo().trim()); a.setDetalle(request.getDetalle());
    a.setConsultasAntesPrincipal(antesPrincipal); a.setConsultasAntesSecundaria(antesSecundaria); a.setConsultasTransferidas(antesSecundaria);
    a.setConsultasDespuesPrincipal(despues); a.setResultado("HISTORIAS_FUSIONADAS"); return a;
  }
  private FusionHistoriaClinicaException error(String resultado, String mensaje, HttpStatus status) { return new FusionHistoriaClinicaException(resultado, mensaje, status); }
}
