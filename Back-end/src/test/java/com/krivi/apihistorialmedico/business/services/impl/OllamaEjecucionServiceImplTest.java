package com.krivi.apihistorialmedico.business.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.krivi.apihistorialmedico.business.services.OllamaService;
import com.krivi.apihistorialmedico.business.services.PacienteDuplicadoService;
import com.krivi.apihistorialmedico.business.services.PacienteService;
import com.krivi.apihistorialmedico.model.api.DuplicadosPacientesResponse;
import com.krivi.apihistorialmedico.model.api.OllamaEjecucionResponse;
import com.krivi.apihistorialmedico.model.api.OllamaInterpretacionResponse;
import com.krivi.apihistorialmedico.model.api.PacienteResponse;
import com.krivi.apihistorialmedico.model.api.PacienteDuplicadoComparacionResponse;
import com.krivi.apihistorialmedico.model.api.ResponseModelGet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class OllamaEjecucionServiceImplTest {
  private OllamaService ollamaService;
  private PacienteService pacienteService;
  private PacienteDuplicadoService pacienteDuplicadoService;
  private OllamaEjecucionServiceImpl service;

  @BeforeEach
  void setUp() {
    ollamaService = Mockito.mock(OllamaService.class);
    pacienteService = Mockito.mock(PacienteService.class);
    pacienteDuplicadoService = Mockito.mock(PacienteDuplicadoService.class);
    service = new OllamaEjecucionServiceImpl(
        ollamaService,
        pacienteService,
        pacienteDuplicadoService
    );
  }

  @Test
  void verificaExistenciaReutilizandoBusquedaQueAdmiteDuplicados() {
    when(ollamaService.interpretar("mensaje")).thenReturn(
        new OllamaInterpretacionResponse(
            "PACIENTES", "VERIFICAR_EXISTENCIA", "72845292", null
        )
    );
    ResponseModelGet<PacienteResponse> busqueda = new ResponseModelGet<>();
    busqueda.setData(List.of(PacienteResponse.builder().build(), PacienteResponse.builder().build()));
    when(pacienteService.search(null, "72845292", 25)).thenReturn(busqueda);

    OllamaEjecucionResponse response = service.ejecutar("mensaje");

    assertThat(response.encontrado()).isTrue();
    assertThat(response.dni()).isEqualTo("72845292");
    assertThat(response.mensaje()).isEqualTo("El paciente se encuentra registrado.");
    verify(pacienteService).search(null, "72845292", 25);
  }

  @Test
  void informaQueNoExisteCuandoLaBusquedaNoDevuelveRegistros() {
    when(ollamaService.interpretar("mensaje")).thenReturn(
        new OllamaInterpretacionResponse(
            "PACIENTES", "VERIFICAR_EXISTENCIA", "72845292", null
        )
    );
    ResponseModelGet<PacienteResponse> busqueda = new ResponseModelGet<>();
    busqueda.setData(List.of());
    when(pacienteService.search(null, "72845292", 25)).thenReturn(busqueda);

    OllamaEjecucionResponse response = service.ejecutar("mensaje");

    assertThat(response.encontrado()).isFalse();
    assertThat(response.mensaje()).isEqualTo("No se encontró un paciente con ese DNI.");
  }

  @Test
  void noConsultaPacientesCuandoFaltaElDni() {
    when(ollamaService.interpretar("mensaje")).thenReturn(
        new OllamaInterpretacionResponse(
            "PACIENTES", "VERIFICAR_EXISTENCIA", null, null
        )
    );

    OllamaEjecucionResponse response = service.ejecutar("mensaje");

    assertThat(response.encontrado()).isNull();
    assertThat(response.mensaje()).isEqualTo("Falta indicar el DNI del paciente.");
    verify(pacienteService, never()).search(Mockito.any(), Mockito.any(), Mockito.any());
  }

  @Test
  void noEjecutaOtrasIntenciones() {
    when(ollamaService.interpretar("mensaje")).thenReturn(
        new OllamaInterpretacionResponse("PACIENTES", "PACIENTES_ESTADISTICAS", null, null)
    );

    OllamaEjecucionResponse response = service.ejecutar("mensaje");

    assertThat(response.encontrado()).isNull();
    assertThat(response.mensaje()).isEqualTo(
        "Esta intención todavía no está habilitada para ejecución."
    );
    verify(pacienteService, never()).search(Mockito.any(), Mockito.any(), Mockito.any());
  }

  @Test
  void consultaTodosLosGruposDePacientesDuplicadosCuandoNoHayDni() {
    when(ollamaService.interpretar("mensaje")).thenReturn(
        new OllamaInterpretacionResponse("PACIENTES", "PACIENTES_DUPLICADOS", null, null)
    );
    DuplicadosPacientesResponse grupos = DuplicadosPacientesResponse.builder()
        .hayDuplicados(true)
        .totalGrupos(2)
        .build();
    when(pacienteService.obtenerDuplicadosParaIntegracion()).thenReturn(grupos);

    OllamaEjecucionResponse response = service.ejecutar("mensaje");

    assertThat(response.gruposDuplicados()).isSameAs(grupos);
    assertThat(response.comparacionDuplicados()).isNull();
    verify(pacienteService).obtenerDuplicadosParaIntegracion();
    verify(pacienteDuplicadoService, never()).compararPorDni(Mockito.any());
  }

  @Test
  void consultaPacientesDuplicadosPorDni() {
    when(ollamaService.interpretar("mensaje")).thenReturn(
        new OllamaInterpretacionResponse(
            "PACIENTES", "PACIENTES_DUPLICADOS", "72845292", null
        )
    );
    PacienteDuplicadoComparacionResponse comparacion =
        PacienteDuplicadoComparacionResponse.builder()
            .dni("72845292")
            .esDuplicado(true)
            .mensaje("Se encontraron 2 pacientes activos con el mismo DNI.")
            .build();
    when(pacienteDuplicadoService.compararPorDni("72845292")).thenReturn(comparacion);

    OllamaEjecucionResponse response = service.ejecutar("mensaje");

    assertThat(response.comparacionDuplicados()).isSameAs(comparacion);
    assertThat(response.gruposDuplicados()).isNull();
    verify(pacienteDuplicadoService).compararPorDni("72845292");
    verify(pacienteService, never()).obtenerDuplicadosParaIntegracion();
  }

  @Test
  void noConsultaDuplicadosCuandoElDniEsInvalido() {
    when(ollamaService.interpretar("mensaje")).thenReturn(
        new OllamaInterpretacionResponse("PACIENTES", "PACIENTES_DUPLICADOS", "123", null)
    );

    OllamaEjecucionResponse response = service.ejecutar("mensaje");

    assertThat(response.mensaje()).isEqualTo("El DNI debe contener exactamente 8 dígitos.");
    verify(pacienteDuplicadoService, never()).compararPorDni(Mockito.any());
    verify(pacienteService, never()).obtenerDuplicadosParaIntegracion();
  }

  @Test
  void requiereDniCuandoSoloSeInformaNombreParaDuplicados() {
    when(ollamaService.interpretar("mensaje")).thenReturn(
        new OllamaInterpretacionResponse(
            "PACIENTES", "PACIENTES_DUPLICADOS", null, "Ana Torres"
        )
    );

    OllamaEjecucionResponse response = service.ejecutar("mensaje");

    assertThat(response.mensaje()).isEqualTo(
        "Para consultar pacientes duplicados específicos se requiere el DNI."
    );
    verify(pacienteDuplicadoService, never()).compararPorDni(Mockito.any());
    verify(pacienteService, never()).obtenerDuplicadosParaIntegracion();
  }

  @Test
  void buscaPorDniYDevuelveTodosLosRegistrosEncontrados() {
    when(ollamaService.interpretar("mensaje")).thenReturn(
        new OllamaInterpretacionResponse("PACIENTES", "BUSCAR_PACIENTE", "72845292", null)
    );
    List<PacienteResponse> pacientes = List.of(
        PacienteResponse.builder().idPaciente(1).numDocumento("72845292").build(),
        PacienteResponse.builder().idPaciente(2).numDocumento("72845292").build()
    );
    ResponseModelGet<PacienteResponse> busqueda = new ResponseModelGet<>();
    busqueda.setData(pacientes);
    when(pacienteService.search(null, "72845292", 25)).thenReturn(busqueda);

    OllamaEjecucionResponse response = service.ejecutar("mensaje");

    assertThat(response.encontrado()).isTrue();
    assertThat(response.pacientes()).containsExactlyElementsOf(pacientes);
    verify(pacienteService).search(null, "72845292", 25);
  }

  @Test
  void buscaPorNombreEInformaCuandoNoHayResultados() {
    when(ollamaService.interpretar("mensaje")).thenReturn(
        new OllamaInterpretacionResponse("PACIENTES", "BUSCAR_PACIENTE", null, "Ana Torres")
    );
    ResponseModelGet<PacienteResponse> busqueda = new ResponseModelGet<>();
    busqueda.setData(List.of());
    when(pacienteService.search("Ana Torres", null, 25)).thenReturn(busqueda);

    OllamaEjecucionResponse response = service.ejecutar("mensaje");

    assertThat(response.encontrado()).isFalse();
    assertThat(response.pacientes()).isEmpty();
    assertThat(response.mensaje()).isEqualTo(
        "No se encontraron pacientes con el criterio indicado."
    );
  }

  @Test
  void noConsultaPacientesCuandoFaltaElCriterioDeBusqueda() {
    when(ollamaService.interpretar("mensaje")).thenReturn(
        new OllamaInterpretacionResponse("PACIENTES", "BUSCAR_PACIENTE", null, " ")
    );

    OllamaEjecucionResponse response = service.ejecutar("mensaje");

    assertThat(response.encontrado()).isNull();
    assertThat(response.mensaje()).isEqualTo("Falta indicar el DNI o el nombre del paciente.");
    verify(pacienteService, never()).search(Mockito.any(), Mockito.any(), Mockito.any());
  }
}
