package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.exception.ReautenticacionException;
import com.krivi.apihistorialmedico.business.services.ReautenticacionLocalService;
import com.krivi.apihistorialmedico.model.api.ReautenticacionRequest;
import com.krivi.apihistorialmedico.model.api.ReautenticacionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReautenticacionLocalControllerTest {
  private ReautenticacionLocalService service;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    service = mock(ReautenticacionLocalService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new ReautenticacionLocalController(service)).build();
  }

  @Test
  void usaElUsuarioDelHeaderYDevuelveAutorizacionSinContrasena() throws Exception {
    when(service.reautenticar(eq(7), any(ReautenticacionRequest.class))).thenReturn(ReautenticacionResponse.builder()
        .autorizado(true).cargo("ADMINISTRADOR").puedeArchivarPacientes(true)
        .resultado("AUTORIZADO").mensaje("Identidad validada correctamente.").build());

    String body = "{\"contrasena\":\"clave-local\"}";
    mockMvc.perform(post("/api/usuarios/reautenticar")
            .header("X-Usuario-Id", "7").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.autorizado").value(true))
        .andExpect(jsonPath("$.cargo").value("ADMINISTRADOR"))
        .andExpect(jsonPath("$.puedeArchivarPacientes").value(true))
        .andExpect(content().string(not(containsString("clave-local"))));
    verify(service).reautenticar(eq(7), any(ReautenticacionRequest.class));
  }

  @Test
  void devuelveContrasenaIncorrectaSinRevelarSecretos() throws Exception {
    when(service.reautenticar(eq(7), any(ReautenticacionRequest.class))).thenThrow(new ReautenticacionException(
        "CONTRASENA_INCORRECTA", "La contraseña ingresada no es correcta.", "ENFERMERO", HttpStatus.UNAUTHORIZED));

    String incorrecta = "secreto-incorrecto";
    mockMvc.perform(post("/api/usuarios/reautenticar")
            .header("X-Usuario-Id", "7").contentType(MediaType.APPLICATION_JSON)
            .content("{\"contrasena\":\"" + incorrecta + "\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.autorizado").value(false))
        .andExpect(jsonPath("$.resultado").value("CONTRASENA_INCORRECTA"))
        .andExpect(jsonPath("$.puedeArchivarPacientes").value(false))
        .andExpect(content().string(not(containsString(incorrecta))));
  }

  @Test
  void noAceptaIdUsuarioNiCargoEnElCuerpo() throws Exception {
    mockMvc.perform(post("/api/usuarios/reautenticar")
            .header("X-Usuario-Id", "7").contentType(MediaType.APPLICATION_JSON)
            .content("{\"contrasena\":\"clave\",\"idUsuario\":99,\"cargo\":\"ADMINISTRADOR\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.resultado").value("CUERPO_INVALIDO"));
    verifyNoInteractions(service);
  }

  @Test
  void delegaEncabezadoAusenteParaErrorControlado() throws Exception {
    when(service.reautenticar(eq(null), any(ReautenticacionRequest.class))).thenThrow(new ReautenticacionException(
        "USUARIO_REQUERIDO", "Debe indicar el usuario actual.", null, HttpStatus.BAD_REQUEST));

    mockMvc.perform(post("/api/usuarios/reautenticar").contentType(MediaType.APPLICATION_JSON)
            .content("{\"contrasena\":\"clave\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.resultado").value("USUARIO_REQUERIDO"));
  }
}
