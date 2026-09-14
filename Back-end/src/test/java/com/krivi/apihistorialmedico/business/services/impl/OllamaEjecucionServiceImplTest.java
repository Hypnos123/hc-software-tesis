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
import com.krivi.apihistorialmedico.model.api.PacienteRegistroResponse;
import com.krivi.apihistorialmedico.model.api.UltimosPacientesResponse;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
  void conservaInterpretacionValidaDePacientesDuplicados() {
    OllamaInterpretacionResponse interpretacion = new OllamaInterpretacionResponse(
        "PACIENTES", "PACIENTES_DUPLICADOS", "72845292", "Ana Pérez Gómez"
    );

    OllamaInterpretacionResponse normalizada = service.normalizarInterpretacion(interpretacion);

    assertThat(normalizada).isSameAs(interpretacion);
  }

  @Test
  void normalizaUnicamenteCategoriaInconsistenteDePacientesDuplicados() {
    OllamaInterpretacionResponse interpretacion = new OllamaInterpretacionResponse(
        "PACIENTES_DUPLICADOS",
        "PACIENTES_DUPLICADOS",
        "72845292",
        "Ana Pérez Gómez"
    );

    OllamaInterpretacionResponse normalizada = service.normalizarInterpretacion(interpretacion);

    assertThat(normalizada.categoria()).isEqualTo("PACIENTES");
    assertThat(normalizada.intencion()).isEqualTo(interpretacion.intencion());
    assertThat(normalizada.dni()).isEqualTo(interpretacion.dni());
    assertThat(normalizada.nombre()).isEqualTo(interpretacion.nombre());
  }

  @Test
  void conservaInterpretacionValidaDeBuscarPaciente() {
    OllamaInterpretacionResponse interpretacion = new OllamaInterpretacionResponse(
        "PACIENTES", "BUSCAR_PACIENTE", "72845292", "Ana Pérez Gómez"
    );

    OllamaInterpretacionResponse normalizada = service.normalizarInterpretacion(interpretacion);

    assertThat(normalizada).isSameAs(interpretacion);
  }

  @Test
  void conservaInterpretacionValidaDeVerificarExistencia() {
    OllamaInterpretacionResponse interpretacion = new OllamaInterpretacionResponse(
        "PACIENTES", "VERIFICAR_EXISTENCIA", "72845292", null
    );

    OllamaInterpretacionResponse normalizada = service.normalizarInterpretacion(interpretacion);

    assertThat(normalizada).isSameAs(interpretacion);
  }

  @Test
  void normalizaComoNombreUnTextoRecibidoEnDni() {
    OllamaInterpretacionResponse normalizada = service.normalizarInterpretacion(
        new OllamaInterpretacionResponse(
            "PACIENTES", "VERIFICAR_EXISTENCIA", "Rafael Velasquez Morales", null
        )
    );

    assertThat(normalizada.dni()).isNull();
    assertThat(normalizada.nombre()).isEqualTo("Rafael Velasquez Morales");
  }

  @Test
  void normalizaComoDniUnNumeroRecibidoEnNombre() {
    OllamaInterpretacionResponse normalizada = service.normalizarInterpretacion(
        new OllamaInterpretacionResponse(
            "PACIENTES", "BUSCAR_PACIENTE", null, "72845292"
        )
    );

    assertThat(normalizada.dni()).isEqualTo("72845292");
    assertThat(normalizada.nombre()).isNull();
  }

  @Test
  void ejecutaConsultaPorDniDespuesDeNormalizarCategoriaDeDuplicados() {
    when(ollamaService.interpretar("mensaje")).thenReturn(
        new OllamaInterpretacionResponse(
            "PACIENTES_DUPLICADOS", "PACIENTES_DUPLICADOS", "72845292", null
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

    assertThat(response.categoria()).isEqualTo("PACIENTES");
    assertThat(response.intencion()).isEqualTo("PACIENTES_DUPLICADOS");
    assertThat(response.dni()).isEqualTo("72845292");
    assertThat(response.comparacionDuplicados()).isSameAs(comparacion);
    verify(pacienteDuplicadoService).compararPorDni("72845292");
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
    assertThat(response.pacientes()).containsExactlyElementsOf(busqueda.getData());
    assertThat(response.mensaje()).isEqualTo("El paciente se encuentra registrado.");
    verify(pacienteService).search(null, "72845292", 25);
  }

  @Test
  void verificaExistenciaPorNombreYDevuelveTodasLasCoincidencias() {
    String nombre = "Rafael Velasquez Morales";
    when(ollamaService.interpretar("mensaje")).thenReturn(
        new OllamaInterpretacionResponse(
            "PACIENTES", "VERIFICAR_EXISTENCIA", null, nombre
        )
    );
    List<PacienteResponse> pacientes = List.of(
        PacienteResponse.builder().idPaciente(1).nombres("Rafael").build(),
        PacienteResponse.builder().idPaciente(2).nombres("Rafael").build()
    );
    ResponseModelGet<PacienteResponse> busqueda = new ResponseModelGet<>();
    busqueda.setData(pacientes);
    when(pacienteService.search(nombre, null, 25)).thenReturn(busqueda);

    OllamaEjecucionResponse response = service.ejecutar("mensaje");

    assertThat(response.encontrado()).isTrue();
    assertThat(response.nombre()).isEqualTo(nombre);
    assertThat(response.pacientes()).containsExactlyElementsOf(pacientes);
    verify(pacienteService).search(nombre, null, 25);
  }

  @Test
  void verificaExistenciaPorNombreAunqueOllamaLoEntregueComoDni() {
    String nombre = "Rafael Velasquez Morales";
    when(ollamaService.interpretar("mensaje")).thenReturn(
        new OllamaInterpretacionResponse(
            "PACIENTES", "VERIFICAR_EXISTENCIA", nombre, null
        )
    );
    ResponseModelGet<PacienteResponse> busqueda = new ResponseModelGet<>();
    busqueda.setData(List.of(PacienteResponse.builder().idPaciente(1).build()));
    when(pacienteService.search(nombre, null, 25)).thenReturn(busqueda);

    OllamaEjecucionResponse response = service.ejecutar("mensaje");

    assertThat(response.dni()).isNull();
    assertThat(response.nombre()).isEqualTo(nombre);
    assertThat(response.pacientes()).hasSize(1);
    verify(pacienteService).search(nombre, null, 25);
  }

  @Test
  void buscaPorDniAunqueOllamaLoEntregueComoNombre() {
    when(ollamaService.interpretar("mensaje")).thenReturn(
        new OllamaInterpretacionResponse(
            "PACIENTES", "BUSCAR_PACIENTE", null, "72845292"
        )
    );
    ResponseModelGet<PacienteResponse> busqueda = new ResponseModelGet<>();
    busqueda.setData(List.of(PacienteResponse.builder().numDocumento("72845292").build()));
    when(pacienteService.search(null, "72845292", 25)).thenReturn(busqueda);

    OllamaEjecucionResponse response = service.ejecutar("mensaje");

    assertThat(response.dni()).isEqualTo("72845292");
    assertThat(response.nombre()).isNull();
    assertThat(response.pacientes()).hasSize(1);
    verify(pacienteService).search(null, "72845292", 25);
  }

  @Test
  void conservaNumeroCortoComoDniInvalido() {
    when(ollamaService.interpretar("mensaje")).thenReturn(
        new OllamaInterpretacionResponse(
            "PACIENTES", "VERIFICAR_EXISTENCIA", "72845", null
        )
    );

    OllamaEjecucionResponse response = service.ejecutar("mensaje");

    assertThat(response.dni()).isEqualTo("72845");
    assertThat(response.nombre()).isNull();
    assertThat(response.mensaje()).isEqualTo("El DNI debe contener exactamente 8 dígitos.");
    verify(pacienteService, never()).search(Mockito.any(), Mockito.any(), Mockito.any());
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
  void noConsultaPacientesCuandoFaltaDniYNombre() {
    when(ollamaService.interpretar("mensaje")).thenReturn(
        new OllamaInterpretacionResponse(
            "PACIENTES", "VERIFICAR_EXISTENCIA", null, null
        )
    );

    OllamaEjecucionResponse response = service.ejecutar("mensaje");

    assertThat(response.encontrado()).isNull();
    assertThat(response.mensaje()).isEqualTo(
        "Indica el DNI o el nombre del paciente que deseas consultar."
    );
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
  void consultaTresUltimosPacientesCuandoNoSeIndicaLimite() {
    when(ollamaService.interpretar("mensaje")).thenReturn(
        new OllamaInterpretacionResponse(
            "PACIENTES", "ULTIMOS_PACIENTES", null, null, null
        )
    );
    List<PacienteRegistroResponse> pacientes = pacientesRecientes(3);
    when(pacienteService.obtenerUltimosParaIntegracion(3)).thenReturn(
        UltimosPacientesResponse.builder().cantidad(3).pacientes(pacientes).build()
    );

    OllamaEjecucionResponse response = service.ejecutar("mensaje");

    assertThat(response.pacientes()).containsExactlyElementsOf(pacientes);
    assertThat(response.mensaje()).isEqualTo(
        "Estos son los 3 pacientes registrados más recientemente."
    );
    verify(pacienteService).obtenerUltimosParaIntegracion(3);
  }

  @ParameterizedTest
  @CsvSource({"3,3", "4,4", "5,5", "6,6", "1,3", "10,6", "0,3", "-2,3"})
  void limitaLaCantidadDeUltimosPacientesEntreTresYSeis(
      int limiteInterpretado,
      int limiteEsperado
  ) {
    when(ollamaService.interpretar("mensaje")).thenReturn(
        new OllamaInterpretacionResponse(
            "PACIENTES", "ULTIMOS_PACIENTES", null, null, limiteInterpretado
        )
    );
    List<PacienteRegistroResponse> pacientes = pacientesRecientes(limiteEsperado);
    when(pacienteService.obtenerUltimosParaIntegracion(limiteEsperado)).thenReturn(
        UltimosPacientesResponse.builder()
            .cantidad(limiteEsperado)
            .pacientes(pacientes)
            .build()
    );

    OllamaEjecucionResponse response = service.ejecutar("mensaje");

    assertThat(response.pacientes()).hasSize(limiteEsperado);
    verify(pacienteService).obtenerUltimosParaIntegracion(limiteEsperado);
  }

  @Test
  void informaCuandoNoExistenPacientesActivosRecientes() {
    when(ollamaService.interpretar("mensaje")).thenReturn(
        new OllamaInterpretacionResponse(
            "PACIENTES", "ULTIMOS_PACIENTES", null, null, 3
        )
    );
    when(pacienteService.obtenerUltimosParaIntegracion(3)).thenReturn(
        UltimosPacientesResponse.builder().cantidad(0).pacientes(List.of()).build()
    );

    OllamaEjecucionResponse response = service.ejecutar("mensaje");

    assertThat(response.encontrado()).isFalse();
    assertThat(response.mensaje()).isEqualTo("No se encontraron pacientes activos registrados.");
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
  void rechazaNombreIncompletoParaDuplicados() {
    when(ollamaService.interpretar("mensaje")).thenReturn(
        new OllamaInterpretacionResponse(
            "PACIENTES", "PACIENTES_DUPLICADOS", null, "Ana Torres"
        )
    );

    OllamaEjecucionResponse response = service.ejecutar("mensaje");

    assertThat(response.mensaje()).isEqualTo(
        "Para verificar duplicados por nombre se necesita el nombre y los dos apellidos."
    );
    verify(pacienteDuplicadoService, never()).compararPorDni(Mockito.any());
    verify(pacienteService, never()).search(Mockito.any(), Mockito.any(), Mockito.any());
  }

  @Test
  void devuelveTodosLosDuplicadosConNombreCompletoExacto() {
    String nombre = "Ana Pérez Gómez";
    when(ollamaService.interpretar("mensaje")).thenReturn(
        new OllamaInterpretacionResponse("PACIENTES", "PACIENTES_DUPLICADOS", null, nombre)
    );
    PacienteResponse primero = PacienteResponse.builder()
        .idPaciente(1).nombres("Ana").apellidos("Pérez Gómez").build();
    PacienteResponse segundo = PacienteResponse.builder()
        .idPaciente(2).nombres("ANA").apellidos("PEREZ GOMEZ").build();
    PacienteResponse parcial = PacienteResponse.builder()
        .idPaciente(3).nombres("Ana María").apellidos("Pérez Gómez").build();
    ResponseModelGet<PacienteResponse> busqueda = new ResponseModelGet<>();
    busqueda.setData(List.of(primero, segundo, parcial));
    when(pacienteService.search(nombre, null, 25)).thenReturn(busqueda);

    OllamaEjecucionResponse response = service.ejecutar("mensaje");

    assertThat(response.pacientes()).containsExactly(primero, segundo);
    assertThat(response.mensaje()).isEqualTo(
        "Se encontraron 2 pacientes activos con el mismo nombre completo."
    );
    verify(pacienteService).search(nombre, null, 25);
    verify(pacienteDuplicadoService, never()).compararPorDni(Mockito.any());
  }

  @Test
  void informaCuandoElNombreCompletoCorrespondeAUnSoloPaciente() {
    String nombre = "Ana Pérez Gómez";
    when(ollamaService.interpretar("mensaje")).thenReturn(
        new OllamaInterpretacionResponse("PACIENTES", "PACIENTES_DUPLICADOS", null, nombre)
    );
    PacienteResponse paciente = PacienteResponse.builder()
        .idPaciente(1).nombres("Ana").apellidos("Pérez Gómez").build();
    ResponseModelGet<PacienteResponse> busqueda = new ResponseModelGet<>();
    busqueda.setData(List.of(paciente));
    when(pacienteService.search(nombre, null, 25)).thenReturn(busqueda);

    OllamaEjecucionResponse response = service.ejecutar("mensaje");

    assertThat(response.pacientes()).containsExactly(paciente);
    assertThat(response.mensaje()).isEqualTo(
        "El nombre completo corresponde a un único paciente activo y no presenta duplicados."
    );
  }

  @Test
  void informaCuandoNoExisteElNombreCompleto() {
    String nombre = "Ana Pérez Gómez";
    when(ollamaService.interpretar("mensaje")).thenReturn(
        new OllamaInterpretacionResponse("PACIENTES", "PACIENTES_DUPLICADOS", null, nombre)
    );
    ResponseModelGet<PacienteResponse> busqueda = new ResponseModelGet<>();
    busqueda.setData(List.of());
    when(pacienteService.search(nombre, null, 25)).thenReturn(busqueda);

    OllamaEjecucionResponse response = service.ejecutar("mensaje");

    assertThat(response.pacientes()).isEmpty();
    assertThat(response.mensaje()).isEqualTo(
        "No se encontraron pacientes activos con ese nombre completo."
    );
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
  void buscaPorNombreYDevuelveTodosLosRegistrosEncontrados() {
    String nombre = "Rafael Velasquez Morales";
    when(ollamaService.interpretar("mensaje")).thenReturn(
        new OllamaInterpretacionResponse("PACIENTES", "BUSCAR_PACIENTE", null, nombre)
    );
    List<PacienteResponse> pacientes = List.of(
        PacienteResponse.builder().idPaciente(1).nombres("Rafael").build(),
        PacienteResponse.builder().idPaciente(2).nombres("Rafael").build()
    );
    ResponseModelGet<PacienteResponse> busqueda = new ResponseModelGet<>();
    busqueda.setData(pacientes);
    when(pacienteService.search(nombre, null, 25)).thenReturn(busqueda);

    OllamaEjecucionResponse response = service.ejecutar("mensaje");

    assertThat(response.encontrado()).isTrue();
    assertThat(response.nombre()).isEqualTo(nombre);
    assertThat(response.pacientes()).containsExactlyElementsOf(pacientes);
    verify(pacienteService).search(nombre, null, 25);
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
    assertThat(response.mensaje()).isEqualTo(
        "Indica el DNI o el nombre del paciente que deseas consultar."
    );
    verify(pacienteService, never()).search(Mockito.any(), Mockito.any(), Mockito.any());
  }

  private List<PacienteRegistroResponse> pacientesRecientes(int cantidad) {
    return IntStream.rangeClosed(1, cantidad)
        .mapToObj(id -> PacienteRegistroResponse.builder().idPaciente(id).build())
        .toList();
  }
}
