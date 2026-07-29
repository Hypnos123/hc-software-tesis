package com.krivi.apihistorialmedico.business.importacion.store;

import com.krivi.apihistorialmedico.business.exception.PacienteImportacionException;
import com.krivi.apihistorialmedico.config.PacienteImportacionProperties;
import com.krivi.apihistorialmedico.model.api.importacion.PacienteImportacionConfirmacionResponse;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacion;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionErrorCodigo;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionEstado;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class InMemoryPacienteImportacionStore implements PacienteImportacionStore {
  private final ConcurrentMap<UUID, PacienteImportacion> importaciones = new ConcurrentHashMap<>();
  private final ConcurrentMap<UUID, Instant> expiradas = new ConcurrentHashMap<>();
  private final Clock clock;
  private final PacienteImportacionProperties properties;

  public InMemoryPacienteImportacionStore(PacienteImportacionProperties properties) {
    this(properties, Clock.systemUTC());
  }

  InMemoryPacienteImportacionStore(PacienteImportacionProperties properties, Clock clock) {
    this.properties = properties;
    this.clock = clock;
  }

  @Override
  public void guardar(PacienteImportacion importacion) {
    importaciones.put(importacion.getImportacionId(), importacion);
    limpiarExpiradas();
  }

  @Override
  public PacienteImportacion obtener(UUID importacionId) {
    PacienteImportacion importacion = importaciones.get(importacionId);
    if (importacion == null) throw noEncontradaOExpirada(importacionId);
    if (estaExpirada(importacion)) {
      expirar(importacionId, importacion);
      throw expirada();
    }
    return importacion;
  }

  @Override
  public PacienteImportacion iniciarConfirmacion(UUID importacionId) {
    AtomicReference<PacienteImportacion> seleccionada = new AtomicReference<>();
    AtomicReference<PacienteImportacionException> error = new AtomicReference<>();
    importaciones.compute(importacionId, (id, importacion) -> {
      if (importacion == null) {
        error.set(noEncontradaOExpirada(id));
        return null;
      }
      if (estaExpirada(importacion)) {
        expiradas.put(id, clock.instant());
        importacion.setEstado(PacienteImportacionEstado.EXPIRADA);
        importacion.getFilas().clear();
        error.set(expirada());
        return null;
      }
      if (importacion.getEstado() == PacienteImportacionEstado.CONFIRMANDO) {
        error.set(new PacienteImportacionException(PacienteImportacionErrorCodigo.IMPORTACION_YA_CONFIRMADA,
            "La importación ya está siendo confirmada.", HttpStatus.CONFLICT));
        return importacion;
      }
      if (importacion.getEstado() == PacienteImportacionEstado.CANCELADA
          || importacion.getEstado() == PacienteImportacionEstado.EXPIRADA) {
        error.set(expirada());
        return importacion;
      }
      if (importacion.getEstado() == PacienteImportacionEstado.PREVISUALIZADA) {
        importacion.setEstado(PacienteImportacionEstado.CONFIRMANDO);
      }
      seleccionada.set(importacion);
      return importacion;
    });
    if (error.get() != null) throw error.get();
    return seleccionada.get();
  }

  @Override
  public void marcarConfirmada(UUID importacionId, PacienteImportacionConfirmacionResponse resultado) {
    importaciones.computeIfPresent(importacionId, (id, importacion) -> {
      importacion.setEstado(PacienteImportacionEstado.CONFIRMADA);
      importacion.setResultadoConfirmacion(resultado);
      return importacion;
    });
  }

  @Override
  public void restaurarPrevisualizada(UUID importacionId) {
    importaciones.computeIfPresent(importacionId, (id, importacion) -> {
      if (importacion.getEstado() == PacienteImportacionEstado.CONFIRMANDO) {
        importacion.setEstado(PacienteImportacionEstado.PREVISUALIZADA);
      }
      return importacion;
    });
  }

  @Override
  public int limpiarExpiradas() {
    int eliminadas = 0;
    for (var entry : importaciones.entrySet()) {
      if (estaExpirada(entry.getValue()) && importaciones.remove(entry.getKey(), entry.getValue())) {
        entry.getValue().setEstado(PacienteImportacionEstado.EXPIRADA);
        entry.getValue().getFilas().clear();
        expiradas.put(entry.getKey(), clock.instant());
        eliminadas++;
      }
    }
    Instant limiteTumba = clock.instant().minus(properties.tiempoExpiracionMinutos(), ChronoUnit.MINUTES);
    expiradas.entrySet().removeIf(entry -> entry.getValue().isBefore(limiteTumba));
    return eliminadas;
  }

  private boolean estaExpirada(PacienteImportacion importacion) {
    return importacion.getEstado() != PacienteImportacionEstado.CONFIRMANDO
        && !clock.instant().isBefore(importacion.getFechaExpiracion());
  }

  private void expirar(UUID id, PacienteImportacion importacion) {
    if (importaciones.remove(id, importacion)) {
      importacion.setEstado(PacienteImportacionEstado.EXPIRADA);
      importacion.getFilas().clear();
      expiradas.put(id, clock.instant());
    }
  }

  private PacienteImportacionException noEncontradaOExpirada(UUID id) {
    return expiradas.containsKey(id)
        ? expirada()
        : new PacienteImportacionException(PacienteImportacionErrorCodigo.IMPORTACION_NO_ENCONTRADA,
            "La importación solicitada no existe.", HttpStatus.NOT_FOUND);
  }

  private PacienteImportacionException expirada() {
    return new PacienteImportacionException(PacienteImportacionErrorCodigo.IMPORTACION_EXPIRADA,
        "La importación ha expirado.", HttpStatus.GONE);
  }
}
