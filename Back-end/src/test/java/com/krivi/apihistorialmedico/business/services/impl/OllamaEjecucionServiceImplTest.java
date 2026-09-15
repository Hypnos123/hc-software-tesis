package com.krivi.apihistorialmedico.business.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.krivi.apihistorialmedico.business.services.OllamaService;
import com.krivi.apihistorialmedico.business.services.HistoriaClinicaService;
import com.krivi.apihistorialmedico.business.services.PacienteDuplicadoService;
import com.krivi.apihistorialmedico.business.services.PacienteService;
import com.krivi.apihistorialmedico.model.api.DuplicadosPacientesResponse;
import com.krivi.apihistorialmedico.model.api.BusquedaHistoriasClinicasResponse;
import com.krivi.apihistorialmedico.model.api.DuplicadosHistoriasClinicasResponse;
import com.krivi.apihistorialmedico.model.api.HistoriaClinicaIntegracionItemResponse;
import com.krivi.apihistorialmedico.model.api.HistoriasClinicasFaltantesPreviewResponse;
import com.krivi.apihistorialmedico.model.api.PacienteSinHistoriaClinicaResponse;
import com.krivi.apihistorialmedico.model.api.OllamaEjecucionResponse;
import com.krivi.apihistorialmedico.model.api.OllamaInterpretacionResponse;
import com.krivi.apihistorialmedico.model.api.PacienteResponse;
import com.krivi.apihistorialmedico.model.api.PacienteDuplicadoComparacionResponse;
import com.krivi.apihistorialmedico.model.api.ResponseModelGet;
import com.krivi.apihistorialmedico.model.api.PacienteRegistroResponse;
import com.krivi.apihistorialmedico.model.api.UltimosPacientesResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class OllamaEjecucionServiceImplTest {
  private OllamaService ollamaService;
  private PacienteService pacienteService;
  private PacienteDuplicadoService pacienteDuplicadoService;
  private HistoriaClinicaService historiaClinicaService;
  private OllamaEjecucionServiceImpl service;

  @BeforeEach
  void setUp() {
    ollamaService = Mockito.mock(OllamaService.class);
    pacienteService = Mockito.mock(PacienteService.class);
    pacienteDuplicadoService = Mockito.mock(PacienteDuplicadoService.class);
    historiaClinicaService = Mockito.mock(HistoriaClinicaService.class);
    service = new OllamaEjecucionServiceImpl(
        ollamaService,
        pacienteService,
        pacienteDuplicadoService,
        historiaClinicaService
    );
  }

  @Test
  void normalizaLimitesDeUltimosPacientes() {
    assertThat(service.normalizarLimiteUltimos(null)).isEqualTo(3);
    assertThat(service.normalizarLimiteUltimos(-1)).isEqualTo(3);
    assertThat(service.normalizarLimiteUltimos(0)).isEqualTo(3);
    assertThat(service.normalizarLimiteUltimos(1)).isEqualTo(3);
    assertThat(service.normalizarLimiteUltimos(2)).isEqualTo(3);
    assertThat(service.normalizarLimiteUltimos(3)).isEqualTo(3);
    assertThat(service.normalizarLimiteUltimos(4)).isEqualTo(4);
    assertThat(service.normalizarLimiteUltimos(5)).isEqualTo(5);
    assertThat(service.normalizarLimiteUltimos(6)).isEqualTo(6);
    assertThat(service.normalizarLimiteUltimos(10)).isEqualTo(6);
  }

  @Test
  void obtieneUltimosPacientesConLimiteNormalizadoYContratoCompacto() {
    when(ollamaService.interpretar("mensaje")).thenReturn(
        new OllamaInterpretacionResponse(
            "PACIENTES", "ULTIMOS_PACIENTES", null, null, 5
        )
    );
    LocalDateTime fecha = LocalDateTime.of(2026, 8, 14, 10, 30);
    UltimosPacientesResponse resultado = UltimosPacientesResponse.builder()
        .cantidad(1)
        .pacientes(List.of(PacienteRegistroResponse.builder()
            .idPaciente(24)
            .nombreCompleto("Daniela Alejandra Ramirez Soto")
            .dni("74296831")
            .fechaCreacion(fecha)
            .build()))
        .build();
    when(pacienteService.obtenerUltimosParaIntegracion(5)).thenReturn(resultado);

    OllamaEjecucionResponse response = service.ejecutar("mensaje");

    assertThat(response.intencion()).isEqualTo("ULTIMOS_PACIENTES");
    assertThat(response.pacientes()).singleElement().satisfies(paciente -> {
      assertThat(paciente.getIdPaciente()).isEqualTo(24);
      assertThat(paciente.getNombreCompleto()).isEqualTo("Daniela Alejandra Ramirez Soto");
      assertThat(paciente.getNumDocumento()).isEqualTo("74296831");
      assertThat(paciente.getFechaCreacion()).isEqualTo(fecha);
    });
    assertThat(response.mensaje()).isEqualTo(
        "Se encontraron los 1 pacientes registrados más recientemente."
    );
    verify(pacienteService).obtenerUltimosParaIntegracion(5);
  }

  @Test
  void usaTresPorDefectoEInformaCuandoNoHayPacientesActivos() {
    when(ollamaService.interpretar("mensaje")).thenReturn(
        new OllamaInterpretacionResponse(
            "PACIENTES", "ULTIMOS_PACIENTES", null, null, null
        )
    );
    when(pacienteService.obtenerUltimosParaIntegracion(3)).thenReturn(
        UltimosPacientesResponse.builder().cantidad(0).pacientes(List.of()).build()
    );

    OllamaEjecucionResponse response = service.ejecutar("mensaje");

    assertThat(response.pacientes()).isEmpty();
    assertThat(response.encontrado()).isFalse();
    assertThat(response.mensaje()).isEqualTo("No se encontraron pacientes activos registrados.");
    verify(pacienteService).obtenerUltimosParaIntegracion(3);
  }

  @Test
  void consultaHistoriasPorDniYDevuelveMultiplesResultados() {
    String dni = "72845292";
    when(ollamaService.interpretar("mensaje")).thenReturn(
        new OllamaInterpretacionResponse(
            "HISTORIAS_CLINICAS", "CONSULTAR_HISTORIAS", dni, null, null));
    ResponseModelGet<PacienteResponse> pacientes = new ResponseModelGet<>();
    pacientes.setData(List.of(PacienteResponse.builder().idPaciente(7).build()));
    when(pacienteService.search(null, dni, 25)).thenReturn(pacientes);
    List<HistoriaClinicaIntegracionItemResponse> historias = List.of(
        HistoriaClinicaIntegracionItemResponse.builder().idHistoriaClinica(3).build(),
        HistoriaClinicaIntegracionItemResponse.builder().idHistoriaClinica(4).build());
    when(historiaClinicaService.buscarParaIntegracion(dni)).thenReturn(
        BusquedaHistoriasClinicasResponse.builder().encontrado(true)
            .historiasClinicas(historias).build());

    OllamaEjecucionResponse response = service.ejecutar("mensaje");

    assertThat(response.historias()).containsExactlyElementsOf(historias);
    assertThat(response.mensaje()).isEqualTo(
        "Se encontraron 2 historias clínicas para el paciente con DNI 72845292.");
  }

  @Test
  void filtraConsultaDeHistoriasPorTodasLasPalabrasCompletasDelNombre() {
    String nombre = "Fernando Vargas Rios";
    when(ollamaService.interpretar("mensaje")).thenReturn(
        new OllamaInterpretacionResponse(
            "HISTORIAS_CLINICAS", "CONSULTAR_HISTORIAS", null, nombre, null));
    ResponseModelGet<PacienteResponse> pacientes = new ResponseModelGet<>();
    pacientes.setData(List.of(
        PacienteResponse.builder().idPaciente(7).nombres("Fernando Josset")
            .apellidos("Vargas Ríos").build(),
        PacienteResponse.builder().idPaciente(8).nombres("Elena Beatriz")
            .apellidos("Vargas Huamán").build()));
    when(pacienteService.search(nombre, null, 25)).thenReturn(pacientes);
    when(historiaClinicaService.buscarParaIntegracion("paciente:7")).thenReturn(
        BusquedaHistoriasClinicasResponse.builder().encontrado(true)
            .historiasClinicas(List.of(HistoriaClinicaIntegracionItemResponse.builder()
                .idHistoriaClinica(70).idPaciente(7).build()))
            .build());

    OllamaEjecucionResponse response = service.ejecutar("mensaje");

    assertThat(response.historias()).extracting(
        HistoriaClinicaIntegracionItemResponse::getIdPaciente).containsExactly(7);
    verify(historiaClinicaService, never()).buscarParaIntegracion("paciente:8");
  }

  @Test
  void conservaVariasCoincidenciasValidasPorNombreYOmiteTodasCuandoNingunaCoincide() {
    String nombre = "Fernando Vargas Rios";
    when(ollamaService.interpretar("varias")).thenReturn(new OllamaInterpretacionResponse(
        "HISTORIAS_CLINICAS", "CONSULTAR_HISTORIAS", null, nombre, null));
    ResponseModelGet<PacienteResponse> varias = new ResponseModelGet<>();
    varias.setData(List.of(
        PacienteResponse.builder().idPaciente(7).nombres("Fernando Josset")
            .apellidos("Vargas Ríos").build(),
        PacienteResponse.builder().idPaciente(9).nombres("Fernando")
            .apellidos("Vargas de los Ríos").build()));
    when(pacienteService.search(nombre, null, 25)).thenReturn(varias);
    when(historiaClinicaService.buscarParaIntegracion("paciente:7")).thenReturn(
        BusquedaHistoriasClinicasResponse.builder().historiasClinicas(List.of(
            HistoriaClinicaIntegracionItemResponse.builder().idHistoriaClinica(70).idPaciente(7).build())).build());
    when(historiaClinicaService.buscarParaIntegracion("paciente:9")).thenReturn(
        BusquedaHistoriasClinicasResponse.builder().historiasClinicas(List.of(
            HistoriaClinicaIntegracionItemResponse.builder().idHistoriaClinica(90).idPaciente(9).build())).build());

    assertThat(service.ejecutar("varias").historias()).extracting(
        HistoriaClinicaIntegracionItemResponse::getIdPaciente).containsExactly(7, 9);

    when(ollamaService.interpretar("ninguna")).thenReturn(new OllamaInterpretacionResponse(
        "HISTORIAS_CLINICAS", "CONSULTAR_HISTORIAS", null, nombre, null));
    ResponseModelGet<PacienteResponse> ninguna = new ResponseModelGet<>();
    ninguna.setData(List.of(PacienteResponse.builder().idPaciente(8).nombres("Elena Beatriz")
        .apellidos("Vargas Huamán").build()));
    when(pacienteService.search(nombre, null, 25)).thenReturn(ninguna);

    assertThat(service.ejecutar("ninguna").mensaje()).isEqualTo(
        "No se encontró ningún paciente activo con ese nombre.");
  }

  @Test
  void diferenciaPacienteExistenteSinHistoriaDePacienteInexistente() {
    when(ollamaService.interpretar("existente")).thenReturn(
        new OllamaInterpretacionResponse(
            "HISTORIAS_CLINICAS", "CONSULTAR_HISTORIAS", "72845292", null, null));
    ResponseModelGet<PacienteResponse> existente = new ResponseModelGet<>();
    existente.setData(List.of(PacienteResponse.builder().idPaciente(7).build()));
    when(pacienteService.search(null, "72845292", 25)).thenReturn(existente);
    when(historiaClinicaService.buscarParaIntegracion("72845292")).thenReturn(
        BusquedaHistoriasClinicasResponse.builder().encontrado(false)
            .historiasClinicas(List.of()).build());

    assertThat(service.ejecutar("existente").mensaje()).isEqualTo(
        "El paciente se encuentra registrado, pero no tiene historias clínicas asociadas.");

    when(ollamaService.interpretar("inexistente")).thenReturn(
        new OllamaInterpretacionResponse(
            "HISTORIAS_CLINICAS", "CONSULTAR_HISTORIAS", "99999999", null, null));
    ResponseModelGet<PacienteResponse> inexistente = new ResponseModelGet<>();
    inexistente.setData(List.of());
    when(pacienteService.search(null, "99999999", 25)).thenReturn(inexistente);

    assertThat(service.ejecutar("inexistente").mensaje()).isEqualTo(
        "No se encontró ningún paciente activo con ese DNI.");
  }

  @Test
  void consultaDuplicadasGeneralesYEspecificas() {
    DuplicadosHistoriasClinicasResponse duplicados = DuplicadosHistoriasClinicasResponse.builder()
        .hayDuplicados(true).totalGrupos(1).mensaje("Duplicadas").build();
    when(ollamaService.interpretar("general")).thenReturn(new OllamaInterpretacionResponse(
        "HISTORIAS_CLINICAS", "HISTORIAS_DUPLICADAS", null, null, null));
    when(historiaClinicaService.obtenerDuplicadosParaIntegracion(null)).thenReturn(duplicados);

    assertThat(service.ejecutar("general").gruposHistoriasDuplicadas().isHayDuplicados()).isTrue();

    String nombre = "Daniela Alejandra Ramirez Soto";
    when(ollamaService.interpretar("nombre")).thenReturn(new OllamaInterpretacionResponse(
        "HISTORIAS_CLINICAS", "HISTORIAS_DUPLICADAS", null, nombre, null));
    ResponseModelGet<PacienteResponse> pacientes = new ResponseModelGet<>();
    pacientes.setData(List.of(PacienteResponse.builder().nombres("Daniela Alejandra")
        .apellidos("Ramirez Soto").build()));
    when(pacienteService.search(nombre, null, 25)).thenReturn(pacientes);
    when(historiaClinicaService.obtenerDuplicadosPorNombreParaIntegracion(nombre))
        .thenReturn(duplicados);

    assertThat(service.ejecutar("nombre").gruposHistoriasDuplicadas().isHayDuplicados()).isTrue();
  }

  @Test
  void normalizaLimiteDeUltimasHistoriasYListaPacientesSinHistoria() {
    when(ollamaService.interpretar("ultimas")).thenReturn(new OllamaInterpretacionResponse(
        "HISTORIAS_CLINICAS", "ULTIMAS_HISTORIAS", null, null, 10));
    when(historiaClinicaService.obtenerUltimasParaIntegracion(6)).thenReturn(
        BusquedaHistoriasClinicasResponse.builder().encontrado(true)
            .historiasClinicas(List.of(HistoriaClinicaIntegracionItemResponse.builder()
                .idHistoriaClinica(12).build())).build());

    assertThat(service.ejecutar("ultimas").historias()).hasSize(1);
    verify(historiaClinicaService).obtenerUltimasParaIntegracion(6);

    when(ollamaService.interpretar("faltantes")).thenReturn(new OllamaInterpretacionResponse(
        "PACIENTES", "PACIENTES_SIN_HISTORIA", null, null, null));
    List<PacienteSinHistoriaClinicaResponse> faltantes = java.util.stream.IntStream.range(0, 8)
        .mapToObj(indice -> PacienteSinHistoriaClinicaResponse.builder()
            .idPaciente(indice + 1).nombreCompleto("Paciente " + indice)
            .dni("1234567" + indice).build()).toList();
    when(historiaClinicaService.obtenerHistoriasClinicasFaltantes()).thenReturn(
        HistoriasClinicasFaltantesPreviewResponse.builder().cantidad(8)
            .pacientes(faltantes).build());

    OllamaEjecucionResponse response = service.ejecutar("faltantes");

    assertThat(response.pacientes()).hasSize(6);
    assertThat(response.mensaje()).isEqualTo(
        "Se encontraron 8 pacientes activos sin historia clínica.");
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
}
