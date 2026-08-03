package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.exception.ArchivadoPacienteDuplicadoException;
import com.krivi.apihistorialmedico.business.services.ArchivadoPacienteDuplicadoService;
import com.krivi.apihistorialmedico.business.services.PacienteDuplicadoService;
import com.krivi.apihistorialmedico.business.services.PacienteService;
import com.krivi.apihistorialmedico.business.services.ReautenticacionLocalService;
import com.krivi.apihistorialmedico.model.api.ArchivarPacienteDuplicadoRequest;
import com.krivi.apihistorialmedico.model.api.ArchivarPacienteDuplicadoResponse;
import com.krivi.apihistorialmedico.model.api.AuditoriaArchivadoPacienteResponse;
import com.krivi.apihistorialmedico.model.api.PacienteDuplicadoComparacionResponse;
import com.krivi.apihistorialmedico.model.api.ReautenticacionRequest;
import com.krivi.apihistorialmedico.model.api.ReautenticacionResponse;
import com.krivi.apihistorialmedico.model.entity.AuditoriaArchivadoPaciente;
import com.krivi.apihistorialmedico.model.entity.Empleado;
import com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente;
import com.krivi.apihistorialmedico.model.entity.Paciente;
import com.krivi.apihistorialmedico.model.entity.Usuario;
import com.krivi.apihistorialmedico.repository.AuditoriaArchivadoPacienteRepository;
import com.krivi.apihistorialmedico.repository.PacienteRepository;
import com.krivi.apihistorialmedico.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class ArchivadoPacienteDuplicadoServiceImpl implements ArchivadoPacienteDuplicadoService {
  private static final Pattern DNI_PATTERN = Pattern.compile("\\d{8}");
  private static final Set<String> ORIGENES_PERMITIDOS = Set.of("CHATBOT", "SWAGGER", "API_LOCAL");

  private final ReautenticacionLocalService reautenticacionLocalService;
  private final PacienteDuplicadoService pacienteDuplicadoService;
  private final PacienteService pacienteService;
  private final PacienteRepository pacienteRepository;
  private final UsuarioRepository usuarioRepository;
  private final AuditoriaArchivadoPacienteRepository auditoriaRepository;
  private final EntityManager entityManager;

  public ArchivadoPacienteDuplicadoServiceImpl(
      ReautenticacionLocalService reautenticacionLocalService,
      PacienteDuplicadoService pacienteDuplicadoService,
      PacienteService pacienteService,
      PacienteRepository pacienteRepository,
      UsuarioRepository usuarioRepository,
      AuditoriaArchivadoPacienteRepository auditoriaRepository,
      EntityManager entityManager) {
    this.reautenticacionLocalService = reautenticacionLocalService;
    this.pacienteDuplicadoService = pacienteDuplicadoService;
    this.pacienteService = pacienteService;
    this.pacienteRepository = pacienteRepository;
    this.usuarioRepository = usuarioRepository;
    this.auditoriaRepository = auditoriaRepository;
    this.entityManager = entityManager;
  }

  @Override
  @Transactional
  public ArchivarPacienteDuplicadoResponse archivar(Integer idUsuarioActual, Integer idPacienteArchivado,
                                                    ArchivarPacienteDuplicadoRequest request) {
    ReautenticacionRequest credenciales = new ReautenticacionRequest();
    credenciales.setContrasena(request == null ? null : request.getContrasena());
    ReautenticacionResponse autorizacion = reautenticacionLocalService.reautenticar(idUsuarioActual, credenciales);
    validarSolicitudBasica(idPacienteArchivado, request);
    Usuario usuario = usuarioRepository.findById(idUsuarioActual)
        .orElseThrow(() -> error("USUARIO_NO_ENCONTRADO", "El usuario actual no existe.", HttpStatus.NOT_FOUND));
    Empleado empleado = usuario.getEmpleado();

    Paciente paciente = pacienteRepository.findById(idPacienteArchivado)
        .orElseThrow(() -> error("PACIENTE_NO_ENCONTRADO", "El paciente a archivar no existe.", HttpStatus.NOT_FOUND));
    Paciente principal = pacienteRepository.findById(request.getIdPacientePrincipal())
        .orElseThrow(() -> error("PACIENTE_PRINCIPAL_NO_ENCONTRADO", "El paciente principal no existe.", HttpStatus.NOT_FOUND));

    validarEstadosYDni(paciente, principal);
    PacienteDuplicadoComparacionResponse comparacion = pacienteDuplicadoService.compararPorDni(paciente.getNumDocumento().trim());
    validarComparacion(comparacion, paciente.getIdPaciente(), principal.getIdPaciente(), request.getConfirmarRevisionClinica());

    Paciente archivado;
    try {
      archivado = pacienteService.archivarInternamente(paciente.getIdPaciente(), principal.getIdPaciente(),
          usuario.getIdUsuario(), request.getMotivo(), request.getDetalleMotivo());
      entityManager.flush();
    } catch (OptimisticLockException | OptimisticLockingFailureException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      throw new ArchivadoPacienteDuplicadoException("ARCHIVADO_FALLIDO",
          "No fue posible archivar el paciente.", HttpStatus.CONFLICT, exception);
    }

    AuditoriaArchivadoPaciente auditoria = construirAuditoria(archivado, principal, usuario, empleado,
        autorizacion.getCargo(), request, comparacion.isRequiereRevision());
    try {
      auditoria = auditoriaRepository.saveAndFlush(auditoria);
    } catch (OptimisticLockException | OptimisticLockingFailureException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      throw new ArchivadoPacienteDuplicadoException("AUDITORIA_FALLIDA",
          "No fue posible registrar la auditoría; el archivado fue revertido.", HttpStatus.CONFLICT, exception);
    }

    return ArchivarPacienteDuplicadoResponse.builder()
        .archivado(true)
        .idPacienteArchivado(archivado.getIdPaciente())
        .idPacientePrincipal(principal.getIdPaciente())
        .dni(archivado.getNumDocumento())
        .estadoAnterior(EstadoRegistroPaciente.ACTIVO.name())
        .estadoNuevo(EstadoRegistroPaciente.ARCHIVADO.name())
        .idAuditoria(auditoria.getIdAuditoria())
        .usuarioResponsable(auditoria.getUsuarioResponsable())
        .cargoResponsable(auditoria.getCargo())
        .requiereRevisionClinica(comparacion.isRequiereRevision())
        .revisionClinicaConfirmada(Boolean.TRUE.equals(request.getConfirmarRevisionClinica()))
        .resultado("PACIENTE_ARCHIVADO")
        .mensaje("El paciente duplicado fue archivado correctamente.")
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public List<AuditoriaArchivadoPacienteResponse> consultarAuditoria(Integer idUsuarioActual, String dni,
                                                                     Integer idPaciente, LocalDateTime desde,
                                                                     LocalDateTime hasta) {
    reautenticacionLocalService.validarAdministrador(idUsuarioActual);
    String dniNormalizado = normalizarFiltroDni(dni);
    if (idPaciente != null && idPaciente < 1) {
      throw error("PACIENTE_PRINCIPAL_INVALIDO", "El identificador del paciente no es válido.", HttpStatus.BAD_REQUEST);
    }
    if (desde != null && hasta != null && desde.isAfter(hasta)) {
      throw error("RANGO_FECHAS_INVALIDO", "La fecha inicial no puede ser posterior a la fecha final.", HttpStatus.BAD_REQUEST);
    }
    return auditoriaRepository.buscar(dniNormalizado, idPaciente, desde, hasta).stream()
        .map(this::toResponse)
        .toList();
  }

  private void validarSolicitudBasica(Integer idPacienteArchivado, ArchivarPacienteDuplicadoRequest request) {
    if (idPacienteArchivado == null || idPacienteArchivado < 1) {
      throw error("PACIENTE_NO_ENCONTRADO", "El paciente a archivar no existe.", HttpStatus.NOT_FOUND);
    }
    if (request == null || request.getIdPacientePrincipal() == null || request.getIdPacientePrincipal() < 1) {
      throw error("PACIENTE_PRINCIPAL_INVALIDO", "Debe indicar un paciente principal válido.", HttpStatus.BAD_REQUEST);
    }
    if (Objects.equals(idPacienteArchivado, request.getIdPacientePrincipal())) {
      throw error("PACIENTE_PRINCIPAL_INVALIDO", "El paciente principal debe ser distinto del paciente archivado.", HttpStatus.BAD_REQUEST);
    }
    if (request.getMotivo() == null || request.getMotivo().isBlank()) {
      throw error("MOTIVO_REQUERIDO", "El motivo de archivado es obligatorio.", HttpStatus.BAD_REQUEST);
    }
    if (request.getMotivo().trim().length() > 45
        || (request.getDetalleMotivo() != null && request.getDetalleMotivo().trim().length() > 500)) {
      throw error("MOTIVO_INVALIDO", "El motivo o su detalle supera la longitud permitida.", HttpStatus.BAD_REQUEST);
    }
    request.setOrigen(normalizarOrigen(request.getOrigen()));
  }

  private void validarEstadosYDni(Paciente paciente, Paciente principal) {
    if (paciente.getEstadoRegistro() != EstadoRegistroPaciente.ACTIVO) {
      throw error("PACIENTE_YA_ARCHIVADO", "El paciente ya está archivado.", HttpStatus.CONFLICT);
    }
    if (principal.getEstadoRegistro() != EstadoRegistroPaciente.ACTIVO) {
      throw error("PACIENTE_PRINCIPAL_ARCHIVADO", "El paciente principal está archivado.", HttpStatus.CONFLICT);
    }
    String dniPaciente = paciente.getNumDocumento() == null ? "" : paciente.getNumDocumento().trim();
    String dniPrincipal = principal.getNumDocumento() == null ? "" : principal.getNumDocumento().trim();
    if (!DNI_PATTERN.matcher(dniPaciente).matches() || !dniPaciente.equals(dniPrincipal)) {
      throw error("PACIENTES_NO_SON_DUPLICADOS", "Los pacientes no corresponden al mismo DNI.", HttpStatus.BAD_REQUEST);
    }
  }

  private void validarComparacion(PacienteDuplicadoComparacionResponse comparacion, Integer idArchivado,
                                  Integer idPrincipal, Boolean confirmarRevision) {
    boolean contieneSeleccion = comparacion.isEsDuplicado()
        && comparacion.getPacientes().stream().anyMatch(item -> idArchivado.equals(item.getIdPaciente()))
        && comparacion.getPacientes().stream().anyMatch(item -> idPrincipal.equals(item.getIdPaciente()));
    if (!contieneSeleccion) {
      throw error("PACIENTES_NO_SON_DUPLICADOS", "Los pacientes seleccionados no son duplicados activos.", HttpStatus.BAD_REQUEST);
    }
    if (comparacion.isRequiereRevision() && !Boolean.TRUE.equals(confirmarRevision)) {
      throw error("CONFIRMACION_REVISION_REQUERIDA",
          "Dos o más registros contienen información clínica. Debe confirmar la revisión antes de archivar.",
          HttpStatus.CONFLICT);
    }
  }

  private AuditoriaArchivadoPaciente construirAuditoria(
      Paciente archivado, Paciente principal, Usuario usuario, Empleado empleado, String cargo,
      ArchivarPacienteDuplicadoRequest request, boolean requiereRevision) {
    AuditoriaArchivadoPaciente auditoria = new AuditoriaArchivadoPaciente();
    auditoria.setPacienteArchivado(archivado);
    auditoria.setPacientePrincipal(principal);
    auditoria.setUsuario(usuario);
    auditoria.setEmpleado(empleado);
    auditoria.setCargo(cargo);
    auditoria.setDni(archivado.getNumDocumento().trim());
    auditoria.setMotivo(request.getMotivo().trim());
    auditoria.setDetalle(request.getDetalleMotivo() == null || request.getDetalleMotivo().isBlank()
        ? null : request.getDetalleMotivo().trim());
    auditoria.setEstadoAnterior(EstadoRegistroPaciente.ACTIVO.name());
    auditoria.setEstadoNuevo(EstadoRegistroPaciente.ARCHIVADO.name());
    auditoria.setRequirioRevisionClinica(requiereRevision);
    auditoria.setConfirmoRevisionClinica(Boolean.TRUE.equals(request.getConfirmarRevisionClinica()));
    auditoria.setOrigen(request.getOrigen());
    auditoria.setNombrePacienteArchivado(nombreCompleto(archivado));
    auditoria.setNombrePacientePrincipal(nombreCompleto(principal));
    auditoria.setUsuarioResponsable(Objects.toString(usuario.getUsuario(), ""));
    return auditoria;
  }

  private AuditoriaArchivadoPacienteResponse toResponse(AuditoriaArchivadoPaciente auditoria) {
    return AuditoriaArchivadoPacienteResponse.builder()
        .idAuditoria(auditoria.getIdAuditoria())
        .idPacienteArchivado(auditoria.getPacienteArchivado().getIdPaciente())
        .idPacientePrincipal(auditoria.getPacientePrincipal().getIdPaciente())
        .idUsuario(auditoria.getUsuario().getIdUsuario())
        .idEmpleado(auditoria.getEmpleado().getIdEmpleado())
        .cargo(auditoria.getCargo())
        .dni(auditoria.getDni())
        .motivo(auditoria.getMotivo())
        .detalle(auditoria.getDetalle())
        .estadoAnterior(auditoria.getEstadoAnterior())
        .estadoNuevo(auditoria.getEstadoNuevo())
        .requirioRevisionClinica(auditoria.isRequirioRevisionClinica())
        .confirmoRevisionClinica(auditoria.isConfirmoRevisionClinica())
        .origen(auditoria.getOrigen())
        .fecha(auditoria.getFecha())
        .nombrePacienteArchivado(auditoria.getNombrePacienteArchivado())
        .nombrePacientePrincipal(auditoria.getNombrePacientePrincipal())
        .usuarioResponsable(auditoria.getUsuarioResponsable())
        .build();
  }

  private String normalizarOrigen(String origen) {
    String valor = origen == null || origen.isBlank() ? "API_LOCAL" : origen.trim().toUpperCase(Locale.ROOT);
    if (!ORIGENES_PERMITIDOS.contains(valor)) {
      throw error("ORIGEN_INVALIDO", "El origen debe ser CHATBOT, SWAGGER o API_LOCAL.", HttpStatus.BAD_REQUEST);
    }
    return valor;
  }

  private String normalizarFiltroDni(String dni) {
    if (dni == null || dni.isBlank()) return null;
    String valor = dni.trim();
    if (!DNI_PATTERN.matcher(valor).matches()) {
      throw error("DNI_INVALIDO", "El DNI debe contener exactamente ocho dígitos.", HttpStatus.BAD_REQUEST);
    }
    return valor;
  }

  private String nombreCompleto(Paciente paciente) {
    return (Objects.toString(paciente.getNombres(), "") + " " + Objects.toString(paciente.getApellidos(), ""))
        .replaceAll("\\s+", " ").trim();
  }

  private ArchivadoPacienteDuplicadoException error(String resultado, String mensaje, HttpStatus status) {
    return new ArchivadoPacienteDuplicadoException(resultado, mensaje, status);
  }
}
