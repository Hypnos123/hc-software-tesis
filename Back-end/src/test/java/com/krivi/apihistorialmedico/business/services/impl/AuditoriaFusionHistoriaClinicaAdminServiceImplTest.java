package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.exception.ConsultaFusionAuditoriaException;
import com.krivi.apihistorialmedico.business.services.ReautenticacionLocalService;
import com.krivi.apihistorialmedico.model.api.*;
import com.krivi.apihistorialmedico.model.entity.*;
import com.krivi.apihistorialmedico.repository.AuditoriaFusionHistoriaClinicaRepository;
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

class AuditoriaFusionHistoriaClinicaAdminServiceImplTest {
  private ReautenticacionLocalService reautenticacion;
  private AuditoriaFusionHistoriaClinicaRepository repository;
  private AuditoriaFusionHistoriaClinicaAdminServiceImpl service;

  @BeforeEach
  void setUp() {
    reautenticacion = mock(ReautenticacionLocalService.class); repository = mock(AuditoriaFusionHistoriaClinicaRepository.class);
    service = new AuditoriaFusionHistoriaClinicaAdminServiceImpl(reautenticacion, repository);
  }

  @Test
  void paginaOrdenaPorFechaEIdDescendenteYMapeaResumen() {
    AuditoriaFusionHistoriaClinica auditoria = auditoria(3, 19, 16, 1);
    when(repository.buscar(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(new PageImpl<>(List.of(auditoria), PageRequest.of(0, 10), 1));
    PaginaResponse<FusionHistoriaClinicaAuditoriaResumenResponse> pagina = service.listar(7, 0, 10, null,
        null, null, null, null, null, null, null, null, null);
    verify(reautenticacion).validarAdministrador(7);
    assertEquals(1, pagina.getContent().get(0).getConsultasTransferidas());
    ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
    verify(repository).buscar(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), captor.capture());
    assertEquals(Sort.Direction.DESC, captor.getValue().getSort().getOrderFor("fecha").getDirection());
    assertEquals(Sort.Direction.DESC, captor.getValue().getSort().getOrderFor("idAuditoria").getDirection());
  }

  @Test
  void transmiteTodosLosFiltrosAlRepositorio() {
    LocalDateTime desde = LocalDateTime.of(2026, 8, 1, 0, 0), hasta = LocalDateTime.of(2026, 8, 31, 23, 59);
    when(repository.buscar(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(Page.empty());
    service.listar(7, 0, 10, null, " Ana ", "12345678", 4, 19, 16, 7, "HISTORIAS_FUSIONADAS", desde, hasta);
    verify(repository).buscar(eq("Ana"), eq("12345678"), eq(4), eq(19), eq(16), eq(7),
        eq("HISTORIAS_FUSIONADAS"), eq(desde), eq(hasta), any());
  }

  @Test
  void detalleConservaConteosYGeneraNarrativaSingular() {
    when(repository.buscarDetalle(3)).thenReturn(Optional.of(auditoria(3, 19, 16, 1)));
    FusionHistoriaClinicaAuditoriaDetalleResponse detalle = service.obtenerDetalle(7, 3);
    assertEquals(1, detalle.getConsultasTransferidas());
    assertEquals("La HC 16 fue fusionada en la HC 19. Se conservó la HC 19 y se transfirió 1 consulta desde la HC 16.", detalle.getExplicacion());
  }

  @Test
  void narrativaUsaPluralParaVariasConsultas() {
    when(repository.buscarDetalle(3)).thenReturn(Optional.of(auditoria(3, 19, 16, 2)));
    assertTrue(service.obtenerDetalle(7, 3).getExplicacion().contains("2 consultas"));
  }

  @Test
  void auditoriaInexistenteNoSeInventa() {
    when(repository.buscarDetalle(99)).thenReturn(Optional.empty());
    ConsultaFusionAuditoriaException error = assertThrows(ConsultaFusionAuditoriaException.class, () -> service.obtenerDetalle(7, 99));
    assertEquals("AUDITORIA_FUSION_NO_ENCONTRADA", error.getResultado());
  }

  @Test
  void rechazaRangoInvalido() {
    assertThrows(ConsultaFusionAuditoriaException.class, () -> service.listar(7, 0, 10, null, null, null,
        null, null, null, null, null, LocalDateTime.of(2026, 9, 1, 0, 0), LocalDateTime.of(2026, 8, 1, 0, 0)));
    verifyNoInteractions(repository);
  }

  private AuditoriaFusionHistoriaClinica auditoria(int id, int principalId, int eliminadaId, long transferidas) {
    Paciente p = new Paciente(); p.setIdPaciente(4); p.setNombres("Ana"); p.setApellidos("Paciente"); p.setNumDocumento("12345678");
    HistoriaClinica h = new HistoriaClinica(); h.setIdHistoriaClinica(principalId); h.setPaciente(p);
    Usuario u = new Usuario(); u.setIdUsuario(7); u.setUsuario("admin");
    Empleado e = new Empleado(); e.setIdEmpleado(6); e.setNombres("Ada"); e.setApellidos("Admin");
    AuditoriaFusionHistoriaClinica a = new AuditoriaFusionHistoriaClinica(); a.setIdAuditoria(id); a.setHistoriaPrincipal(h);
    a.setIdHistoriaEliminada(eliminadaId); a.setPaciente(p); a.setUsuario(u); a.setEmpleado(e); a.setCargo("ADMINISTRADOR");
    a.setOrigen("CHATBOT"); a.setMotivo("DUPLICIDAD"); a.setConsultasAntesPrincipal(2); a.setConsultasAntesSecundaria(transferidas);
    a.setConsultasTransferidas(transferidas); a.setConsultasDespuesPrincipal(2 + transferidas); a.setResultado("HISTORIAS_FUSIONADAS");
    a.setFecha(LocalDateTime.of(2026, 8, 14, 10, 0)); return a;
  }
}
