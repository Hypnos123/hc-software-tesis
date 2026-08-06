package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.services.AsistenteService;
import com.krivi.apihistorialmedico.model.api.AsistenteResponse;
import com.krivi.apihistorialmedico.model.entity.Usuario;
import com.krivi.apihistorialmedico.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AsistenteControllerTest {
  private AsistenteService asistenteService;
  private UsuarioRepository usuarioRepository;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    asistenteService = mock(AsistenteService.class);
    usuarioRepository = mock(UsuarioRepository.class);
    AsistenteController controller = new AsistenteController();
    controller.asistenteService = asistenteService;
    controller.usuarioRepository = usuarioRepository;
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
  }

  @Test
  void rechazaConsultasSinUsuarioAutenticadoOConIdentificadorInexistente() throws Exception {
    mockMvc.perform(post("/asistente/preguntar").contentType(MediaType.APPLICATION_JSON)
            .content("{\"pregunta\":\"¿Existen pacientes duplicados?\"}"))
        .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.intencion").value("NO_AUTENTICADO"));

    when(usuarioRepository.findById(99)).thenReturn(Optional.empty());
    mockMvc.perform(post("/asistente/preguntar").header("X-Usuario-Id", "99").contentType(MediaType.APPLICATION_JSON)
            .content("{\"pregunta\":\"¿Existen historias clínicas duplicadas?\"}"))
        .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.intencion").value("NO_AUTENTICADO"));

    verifyNoInteractions(asistenteService);
  }

  @Test
  void permiteLaConsultaCuandoElUsuarioExiste() throws Exception {
    when(usuarioRepository.findById(7)).thenReturn(Optional.of(new Usuario()));
    when(asistenteService.preguntar(any(), eq(7))).thenReturn(AsistenteResponse.builder()
        .intencion("AYUDA_USO_SISTEMA").respuesta("Respuesta autorizada").datos(Map.of()).build());

    mockMvc.perform(post("/asistente/preguntar").header("X-Usuario-Id", "7").contentType(MediaType.APPLICATION_JSON)
            .content("{\"pregunta\":\"¿Qué preguntas puedo hacer?\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.respuesta").value("Respuesta autorizada"));

    verify(asistenteService).preguntar(any(), eq(7));
  }
}
