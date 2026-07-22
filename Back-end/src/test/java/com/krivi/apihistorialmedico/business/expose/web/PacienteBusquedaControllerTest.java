package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.exception.BusquedaPacienteException;
import com.krivi.apihistorialmedico.business.services.PacienteService;
import com.krivi.apihistorialmedico.model.api.BusquedaPacienteResponse;
import com.krivi.apihistorialmedico.model.api.PacienteBusquedaItemResponse;
import com.krivi.apihistorialmedico.model.api.EstadisticasPacientesResponse;
import com.krivi.apihistorialmedico.model.api.DuplicadosPacientesResponse;
import com.krivi.apihistorialmedico.model.api.GrupoDuplicadoDniResponse;
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

  @Test
  void incluyeListaPacientesVaciaCuandoNoHayResultados() throws Exception {
    BusquedaPacienteResponse response = BusquedaPacienteResponse.builder()
        .encontrado(false)
        .tipoResultado("sin_resultados")
        .pacientes(List.of())
        .mensaje("No se encontró ningún paciente con el criterio indicado.")
        .build();
    when(pacienteService.buscarParaIntegracion("99999999")).thenReturn(response);

    mockMvc.perform(get("/api/pacientes/buscar").param("criterio", "99999999"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.encontrado").value(false))
        .andExpect(jsonPath("$.tipoResultado").value("sin_resultados"))
        .andExpect(jsonPath("$.pacientes").isArray())
        .andExpect(jsonPath("$.pacientes").isEmpty())
        .andExpect(jsonPath("$.mensaje").value("No se encontró ningún paciente con el criterio indicado."));
  }

  @Test
  void exponeEstadisticasDePacientes() throws Exception {
    when(pacienteService.obtenerEstadisticasParaIntegracion())
        .thenReturn(EstadisticasPacientesResponse.builder().totalPacientes(25).registradosHoy(3).build());

    mockMvc.perform(get("/api/pacientes/estadisticas"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalPacientes").value(25))
        .andExpect(jsonPath("$.registradosHoy").value(3));
  }

  @Test
  void exponeGruposDeDuplicadosPorDni() throws Exception {
    DuplicadosPacientesResponse response = DuplicadosPacientesResponse.builder()
        .hayDuplicados(true)
        .totalGrupos(1)
        .duplicados(List.of(GrupoDuplicadoDniResponse.builder()
            .tipo("dni").valorCoincidente("72845292").cantidad(2).pacientes(List.of()).build()))
        .build();
    when(pacienteService.obtenerDuplicadosParaIntegracion()).thenReturn(response);

    mockMvc.perform(get("/api/pacientes/duplicados"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hayDuplicados").value(true))
        .andExpect(jsonPath("$.totalGrupos").value(1))
        .andExpect(jsonPath("$.duplicados[0].tipo").value("dni"))
        .andExpect(jsonPath("$.duplicados[0].valorCoincidente").value("72845292"));
  }
}
