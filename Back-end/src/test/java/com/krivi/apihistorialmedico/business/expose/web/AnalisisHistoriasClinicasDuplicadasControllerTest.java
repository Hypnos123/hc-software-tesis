package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.services.AnalisisHistoriasClinicasDuplicadasService;
import com.krivi.apihistorialmedico.model.api.AnalisisHistoriasClinicasDuplicadasResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AnalisisHistoriasClinicasDuplicadasControllerTest {
  @Test
  void exponeAnalisisDetalladoDeSoloLectura() throws Exception {
    AnalisisHistoriasClinicasDuplicadasService service = mock(AnalisisHistoriasClinicasDuplicadasService.class);
    when(service.analizar(List.of(7, 8))).thenReturn(AnalisisHistoriasClinicasDuplicadasResponse.builder()
        .tipoDuplicidad("MISMO_PACIENTE").idHistoriaClinicaRecomendada(7).futuraFusionPermitida(true)
        .motivosRecomendacion(List.of("Es la más antigua.")).historiasComparadas(List.of())
        .posiblesCoincidencias(List.of()).advertenciasIntegridad(List.of()).build());
    MockMvc mvc = MockMvcBuilders.standaloneSetup(new AnalisisHistoriasClinicasDuplicadasController(service)).build();

    mvc.perform(post("/api/historias-clinicas/duplicados/analizar")
            .contentType("application/json").content("{\"idsHistoriasClinicas\":[7,8]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tipoDuplicidad").value("MISMO_PACIENTE"))
        .andExpect(jsonPath("$.idHistoriaClinicaRecomendada").value(7))
        .andExpect(jsonPath("$.futuraFusionPermitida").value(true));
  }
}
