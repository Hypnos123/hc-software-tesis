package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.exception.ConsultaFusionAuditoriaException;
import com.krivi.apihistorialmedico.business.exception.ReautenticacionException;
import com.krivi.apihistorialmedico.business.services.AuditoriaFusionHistoriaClinicaAdminService;
import com.krivi.apihistorialmedico.model.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuditoriaFusionHistoriaClinicaAdminControllerTest {
  private AuditoriaFusionHistoriaClinicaAdminService service;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    service = mock(AuditoriaFusionHistoriaClinicaAdminService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new AuditoriaFusionHistoriaClinicaAdminController(service)).build();
  }

  @Test
  void administradorListaFusionesConPaginacionYFiltros() throws Exception {
    when(service.listar(eq(7), eq(1), eq(25), eq("fecha,desc"), eq("Ana"), eq("12345678"), eq(4),
        eq(19), eq(16), isNull(), isNull(), any(), any())).thenReturn(PaginaResponse.<FusionHistoriaClinicaAuditoriaResumenResponse>builder()
        .content(List.of(FusionHistoriaClinicaAuditoriaResumenResponse.builder().idAuditoria(3).consultasTransferidas(1).build()))
        .page(1).size(25).totalElements(26).totalPages(2).build());
    mockMvc.perform(get("/api/admin/auditoria/fusiones-historias-clinicas").header("X-Usuario-Id", "7")
            .param("page", "1").param("size", "25").param("sort", "fecha,desc").param("search", "Ana")
            .param("dni", "12345678").param("idPaciente", "4").param("idHistoriaPrincipal", "19")
            .param("idHistoriaEliminada", "16").param("desde", "2026-08-01T00:00:00").param("hasta", "2026-08-31T23:59:59"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].idAuditoria").value(3))
        .andExpect(jsonPath("$.content[0].consultasTransferidas").value(1)).andExpect(jsonPath("$.totalPages").value(2));
  }

  @Test
  void obtieneDetalleExistente() throws Exception {
    when(service.obtenerDetalle(7, 3)).thenReturn(FusionHistoriaClinicaAuditoriaDetalleResponse.builder()
        .idAuditoria(3).idHistoriaPrincipal(19).idHistoriaEliminada(16).consultasTransferidas(1).build());
    mockMvc.perform(get("/api/admin/auditoria/fusiones-historias-clinicas/3").header("X-Usuario-Id", "7"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.idHistoriaPrincipal").value(19))
        .andExpect(jsonPath("$.idHistoriaEliminada").value(16));
  }

  @Test
  void auditoriaInexistenteDevuelve404() throws Exception {
    when(service.obtenerDetalle(7, 999)).thenThrow(new ConsultaFusionAuditoriaException(
        "AUDITORIA_FUSION_NO_ENCONTRADA", "No existe.", HttpStatus.NOT_FOUND));
    mockMvc.perform(get("/api/admin/auditoria/fusiones-historias-clinicas/999").header("X-Usuario-Id", "7"))
        .andExpect(status().isNotFound()).andExpect(jsonPath("$.resultado").value("AUDITORIA_FUSION_NO_ENCONTRADA"));
  }

  @Test void doctorRecibe403() throws Exception { comprobarNoAutorizado(8, "DOCTOR"); }
  @Test void enfermeroRecibe403() throws Exception { comprobarNoAutorizado(9, "ENFERMERO"); }

  @Test
  void ausenciaDeUsuarioRecibe400() throws Exception {
    when(service.listar(isNull(), anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenThrow(new ReautenticacionException("USUARIO_REQUERIDO", "Debe indicar usuario.", null, HttpStatus.BAD_REQUEST));
    mockMvc.perform(get("/api/admin/auditoria/fusiones-historias-clinicas"))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.resultado").value("USUARIO_REQUERIDO"));
  }

  @Test
  void usuarioInexistenteRecibe404() throws Exception {
    when(service.listar(eq(999), anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenThrow(new ReautenticacionException("USUARIO_NO_ENCONTRADO", "No existe.", null, HttpStatus.NOT_FOUND));
    mockMvc.perform(get("/api/admin/auditoria/fusiones-historias-clinicas").header("X-Usuario-Id", "999"))
        .andExpect(status().isNotFound()).andExpect(jsonPath("$.resultado").value("USUARIO_NO_ENCONTRADO"));
  }

  private void comprobarNoAutorizado(int id, String cargo) throws Exception {
    when(service.listar(eq(id), anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenThrow(new ReautenticacionException("CARGO_NO_AUTORIZADO", "Solo Administrador.", cargo, HttpStatus.FORBIDDEN));
    mockMvc.perform(get("/api/admin/auditoria/fusiones-historias-clinicas").header("X-Usuario-Id", id))
        .andExpect(status().isForbidden()).andExpect(jsonPath("$.resultado").value("CARGO_NO_AUTORIZADO"));
  }
}
