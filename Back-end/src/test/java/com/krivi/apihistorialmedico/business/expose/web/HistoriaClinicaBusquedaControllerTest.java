package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.exception.BusquedaHistoriaClinicaException;
import com.krivi.apihistorialmedico.business.services.HistoriaClinicaService;
import com.krivi.apihistorialmedico.model.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HistoriaClinicaBusquedaControllerTest {
  private HistoriaClinicaService historiaClinicaService;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    historiaClinicaService = mock(HistoriaClinicaService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new HistoriaClinicaBusquedaController(historiaClinicaService)).build();
  }

  @Test
  void exponeContratoMinimoDeBusqueda() throws Exception {
    when(historiaClinicaService.buscarParaIntegracion("historia:5")).thenReturn(BusquedaHistoriasClinicasResponse.builder()
        .encontrado(true).tipoResultado("unico").historiasClinicas(List.of(HistoriaClinicaIntegracionItemResponse.builder()
            .idHistoriaClinica(5).idPaciente(2).dni("72845292").nombreCompleto("Ana Lima").fechaCreacion(LocalDateTime.of(2026, 7, 23, 9, 0)).build())).build());

    mockMvc.perform(get("/api/historias-clinicas/buscar").param("criterio", "historia:5"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.historiasClinicas[0].idHistoriaClinica").value(5))
        .andExpect(jsonPath("$.historiasClinicas[0].idPaciente").value(2))
        .andExpect(jsonPath("$.historiasClinicas[0].dni").value("72845292"))
        .andExpect(jsonPath("$.historiasClinicas[0].antecedentes").doesNotExist());
  }

  @Test
  void exponeEstadisticasYDuplicados() throws Exception {
    when(historiaClinicaService.obtenerEstadisticasParaIntegracion()).thenReturn(EstadisticasHistoriasClinicasResponse.builder().totalHistoriasClinicas(10).creadasHoy(2).build());
    when(historiaClinicaService.obtenerDuplicadosParaIntegracion()).thenReturn(DuplicadosHistoriasClinicasResponse.builder().hayDuplicados(true).totalGrupos(1)
        .duplicados(List.of(GrupoDuplicadoHistoriaClinicaResponse.builder().tipo("dni").valorCoincidente("72845292").cantidad(2).historiasClinicas(List.of()).build())).build());

    mockMvc.perform(get("/api/historias-clinicas/estadisticas")).andExpect(status().isOk())
        .andExpect(jsonPath("$.totalHistoriasClinicas").value(10)).andExpect(jsonPath("$.creadasHoy").value(2));
    mockMvc.perform(get("/api/historias-clinicas/duplicados")).andExpect(status().isOk())
        .andExpect(jsonPath("$.duplicados[0].tipo").value("dni"));
  }

  @Test
  void devuelveBadRequestParaCriterioInvalido() throws Exception {
    when(historiaClinicaService.buscarParaIntegracion("dni: ABC")).thenThrow(new BusquedaHistoriaClinicaException("CRITERIO_INVALIDO", "Criterio inválido", HttpStatus.BAD_REQUEST));
    mockMvc.perform(get("/api/historias-clinicas/buscar").param("criterio", "dni: ABC"))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("CRITERIO_INVALIDO"));
  }
}
