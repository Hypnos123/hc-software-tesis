package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.exception.ConsultaMedicaIntegracionException;
import com.krivi.apihistorialmedico.model.api.ResumenConsultasPacienteResponse;
import com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente;
import com.krivi.apihistorialmedico.model.entity.Paciente;
import com.krivi.apihistorialmedico.model.entity.Usuario;
import com.krivi.apihistorialmedico.repository.ConsultaRepository;
import com.krivi.apihistorialmedico.repository.HistoriaClinicaRepository;
import com.krivi.apihistorialmedico.repository.PacienteRepository;
import com.krivi.apihistorialmedico.repository.UsuarioRepository;
import com.krivi.apihistorialmedico.repository.AntecedentesRepository;
import com.krivi.apihistorialmedico.model.projection.ConsultaResumenRecienteProjection;
import com.krivi.apihistorialmedico.model.entity.Antecedentes;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResumenConsultasPacienteServiceTest {
  private ConsultaRepository consultaRepository;
  private PacienteRepository pacienteRepository;
  private HistoriaClinicaRepository historiaClinicaRepository;
  private UsuarioRepository usuarioRepository;
  private AntecedentesRepository antecedentesRepository;
  private ConsultaMedicaIntegracionServiceImpl service;

  @BeforeEach void setUp() {
    consultaRepository = mock(ConsultaRepository.class);
    pacienteRepository = mock(PacienteRepository.class);
    historiaClinicaRepository = mock(HistoriaClinicaRepository.class);
    usuarioRepository = mock(UsuarioRepository.class);
    antecedentesRepository = mock(AntecedentesRepository.class);
    service = new ConsultaMedicaIntegracionServiceImpl(consultaRepository, pacienteRepository,
        historiaClinicaRepository, usuarioRepository, antecedentesRepository);
  }

  @Test void administradorPuedeConsultarPacienteActivoConVariasHistorias() {
    prepararUsuario(1, "ADMINISTRADOR");
    Paciente paciente = paciente(10, "12345678", EstadoRegistroPaciente.ACTIVO);
    when(pacienteRepository.findById(10)).thenReturn(Optional.of(paciente));
    when(historiaClinicaRepository.findIdsByPacienteId(10)).thenReturn(List.of(2, 7));
    when(consultaRepository.resumirAtendidasByPacienteId(10)).thenReturn(java.util.Collections.singletonList(new Object[]{4L, null, null}));

    ResumenConsultasPacienteResponse resumen = service.obtenerResumenPaciente(10, 1);

    assertEquals(10, resumen.getPaciente().getIdPaciente());
    assertEquals(List.of(2, 7), resumen.getPaciente().getIdsHistoriasClinicas());
    assertEquals(2L, resumen.getPaciente().getCantidadHistoriasClinicas());
    assertEquals(4L, resumen.getResumenAtencion().getTotalConsultasAtendidas());
    verify(consultaRepository).resumirAtendidasByPacienteId(10);
    verify(consultaRepository, never()).countByHistoriaClinicaIdHistoriaClinica(anyInt());
  }

  @Test void doctorPuedeConsultar() {
    prepararUsuario(2, "DOCTOR");
    when(pacienteRepository.findById(10)).thenReturn(Optional.of(paciente(10, "12345678", EstadoRegistroPaciente.ACTIVO)));
    when(historiaClinicaRepository.findIdsByPacienteId(10)).thenReturn(List.of());
    assertNotNull(service.obtenerResumenPaciente(10, 2));
  }

  @Test void enfermeroNoPuedeConsultar() {
    prepararUsuario(3, "ENFERMERO");
    assertError("ROL_SIN_PERMISO", 403, () -> service.obtenerResumenPaciente(10, 3));
    verifyNoInteractions(pacienteRepository, historiaClinicaRepository, consultaRepository);
  }

  @Test void requiereUsuarioExistente() {
    assertError("USUARIO_REQUERIDO", 401, () -> service.obtenerResumenPaciente(10, null));
    when(usuarioRepository.findById(99)).thenReturn(Optional.empty());
    assertError("USUARIO_INEXISTENTE", 401, () -> service.obtenerResumenPaciente(10, 99));
  }

  @Test void rechazaIdInvalidoYPacienteInexistente() {
    assertError("ID_PACIENTE_INVALIDO", 400, () -> service.obtenerResumenPaciente(0, 1));
    prepararUsuario(1, "ADMINISTRADOR");
    when(pacienteRepository.findById(55)).thenReturn(Optional.empty());
    assertError("PACIENTE_INEXISTENTE", 404, () -> service.obtenerResumenPaciente(55, 1));
  }

  @Test void pacienteArchivadoNoSigueNiCombinaPacientePrincipal() {
    prepararUsuario(1, "ADMINISTRADOR");
    Paciente archivado = paciente(10, "12345678", EstadoRegistroPaciente.ARCHIVADO);
    archivado.setPacientePrincipal(paciente(11, "12345678", EstadoRegistroPaciente.ACTIVO));
    when(pacienteRepository.findById(10)).thenReturn(Optional.of(archivado));

    assertError("PACIENTE_ARCHIVADO", 409, () -> service.obtenerResumenPaciente(10, 1));
    verifyNoInteractions(historiaClinicaRepository, consultaRepository);
    verify(pacienteRepository, never()).findById(11);
  }

  @Test void pacientesConMismoDniSeMantienenSeparadosYElConteoEsSoloAtendido() {
    prepararUsuario(1, "ADMINISTRADOR");
    when(pacienteRepository.findById(10)).thenReturn(Optional.of(paciente(10, "12345678", EstadoRegistroPaciente.ACTIVO)));
    when(historiaClinicaRepository.findIdsByPacienteId(10)).thenReturn(List.of(2));
    when(consultaRepository.resumirAtendidasByPacienteId(10)).thenReturn(java.util.Collections.singletonList(new Object[]{2L, null, null}));

    ResumenConsultasPacienteResponse resumen = service.obtenerResumenPaciente(10, 1);

    assertEquals(2L, resumen.getResumenAtencion().getTotalConsultasAtendidas());
    verify(pacienteRepository).findById(10);
    verify(pacienteRepository, never()).findByNumDocumentoAndEstadoRegistroOrderByIdPacienteAsc(anyString(), any());
    verify(consultaRepository).resumirAtendidasByPacienteId(10);
  }

  @Test void construyeAgregadosYLimitaConsultasRecientesSinFuncionesVitales() {
    prepararUsuario(1, "ADMINISTRADOR");
    when(pacienteRepository.findById(10)).thenReturn(Optional.of(paciente(10, "12345678", EstadoRegistroPaciente.ACTIVO)));
    when(historiaClinicaRepository.findIdsByPacienteId(10)).thenReturn(List.of(2, 7));
    java.sql.Date primera = java.sql.Date.valueOf("2025-01-02");
    java.sql.Date ultimaFecha = java.sql.Date.valueOf("2026-08-01");
    when(consultaRepository.resumirAtendidasByPacienteId(10)).thenReturn(
        java.util.Collections.singletonList(new Object[]{5L, primera, ultimaFecha}));
    when(consultaRepository.contarTiposAtendidosByPacienteId(10)).thenReturn(List.of(
        new Object[]{1, "RESPIRATORIA", 3L}, new Object[]{2, "ALERGICA", 2L}));
    when(consultaRepository.contarEspecialidadesAtendidasByPacienteId(10)).thenReturn(List.of(
        new Object[]{"MEDICINA_GENERAL", 4L}, new Object[]{"DERMATOLOGIA", 1L}));
    ConsultaResumenRecienteProjection reciente = mock(ConsultaResumenRecienteProjection.class);
    when(reciente.getIdConsulta()).thenReturn(20); when(reciente.getIdHistoriaClinica()).thenReturn(7);
    when(reciente.getFechaConsulta()).thenReturn(ultimaFecha); when(reciente.getEspecialidad()).thenReturn("MEDICINA_GENERAL");
    when(reciente.getDoctor()).thenReturn(" Ana   Ruiz "); when(reciente.getDiagnostico()).thenReturn("Registro profesional");
    when(consultaRepository.findRecientesAtendidasByPacienteId(eq(10), any(Pageable.class))).thenReturn(List.of(reciente));
    when(consultaRepository.resumirCalidadAtendidasByPacienteId(10)).thenReturn(
        java.util.Collections.singletonList(new Object[]{0L, 0L, 0L, 0L}));
    Antecedentes antecedentes = new Antecedentes(); antecedentes.setEnfermedadesPrevias("ASMA");
    when(antecedentesRepository.findByPacienteIdPaciente(10)).thenReturn(List.of(antecedentes));

    ResumenConsultasPacienteResponse resumen = service.obtenerResumenPaciente(10, 1);

    assertEquals(primera, resumen.getResumenAtencion().getFechaPrimeraConsulta());
    assertEquals(ultimaFecha, resumen.getResumenAtencion().getFechaUltimaConsulta());
    assertEquals("Ana Ruiz", resumen.getResumenAtencion().getUltimoDoctor());
    assertEquals(60D, resumen.getTiposEnfermedad().getFirst().getPorcentaje());
    assertEquals(80D, resumen.getEspecialidades().getFirst().getPorcentaje());
    assertEquals("ASMA", resumen.getAntecedentes().getEnfermedadesPrevias());
    assertEquals(20, resumen.getConsultasRecientes().getFirst().getIdConsulta());
    assertEquals(ultimaFecha, resumen.getConsultasRecientes().getFirst().getFecha());
    assertEquals("Registro profesional", resumen.getEvaluacionesRecientes().getFirst().getDiagnostico());
    assertNull(resumen.getFuncionesVitales().getPresionSistolica());
    verify(consultaRepository).findRecientesAtendidasByPacienteId(eq(10), argThat(p -> p.getPageSize() == 3));
  }

  private void prepararUsuario(int id, String rol) {
    Usuario usuario = new Usuario(); usuario.setIdUsuario(id); usuario.setTipoUsuario(rol); usuario.setEstado(true);
    when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));
  }

  private Paciente paciente(int id, String dni, EstadoRegistroPaciente estado) {
    Paciente paciente = new Paciente(); paciente.setIdPaciente(id); paciente.setNombres("Ana");
    paciente.setApellidos("Torres"); paciente.setNumDocumento(dni); paciente.setEstadoRegistro(estado);
    return paciente;
  }

  private void assertError(String resultado, int status, Runnable accion) {
    ConsultaMedicaIntegracionException error = assertThrows(ConsultaMedicaIntegracionException.class, accion::run);
    assertEquals(resultado, error.getCodigo()); assertEquals(status, error.getStatus().value());
  }
}
