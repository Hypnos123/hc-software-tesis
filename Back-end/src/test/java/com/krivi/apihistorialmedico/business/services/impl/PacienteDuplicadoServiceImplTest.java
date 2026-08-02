package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.exception.PacienteDuplicadoException;
import com.krivi.apihistorialmedico.model.api.PacienteDuplicadoComparacionResponse;
import com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente;
import com.krivi.apihistorialmedico.model.entity.Paciente;
import com.krivi.apihistorialmedico.repository.AntecedentesRepository;
import com.krivi.apihistorialmedico.repository.ConsultaRepository;
import com.krivi.apihistorialmedico.repository.HistoriaClinicaRepository;
import com.krivi.apihistorialmedico.repository.PacienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PacienteDuplicadoServiceImplTest {
  private static final String DNI = "12345678";

  @Mock PacienteRepository pacienteRepository;
  @Mock HistoriaClinicaRepository historiaClinicaRepository;
  @Mock ConsultaRepository consultaRepository;
  @Mock AntecedentesRepository antecedentesRepository;
  @InjectMocks PacienteDuplicadoServiceImpl service;

  @BeforeEach
  void prepararResumenesVacios() {
    lenient().when(historiaClinicaRepository.resumirPorPacientes(anyList())).thenReturn(List.of());
    lenient().when(consultaRepository.resumirPorPacientes(anyList())).thenReturn(List.of());
    lenient().when(antecedentesRepository.resumirPorPacientes(anyList())).thenReturn(List.of());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"1234567", "123456789", "1234A678", " 12345678", "12345678 "})
  void rechazaDniNuloVacioOConFormatoInvalido(String dni) {
    PacienteDuplicadoException error = assertThrows(PacienteDuplicadoException.class,
        () -> service.compararPorDni(dni));

    assertEquals("DNI_INVALIDO", error.getCodigo());
    assertEquals(400, error.getStatus().value());
    verifyNoInteractions(pacienteRepository, historiaClinicaRepository, consultaRepository, antecedentesRepository);
  }

  @Test
  void devuelveSinPacientesParaDniInexistenteSinEjecutarAgregaciones() {
    when(pacienteRepository.findByNumDocumentoAndEstadoRegistroOrderByIdPacienteAsc(DNI, EstadoRegistroPaciente.ACTIVO))
        .thenReturn(List.of());

    PacienteDuplicadoComparacionResponse response = service.compararPorDni(DNI);

    assertEquals("SIN_PACIENTES", response.getResultado());
    assertEquals(0, response.getCantidadPacientesActivos());
    assertFalse(response.isEsDuplicado());
    verifyNoInteractions(historiaClinicaRepository, consultaRepository, antecedentesRepository);
  }

  @Test
  void informaQueUnSoloPacienteActivoNoEsDuplicado() {
    when(pacienteRepository.findByNumDocumentoAndEstadoRegistroOrderByIdPacienteAsc(DNI, EstadoRegistroPaciente.ACTIVO))
        .thenReturn(List.of(paciente(1, 10, fecha(2025, 1, 1))));

    PacienteDuplicadoComparacionResponse response = service.compararPorDni(DNI);

    assertEquals("SIN_DUPLICADOS", response.getResultado());
    assertFalse(response.isEsDuplicado());
    assertEquals(1, response.getPacientes().size());
    assertNull(response.getIdPacienteRecomendado());
  }

  @Test
  void recomiendaElMasAntiguoCuandoDosPacientesNoTienenInformacionClinica() {
    prepararPacientes(paciente(2, 4, fecha(2025, 2, 1)), paciente(1, 4, fecha(2024, 1, 1)));

    PacienteDuplicadoComparacionResponse response = service.compararPorDni(DNI);

    assertEquals(1, response.getIdPacienteRecomendado());
    assertTrue(response.isPermitirArchivadoSimple());
    assertFalse(response.isRequiereRevision());
    assertTrue(response.getRazonesRecomendacion().stream().anyMatch(razon -> razon.contains("más antiguo")));
  }

  @Test
  void priorizaMayorCantidadDeConsultas() {
    prepararPacientes(paciente(1, 10, fecha(2024, 1, 1)), paciente(2, 2, fecha(2025, 1, 1)));
    when(consultaRepository.resumirPorPacientes(anyList())).thenReturn(List.of(
        filaActividad(2, 4, fecha(2025, 1, 1))));

    PacienteDuplicadoComparacionResponse response = service.compararPorDni(DNI);

    assertEquals(2, response.getIdPacienteRecomendado());
    assertTrue(response.getRazonesRecomendacion().contains("Tiene 4 consultas registradas."));
    assertTrue(response.getRazonesRecomendacion().stream().anyMatch(razon -> razon.contains("mayor cantidad de consultas")));
  }

  @Test
  void priorizaMayorCantidadDeHistoriasCuandoLasConsultasEmpatan() {
    prepararPacientes(paciente(1, 10, fecha(2024, 1, 1)), paciente(2, 2, fecha(2025, 1, 1)));
    when(historiaClinicaRepository.resumirPorPacientes(anyList())).thenReturn(List.of(
        filaActividad(2, 2, fecha(2025, 1, 1))));

    PacienteDuplicadoComparacionResponse response = service.compararPorDni(DNI);

    assertEquals(2, response.getIdPacienteRecomendado());
    assertTrue(response.getRazonesRecomendacion().contains("Tiene 2 historias clínicas."));
  }

  @Test
  void priorizaAntecedentesInformados() {
    prepararPacientes(paciente(1, 10, fecha(2024, 1, 1)), paciente(2, 2, fecha(2025, 1, 1)));
    when(antecedentesRepository.resumirPorPacientes(anyList())).thenReturn(List.of(filaAntecedentes(2, 1, 3)));

    PacienteDuplicadoComparacionResponse response = service.compararPorDni(DNI);

    assertEquals(2, response.getIdPacienteRecomendado());
    assertTrue(response.getRazonesRecomendacion().contains("Posee 3 grupos de antecedentes informados."));
    assertTrue(response.isPermitirArchivadoSimple());
  }

  @Test
  void marcaRevisionCuandoDosPacientesTienenInformacionClinica() {
    prepararPacientes(paciente(1, 5, fecha(2024, 1, 1)), paciente(2, 5, fecha(2025, 1, 1)));
    when(historiaClinicaRepository.resumirPorPacientes(anyList())).thenReturn(List.of(filaActividad(1, 1, fecha(2025, 1, 1))));
    when(consultaRepository.resumirPorPacientes(anyList())).thenReturn(List.of(filaActividad(2, 1, fecha(2025, 2, 1))));

    PacienteDuplicadoComparacionResponse response = service.compararPorDni(DNI);

    assertEquals("REQUIERE_REVISION_O_FUSION", response.getResultado());
    assertTrue(response.isRequiereRevision());
    assertFalse(response.isPermitirArchivadoSimple());
    assertNotNull(response.getAdvertencia());
  }

  @Test
  void permiteAnalisisSimpleCuandoSoloUnoTieneInformacionClinica() {
    prepararPacientes(paciente(1, 5, fecha(2024, 1, 1)), paciente(2, 5, fecha(2025, 1, 1)));
    when(historiaClinicaRepository.resumirPorPacientes(anyList())).thenReturn(List.of(filaActividad(2, 1, fecha(2025, 1, 1))));

    PacienteDuplicadoComparacionResponse response = service.compararPorDni(DNI);

    assertEquals(2, response.getIdPacienteRecomendado());
    assertEquals("DUPLICADOS_ENCONTRADOS", response.getResultado());
    assertTrue(response.isPermitirArchivadoSimple());
    assertFalse(response.isRequiereRevision());
  }

  @Test
  void soportaTresOMasDuplicadosYEligeEntreTodos() {
    prepararPacientes(paciente(1, 3, fecha(2024, 1, 1)), paciente(2, 6, fecha(2025, 1, 1)),
        paciente(3, 8, fecha(2026, 1, 1)), paciente(4, 10, fecha(2023, 1, 1)));
    when(consultaRepository.resumirPorPacientes(anyList())).thenReturn(List.of(filaActividad(3, 5, fecha(2026, 1, 1))));

    PacienteDuplicadoComparacionResponse response = service.compararPorDni(DNI);

    assertEquals(4, response.getCantidadPacientesActivos());
    assertEquals(3, response.getIdPacienteRecomendado());
  }

  @Test
  void desempataPorCompletitudPersonal() {
    prepararPacientes(paciente(1, 3, fecha(2024, 1, 1)), paciente(2, 8, fecha(2025, 1, 1)));

    PacienteDuplicadoComparacionResponse response = service.compararPorDni(DNI);

    assertEquals(2, response.getIdPacienteRecomendado());
    assertTrue(response.getRazonesRecomendacion().stream().anyMatch(razon -> razon.contains("completitud")));
  }

  @Test
  void desempataPorActividadClinicaMasReciente() {
    prepararPacientes(paciente(1, 5, fecha(2024, 1, 1)), paciente(2, 5, fecha(2025, 1, 1)));
    when(consultaRepository.resumirPorPacientes(anyList())).thenReturn(List.of(
        filaActividad(1, 1, fecha(2025, 1, 1)), filaActividad(2, 1, fecha(2026, 1, 1))));

    PacienteDuplicadoComparacionResponse response = service.compararPorDni(DNI);

    assertEquals(2, response.getIdPacienteRecomendado());
    assertTrue(response.getRazonesRecomendacion().stream().anyMatch(razon -> razon.contains("más reciente")));
  }

  @Test
  void pacientesArchivadosNoParticipan() {
    prepararPacientes(paciente(1, 5, fecha(2024, 1, 1)), paciente(2, 5, fecha(2025, 1, 1)));

    service.compararPorDni(DNI);

    verify(pacienteRepository).findByNumDocumentoAndEstadoRegistroOrderByIdPacienteAsc(DNI, EstadoRegistroPaciente.ACTIVO);
    verify(pacienteRepository, never()).findByNumDocumentoAndEstadoRegistroOrderByIdPacienteAsc(DNI, EstadoRegistroPaciente.ARCHIVADO);
  }

  @Test
  void usaConsultasAgregadasSinNMasUnoYNoModificaEntidades() {
    Paciente primero = paciente(1, 5, fecha(2024, 1, 1));
    Paciente segundo = paciente(2, 5, fecha(2025, 1, 1));
    prepararPacientes(primero, segundo);

    service.compararPorDni(DNI);

    verify(historiaClinicaRepository, times(1)).resumirPorPacientes(List.of(1, 2));
    verify(consultaRepository, times(1)).resumirPorPacientes(List.of(1, 2));
    verify(antecedentesRepository, times(1)).resumirPorPacientes(List.of(1, 2));
    verify(pacienteRepository, never()).save(any());
    verify(historiaClinicaRepository, never()).save(any());
    verify(consultaRepository, never()).save(any());
    verify(antecedentesRepository, never()).save(any());
    assertEquals(EstadoRegistroPaciente.ACTIVO, primero.getEstadoRegistro());
    assertEquals(EstadoRegistroPaciente.ACTIVO, segundo.getEstadoRegistro());
  }

  @Test
  void recomendacionEsDeterministaCuandoTodosLosDatosEmpatan() {
    prepararPacientes(paciente(9, 5, fecha(2024, 1, 1)), paciente(4, 5, fecha(2024, 1, 1)));

    Integer primera = service.compararPorDni(DNI).getIdPacienteRecomendado();
    Integer segunda = service.compararPorDni(DNI).getIdPacienteRecomendado();

    assertEquals(4, primera);
    assertEquals(primera, segunda);
  }

  private void prepararPacientes(Paciente... pacientes) {
    when(pacienteRepository.findByNumDocumentoAndEstadoRegistroOrderByIdPacienteAsc(DNI, EstadoRegistroPaciente.ACTIVO))
        .thenReturn(List.of(pacientes));
  }

  private Paciente paciente(int id, int camposCompletos, LocalDateTime fechaCreacion) {
    Paciente paciente = new Paciente();
    paciente.setIdPaciente(id);
    paciente.setEstadoRegistro(EstadoRegistroPaciente.ACTIVO);
    paciente.setNumDocumento(DNI);
    paciente.setFechaCreacion(fechaCreacion);
    paciente.setUltimaActualizacion(fechaCreacion);
    Object[] valores = {"Nombres", "Apellidos", java.sql.Date.valueOf("2025-01-01"), java.sql.Date.valueOf("1990-01-01"),
        "SOLTERO", DNI, "F", "Dirección", "Distrito", "Familiar"};
    if (camposCompletos > 0) paciente.setNombres((String) valores[0]);
    if (camposCompletos > 1) paciente.setApellidos((String) valores[1]);
    if (camposCompletos > 2) paciente.setFechaIngreso((java.util.Date) valores[2]);
    if (camposCompletos > 3) paciente.setFechaNacimiento((java.util.Date) valores[3]);
    if (camposCompletos > 4) paciente.setEstadoCivil((String) valores[4]);
    if (camposCompletos > 6) paciente.setSexo((String) valores[6]);
    if (camposCompletos > 7) paciente.setDireccion((String) valores[7]);
    if (camposCompletos > 8) paciente.setDistrito((String) valores[8]);
    if (camposCompletos > 9) paciente.setTraidoPor((String) valores[9]);
    return paciente;
  }

  private Object[] filaActividad(int idPaciente, long cantidad, LocalDateTime ultimaActividad) {
    return new Object[]{idPaciente, cantidad, ultimaActividad};
  }

  private Object[] filaAntecedentes(int idPaciente, long cantidad, long grupos) {
    return new Object[]{idPaciente, cantidad, grupos};
  }

  private LocalDateTime fecha(int anio, int mes, int dia) {
    return LocalDateTime.of(anio, mes, dia, 10, 0);
  }
}
