package com.krivi.apihistorialmedico.business.services.impl.importacion;

import com.krivi.apihistorialmedico.business.exception.PacienteImportacionException;
import com.krivi.apihistorialmedico.business.importacion.PacienteExcelReader;
import com.krivi.apihistorialmedico.business.importacion.PacienteExcelValidationResult;
import com.krivi.apihistorialmedico.business.services.importacion.PacienteImportacionValidacionService;
import com.krivi.apihistorialmedico.business.importacion.store.PacienteImportacionStore;
import com.krivi.apihistorialmedico.config.PacienteImportacionProperties;
import com.krivi.apihistorialmedico.model.api.importacion.PacienteImportacionAdvertenciaResponse;
import com.krivi.apihistorialmedico.model.api.importacion.PacienteImportacionErrorResponse;
import com.krivi.apihistorialmedico.model.api.importacion.PacienteImportacionFilaDetalleResponse;
import com.krivi.apihistorialmedico.model.api.importacion.PacienteImportacionResumenResponse;
import com.krivi.apihistorialmedico.model.api.importacion.PacienteImportacionValidacionResponse;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionError;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionErrorCodigo;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionFila;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionFilaEstado;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacion;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionEstado;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionResumen;
import com.krivi.apihistorialmedico.repository.PacienteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class PacienteImportacionValidacionServiceImpl implements PacienteImportacionValidacionService {
  private static final long BYTES_POR_MB = 1024L * 1024L;

  private final PacienteExcelReader reader;
  private final PacienteRepository pacienteRepository;
  private final PacienteImportacionProperties properties;
  private final PacienteImportacionStore importacionStore;

  public PacienteImportacionValidacionServiceImpl(
      PacienteExcelReader reader,
      PacienteRepository pacienteRepository,
      PacienteImportacionProperties properties,
      PacienteImportacionStore importacionStore
  ) {
    this.reader = reader;
    this.pacienteRepository = pacienteRepository;
    this.properties = properties;
    this.importacionStore = importacionStore;
  }

  @Override
  public PacienteImportacionValidacionResponse validar(MultipartFile archivo) {
    byte[] contenido = validarYLeerContenido(archivo);
    PacienteExcelValidationResult lectura = reader.leer(contenido);
    List<PacienteImportacionFila> filas = new ArrayList<>(lectura.filas());

    Map<String, List<PacienteImportacionFila>> filasPorDni = agruparDnisValidos(filas);
    Set<String> duplicadosArchivo = filasPorDni.entrySet().stream()
        .filter(entry -> entry.getValue().size() > 1)
        .map(Map.Entry::getKey)
        .collect(Collectors.toSet());
    Set<String> dnisExistentes = consultarDnisExistentes(filasPorDni.keySet());

    for (PacienteImportacionFila fila : filas) {
      boolean tieneErroresDatos = !fila.getErrores().isEmpty();
      String dni = fila.getPaciente().getDni();
      boolean duplicado = dniValido(dni) && duplicadosArchivo.contains(dni);
      boolean existente = dniValido(dni) && dnisExistentes.contains(dni);
      if (duplicado) agregarErrorSiNoExiste(fila, PacienteImportacionErrorCodigo.DNI_DUPLICADO_ARCHIVO,
          "El DNI está repetido dentro del archivo.");
      if (existente) agregarErrorSiNoExiste(fila, PacienteImportacionErrorCodigo.DNI_EXISTENTE,
          "El DNI ya se encuentra registrado.");

      if (tieneErroresDatos) fila.setEstado(PacienteImportacionFilaEstado.ERROR_DATOS);
      else if (duplicado) fila.setEstado(PacienteImportacionFilaEstado.DNI_DUPLICADO_ARCHIVO);
      else if (existente) fila.setEstado(PacienteImportacionFilaEstado.DNI_EXISTENTE);
      else fila.setEstado(PacienteImportacionFilaEstado.VALIDO);
    }

    return construirYGuardarRespuesta(filas, lectura.filasVaciasIgnoradas(), duplicadosArchivo.size());
  }

  private byte[] validarYLeerContenido(MultipartFile archivo) {
    if (archivo == null || archivo.isEmpty()) {
      throw error(PacienteImportacionErrorCodigo.ARCHIVO_VACIO,
          "Debe adjuntar un archivo Excel no vacío.", HttpStatus.BAD_REQUEST);
    }
    long maximo = properties.maxTamanoMb() * BYTES_POR_MB;
    if (archivo.getSize() > maximo) {
      throw error(PacienteImportacionErrorCodigo.ARCHIVO_DEMASIADO_GRANDE,
          "El archivo supera el tamaño máximo permitido.", HttpStatus.PAYLOAD_TOO_LARGE);
    }
    String nombre = archivo.getOriginalFilename();
    if (nombre == null || !nombre.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
      throw error(PacienteImportacionErrorCodigo.FORMATO_NO_PERMITIDO,
          "Solo se permiten archivos con extensión .xlsx.", HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }
    try {
      byte[] contenido = archivo.getBytes();
      if (contenido.length == 0) {
        throw error(PacienteImportacionErrorCodigo.ARCHIVO_VACIO,
            "Debe adjuntar un archivo Excel no vacío.", HttpStatus.BAD_REQUEST);
      }
      return contenido;
    } catch (IOException exception) {
      throw error(PacienteImportacionErrorCodigo.ARCHIVO_CORRUPTO,
          "No se pudo leer el archivo enviado.", HttpStatus.BAD_REQUEST);
    }
  }

  private Map<String, List<PacienteImportacionFila>> agruparDnisValidos(List<PacienteImportacionFila> filas) {
    Map<String, List<PacienteImportacionFila>> resultado = new HashMap<>();
    for (PacienteImportacionFila fila : filas) {
      String dni = fila.getPaciente().getDni();
      if (dniValido(dni)) resultado.computeIfAbsent(dni, ignored -> new ArrayList<>()).add(fila);
    }
    return resultado;
  }

  private Set<String> consultarDnisExistentes(Collection<String> dnis) {
    if (dnis.isEmpty()) return Set.of();
    Set<String> encontrados = pacienteRepository.findDnisExistentes(dnis);
    return encontrados == null ? Set.of() : new HashSet<>(encontrados);
  }

  private boolean dniValido(String dni) {
    return dni != null && dni.matches("\\d{8}");
  }

  private void agregarErrorSiNoExiste(
      PacienteImportacionFila fila,
      PacienteImportacionErrorCodigo codigo,
      String mensaje
  ) {
    if (fila.getErrores().stream().noneMatch(error -> error.getCodigo() == codigo)) {
      fila.getErrores().add(PacienteImportacionError.builder()
          .codigo(codigo).campo("DNI").mensaje(mensaje).build());
    }
  }

  private PacienteImportacionValidacionResponse construirYGuardarRespuesta(
      List<PacienteImportacionFila> filas,
      int filasVaciasIgnoradas,
      int gruposDuplicados
  ) {
    PacienteImportacionResumenResponse resumen = PacienteImportacionResumenResponse.builder()
        .registrosAnalizados(filas.size())
        .validos(contar(filas, PacienteImportacionFilaEstado.VALIDO))
        .conErrores(contar(filas, PacienteImportacionFilaEstado.ERROR_DATOS))
        .filasConDniDuplicado(contar(filas, PacienteImportacionFilaEstado.DNI_DUPLICADO_ARCHIVO))
        .gruposDniDuplicados(gruposDuplicados)
        .dniExistentes(contar(filas, PacienteImportacionFilaEstado.DNI_EXISTENTE))
        .conAdvertencias((int) filas.stream().filter(fila -> !fila.getAdvertencias().isEmpty()).count())
        .filasVaciasIgnoradas(filasVaciasIgnoradas)
        .build();

    UUID importacionId = UUID.randomUUID();
    Instant fechaCreacion = Instant.now();
    Instant expiraEn = fechaCreacion.plus(properties.tiempoExpiracionMinutos(), ChronoUnit.MINUTES);
    PacienteImportacion importacion = PacienteImportacion.builder()
        .importacionId(importacionId)
        .versionPlantilla(properties.versionPlantilla())
        .fechaCreacion(fechaCreacion)
        .fechaExpiracion(expiraEn)
        .estado(PacienteImportacionEstado.PREVISUALIZADA)
        .resumen(toModel(resumen))
        .filas(filas.stream().map(this::paraAlmacenamiento).collect(Collectors.toCollection(ArrayList::new)))
        .build();
    importacionStore.guardar(importacion);

    return PacienteImportacionValidacionResponse.builder()
        .importacionId(importacionId)
        .estado(PacienteImportacionEstado.PREVISUALIZADA)
        .expiraEn(expiraEn)
        .resumen(resumen)
        .filas(filas.stream().map(this::toResponse).toList())
        .build();
  }

  private PacienteImportacionResumen toModel(PacienteImportacionResumenResponse resumen) {
    return PacienteImportacionResumen.builder()
        .registrosAnalizados(resumen.getRegistrosAnalizados())
        .validos(resumen.getValidos())
        .conErrores(resumen.getConErrores())
        .filasConDniDuplicado(resumen.getFilasConDniDuplicado())
        .gruposDniDuplicados(resumen.getGruposDniDuplicados())
        .dniExistentes(resumen.getDniExistentes())
        .conAdvertencias(resumen.getConAdvertencias())
        .filasVaciasIgnoradas(resumen.getFilasVaciasIgnoradas())
        .build();
  }

  private PacienteImportacionFila paraAlmacenamiento(PacienteImportacionFila fila) {
    return PacienteImportacionFila.builder()
        .numeroFila(fila.getNumeroFila())
        .estado(fila.getEstado())
        .paciente(fila.getPaciente())
        .antecedentes(fila.getAntecedentes())
        .errores(new ArrayList<>(fila.getErrores()))
        .advertencias(new ArrayList<>(fila.getAdvertencias()))
        .build();
  }

  private int contar(List<PacienteImportacionFila> filas, PacienteImportacionFilaEstado estado) {
    return (int) filas.stream().filter(fila -> fila.getEstado() == estado).count();
  }

  private PacienteImportacionFilaDetalleResponse toResponse(PacienteImportacionFila fila) {
    String nombreCompleto = String.join(" ", fila.getPaciente().getNombres(), fila.getPaciente().getApellidos()).trim();
    return PacienteImportacionFilaDetalleResponse.builder()
        .numeroFila(fila.getNumeroFila()).nombreCompleto(nombreCompleto)
        .dni(fila.getPaciente().getDni()).estado(fila.getEstado())
        .paciente(fila.getPaciente()).antecedentes(fila.getAntecedentes())
        .errores(fila.getErrores().stream().map(error -> PacienteImportacionErrorResponse.builder()
            .numeroFila(fila.getNumeroFila()).codigo(error.getCodigo()).campo(error.getCampo())
            .mensaje(error.getMensaje()).build()).toList())
        .advertencias(fila.getAdvertencias().stream().map(advertencia ->
            PacienteImportacionAdvertenciaResponse.builder().numeroFila(fila.getNumeroFila())
                .codigo(advertencia.getCodigo()).campo(advertencia.getCampo())
                .mensaje(advertencia.getMensaje()).build()).toList())
        .build();
  }

  private PacienteImportacionException error(
      PacienteImportacionErrorCodigo codigo,
      String mensaje,
      HttpStatus status
  ) {
    return new PacienteImportacionException(codigo, mensaje, status);
  }
}
