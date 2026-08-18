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
  private ConsultaMedicaIntegracionServiceImpl service;

  @BeforeEach void setUp() {
    consultaRepository = mock(ConsultaRepository.class);
    pacienteRepository = mock(PacienteRepository.class);
    historiaClinicaRepository = mock(HistoriaClinicaRepository.class);
    usuarioRepository = mock(UsuarioRepository.class);
    service = new ConsultaMedicaIntegracionServiceImpl(consultaRepository, pacienteRepository,
        historiaClinicaRepository, usuarioRepository);
  }

  @Test void administradorPuedeConsultarPacienteActivoConVariasHistorias() {
    prepararUsuario(1, "ADMINISTRADOR");
    Paciente paciente = paciente(10, "12345678", EstadoRegistroPaciente.ACTIVO);
    when(pacienteRepository.findById(10)).thenReturn(Optional.of(paciente));
    when(historiaClinicaRepository.findIdsByPacienteId(10)).thenReturn(List.of(2, 7));
    when(consultaRepository.countAtendidasByPacienteId(10)).thenReturn(4L);

    ResumenConsultasPacienteResponse resumen = service.obtenerResumenPaciente(10, 1);

    assertEquals(10, resumen.getPaciente().getIdPaciente());
    assertEquals(List.of(2, 7), resumen.getPaciente().getIdsHistoriasClinicas());
    assertEquals(2L, resumen.getPaciente().getCantidadHistoriasClinicas());
    assertEquals(4L, resumen.getResumenAtencion().getTotalConsultasAtendidas());
    verify(consultaRepository).countAtendidasByPacienteId(10);
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
    when(consultaRepository.countAtendidasByPacienteId(10)).thenReturn(2L);

    ResumenConsultasPacienteResponse resumen = service.obtenerResumenPaciente(10, 1);

    assertEquals(2L, resumen.getResumenAtencion().getTotalConsultasAtendidas());
    verify(pacienteRepository).findById(10);
    verify(pacienteRepository, never()).findByNumDocumentoAndEstadoRegistroOrderByIdPacienteAsc(anyString(), any());
    verify(consultaRepository).countAtendidasByPacienteId(10);
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
