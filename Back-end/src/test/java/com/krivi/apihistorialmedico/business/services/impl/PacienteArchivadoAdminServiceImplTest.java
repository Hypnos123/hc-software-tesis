package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.exception.ConsultaPacienteArchivadoException;
import com.krivi.apihistorialmedico.business.services.ReautenticacionLocalService;
import com.krivi.apihistorialmedico.model.api.*;
import com.krivi.apihistorialmedico.model.entity.*;
import com.krivi.apihistorialmedico.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PacienteArchivadoAdminServiceImplTest {
  private ReautenticacionLocalService reautenticacion;
  private PacienteRepository pacientes;
  private AuditoriaArchivadoPacienteRepository auditorias;
  private HistoriaClinicaRepository historias;
  private ConsultaRepository consultas;
  private AntecedentesRepository antecedentes;
  private PacienteArchivadoAdminServiceImpl service;

  @BeforeEach
  void setUp() {
    reautenticacion = mock(ReautenticacionLocalService.class); pacientes = mock(PacienteRepository.class);
    auditorias = mock(AuditoriaArchivadoPacienteRepository.class); historias = mock(HistoriaClinicaRepository.class);
    consultas = mock(ConsultaRepository.class); antecedentes = mock(AntecedentesRepository.class);
    service = new PacienteArchivadoAdminServiceImpl(reautenticacion, pacientes, auditorias, historias, consultas, antecedentes);
  }

  @Test
  void listaSoloLaPaginaArchivadaDevueltaPorRepositorioYEnriqueceConUltimaAuditoria() {
    Paciente principal = paciente(2, EstadoRegistroPaciente.ACTIVO, "Ana", "Principal", "12345678");
    Paciente archivado = paciente(5, EstadoRegistroPaciente.ARCHIVADO, "Ana", "Duplicada", "12345678");
    archivado.setFechaArchivado(LocalDateTime.of(2026, 8, 10, 12, 0)); archivado.setPacientePrincipal(principal);
    archivado.setMotivoArchivado("DUPLICADO");
    when(pacientes.buscarArchivados(any(), any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(archivado), PageRequest.of(0, 10), 1));
    AuditoriaArchivadoPaciente auditoria = new AuditoriaArchivadoPaciente(); auditoria.setIdAuditoria(11);
    auditoria.setPacienteArchivado(archivado); auditoria.setUsuarioResponsable("admin"); auditoria.setNombrePacientePrincipal("Ana Principal");
    when(auditorias.buscarUltimasPorPacientes(List.of(5))).thenReturn(List.of(auditoria));

    PaginaResponse<PacienteArchivadoResumenResponse> resultado = service.listar(7, 0, 10, null, null, null, null, null, null);

    verify(reautenticacion).validarAdministrador(7);
    assertEquals(1, resultado.getTotalElements()); assertEquals("ARCHIVADO", resultado.getContent().get(0).getEstadoRegistro());
    assertEquals("admin", resultado.getContent().get(0).getUsuarioResponsable()); assertEquals(11, resultado.getContent().get(0).getIdAuditoria());
  }

  @Test
  void paginaYOrdenPredeterminadoSonCorrectos() {
    when(pacientes.buscarArchivados(any(), any(), any(), any(), any(), any())).thenReturn(Page.empty(PageRequest.of(2, 25)));
    service.listar(7, 2, 25, null, null, null, null, null, null);
    ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
    verify(pacientes).buscarArchivados(isNull(), isNull(), isNull(), isNull(), isNull(), captor.capture());
    Pageable pageable = captor.getValue();
    assertEquals(2, pageable.getPageNumber()); assertEquals(25, pageable.getPageSize());
    assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor("fechaArchivado").getDirection());
    assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor("idPaciente").getDirection());
  }

  @Test
  void transmiteBusquedaDniIdYFechasAlRepositorio() {
    LocalDateTime desde = LocalDateTime.of(2026, 8, 1, 0, 0), hasta = LocalDateTime.of(2026, 8, 31, 23, 59);
    when(pacientes.buscarArchivados(any(), any(), any(), any(), any(), any())).thenReturn(Page.empty());
    service.listar(7, 0, 10, null, " Rosa ", "12345678", 5, desde, hasta);
    verify(pacientes).buscarArchivados(eq("Rosa"), eq("12345678"), eq(5), eq(desde), eq(hasta), any());
  }

  @Test
  void detalleIncluyeAuditoriaPrincipalYConteos() {
    Paciente principal = paciente(2, EstadoRegistroPaciente.ACTIVO, "Ana", "Principal", "12345678");
    Paciente archivado = paciente(5, EstadoRegistroPaciente.ARCHIVADO, "Ana", "Duplicada", "12345678"); archivado.setPacientePrincipal(principal);
    when(pacientes.findByIdPacienteAndEstadoRegistro(5, EstadoRegistroPaciente.ARCHIVADO)).thenReturn(Optional.of(archivado));
    AuditoriaArchivadoPaciente auditoria = new AuditoriaArchivadoPaciente(); auditoria.setIdAuditoria(12); auditoria.setUsuarioResponsable("admin");
    auditoria.setCargo("ADMINISTRADOR"); auditoria.setOrigen("CHATBOT");
    when(auditorias.buscarPorPacienteMasReciente(eq(5), any())).thenReturn(List.of(auditoria));
    when(historias.countByPacienteIdPaciente(5)).thenReturn(2L); when(consultas.countByPacienteIdPaciente(5)).thenReturn(4L);
    when(antecedentes.countByPacienteIdPaciente(5)).thenReturn(1L);
    PacienteArchivadoDetalleResponse detalle = service.obtenerDetalle(7, 5);
    assertEquals(2, detalle.getCantidadHistoriasClinicas()); assertEquals(4, detalle.getCantidadConsultas());
    assertEquals(2, detalle.getPacientePrincipal().getIdPaciente()); assertEquals("admin", detalle.getUsuarioResponsable());
  }

  @Test
  void pacienteActivoONoExistenteNoSeExponeComoArchivado() {
    when(pacientes.findByIdPacienteAndEstadoRegistro(5, EstadoRegistroPaciente.ARCHIVADO)).thenReturn(Optional.empty());
    ConsultaPacienteArchivadoException error = assertThrows(ConsultaPacienteArchivadoException.class, () -> service.obtenerDetalle(7, 5));
    assertEquals("PACIENTE_ARCHIVADO_NO_ENCONTRADO", error.getResultado());
  }

  @Test
  void rechazaRangoDeFechasInvalido() {
    assertThrows(ConsultaPacienteArchivadoException.class, () -> service.listar(7, 0, 10, null, null, null, null,
        LocalDateTime.of(2026, 9, 1, 0, 0), LocalDateTime.of(2026, 8, 1, 0, 0)));
    verifyNoInteractions(pacientes);
  }

  private Paciente paciente(int id, EstadoRegistroPaciente estado, String nombres, String apellidos, String dni) {
    Paciente p = new Paciente(); p.setIdPaciente(id); p.setEstadoRegistro(estado); p.setNombres(nombres); p.setApellidos(apellidos); p.setNumDocumento(dni); return p;
  }
}
