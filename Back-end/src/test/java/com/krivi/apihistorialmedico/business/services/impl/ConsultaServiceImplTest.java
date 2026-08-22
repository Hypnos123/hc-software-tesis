package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.model.api.ConsultaRequest;
import com.krivi.apihistorialmedico.model.entity.Consulta;
import com.krivi.apihistorialmedico.model.entity.Empleado;
import com.krivi.apihistorialmedico.model.entity.Usuario;
import com.krivi.apihistorialmedico.model.entity.Paciente;
import com.krivi.apihistorialmedico.model.entity.HistoriaClinica;
import com.krivi.apihistorialmedico.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultaServiceImplTest {
  @Mock ConsultaRepository consultaRepository;
  @Mock PacienteRepository pacienteRepository;
  @Mock TipoEnfermedadRepository tipoEnfermedadRepository;
  @Mock UsuarioRepository usuarioRepository;
  @Mock HistoriaClinicaRepository historiaClinicaRepository;
  @Mock EmpleadoRepository empleadoRepository;
  @Mock AntecedentesRepository antecedentesRepository;
  @InjectMocks ConsultaServiceImpl service;

  @Test void finalizarAtencionAsignaFechaSoloLaPrimeraVez() {
    Empleado medico = new Empleado(); medico.setIdEmpleado(1);
    Usuario usuario = new Usuario(); usuario.setIdUsuario(9); usuario.setEstado(true); usuario.setTipoUsuario("DOCTOR"); usuario.setEmpleado(medico);
    Consulta consulta = new Consulta(); consulta.setIdConsulta(4); consulta.setEstado("PENDIENTE"); consulta.setDoctorResponsable(medico);
    when(usuarioRepository.findById(9)).thenReturn(Optional.of(usuario)); when(consultaRepository.findById(4)).thenReturn(Optional.of(consulta)); when(consultaRepository.save(consulta)).thenReturn(consulta);
    ConsultaRequest request = ConsultaRequest.builder().diagnostico("dato").tratamiento("dato").build();
    service.finalizarAtencion(4, request, 9);
    LocalDateTime fechaAtencion = consulta.getFechaAtencion();
    assertEquals("ATENDIDO", consulta.getEstado()); assertNotNull(fechaAtencion);

    consulta.setDiagnostico("otro");
    assertEquals(fechaAtencion, consulta.getFechaAtencion());
  }

  @Test void doctorNoPuedeCrearConsultaPorLlamadaDirecta() {
    Empleado medico = new Empleado(); medico.setCargo("DOCTOR");
    Usuario usuario = new Usuario(); usuario.setEstado(true); usuario.setTipoUsuario("DOCTOR"); usuario.setEmpleado(medico);
    when(usuarioRepository.findById(9)).thenReturn(Optional.of(usuario));

    SecurityException error = assertThrows(SecurityException.class, () -> service.save(ConsultaRequest.builder().build(), 9));

    assertEquals("El rol del usuario no permite crear consultas", error.getMessage());
    verify(consultaRepository, never()).save(any());
  }

  @Test void administradorEnfermeroYDoctorPuedenVisualizarConsulta() {
    Consulta consulta = consultaCompleta();
    when(consultaRepository.findById(4)).thenReturn(Optional.of(consulta));
    when(antecedentesRepository.findByPacienteIdPaciente(20)).thenReturn(java.util.List.of());

    for (int id = 1; id <= 3; id++) {
      String rol = id == 1 ? "ADMINISTRADOR" : id == 2 ? "ENFERMERO" : "DOCTOR";
      Usuario usuario = usuarioActivo(id, rol);
      when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));
      assertEquals(1, service.findById(4, id).getData().size(), rol);
    }
  }

  @Test void usuarioSinPermisoOInexistenteNoPuedeVisualizarConsulta() {
    Consulta consulta = consultaCompleta();
    when(consultaRepository.findById(4)).thenReturn(Optional.of(consulta));
    when(usuarioRepository.findById(8)).thenReturn(Optional.of(usuarioActivo(8, "RECEPCIONISTA")));
    when(usuarioRepository.findById(99)).thenReturn(Optional.empty());

    assertTrue(service.findById(4, 8).getData().isEmpty());
    assertThrows(SecurityException.class, () -> service.findById(4, 99));
  }

  private Usuario usuarioActivo(int id, String rol) {
    Usuario usuario = new Usuario();
    usuario.setIdUsuario(id); usuario.setEstado(true); usuario.setTipoUsuario(rol);
    Empleado empleado = new Empleado(); empleado.setIdEmpleado(id); empleado.setCargo(rol);
    usuario.setEmpleado(empleado);
    return usuario;
  }

  private Consulta consultaCompleta() {
    Paciente paciente = new Paciente(); paciente.setIdPaciente(20);
    HistoriaClinica historia = new HistoriaClinica(); historia.setIdHistoriaClinica(30); historia.setPaciente(paciente);
    Consulta consulta = new Consulta(); consulta.setIdConsulta(4); consulta.setPaciente(paciente); consulta.setHistoriaClinica(historia);
    return consulta;
  }
}
