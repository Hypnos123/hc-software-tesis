package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.model.api.ConsultaRequest;
import com.krivi.apihistorialmedico.model.entity.Consulta;
import com.krivi.apihistorialmedico.model.entity.Empleado;
import com.krivi.apihistorialmedico.model.entity.Usuario;
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
}
