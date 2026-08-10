package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.services.HistoriaClinicaService;
import com.krivi.apihistorialmedico.model.api.CreacionHistoriaClinicaFaltanteResponse;
import com.krivi.apihistorialmedico.model.api.CrearHistoriasClinicasFaltantesResponse;
import com.krivi.apihistorialmedico.model.api.EstadoCreacionHistoriaClinicaFaltante;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoriaClinicaFaltanteMasivaServiceImplTest {
  @Mock private HistoriaClinicaService historiaClinicaService;
  @InjectMocks private HistoriaClinicaFaltanteMasivaServiceImpl servicio;

  @Test
  void creaTodosLosPacientesValidosYCalculaElResumen() {
    when(historiaClinicaService.crearHistoriaClinicaSiFalta(1)).thenReturn(resultado(1,
        EstadoCreacionHistoriaClinicaFaltante.CREADA, 101));
    when(historiaClinicaService.crearHistoriaClinicaSiFalta(2)).thenReturn(resultado(2,
        EstadoCreacionHistoriaClinicaFaltante.CREADA, 102));

    CrearHistoriasClinicasFaltantesResponse response =
        servicio.crearHistoriasClinicasFaltantes(List.of(1, 2));

    assertEquals(2, response.getTotalSolicitados());
    assertEquals(2, response.getTotalProcesados());
    assertEquals(2, response.getCreadas());
    assertEquals(0, response.getOmitidas());
    assertEquals(List.of(1, 2), response.getResultados().stream()
        .map(CreacionHistoriaClinicaFaltanteResponse::getIdPaciente).toList());
  }

  @Test
  void resumeTodosLosEstadosSinDetenersePorUnErrorIntermedio() {
    when(historiaClinicaService.crearHistoriaClinicaSiFalta(1)).thenReturn(resultado(1,
        EstadoCreacionHistoriaClinicaFaltante.CREADA, 101));
    when(historiaClinicaService.crearHistoriaClinicaSiFalta(2)).thenReturn(resultado(2,
        EstadoCreacionHistoriaClinicaFaltante.OMITIDA_YA_TIENE_HISTORIA, null));
    when(historiaClinicaService.crearHistoriaClinicaSiFalta(3)).thenReturn(resultado(3,
        EstadoCreacionHistoriaClinicaFaltante.PACIENTE_NO_ENCONTRADO, null));
    when(historiaClinicaService.crearHistoriaClinicaSiFalta(4)).thenReturn(resultado(4,
        EstadoCreacionHistoriaClinicaFaltante.PACIENTE_INACTIVO, null));
    when(historiaClinicaService.crearHistoriaClinicaSiFalta(5)).thenReturn(resultado(5,
        EstadoCreacionHistoriaClinicaFaltante.ERROR, null));
    when(historiaClinicaService.crearHistoriaClinicaSiFalta(6)).thenReturn(resultado(6,
        EstadoCreacionHistoriaClinicaFaltante.CREADA, 106));

    CrearHistoriasClinicasFaltantesResponse response =
        servicio.crearHistoriasClinicasFaltantes(List.of(1, 2, 3, 4, 5, 6));

    assertEquals(6, response.getTotalProcesados());
    assertEquals(2, response.getCreadas());
    assertEquals(1, response.getOmitidas());
    assertEquals(1, response.getNoEncontrados());
    assertEquals(1, response.getInactivos());
    assertEquals(1, response.getErrores());
    verify(historiaClinicaService).crearHistoriaClinicaSiFalta(6);
  }

  @Test
  void continuaConElSiguientePacienteSiLaOperacionIndividualLanzaExcepcion() {
    when(historiaClinicaService.crearHistoriaClinicaSiFalta(1)).thenReturn(resultado(1,
        EstadoCreacionHistoriaClinicaFaltante.CREADA, 101));
    when(historiaClinicaService.crearHistoriaClinicaSiFalta(2)).thenThrow(new RuntimeException("fallo inesperado"));
    when(historiaClinicaService.crearHistoriaClinicaSiFalta(3)).thenReturn(resultado(3,
        EstadoCreacionHistoriaClinicaFaltante.CREADA, 103));

    CrearHistoriasClinicasFaltantesResponse response =
        servicio.crearHistoriasClinicasFaltantes(List.of(1, 2, 3));

    assertEquals(2, response.getCreadas());
    assertEquals(1, response.getErrores());
    assertEquals(EstadoCreacionHistoriaClinicaFaltante.ERROR, response.getResultados().get(1).getEstado());
    InOrder orden = inOrder(historiaClinicaService);
    orden.verify(historiaClinicaService).crearHistoriaClinicaSiFalta(1);
    orden.verify(historiaClinicaService).crearHistoriaClinicaSiFalta(2);
    orden.verify(historiaClinicaService).crearHistoriaClinicaSiFalta(3);
  }

  @Test
  void eliminaIdsDuplicadosYConservaElOrdenDePrimeraAparicion() {
    when(historiaClinicaService.crearHistoriaClinicaSiFalta(12)).thenReturn(resultado(12,
        EstadoCreacionHistoriaClinicaFaltante.CREADA, 112));
    when(historiaClinicaService.crearHistoriaClinicaSiFalta(14)).thenReturn(resultado(14,
        EstadoCreacionHistoriaClinicaFaltante.CREADA, 114));
    when(historiaClinicaService.crearHistoriaClinicaSiFalta(20)).thenReturn(resultado(20,
        EstadoCreacionHistoriaClinicaFaltante.CREADA, 120));

    CrearHistoriasClinicasFaltantesResponse response =
        servicio.crearHistoriasClinicasFaltantes(List.of(12, 12, 14, 14, 20));

    assertEquals(5, response.getTotalSolicitados());
    assertEquals(3, response.getTotalProcesados());
    assertEquals(List.of(12, 14, 20), response.getResultados().stream()
        .map(CreacionHistoriaClinicaFaltanteResponse::getIdPaciente).toList());
    verify(historiaClinicaService, times(1)).crearHistoriaClinicaSiFalta(12);
    verify(historiaClinicaService, times(1)).crearHistoriaClinicaSiFalta(14);
    verify(historiaClinicaService, times(1)).crearHistoriaClinicaSiFalta(20);
  }

  @Test
  void listaNulaOVaciaNoProcesaPacientes() {
    CrearHistoriasClinicasFaltantesResponse nula = servicio.crearHistoriasClinicasFaltantes(null);
    CrearHistoriasClinicasFaltantesResponse vacia = servicio.crearHistoriasClinicasFaltantes(List.of());

    assertEquals(0, nula.getTotalSolicitados());
    assertEquals(0, nula.getTotalProcesados());
    assertEquals(0, vacia.getTotalSolicitados());
    assertEquals(0, vacia.getTotalProcesados());
    assertFalse(nula.getResultados() == null);
    verifyNoInteractions(historiaClinicaService);
  }

  @Test
  void ignoraIdsNulosSinRomperElLote() {
    when(historiaClinicaService.crearHistoriaClinicaSiFalta(10)).thenReturn(resultado(10,
        EstadoCreacionHistoriaClinicaFaltante.CREADA, 110));

    CrearHistoriasClinicasFaltantesResponse response =
        servicio.crearHistoriasClinicasFaltantes(java.util.Arrays.asList(null, 10, null, 10));

    assertEquals(4, response.getTotalSolicitados());
    assertEquals(1, response.getTotalProcesados());
    assertEquals(1, response.getCreadas());
    verify(historiaClinicaService, times(1)).crearHistoriaClinicaSiFalta(10);
    verify(historiaClinicaService, never()).crearHistoriaClinicaSiFalta(null);
  }

  @Test
  void coordinadorSoloDependeDelServicioIndividualYNoDeRepositorios() {
    List<Class<?>> tiposDeCampos = java.util.Arrays.stream(HistoriaClinicaFaltanteMasivaServiceImpl.class
        .getDeclaredFields()).map(Field::getType).toList();

    assertEquals(List.of(HistoriaClinicaService.class), tiposDeCampos);
  }

  private CreacionHistoriaClinicaFaltanteResponse resultado(Integer idPaciente,
      EstadoCreacionHistoriaClinicaFaltante estado, Integer idHistoriaClinica) {
    return CreacionHistoriaClinicaFaltanteResponse.builder()
        .idPaciente(idPaciente)
        .estado(estado)
        .idHistoriaClinica(idHistoriaClinica)
        .build();
  }
}
