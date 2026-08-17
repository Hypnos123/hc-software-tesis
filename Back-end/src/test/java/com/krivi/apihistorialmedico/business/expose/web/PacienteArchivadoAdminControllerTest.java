package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.exception.ConsultaPacienteArchivadoException;
import com.krivi.apihistorialmedico.business.exception.ReautenticacionException;
import com.krivi.apihistorialmedico.business.services.PacienteArchivadoAdminService;
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

class PacienteArchivadoAdminControllerTest {
  private PacienteArchivadoAdminService service;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    service = mock(PacienteArchivadoAdminService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new PacienteArchivadoAdminController(service)).build();
  }

  @Test
  void administradorObtieneListadoPaginadoConFiltros() throws Exception {
    when(service.listar(eq(7), eq(1), eq(25), eq("fechaArchivado,desc"), eq("Rosa"), eq("12345678"),
        eq(13), any(), any())).thenReturn(PaginaResponse.<PacienteArchivadoResumenResponse>builder()
        .content(List.of(PacienteArchivadoResumenResponse.builder().idPaciente(13).estadoRegistro("ARCHIVADO").build()))
        .page(1).size(25).totalElements(26).totalPages(2).build());

    mockMvc.perform(get("/api/admin/pacientes-archivados").header("X-Usuario-Id", "7")
            .param("page", "1").param("size", "25").param("sort", "fechaArchivado,desc")
            .param("search", "Rosa").param("dni", "12345678").param("idPaciente", "13")
            .param("desde", "2026-08-01T00:00:00").param("hasta", "2026-08-31T23:59:59"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].idPaciente").value(13))
        .andExpect(jsonPath("$.content[0].estadoRegistro").value("ARCHIVADO"))
        .andExpect(jsonPath("$.page").value(1)).andExpect(jsonPath("$.totalPages").value(2));
  }

  @Test
  void obtieneDetalle() throws Exception {
    when(service.obtenerDetalle(7, 13)).thenReturn(PacienteArchivadoDetalleResponse.builder()
        .idPaciente(13).estadoRegistro("ARCHIVADO").cantidadConsultas(2).build());
    mockMvc.perform(get("/api/admin/pacientes-archivados/13").header("X-Usuario-Id", "7"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.idPaciente").value(13))
        .andExpect(jsonPath("$.cantidadConsultas").value(2));
  }

  @Test
  void pacienteArchivadoInexistenteDevuelve404() throws Exception {
    when(service.obtenerDetalle(7, 999)).thenThrow(new ConsultaPacienteArchivadoException(
        "PACIENTE_ARCHIVADO_NO_ENCONTRADO", "El paciente archivado no existe.", HttpStatus.NOT_FOUND));
    mockMvc.perform(get("/api/admin/pacientes-archivados/999").header("X-Usuario-Id", "7"))
        .andExpect(status().isNotFound()).andExpect(jsonPath("$.resultado").value("PACIENTE_ARCHIVADO_NO_ENCONTRADO"));
  }

  @Test
  void doctorRecibe403() throws Exception { comprobarCargoNoAutorizado(8, "DOCTOR"); }

  @Test
  void enfermeroRecibe403() throws Exception { comprobarCargoNoAutorizado(9, "ENFERMERO"); }

  @Test
  void usuarioNoIndicadoConservaEstadoDelMecanismoActual() throws Exception {
    when(service.listar(isNull(), anyInt(), anyInt(), any(), any(), any(), any(), any(), any()))
        .thenThrow(new ReautenticacionException("USUARIO_REQUERIDO", "Debe indicar el usuario actual.", null, HttpStatus.BAD_REQUEST));
    mockMvc.perform(get("/api/admin/pacientes-archivados"))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.resultado").value("USUARIO_REQUERIDO"));
  }

  @Test
  void usuarioInexistenteConserva404DelMecanismoActual() throws Exception {
    when(service.listar(eq(999), anyInt(), anyInt(), any(), any(), any(), any(), any(), any()))
        .thenThrow(new ReautenticacionException("USUARIO_NO_ENCONTRADO", "El usuario actual no existe.", null, HttpStatus.NOT_FOUND));
    mockMvc.perform(get("/api/admin/pacientes-archivados").header("X-Usuario-Id", "999"))
        .andExpect(status().isNotFound()).andExpect(jsonPath("$.resultado").value("USUARIO_NO_ENCONTRADO"));
  }

  private void comprobarCargoNoAutorizado(int idUsuario, String cargo) throws Exception {
    when(service.listar(eq(idUsuario), anyInt(), anyInt(), any(), any(), any(), any(), any(), any()))
        .thenThrow(new ReautenticacionException("CARGO_NO_AUTORIZADO", "Solo Administrador.", cargo, HttpStatus.FORBIDDEN));
    mockMvc.perform(get("/api/admin/pacientes-archivados").header("X-Usuario-Id", idUsuario))
        .andExpect(status().isForbidden()).andExpect(jsonPath("$.resultado").value("CARGO_NO_AUTORIZADO"));
  }
}
