package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.exception.ArchivadoPacienteDuplicadoException;
import com.krivi.apihistorialmedico.business.exception.ReautenticacionException;
import com.krivi.apihistorialmedico.business.services.PacienteDuplicadoService;
import com.krivi.apihistorialmedico.business.services.PacienteService;
import com.krivi.apihistorialmedico.business.services.ReautenticacionLocalService;
import com.krivi.apihistorialmedico.model.api.ArchivarPacienteDuplicadoRequest;
import com.krivi.apihistorialmedico.model.api.PacienteDuplicadoComparacionResponse;
import com.krivi.apihistorialmedico.model.api.PacienteDuplicadoDetalleResponse;
import com.krivi.apihistorialmedico.model.api.ReautenticacionResponse;
import com.krivi.apihistorialmedico.model.entity.Antecedentes;
import com.krivi.apihistorialmedico.model.entity.AuditoriaArchivadoPaciente;
import com.krivi.apihistorialmedico.model.entity.Consulta;
import com.krivi.apihistorialmedico.model.entity.Empleado;
import com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente;
import com.krivi.apihistorialmedico.model.entity.Paciente;
import com.krivi.apihistorialmedico.model.entity.Usuario;
import com.krivi.apihistorialmedico.repository.AuditoriaArchivadoPacienteRepository;
import com.krivi.apihistorialmedico.repository.PacienteRepository;
import com.krivi.apihistorialmedico.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArchivadoPacienteDuplicadoServiceImplTest {
  @Mock ReautenticacionLocalService reautenticacionLocalService;
  @Mock PacienteDuplicadoService pacienteDuplicadoService;
  @Mock PacienteService pacienteService;
  @Mock PacienteRepository pacienteRepository;
  @Mock UsuarioRepository usuarioRepository;
  @Mock AuditoriaArchivadoPacienteRepository auditoriaRepository;
  @Mock EntityManager entityManager;
  @InjectMocks ArchivadoPacienteDuplicadoServiceImpl service;

  @Test
  void administradorArchivaYGuardaAuditoriaDespuesDelArchivado() {
    Escenario escenario = prepararExito("ADMINISTRADOR", false, 2);

    var response = service.archivar(7, 1, escenario.request());

    assertTrue(response.isArchivado());
    assertEquals("PACIENTE_ARCHIVADO", response.getResultado());
    assertEquals(90, response.getIdAuditoria());
    InOrder orden = inOrder(pacienteService, entityManager, auditoriaRepository);
    orden.verify(pacienteService).archivarInternamente(1, 2, 7, "PACIENTE_DUPLICADO", "Revisión local");
    orden.verify(entityManager).flush();
    orden.verify(auditoriaRepository).saveAndFlush(any(AuditoriaArchivadoPaciente.class));
  }

  @Test
  void enfermeroYAliasAutorizadoPuedenArchivar() {
    Escenario enfermero = prepararExito("ENFERMERO", false, 2);
    assertTrue(service.archivar(7, 1, enfermero.request()).isArchivado());

    reset(reautenticacionLocalService, pacienteDuplicadoService, pacienteService,
        pacienteRepository, usuarioRepository, auditoriaRepository);
    Escenario alias = prepararExito("ENFERMERO", false, 2);
    assertEquals("ENFERMERO", service.archivar(7, 1, alias.request()).getCargoResponsable());
  }

  @Test
  void doctorYContrasenaIncorrectaSePropaganSinModificarPacientes() {
    when(reautenticacionLocalService.reautenticar(eq(7), any()))
        .thenThrow(new ReautenticacionException("CARGO_NO_AUTORIZADO", "Sin permiso", "DOCTOR", HttpStatus.FORBIDDEN));
    assertEquals("CARGO_NO_AUTORIZADO", assertThrows(ReautenticacionException.class,
        () -> service.archivar(7, 1, request())).getResultado());
    verifyNoInteractions(pacienteService, auditoriaRepository);

    reset(reautenticacionLocalService);
    when(reautenticacionLocalService.reautenticar(eq(7), any()))
        .thenThrow(new ReautenticacionException("CONTRASENA_INCORRECTA", "Incorrecta", "ADMINISTRADOR", HttpStatus.UNAUTHORIZED));
    assertEquals("CONTRASENA_INCORRECTA", assertThrows(ReautenticacionException.class,
        () -> service.archivar(7, 1, request())).getResultado());
    verifyNoInteractions(pacienteService, auditoriaRepository);
  }

  @Test
  void usuarioInexistenteInactivoYEmpleadoInactivoSePropagan() {
    for (String codigo : List.of("USUARIO_NO_ENCONTRADO", "USUARIO_INACTIVO", "EMPLEADO_INACTIVO")) {
      reset(reautenticacionLocalService);
      when(reautenticacionLocalService.reautenticar(eq(7), any()))
          .thenThrow(new ReautenticacionException(codigo, "Rechazado", null, HttpStatus.FORBIDDEN));
      assertEquals(codigo, assertThrows(ReautenticacionException.class,
          () -> service.archivar(7, 1, request())).getResultado());
    }
    verifyNoInteractions(pacienteService, auditoriaRepository);
  }

  @Test
  void rechazaPacienteArchivadoPrincipalArchivadoEIdsIguales() {
    prepararAutorizacion("ADMINISTRADOR");
    Usuario usuario = usuario();
    when(usuarioRepository.findById(7)).thenReturn(Optional.of(usuario));

    Paciente archivado = paciente(1, "12345678", EstadoRegistroPaciente.ARCHIVADO);
    when(pacienteRepository.findById(1)).thenReturn(Optional.of(archivado));
    when(pacienteRepository.findById(2)).thenReturn(Optional.of(paciente(2, "12345678", EstadoRegistroPaciente.ACTIVO)));
    assertCodigo("PACIENTE_YA_ARCHIVADO", request());

    when(pacienteRepository.findById(1)).thenReturn(Optional.of(paciente(1, "12345678", EstadoRegistroPaciente.ACTIVO)));
    when(pacienteRepository.findById(2)).thenReturn(Optional.of(paciente(2, "12345678", EstadoRegistroPaciente.ARCHIVADO)));
    assertCodigo("PACIENTE_PRINCIPAL_ARCHIVADO", request());

    ArchivarPacienteDuplicadoRequest iguales = request();
    iguales.setIdPacientePrincipal(1);
    assertCodigo("PACIENTE_PRINCIPAL_INVALIDO", iguales);
    verifyNoInteractions(auditoriaRepository);
  }

  @Test
  void distinguePacientesInexistentesYRechazaDniDistinto() {
    prepararAutorizacion("ADMINISTRADOR");
    when(usuarioRepository.findById(7)).thenReturn(Optional.of(usuario()));
    when(pacienteRepository.findById(1)).thenReturn(Optional.empty());
    assertCodigo("PACIENTE_NO_ENCONTRADO", request());

    when(pacienteRepository.findById(1)).thenReturn(Optional.of(paciente(1, "12345678", EstadoRegistroPaciente.ACTIVO)));
    when(pacienteRepository.findById(2)).thenReturn(Optional.empty());
    assertCodigo("PACIENTE_PRINCIPAL_NO_ENCONTRADO", request());

    when(pacienteRepository.findById(2)).thenReturn(Optional.of(paciente(2, "87654321", EstadoRegistroPaciente.ACTIVO)));
    assertCodigo("PACIENTES_NO_SON_DUPLICADOS", request());
    verifyNoInteractions(auditoriaRepository);
  }

  @Test
  void exigeMotivoAntesDeConsultarPacientes() {
    prepararAutorizacion("ADMINISTRADOR");
    ArchivarPacienteDuplicadoRequest request = request();
    request.setMotivo("   ");

    assertCodigo("MOTIVO_REQUERIDO", request);

    verifyNoInteractions(pacienteRepository, pacienteService, auditoriaRepository);
  }

  @Test
  void casoClinicoSimpleContinuaSinConfirmacion() {
    Escenario escenario = prepararExito("ADMINISTRADOR", false, 2);
    escenario.request().setConfirmarRevisionClinica(false);

    var response = service.archivar(7, 1, escenario.request());

    assertFalse(response.isRequiereRevisionClinica());
    assertFalse(response.isRevisionClinicaConfirmada());
  }

  @Test
  void dosClinicosExigenConfirmacionYConConfirmacionArchivan() {
    Escenario escenario = prepararExito("ADMINISTRADOR", true, 2);
    escenario.request().setConfirmarRevisionClinica(false);
    assertCodigo("CONFIRMACION_REVISION_REQUERIDA", escenario.request());
    verifyNoInteractions(pacienteService, auditoriaRepository);

    escenario.request().setConfirmarRevisionClinica(true);
    var response = service.archivar(7, 1, escenario.request());
    assertTrue(response.isRequiereRevisionClinica());
    assertTrue(response.isRevisionClinicaConfirmada());
  }

  @Test
  void soportaTresOMasDuplicadosActivos() {
    Escenario escenario = prepararExito("ADMINISTRADOR", false, 3);

    assertTrue(service.archivar(7, 1, escenario.request()).isArchivado());

    verify(pacienteDuplicadoService).compararPorDni("12345678");
  }

  @Test
  void conservaPrincipalYRelacionesClinicasYSoloUsaArchivadoLogico() {
    Escenario escenario = prepararExito("ADMINISTRADOR", false, 2);
    List<Consulta> consultas = new ArrayList<>();
    List<Antecedentes> antecedentes = new ArrayList<>();
    escenario.archivado().setConsultas(consultas);
    escenario.archivado().setAntecedentes(antecedentes);

    service.archivar(7, 1, escenario.request());

    assertEquals(EstadoRegistroPaciente.ACTIVO, escenario.principal().getEstadoRegistro());
    assertSame(consultas, escenario.archivado().getConsultas());
    assertSame(antecedentes, escenario.archivado().getAntecedentes());
    verify(pacienteRepository, never()).delete(any());
    verify(pacienteRepository, never()).deleteById(anyInt());
  }

  @Test
  void auditoriaContieneResponsableCargoMotivoEstadosYSinContrasena() {
    Escenario escenario = prepararExito("ADMINISTRADOR", true, 2);
    escenario.request().setConfirmarRevisionClinica(true);
    ArgumentCaptor<AuditoriaArchivadoPaciente> captor = ArgumentCaptor.forClass(AuditoriaArchivadoPaciente.class);

    service.archivar(7, 1, escenario.request());

    verify(auditoriaRepository).saveAndFlush(captor.capture());
    AuditoriaArchivadoPaciente auditoria = captor.getValue();
    assertEquals(7, auditoria.getUsuario().getIdUsuario());
    assertEquals(3, auditoria.getEmpleado().getIdEmpleado());
    assertEquals("ADMINISTRADOR", auditoria.getCargo());
    assertEquals("PACIENTE_DUPLICADO", auditoria.getMotivo());
    assertEquals("ACTIVO", auditoria.getEstadoAnterior());
    assertEquals("ARCHIVADO", auditoria.getEstadoNuevo());
    assertTrue(auditoria.isRequirioRevisionClinica());
    assertTrue(auditoria.isConfirmoRevisionClinica());
    assertFalse(java.util.Arrays.stream(AuditoriaArchivadoPaciente.class.getDeclaredFields())
        .anyMatch(field -> field.getName().toLowerCase().contains("contrasena")));
  }

  @Test
  void falloAuditoriaLanzaErrorTransaccionalYNoSeOculta() throws Exception {
    prepararExito("ADMINISTRADOR", false, 2);
    doThrow(new RuntimeException("Error de auditoría"))
        .when(auditoriaRepository)
        .saveAndFlush(any(AuditoriaArchivadoPaciente.class));

    ArchivadoPacienteDuplicadoException error = assertThrows(ArchivadoPacienteDuplicadoException.class,
        () -> service.archivar(7, 1, request()));

    assertEquals("AUDITORIA_FALLIDA", error.getResultado());
    assertNotNull(error.getCause());
    assertEquals("Error de auditoría", error.getCause().getMessage());
    verify(auditoriaRepository).saveAndFlush(any(AuditoriaArchivadoPaciente.class));
    assertNotNull(ArchivadoPacienteDuplicadoServiceImpl.class
        .getMethod("archivar", Integer.class, Integer.class, ArchivarPacienteDuplicadoRequest.class)
        .getAnnotation(Transactional.class));
  }

  @Test
  void falloArchivadoNoGeneraAuditoriaYConflictoVersionSePropaga() {
    prepararExito("ADMINISTRADOR", false, 2);
    when(pacienteService.archivarInternamente(anyInt(), anyInt(), anyInt(), anyString(), anyString()))
        .thenThrow(new RuntimeException("fallo"));
    assertCodigo("ARCHIVADO_FALLIDO", request());
    verifyNoInteractions(auditoriaRepository);

    reset(pacienteService);
    when(pacienteService.archivarInternamente(anyInt(), anyInt(), anyInt(), anyString(), anyString()))
        .thenThrow(new OptimisticLockingFailureException("version"));
    assertThrows(OptimisticLockingFailureException.class, () -> service.archivar(7, 1, request()));
    verifyNoInteractions(auditoriaRepository);
  }

  @Test
  void segundoArchivadoNoCreaAuditoriaDuplicada() {
    prepararAutorizacion("ADMINISTRADOR");
    when(usuarioRepository.findById(7)).thenReturn(Optional.of(usuario()));
    when(pacienteRepository.findById(1)).thenReturn(Optional.of(paciente(1, "12345678", EstadoRegistroPaciente.ARCHIVADO)));
    when(pacienteRepository.findById(2)).thenReturn(Optional.of(paciente(2, "12345678", EstadoRegistroPaciente.ACTIVO)));

    assertCodigo("PACIENTE_YA_ARCHIVADO", request());

    verifyNoInteractions(auditoriaRepository);
  }

  @Test
  void consultaAuditoriaSoloPermiteAdministradorYNoModificaDatos() {
    when(auditoriaRepository.buscar(null, null, null, null)).thenReturn(List.of());
    assertTrue(service.consultarAuditoria(7, null, null, null, null).isEmpty());
    verify(reautenticacionLocalService).validarAdministrador(7);
    verify(auditoriaRepository).buscar(null, null, null, null);
    verify(auditoriaRepository, never()).save(any());

    reset(reautenticacionLocalService, auditoriaRepository);
    when(reautenticacionLocalService.validarAdministrador(8))
        .thenThrow(new ReautenticacionException("CARGO_NO_AUTORIZADO", "Solo administrador", "ENFERMERO", HttpStatus.FORBIDDEN));
    assertThrows(ReautenticacionException.class,
        () -> service.consultarAuditoria(8, null, null, null, null));
    verifyNoInteractions(auditoriaRepository);
  }

  private Escenario prepararExito(String cargo, boolean requiereRevision, int cantidadDuplicados) {
    prepararAutorizacion(cargo);
    Usuario usuario = usuario();
    Paciente archivado = paciente(1, "12345678", EstadoRegistroPaciente.ACTIVO);
    Paciente principal = paciente(2, "12345678", EstadoRegistroPaciente.ACTIVO);
    lenient().when(usuarioRepository.findById(7)).thenReturn(Optional.of(usuario));
    lenient().when(pacienteRepository.findById(1)).thenReturn(Optional.of(archivado));
    lenient().when(pacienteRepository.findById(2)).thenReturn(Optional.of(principal));
    List<PacienteDuplicadoDetalleResponse> detalles = new ArrayList<>();
    for (int id = 1; id <= cantidadDuplicados; id++) {
      detalles.add(PacienteDuplicadoDetalleResponse.builder().idPaciente(id).build());
    }
    lenient().when(pacienteDuplicadoService.compararPorDni("12345678"))
        .thenReturn(PacienteDuplicadoComparacionResponse.builder()
            .dni("12345678").esDuplicado(true).pacientes(detalles)
            .requiereRevision(requiereRevision).permitirArchivadoSimple(!requiereRevision).build());
    lenient().when(pacienteService.archivarInternamente(1, 2, 7, "PACIENTE_DUPLICADO", "Revisión local"))
        .thenAnswer(invocation -> {
          archivado.setEstadoRegistro(EstadoRegistroPaciente.ARCHIVADO);
          archivado.setPacientePrincipal(principal);
          return archivado;
        });
    lenient().when(auditoriaRepository.saveAndFlush(any(AuditoriaArchivadoPaciente.class))).thenAnswer(invocation -> {
      AuditoriaArchivadoPaciente auditoria = invocation.getArgument(0);
      auditoria.setIdAuditoria(90);
      return auditoria;
    });
    return new Escenario(request(), archivado, principal);
  }

  private void prepararAutorizacion(String cargo) {
    when(reautenticacionLocalService.reautenticar(eq(7), any()))
        .thenReturn(ReautenticacionResponse.builder().autorizado(true).cargo(cargo).build());
  }

  private void assertCodigo(String codigo, ArchivarPacienteDuplicadoRequest request) {
    assertEquals(codigo, assertThrows(ArchivadoPacienteDuplicadoException.class,
        () -> service.archivar(7, 1, request)).getResultado());
  }

  private ArchivarPacienteDuplicadoRequest request() {
    ArchivarPacienteDuplicadoRequest request = new ArchivarPacienteDuplicadoRequest();
    request.setIdPacientePrincipal(2);
    request.setMotivo("PACIENTE_DUPLICADO");
    request.setDetalleMotivo("Revisión local");
    request.setContrasena("clave-local");
    request.setConfirmarRevisionClinica(false);
    request.setOrigen("SWAGGER");
    return request;
  }

  private Usuario usuario() {
    Empleado empleado = new Empleado();
    empleado.setIdEmpleado(3);
    empleado.setEstado(true);
    empleado.setCargo("Administrador");
    Usuario usuario = new Usuario();
    usuario.setIdUsuario(7);
    usuario.setUsuario("admin");
    usuario.setEstado(true);
    usuario.setEmpleado(empleado);
    return usuario;
  }

  private Paciente paciente(int id, String dni, EstadoRegistroPaciente estado) {
    Paciente paciente = new Paciente();
    paciente.setIdPaciente(id);
    paciente.setNombres("Paciente");
    paciente.setApellidos(String.valueOf(id));
    paciente.setNumDocumento(dni);
    paciente.setEstadoRegistro(estado);
    paciente.setVersion(0L);
    return paciente;
  }

  private record Escenario(ArchivarPacienteDuplicadoRequest request, Paciente archivado, Paciente principal) {
  }
}
