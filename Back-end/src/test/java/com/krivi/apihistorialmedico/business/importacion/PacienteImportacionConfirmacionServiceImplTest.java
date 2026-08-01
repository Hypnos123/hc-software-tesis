package com.krivi.apihistorialmedico.business.importacion;

import com.krivi.apihistorialmedico.business.exception.PacienteImportacionException;
import com.krivi.apihistorialmedico.business.importacion.store.InMemoryPacienteImportacionStore;
import com.krivi.apihistorialmedico.business.services.impl.importacion.PacienteImportacionConfirmacionServiceImpl;
import com.krivi.apihistorialmedico.business.services.importacion.PacienteImportacionWriter;
import com.krivi.apihistorialmedico.config.PacienteImportacionProperties;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacion;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionAntecedentes;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionDatos;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionEstado;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionFila;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionFilaEstado;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionResumen;
import com.krivi.apihistorialmedico.repository.PacienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PacienteImportacionConfirmacionServiceImplTest {
  private InMemoryPacienteImportacionStore store;
  private PacienteRepository repository;
  private PacienteImportacionWriter writer;
  private PacienteImportacionConfirmacionServiceImpl service;

  @BeforeEach
  void setUp() {
    store = new InMemoryPacienteImportacionStore(new PacienteImportacionProperties(50, 2, 15, 18, "1.0"));
    repository = mock(PacienteRepository.class);
    writer = mock(PacienteImportacionWriter.class);
    service = new PacienteImportacionConfirmacionServiceImpl(store, repository, writer);
    when(repository.findDnisExistentes(anyCollection())).thenReturn(Set.of());
  }

  @Test
  void confirmaSoloFilasValidasYConservaIdsGenerados() {
    PacienteImportacion importacion = importacion(List.of(
        fila(2, "01234567", PacienteImportacionFilaEstado.VALIDO),
        fila(3, "12345678", PacienteImportacionFilaEstado.ERROR_DATOS),
        fila(4, "22222222", PacienteImportacionFilaEstado.DNI_DUPLICADO_ARCHIVO),
        fila(5, "33333333", PacienteImportacionFilaEstado.DNI_EXISTENTE),
        fila(6, "87654321", PacienteImportacionFilaEstado.VALIDO)
    ));
    when(writer.registrar(any())).thenReturn(25, 26);
    store.guardar(importacion);

    var response = service.confirmar(importacion.getImportacionId());

    assertThat(response.getEstado()).isEqualTo(PacienteImportacionEstado.CONFIRMADA);
    assertThat(response.getResumen().getFilasValidasEnPrevisualizacion()).isEqualTo(2);
    assertThat(response.getResumen().getPacientesRegistrados()).isEqualTo(2);
    assertThat(response.getResultados()).extracting(resultado -> resultado.getIdPaciente())
        .containsExactly(25, 26);
    verify(writer, times(2)).registrar(any());
  }

  @Test
  void revalidaDnisEnConjuntoYOmitelosQueAparecieronDespues() {
    PacienteImportacion importacion = importacion(List.of(
        fila(2, "01234567", PacienteImportacionFilaEstado.VALIDO),
        fila(3, "12345678", PacienteImportacionFilaEstado.VALIDO)
    ));
    store.guardar(importacion);
    when(repository.findDnisExistentes(anyCollection())).thenReturn(Set.of("01234567"));
    when(writer.registrar(any())).thenReturn(30);

    var response = service.confirmar(importacion.getImportacionId());

    assertThat(response.getResumen().getOmitidosPorDniExistente()).isEqualTo(1);
    assertThat(response.getResumen().getPacientesRegistrados()).isEqualTo(1);
    assertThat(response.getResultados().getFirst().getErrores().getFirst().getCodigo().name())
        .isEqualTo("DNI_EXISTENTE_AL_CONFIRMAR");
    verify(repository, times(1)).findDnisExistentes(anyCollection());
    verify(writer, times(1)).registrar(any());
  }

  @Test
  void errorDeUnaFilaNoImpideRegistrarLaSiguiente() {
    PacienteImportacion importacion = importacion(List.of(
        fila(2, "01234567", PacienteImportacionFilaEstado.VALIDO),
        fila(3, "12345678", PacienteImportacionFilaEstado.VALIDO)
    ));
    store.guardar(importacion);
    when(writer.registrar(any())).thenThrow(new RuntimeException("fallo simulado")).thenReturn(40);

    var response = service.confirmar(importacion.getImportacionId());

    assertThat(response.getResumen().getErroresAlRegistrar()).isEqualTo(1);
    assertThat(response.getResumen().getPacientesRegistrados()).isEqualTo(1);
    assertThat(response.getResultados()).extracting(resultado -> resultado.getEstado()).containsExactly(
        PacienteImportacionFilaEstado.NO_REGISTRADO, PacienteImportacionFilaEstado.REGISTRADO
    );
  }

  @Test
  void segundaConfirmacionDevuelveResultadoSinRepetirInserciones() {
    PacienteImportacion importacion = importacion(List.of(fila(2, "01234567", PacienteImportacionFilaEstado.VALIDO)));
    store.guardar(importacion);
    when(writer.registrar(any())).thenReturn(25);

    var primera = service.confirmar(importacion.getImportacionId());
    var segunda = service.confirmar(importacion.getImportacionId());

    assertThat(segunda).isSameAs(primera);
    verify(writer, times(1)).registrar(any());
  }

  @Test
  void dosConfirmacionesSimultaneasNoProcesanDosVeces() throws Exception {
    PacienteImportacion importacion = importacion(List.of(fila(2, "01234567", PacienteImportacionFilaEstado.VALIDO)));
    store.guardar(importacion);
    CountDownLatch inicioWriter = new CountDownLatch(1);
    CountDownLatch liberarWriter = new CountDownLatch(1);
    when(writer.registrar(any())).thenAnswer(invocacion -> {
      inicioWriter.countDown();
      liberarWriter.await(2, TimeUnit.SECONDS);
      return 25;
    });
    var executor = Executors.newSingleThreadExecutor();
    var primera = executor.submit(() -> service.confirmar(importacion.getImportacionId()));
    assertThat(inicioWriter.await(2, TimeUnit.SECONDS)).isTrue();

    assertThatThrownBy(() -> service.confirmar(importacion.getImportacionId()))
        .isInstanceOf(PacienteImportacionException.class);
    liberarWriter.countDown();
    assertThat(primera.get(2, TimeUnit.SECONDS).getResumen().getPacientesRegistrados()).isEqualTo(1);
    executor.shutdownNow();
    verify(writer, times(1)).registrar(any());
  }

  @Test
  void noConsultaNiEscribeCuandoNoHayFilasValidas() {
    PacienteImportacion importacion = importacion(List.of(fila(2, "01234567", PacienteImportacionFilaEstado.ERROR_DATOS)));
    store.guardar(importacion);

    var response = service.confirmar(importacion.getImportacionId());

    assertThat(response.getResumen().getFilasValidasEnPrevisualizacion()).isZero();
    verify(repository, never()).findDnisExistentes(anyCollection());
    verify(writer, never()).registrar(any());
  }

  private PacienteImportacion importacion(List<PacienteImportacionFila> filas) {
    return PacienteImportacion.builder()
        .importacionId(UUID.randomUUID()).versionPlantilla("1.0")
        .fechaCreacion(Instant.now()).fechaExpiracion(Instant.now().plusSeconds(900))
        .estado(PacienteImportacionEstado.PREVISUALIZADA)
        .resumen(PacienteImportacionResumen.builder().registrosAnalizados(filas.size()).build())
        .filas(new ArrayList<>(filas)).build();
  }

  private PacienteImportacionFila fila(int numero, String dni, PacienteImportacionFilaEstado estado) {
    return PacienteImportacionFila.builder().numeroFila(numero).estado(estado)
        .paciente(PacienteImportacionDatos.builder().apellidos("Pérez").nombres("Ana")
            .fechaNacimiento(LocalDate.of(1990, 5, 10)).estadoCivil("SOLTERO").dni(dni)
            .sexo("F").direccion("Dirección").distrito("Lima").traidoPor("").build())
        .antecedentes(PacienteImportacionAntecedentes.builder().alimentacion("Balanceada")
            .habitos("No refiere").vivienda("Casa").desarrolloPsicomotor("Normal")
            .vacunas("Completas").educacion("S1").enfermedadesPrevias("Ninguna")
            .cirugiasPrevias("Ninguna").alergiasMedicamentos("No refiere").build())
        .build();
  }
}
