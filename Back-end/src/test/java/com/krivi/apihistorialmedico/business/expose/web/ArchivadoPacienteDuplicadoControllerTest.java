package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.exception.ArchivadoPacienteDuplicadoException;
import com.krivi.apihistorialmedico.business.exception.ReautenticacionException;
import com.krivi.apihistorialmedico.business.services.ArchivadoPacienteDuplicadoService;
import com.krivi.apihistorialmedico.model.api.ArchivarPacienteDuplicadoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ArchivadoPacienteDuplicadoControllerTest {
  private ArchivadoPacienteDuplicadoService service;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    service = mock(ArchivadoPacienteDuplicadoService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new ArchivadoPacienteDuplicadoController(service)).build();
  }

  @Test
  void archivaConHeaderYBodyPermitido() throws Exception {
    when(service.archivar(eq(7), eq(13), any())).thenReturn(ArchivarPacienteDuplicadoResponse.builder()
        .archivado(true).idPacienteArchivado(13).idPacientePrincipal(10)
        .resultado("PACIENTE_ARCHIVADO").build());

    mockMvc.perform(post("/api/pacientes/13/archivar-duplicado")
            .header("X-Usuario-Id", "7")
            .contentType("application/json")
            .content("""
                {"idPacientePrincipal":10,"motivo":"PACIENTE_DUPLICADO",
                 "detalleMotivo":"Registro repetido","contrasena":"temporal",
                 "confirmarRevisionClinica":true,"origen":"SWAGGER"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.archivado").value(true))
        .andExpect(jsonPath("$.resultado").value("PACIENTE_ARCHIVADO"))
        .andExpect(jsonPath("$.contrasena").doesNotExist());
  }

  @Test
  void noAceptaIdUsuarioOCargoEnElBody() throws Exception {
    mockMvc.perform(post("/api/pacientes/13/archivar-duplicado")
            .header("X-Usuario-Id", "7")
            .contentType("application/json")
            .content("""
                {"idPacientePrincipal":10,"motivo":"PACIENTE_DUPLICADO",
                 "contrasena":"temporal","idUsuario":99,"cargo":"ADMINISTRADOR"}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.resultado").value("CUERPO_INVALIDO"));
    verifyNoInteractions(service);
  }

  @Test
  void devuelveCodigosDeReautenticacionYArchivado() throws Exception {
    when(service.archivar(eq(7), eq(13), any())).thenThrow(
        new ReautenticacionException("CARGO_NO_AUTORIZADO", "Sin permiso", "DOCTOR", HttpStatus.FORBIDDEN));
    mockMvc.perform(post("/api/pacientes/13/archivar-duplicado")
            .header("X-Usuario-Id", "7").contentType("application/json")
            .content("{\"idPacientePrincipal\":10,\"motivo\":\"DUPLICADO\",\"contrasena\":\"x\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.resultado").value("CARGO_NO_AUTORIZADO"));

    reset(service);
    when(service.archivar(eq(7), eq(13), any())).thenThrow(
        new ArchivadoPacienteDuplicadoException("CONFIRMACION_REVISION_REQUERIDA", "Confirme", HttpStatus.CONFLICT));
    mockMvc.perform(post("/api/pacientes/13/archivar-duplicado")
            .header("X-Usuario-Id", "7").contentType("application/json")
            .content("{\"idPacientePrincipal\":10,\"motivo\":\"DUPLICADO\",\"contrasena\":\"x\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.resultado").value("CONFIRMACION_REVISION_REQUERIDA"));
  }

  @Test
  void conflictoOptimistaDevuelveHttp409Controlado() throws Exception {
    when(service.archivar(eq(7), eq(13), any())).thenThrow(new OptimisticLockingFailureException("version"));

    mockMvc.perform(post("/api/pacientes/13/archivar-duplicado")
            .header("X-Usuario-Id", "7").contentType("application/json")
            .content("{\"idPacientePrincipal\":10,\"motivo\":\"DUPLICADO\",\"contrasena\":\"x\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.resultado").value("CONFLICTO_VERSION"));
  }

  @Test
  void consultaAuditoriaConUsuarioActualYFiltros() throws Exception {
    when(service.consultarAuditoria(eq(7), eq("12345678"), eq(13), isNull(), isNull()))
        .thenReturn(List.of());

    mockMvc.perform(get("/api/auditoria/pacientes-archivados")
            .header("X-Usuario-Id", "7")
            .param("dni", "12345678").param("idPaciente", "13"))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
  }
}
