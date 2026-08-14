package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.services.FusionHistoriaClinicaService;
import com.krivi.apihistorialmedico.model.api.FusionarHistoriasClinicasResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FusionHistoriaClinicaControllerTest {
  @Test void exponeFusionConUsuarioEnCabecera() throws Exception {
    FusionHistoriaClinicaService service=mock(FusionHistoriaClinicaService.class);
    when(service.fusionar(eq(3),eq(8),any())).thenReturn(FusionarHistoriasClinicasResponse.builder().fusionada(true).idHistoriaPrincipal(7).resultado("HISTORIAS_FUSIONADAS").build());
    MockMvc mvc=MockMvcBuilders.standaloneSetup(new FusionHistoriaClinicaController(service)).build();
    mvc.perform(post("/api/historias-clinicas/8/fusionar").header("X-Usuario-Id","3").contentType("application/json")
        .content("{\"idHistoriaPrincipal\":7,\"contrasena\":\"clave\",\"confirmacion\":true}"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.fusionada").value(true)).andExpect(jsonPath("$.idHistoriaPrincipal").value(7));
  }
}
