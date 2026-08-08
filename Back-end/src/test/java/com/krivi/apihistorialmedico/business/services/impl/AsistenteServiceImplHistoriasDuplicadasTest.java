package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.services.HistoriaClinicaService;
import com.krivi.apihistorialmedico.model.api.*;
import com.krivi.apihistorialmedico.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsistenteServiceImplHistoriasDuplicadasTest {
  private static final String DNI_PRUEBA = "0".repeat(8);
  @Mock private PacienteRepository pacienteRepository;
  @Mock private HistoriaClinicaRepository historiaClinicaRepository;
  @Mock private ConsultaRepository consultaRepository;
  @Mock private UsuarioRepository usuarioRepository;
  @Mock private HistoriaClinicaService historiaClinicaService;
  @InjectMocks private AsistenteServiceImpl asistenteService;

  @Test
  void reconoceConsultasGeneralesDeHistoriasDuplicadas() {
    when(historiaClinicaService.obtenerDuplicadosParaIntegracion(null)).thenReturn(resultadoDuplicado());
    List<String> frases = List.of("¿Existen historias clínicas duplicadas?", "Busca historias clínicas repetidas",
        "Revisa la duplicidad de historias clínicas", "Detecta historias clínicas duplicadas",
        "Busca pacientes con más de una historia clínica");

    frases.forEach(frase -> {
      AsistenteResponse response = asistenteService.preguntar(request(frase), null);
      assertEquals("HISTORIAS_CLINICAS_DUPLICADAS", response.getIntencion());
      assertTrue(response.getRespuesta().contains("ID historia clínica: 12"));
      assertFalse(response.getRespuesta().contains("posibles pacientes duplicados"));
    });
    verifyNoInteractions(pacienteRepository);
  }

  @Test
  void reconoceConsultasPorDniYDevuelveDetalleSinAccionesDestructivas() {
    when(historiaClinicaService.obtenerDuplicadosParaIntegracion(DNI_PRUEBA)).thenReturn(resultadoDuplicado());
    List<String> frases = List.of("¿El DNI " + DNI_PRUEBA + " tiene historias clínicas duplicadas?",
        "Busca historias repetidas del DNI " + DNI_PRUEBA, "Verifica historias clínicas del paciente con DNI " + DNI_PRUEBA);

    frases.forEach(frase -> {
      AsistenteResponse response = asistenteService.preguntar(request(frase), null);
      assertEquals("HISTORIAS_CLINICAS_DUPLICADAS", response.getIntencion());
      assertTrue(response.getRespuesta().contains("Se recomienda conservar"));
      assertFalse(response.getRespuesta().toLowerCase().contains("contraseña"));
      assertFalse(response.getRespuesta().toLowerCase().contains("archivar"));
    });
    verify(historiaClinicaService, times(3)).obtenerDuplicadosParaIntegracion(DNI_PRUEBA);
  }

  private AsistenteRequest request(String pregunta) {
    AsistenteRequest request = new AsistenteRequest();
    request.setPregunta(pregunta);
    return request;
  }

  private DuplicadosHistoriasClinicasResponse resultadoDuplicado() {
    HistoriaClinicaIntegracionItemResponse historia = HistoriaClinicaIntegracionItemResponse.builder()
        .idHistoriaClinica(12).idPaciente(4).nombreCompleto("PACIENTE PRUEBA UNO").dni(DNI_PRUEBA)
        .fechaCreacion(LocalDateTime.of(2025, 1, 1, 9, 0)).ultimaActualizacion(LocalDateTime.of(2026, 1, 1, 9, 0))
        .cantidadConsultas(3).estado("ACTIVA").build();
    GrupoDuplicadoHistoriaClinicaResponse grupo = GrupoDuplicadoHistoriaClinicaResponse.builder()
        .tipo("dni").valorCoincidente(DNI_PRUEBA).cantidad(2).historiasClinicas(List.of(historia))
        .idHistoriaClinicaRecomendada(12)
        .recomendacion("Se recomienda conservar la historia clínica ID 12 porque contiene 3 consultas.").build();
    return DuplicadosHistoriasClinicasResponse.builder().hayDuplicados(true).totalGrupos(1).dniConsultado(DNI_PRUEBA)
        .mensaje("Se encontraron 2 posibles historias clínicas duplicadas para el DNI de prueba.")
        .duplicados(List.of(grupo)).build();
  }
}
