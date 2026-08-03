package com.krivi.apihistorialmedico.business.services.impl.importacion;

import com.krivi.apihistorialmedico.business.importacion.store.PacienteImportacionStore;
import com.krivi.apihistorialmedico.business.services.importacion.PacienteImportacionConfirmacionService;
import com.krivi.apihistorialmedico.business.services.importacion.PacienteImportacionWriter;
import com.krivi.apihistorialmedico.model.api.importacion.PacienteImportacionAdvertenciaResponse;
import com.krivi.apihistorialmedico.model.api.importacion.PacienteImportacionConfirmacionResponse;
import com.krivi.apihistorialmedico.model.api.importacion.PacienteImportacionConfirmacionResumenResponse;
import com.krivi.apihistorialmedico.model.api.importacion.PacienteImportacionErrorResponse;
import com.krivi.apihistorialmedico.model.api.importacion.PacienteImportacionFilaDetalleResponse;
import com.krivi.apihistorialmedico.model.api.importacion.PacienteImportacionResultadoRegistroResponse;
import com.krivi.apihistorialmedico.model.api.importacion.PacienteImportacionResumenResponse;
import com.krivi.apihistorialmedico.model.api.importacion.PacienteImportacionValidacionResponse;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacion;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionErrorCodigo;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionFila;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionFilaEstado;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionResumen;
import com.krivi.apihistorialmedico.repository.PacienteRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.UUID;

@Service
public class PacienteImportacionConfirmacionServiceImpl implements PacienteImportacionConfirmacionService {
  private final PacienteImportacionStore store;
  private final PacienteRepository pacienteRepository;
  private final PacienteImportacionWriter writer;

  public PacienteImportacionConfirmacionServiceImpl(
      PacienteImportacionStore store,
      PacienteRepository pacienteRepository,
      PacienteImportacionWriter writer
  ) {
    this.store = store;
    this.pacienteRepository = pacienteRepository;
    this.writer = writer;
  }

  @Override
  public PacienteImportacionValidacionResponse obtener(UUID importacionId) {
    return toPreview(store.obtener(importacionId));
  }

  @Override
  public PacienteImportacionConfirmacionResponse confirmar(UUID importacionId) {
    PacienteImportacion importacion = store.iniciarConfirmacion(importacionId);
    if (importacion.getResultadoConfirmacion() != null) return importacion.getResultadoConfirmacion();

    List<PacienteImportacionFila> candidatas = importacion.getFilas().stream()
        .filter(fila -> fila.getEstado() == PacienteImportacionFilaEstado.VALIDO)
        .toList();
    Set<String> existentes;
    try {
      existentes = consultarExistentes(candidatas.stream()
          .map(fila -> fila.getPaciente().getDni()).toList());
    } catch (RuntimeException exception) {
      store.restaurarPrevisualizada(importacionId);
      throw exception;
    }
    var resultados = new java.util.ArrayList<PacienteImportacionResultadoRegistroResponse>();
    int registrados = 0;
    int omitidos = 0;
    int errores = 0;

    for (PacienteImportacionFila fila : candidatas) {
      if (existentes.contains(fila.getPaciente().getDni())) {
        omitidos++;
        boolean archivado = Optional.ofNullable(pacienteRepository.findDnisArchivados(
            List.of(fila.getPaciente().getDni()))).orElse(Set.of()).contains(fila.getPaciente().getDni());
        resultados.add(resultadoError(fila,
            archivado ? PacienteImportacionErrorCodigo.DNI_ARCHIVADO_EXISTENTE_AL_CONFIRMAR : PacienteImportacionErrorCodigo.DNI_EXISTENTE_AL_CONFIRMAR,
            archivado ? "El DNI pertenece a un paciente archivado y no puede registrarse nuevamente."
                : "El DNI fue registrado después de generar la previsualización."));
        continue;
      }
      try {
        Integer idPaciente = writer.registrar(fila);
        registrados++;
        resultados.add(PacienteImportacionResultadoRegistroResponse.builder()
            .numeroFila(fila.getNumeroFila()).dni(fila.getPaciente().getDni())
            .estado(PacienteImportacionFilaEstado.REGISTRADO).idPaciente(idPaciente).build());
      } catch (RuntimeException exception) {
        errores++;
        resultados.add(resultadoError(fila, PacienteImportacionErrorCodigo.ERROR_REGISTRO,
            "No se pudo registrar el paciente y sus antecedentes."));
      }
    }

    PacienteImportacionConfirmacionResponse response = PacienteImportacionConfirmacionResponse.builder()
        .importacionId(importacionId)
        .estado(com.krivi.apihistorialmedico.model.importacion.PacienteImportacionEstado.CONFIRMADA)
        .resumen(PacienteImportacionConfirmacionResumenResponse.builder()
            .filasValidasEnPrevisualizacion(candidatas.size())
            .pacientesRegistrados(registrados)
            .omitidosPorDniExistente(omitidos)
            .erroresAlRegistrar(errores)
            .build())
        .resultados(resultados)
        .build();
    store.marcarConfirmada(importacionId, response);
    return response;
  }

