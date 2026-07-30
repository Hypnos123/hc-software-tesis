package com.krivi.apihistorialmedico.business.importacion.store;

import com.krivi.apihistorialmedico.business.exception.PacienteImportacionException;
import com.krivi.apihistorialmedico.config.PacienteImportacionProperties;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacion;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionErrorCodigo;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionEstado;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionFila;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryPacienteImportacionStoreTest {
  private final PacienteImportacionProperties properties =
      new PacienteImportacionProperties(50, 2, 15, 18, "1.0");

  @Test
  void guardaConsultaEIniciaConfirmacionAtomicamente() {
    Instant ahora = Instant.parse("2026-07-29T10:00:00Z");
    var store = new InMemoryPacienteImportacionStore(properties, Clock.fixed(ahora, ZoneOffset.UTC));
    PacienteImportacion importacion = importacion(ahora.plusSeconds(900));

    store.guardar(importacion);

    assertThat(store.obtener(importacion.getImportacionId())).isSameAs(importacion);
    assertThat(store.iniciarConfirmacion(importacion.getImportacionId()).getEstado())
        .isEqualTo(PacienteImportacionEstado.CONFIRMANDO);
    assertThatThrownBy(() -> store.iniciarConfirmacion(importacion.getImportacionId()))
        .isInstanceOfSatisfying(PacienteImportacionException.class,
            error -> assertThat(error.getEstadoHttp().value()).isEqualTo(409));
  }

  @Test
  void distingueInexistenteDeExpiradaYEliminaDatosNormalizados() {
    Instant ahora = Instant.parse("2026-07-29T10:00:00Z");
    var store = new InMemoryPacienteImportacionStore(properties, Clock.fixed(ahora, ZoneOffset.UTC));
    UUID inexistente = UUID.randomUUID();
    PacienteImportacion importacion = importacion(ahora.minusSeconds(1));
    importacion.setFilas(new ArrayList<>(java.util.List.of(new PacienteImportacionFila())));
    store.guardar(importacion);

    assertThatThrownBy(() -> store.obtener(inexistente))
        .isInstanceOfSatisfying(PacienteImportacionException.class,
            error -> assertThat(error.getCodigo()).isEqualTo(PacienteImportacionErrorCodigo.IMPORTACION_NO_ENCONTRADA));
    assertThatThrownBy(() -> store.obtener(importacion.getImportacionId()))
        .isInstanceOfSatisfying(PacienteImportacionException.class, error -> {
          assertThat(error.getCodigo()).isEqualTo(PacienteImportacionErrorCodigo.IMPORTACION_EXPIRADA);
          assertThat(error.getEstadoHttp().value()).isEqualTo(410);
        });
    assertThat(importacion.getFilas()).isEmpty();
  }

  @Test
  void limpiezaPerezosaRetiraImportacionesExpiradas() {
    Instant ahora = Instant.parse("2026-07-29T10:00:00Z");
    var store = new InMemoryPacienteImportacionStore(properties, Clock.fixed(ahora, ZoneOffset.UTC));
    PacienteImportacion importacion = importacion(ahora.plusSeconds(60));
    store.guardar(importacion);
    importacion.setFechaExpiracion(ahora.minusSeconds(1));

    assertThat(store.limpiarExpiradas()).isEqualTo(1);
  }

  @Test
  void modeloTemporalNoContieneArchivoNiBytesDelExcel() {
    assertThat(java.util.Arrays.stream(PacienteImportacion.class.getDeclaredFields())
        .map(java.lang.reflect.Field::getType))
        .doesNotContain(byte[].class, java.io.File.class);
  }

  private PacienteImportacion importacion(Instant expira) {
    return PacienteImportacion.builder()
        .importacionId(UUID.randomUUID()).fechaCreacion(expira.minusSeconds(30))
        .fechaExpiracion(expira).estado(PacienteImportacionEstado.PREVISUALIZADA)
        .filas(new ArrayList<>()).build();
  }
}
