package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.model.api.AntecedentesRequest;
import com.krivi.apihistorialmedico.model.entity.Antecedentes;
import com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente;
import com.krivi.apihistorialmedico.model.entity.Paciente;
import com.krivi.apihistorialmedico.repository.AntecedentesRepository;
import com.krivi.apihistorialmedico.repository.PacienteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AntecedentesServiceImplTest {
  @Mock AntecedentesRepository antecedentesRepository;
  @Mock PacienteRepository pacienteRepository;
  @InjectMocks AntecedentesServiceImpl service;

  @Test
  void creaAntecedentesConElPacienteActivoAdministradoYSuVersionReal() {
    Paciente paciente = pacienteActivo(20, 7L);
    when(pacienteRepository.findByIdPacienteAndEstadoRegistro(20, EstadoRegistroPaciente.ACTIVO))
        .thenReturn(Optional.of(paciente));
    when(antecedentesRepository.save(any(Antecedentes.class))).thenAnswer(invocation -> {
      Antecedentes antecedentes = invocation.getArgument(0);
      antecedentes.setIdAntecedentes(30);
      return antecedentes;
    });

    var response = service.save(request(null, 20, "Asma"));

    ArgumentCaptor<Antecedentes> captor = ArgumentCaptor.forClass(Antecedentes.class);
    verify(antecedentesRepository).save(captor.capture());
    assertSame(paciente, captor.getValue().getPaciente());
    assertEquals(7L, captor.getValue().getPaciente().getVersion());
    assertEquals(30, response.getIdGenerado());
    verify(pacienteRepository).findByIdPacienteAndEstadoRegistro(20, EstadoRegistroPaciente.ACTIVO);
    verify(pacienteRepository, never()).save(any());
  }

  @Test
  void editaLaEntidadDeAntecedentesExistenteSinReemplazarSuPaciente() {
    Paciente paciente = pacienteActivo(20, 5L);
    Antecedentes existentes = new Antecedentes();
    existentes.setIdAntecedentes(30);
    existentes.setPaciente(paciente);
    existentes.setEnfermedadesPrevias("Ninguna");
    when(pacienteRepository.findByIdPacienteAndEstadoRegistro(20, EstadoRegistroPaciente.ACTIVO))
        .thenReturn(Optional.of(paciente));
    when(antecedentesRepository.findByIdAntecedentesAndPacienteEstadoRegistro(30, EstadoRegistroPaciente.ACTIVO))
        .thenReturn(Optional.of(existentes));

    service.update(request(30, 20, "Asma controlada"));

    assertEquals("Asma controlada", existentes.getEnfermedadesPrevias());
    assertSame(paciente, existentes.getPaciente());
    assertEquals(5L, paciente.getVersion());
    verify(antecedentesRepository).save(existentes);
    verify(pacienteRepository, never()).save(any());
  }

  @Test
  void permiteConsultarNuevamenteLosAntecedentesGuardados() {
    Paciente paciente = pacienteActivo(20, 3L);
    Antecedentes guardados = new Antecedentes();
    guardados.setIdAntecedentes(30);
    guardados.setPaciente(paciente);
    guardados.setEnfermedadesPrevias("Asma");
    when(antecedentesRepository.findByPacienteIdPacienteAndPacienteEstadoRegistro(20, EstadoRegistroPaciente.ACTIVO))
        .thenReturn(List.of(guardados));

    var response = service.findByPaciente(20);

    assertEquals(1, response.getData().size());
    assertEquals("Asma", response.getData().getFirst().getEnfermedadesPrevias());
    assertEquals(20, response.getData().getFirst().getIdPaciente());
  }

  @Test
  void rechazaPacienteArchivadoSinIntentarPersistirAntecedentes() {
    when(pacienteRepository.findByIdPacienteAndEstadoRegistro(20, EstadoRegistroPaciente.ACTIVO))
        .thenReturn(Optional.empty());

    IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
        () -> service.save(request(null, 20, "Asma")));

    assertTrue(error.getMessage().contains("archivado"));
    verify(antecedentesRepository, never()).save(any());
  }

  private Paciente pacienteActivo(int id, long version) {
    Paciente paciente = new Paciente();
    paciente.setIdPaciente(id);
    paciente.setNombres("Ana");
    paciente.setApellidos("Paz");
    paciente.setEstadoRegistro(EstadoRegistroPaciente.ACTIVO);
    paciente.setVersion(version);
    return paciente;
  }

  private AntecedentesRequest request(Integer idAntecedentes, int idPaciente, String enfermedad) {
    return AntecedentesRequest.builder()
        .idAntecedentes(idAntecedentes)
        .idPaciente(idPaciente)
        .alimentacion("Normal")
        .enfermedadesPrevias(enfermedad)
        .build();
  }
}