  private Set<String> consultarExistentes(Collection<String> dnis) {
    if (dnis.isEmpty()) return Set.of();
    Set<String> existentes = pacienteRepository.findDnisExistentes(dnis);
    return existentes == null ? Set.of() : new HashSet<>(existentes);
  }

  private PacienteImportacionResultadoRegistroResponse resultadoError(
      PacienteImportacionFila fila,
      PacienteImportacionErrorCodigo codigo,
      String mensaje
  ) {
    return PacienteImportacionResultadoRegistroResponse.builder()
        .numeroFila(fila.getNumeroFila()).dni(fila.getPaciente().getDni())
        .estado(PacienteImportacionFilaEstado.NO_REGISTRADO)
        .errores(List.of(PacienteImportacionErrorResponse.builder()
            .numeroFila(fila.getNumeroFila()).codigo(codigo).campo("DNI").mensaje(mensaje).build()))
        .build();
  }

  private PacienteImportacionValidacionResponse toPreview(PacienteImportacion importacion) {
    return PacienteImportacionValidacionResponse.builder()
        .importacionId(importacion.getImportacionId())
        .estado(importacion.getEstado())
        .expiraEn(importacion.getFechaExpiracion())
        .resumen(toResumen(importacion.getResumen()))
        .filas(importacion.getFilas().stream().map(this::toFila).toList())
        .build();
  }

  private PacienteImportacionResumenResponse toResumen(PacienteImportacionResumen resumen) {
    return PacienteImportacionResumenResponse.builder()
        .registrosAnalizados(resumen.getRegistrosAnalizados()).validos(resumen.getValidos())
        .conErrores(resumen.getConErrores()).filasConDniDuplicado(resumen.getFilasConDniDuplicado())
        .gruposDniDuplicados(resumen.getGruposDniDuplicados()).dniExistentes(resumen.getDniExistentes())
        .conAdvertencias(resumen.getConAdvertencias()).filasVaciasIgnoradas(resumen.getFilasVaciasIgnoradas())
        .build();
  }

  private PacienteImportacionFilaDetalleResponse toFila(PacienteImportacionFila fila) {
    String nombre = String.join(" ", fila.getPaciente().getNombres(), fila.getPaciente().getApellidos()).trim();
    return PacienteImportacionFilaDetalleResponse.builder()
        .numeroFila(fila.getNumeroFila()).nombreCompleto(nombre).dni(fila.getPaciente().getDni())
        .estado(fila.getEstado()).paciente(fila.getPaciente()).antecedentes(fila.getAntecedentes())
        .errores(fila.getErrores().stream().map(error -> PacienteImportacionErrorResponse.builder()
            .numeroFila(fila.getNumeroFila()).codigo(error.getCodigo()).campo(error.getCampo())
            .mensaje(error.getMensaje()).build()).toList())
        .advertencias(fila.getAdvertencias().stream().map(advertencia ->
            PacienteImportacionAdvertenciaResponse.builder().numeroFila(fila.getNumeroFila())
                .codigo(advertencia.getCodigo()).campo(advertencia.getCampo())
                .mensaje(advertencia.getMensaje()).build()).toList())
        .build();
  }
}
