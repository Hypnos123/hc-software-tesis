package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.exception.BusquedaHistoriaClinicaException;
import com.krivi.apihistorialmedico.model.api.BusquedaHistoriasClinicasResponse;
import com.krivi.apihistorialmedico.model.api.DuplicadosHistoriasClinicasResponse;
import com.krivi.apihistorialmedico.model.api.EstadisticasHistoriasClinicasResponse;
import com.krivi.apihistorialmedico.model.entity.HistoriaClinica;
import com.krivi.apihistorialmedico.model.entity.Paciente;
import com.krivi.apihistorialmedico.repository.AntecedentesRepository;
import com.krivi.apihistorialmedico.repository.HistoriaClinicaRepository;
import com.krivi.apihistorialmedico.repository.PacienteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoriaClinicaServiceImplTest {
  @Mock private HistoriaClinicaRepository historiaClinicaRepository;
  @Mock private PacienteRepository pacienteRepository;
  @Mock private AntecedentesRepository antecedentesRepository;
  @InjectMocks private HistoriaClinicaServiceImpl historiaClinicaService;

  @Test
  void buscaNumeroCortoPorHistoriaYPacienteYEliminaRepetidos() {
    HistoriaClinica historiaPorId = historia(5, 10, "72845292", "Ana", "Lima");
    HistoriaClinica historiaPorPaciente = historia(7, 5, "12345678", "Bruno", "Paz");
    when(historiaClinicaRepository.findForIntegracionByIdHistoriaClinica(5)).thenReturn(List.of(historiaPorId));
    when(historiaClinicaRepository.findForIntegracionByIdPaciente(5)).thenReturn(List.of(historiaPorId, historiaPorPaciente));

    BusquedaHistoriasClinicasResponse response = historiaClinicaService.buscarParaIntegracion("5");

    assertTrue(response.isEncontrado());
    assertEquals("multiple", response.getTipoResultado());
    assertEquals(List.of(5, 7), response.getHistoriasClinicas().stream().map(h -> h.getIdHistoriaClinica()).toList());
  }

  @Test
  void admitePrefijosExplicitosYNormalizaDniSoloConTrim() {
    HistoriaClinica historia = historia(8, 4, " 72845292 ", "Carmen", "Ríos");
    when(historiaClinicaRepository.findForIntegracionByIdHistoriaClinica(8)).thenReturn(List.of(historia));
    when(historiaClinicaRepository.findForIntegracionByDni("72845292")).thenReturn(List.of(historia));

    assertEquals(8, historiaClinicaService.buscarParaIntegracion("historia:8").getHistoriasClinicas().getFirst().getIdHistoriaClinica());
    assertEquals("72845292", historiaClinicaService.buscarParaIntegracion(" dni: 72845292 ").getHistoriasClinicas().getFirst().getDni());
    verify(historiaClinicaRepository).findForIntegracionByDni("72845292");
  }

  @Test
  void rechazaDnisInvalidosYNumerosDeMasDeOchoDigitos() {
    assertThrows(BusquedaHistoriaClinicaException.class, () -> historiaClinicaService.buscarParaIntegracion("dni: ABC12345"));
    assertThrows(BusquedaHistoriaClinicaException.class, () -> historiaClinicaService.buscarParaIntegracion("123456789"));
  }

  @Test
  void buscaPorNombreParcial() {
    when(historiaClinicaRepository.findForIntegracionByNombre("Rosa")).thenReturn(List.of(historia(1, 2, "12345678", "Rosa", "López")));
    assertEquals("Rosa López", historiaClinicaService.buscarParaIntegracion("Rosa").getHistoriasClinicas().getFirst().getNombreCompleto());
  }

  @Test
  void calculaEstadisticasConRangoDeLima() {
    LocalDateTime inicio = LocalDate.now(ZoneId.of("America/Lima")).atStartOfDay();
    when(historiaClinicaRepository.count()).thenReturn(12L);
    when(historiaClinicaRepository.countByFechaCreacionGreaterThanEqualAndFechaCreacionLessThan(inicio, inicio.plusDays(1))).thenReturn(3L);

    EstadisticasHistoriasClinicasResponse response = historiaClinicaService.obtenerEstadisticasParaIntegracion();

    assertEquals(12, response.getTotalHistoriasClinicas());
    assertEquals(3, response.getCreadasHoy());
  }

  @Test
  void detectaDuplicadosPorDniNormalizadoYPorPacienteSinNombres() {
    HistoriaClinica primera = historia(1, 10, " 72845292", "Mismo", "Nombre");
    HistoriaClinica segunda = historia(2, 11, "72845292 ", "Mismo", "Nombre");
    when(historiaClinicaRepository.findIdsPacienteConHistoriasDuplicadas()).thenReturn(List.of(20));
    when(historiaClinicaRepository.findForIntegracionByIdPaciente(20)).thenReturn(List.of(historia(3, 20, "11111111", "Otro", "Paciente"), historia(4, 20, "22222222", "Otro", "Paciente")));
    when(historiaClinicaRepository.findDnisNormalizadosConHistoriasDuplicadas()).thenReturn(List.of("72845292"));
    when(historiaClinicaRepository.findForIntegracionByDni("72845292")).thenReturn(List.of(primera, segunda));

    DuplicadosHistoriasClinicasResponse response = historiaClinicaService.obtenerDuplicadosParaIntegracion();

    assertTrue(response.isHayDuplicados());
    assertEquals(2, response.getTotalGrupos());
    assertEquals("idPaciente", response.getDuplicados().getFirst().getTipo());
    assertEquals("dni", response.getDuplicados().get(1).getTipo());
  }

  private HistoriaClinica historia(int idHistoria, int idPaciente, String dni, String nombres, String apellidos) {
    Paciente paciente = new Paciente();
    paciente.setIdPaciente(idPaciente); paciente.setNumDocumento(dni); paciente.setNombres(nombres); paciente.setApellidos(apellidos);
    HistoriaClinica historia = new HistoriaClinica();
    historia.setIdHistoriaClinica(idHistoria); historia.setPaciente(paciente); historia.setFechaCreacion(LocalDateTime.of(2026, 7, 23, 9, 0));
    return historia;
  }
}
