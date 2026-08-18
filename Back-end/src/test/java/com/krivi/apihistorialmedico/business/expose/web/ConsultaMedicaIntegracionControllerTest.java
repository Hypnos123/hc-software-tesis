package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.exception.ConsultaMedicaIntegracionException;
import com.krivi.apihistorialmedico.business.services.ConsultaMedicaIntegracionService;
import com.krivi.apihistorialmedico.model.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ConsultaMedicaIntegracionControllerTest {
  private ConsultaMedicaIntegracionService service;
  private MockMvc mockMvc;

  @BeforeEach void setUp() {
    service = mock(ConsultaMedicaIntegracionService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new ConsultaMedicaIntegracionController(service)).build();
  }

  @Test void exponeEndpointsAdministrativos() throws Exception {
    when(service.buscar("78451268")).thenReturn(BusquedaConsultasMedicasResponse.builder().encontrado(true).tipoResultado("unico").pacientes(List.of(ConsultaMedicaPacienteResponse.builder().idPaciente(6).dni("78451268").nombreCompleto("Patricia Cárdenas").totalConsultas(1L).consultas(List.of()).build())).build());
    when(service.obtenerEstadisticas()).thenReturn(EstadisticasConsultasMedicasResponse.builder().totalConsultas(1).creadasHoy(1).atendidasHoy(0).totalPendientes(1).totalAtendidas(0).build());
    when(service.obtenerPendientes()).thenReturn(ListadoConsultasMedicasResponse.builder().cantidad(0).consultas(List.of()).build());
    when(service.obtenerUltimas(5)).thenReturn(ListadoConsultasMedicasResponse.builder().cantidad(0).consultas(List.of()).build());
    mockMvc.perform(get("/api/consultas-medicas/buscar").param("criterio", "78451268")).andExpect(status().isOk()).andExpect(jsonPath("$.pacientes[0].totalConsultas").value(1));
    mockMvc.perform(get("/api/consultas-medicas/estadisticas")).andExpect(status().isOk()).andExpect(jsonPath("$.creadasHoy").value(1));
    mockMvc.perform(get("/api/consultas-medicas/pendientes")).andExpect(status().isOk()).andExpect(jsonPath("$.consultas").isEmpty());
    mockMvc.perform(get("/api/consultas-medicas/ultimas").param("limite", "5")).andExpect(status().isOk());
  }

  @Test void devuelveContratoDeErrorParaLimiteInvalido() throws Exception {
    when(service.obtenerUltimas(11)).thenThrow(new ConsultaMedicaIntegracionException("LIMITE_INVALIDO", "El límite debe estar entre 1 y 10.", HttpStatus.BAD_REQUEST));
    mockMvc.perform(get("/api/consultas-medicas/ultimas").param("limite", "11"))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("LIMITE_INVALIDO"));
  }

  @Test void exponeResumenConHeaderDeUsuario() throws Exception {
    ResumenConsultasPacienteResponse response = ResumenConsultasPacienteResponse.builder()
        .paciente(ResumenConsultasPacienteResponse.PacienteResumen.builder().idPaciente(6).build())
        .resumenAtencion(ResumenConsultasPacienteResponse.ResumenAtencion.builder().totalConsultasAtendidas(3L).build())
        .build();
    when(service.obtenerResumenPaciente(6, 4)).thenReturn(response);

    mockMvc.perform(get("/api/consultas-medicas/pacientes/6/resumen").header("X-Usuario-Id", "4"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.paciente.idPaciente").value(6))
        .andExpect(jsonPath("$.resumenAtencion.totalConsultasAtendidas").value(3));
  }

  @Test void resumenDevuelveContratoResultadoMensaje() throws Exception {
    when(service.obtenerResumenPaciente(6, null)).thenThrow(new ConsultaMedicaIntegracionException(
        "USUARIO_REQUERIDO", "Debe indicar el usuario autenticado mediante X-Usuario-Id.", HttpStatus.UNAUTHORIZED));

    mockMvc.perform(get("/api/consultas-medicas/pacientes/6/resumen"))
        .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.resultado").value("USUARIO_REQUERIDO"))
        .andExpect(jsonPath("$.mensaje").exists());
  }
}
