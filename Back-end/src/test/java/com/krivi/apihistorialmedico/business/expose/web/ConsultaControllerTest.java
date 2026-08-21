package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.services.ConsultaService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConsultaControllerTest {
  @Test
  void devuelveForbiddenCuandoElRolNoPuedeCrearConsultas() throws Exception {
    ConsultaService service = mock(ConsultaService.class);
    when(service.save(any(), eq(9))).thenThrow(new SecurityException("El rol del usuario no permite crear consultas"));
    ConsultaController controller = new ConsultaController();
    ReflectionTestUtils.setField(controller, "consultaService", service);
    MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

    mockMvc.perform(post("/consulta/insert/consulta")
            .header("X-Usuario-Id", "9")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("El rol del usuario no permite crear consultas"));
  }
}
