package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.exception.BusquedaPacienteException;
import com.krivi.apihistorialmedico.business.services.PacienteService;
import com.krivi.apihistorialmedico.model.api.BusquedaPacienteResponse;
import com.krivi.apihistorialmedico.model.api.PacienteBusquedaItemResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PacienteBusquedaControllerTest {
  private PacienteService pacienteService;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    pacienteService = mock(PacienteService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new PacienteBusquedaController(pacienteService)).build();
  }

  @Test
  void exponeLaRutaYContratoDeBusqueda() throws Exception {
    BusquedaPacienteResponse response = BusquedaPacienteResponse.builder()
        .encontrado(true)
        .tipoResultado("unico")
        .pacientes(List.of(PacienteBusquedaItemResponse.builder()
            .idPaciente(6).dni("78451268").nombreCompleto("Patricia Elena Cárdenas Torres")
            .tieneHistoriaClinica(true).build()))
        .build();
    when(pacienteService.buscarParaIntegracion("78451268")).thenReturn(response);

    mockMvc.perform(get("/api/pacientes/buscar").param("criterio", "78451268"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.encontrado").value(true))
        .andExpect(jsonPath("$.tipoResultado").value("unico"))
        .andExpect(jsonPath("$.pacientes[0].idPaciente").value(6))
        .andExpect(jsonPath("$.pacientes[0].tieneHistoriaClinica").value(true));
  }

  @Test
  void retornaBadRequestParaCriterioVacio() throws Exception {
    when(pacienteService.buscarParaIntegracion(" "))
        .thenThrow(new BusquedaPacienteException("CRITERIO_VACIO", "El criterio de búsqueda es obligatorio.", HttpStatus.BAD_REQUEST));

    mockMvc.perform(get("/api/pacientes/buscar").param("criterio", " "))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("CRITERIO_VACIO"))
        .andExpect(jsonPath("$.mensaje").value("El criterio de búsqueda es obligatorio."));
  }
}
