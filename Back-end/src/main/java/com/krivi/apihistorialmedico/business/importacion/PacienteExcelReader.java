package com.krivi.apihistorialmedico.business.importacion;

import com.krivi.apihistorialmedico.business.exception.PacienteImportacionException;
import com.krivi.apihistorialmedico.config.PacienteImportacionProperties;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionAntecedentes;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionDatos;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionError;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionErrorCodigo;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionFila;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionFilaEstado;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class PacienteExcelReader {
  private static final DateTimeFormatter FECHA_FORMATTER = DateTimeFormatter
      .ofPattern("dd/MM/uuuu", Locale.ROOT)
      .withResolverStyle(ResolverStyle.STRICT);
  private static final int COLUMNA_FECHA_NACIMIENTO = 2;
  private static final int COLUMNA_ESTADO_CIVIL = 3;
  private static final int COLUMNA_DNI = 4;
  private static final int COLUMNA_SEXO = 5;
  private static final int COLUMNA_EDUCACION = 14;
  private static final int COLUMNA_TRAIDO_POR = 8;
  private static final ZoneId ZONA_HORARIA_LIMA = ZoneId.of("America/Lima");

  private final PacienteImportacionProperties properties;
  private final DataFormatter formatter = new DataFormatter(Locale.ROOT);

  public PacienteExcelReader(PacienteImportacionProperties properties) {
    this.properties = properties;
  }

  public PacienteExcelValidationResult leer(byte[] contenido) {
    try (ByteArrayInputStream input = new ByteArrayInputStream(contenido);
         XSSFWorkbook workbook = new XSSFWorkbook(input)) {
      validarVersion(workbook);
      Sheet pacientes = workbook.getSheet(PacienteExcelTemplateGenerator.HOJA_PACIENTES);
      if (pacientes == null) {
        throw global(PacienteImportacionErrorCodigo.HOJA_PRINCIPAL_FALTANTE,
            "La plantilla no contiene la hoja Pacientes.");
      }
      validarEncabezados(pacientes);
      return leerFilas(pacientes);
    } catch (PacienteImportacionException exception) {
      throw exception;
    } catch (IOException | POIXMLException | IllegalArgumentException exception) {
      throw global(PacienteImportacionErrorCodigo.ARCHIVO_CORRUPTO,
          "El archivo no es un libro Excel OOXML válido o está corrupto.");
    }
  }

  private void validarVersion(XSSFWorkbook workbook) {
    var propiedad = workbook.getProperties().getCustomProperties()
        .getProperty(PacienteExcelTemplateGenerator.PROPIEDAD_VERSION);
    String version = propiedad == null ? null : propiedad.getLpwstr();
    if (version == null || version.isBlank()) {
      throw global(PacienteImportacionErrorCodigo.PLANTILLA_NO_RECONOCIDA,
          "No se pudo identificar la versión de la plantilla.");
    }
    if (!properties.versionPlantilla().equals(version.trim())) {
      throw global(PacienteImportacionErrorCodigo.VERSION_INCOMPATIBLE,
          "La versión de la plantilla no es compatible con el sistema.");
    }
  }

  private void validarEncabezados(Sheet sheet) {
    Row row = sheet.getRow(0);
    if (row == null) {
      throw global(PacienteImportacionErrorCodigo.COLUMNA_FALTANTE,
          "La plantilla no contiene la fila de encabezados.");
    }
    int ultimaColumna = row.getLastCellNum();
    if (ultimaColumna != properties.maxColumnas()) {
      PacienteImportacionErrorCodigo codigo = ultimaColumna < properties.maxColumnas()
          ? PacienteImportacionErrorCodigo.COLUMNA_FALTANTE
          : PacienteImportacionErrorCodigo.PLANTILLA_NO_RECONOCIDA;
      throw global(codigo, "La cantidad de columnas de la plantilla no es válida.");
    }

    List<String> recibidos = new ArrayList<>();
    for (int columna = 0; columna < properties.maxColumnas(); columna++) {
      recibidos.add(limpiarTexto(formatter.formatCellValue(row.getCell(columna))));
    }
    if (recibidos.stream().anyMatch(String::isBlank)) {
      throw global(PacienteImportacionErrorCodigo.COLUMNA_FALTANTE,
          "La plantilla contiene una columna obligatoria sin encabezado.");
    }
    long distintos = recibidos.stream().map(this::claveComparacion).distinct().count();
    if (distintos != recibidos.size()) {
      throw global(PacienteImportacionErrorCodigo.ENCABEZADO_DUPLICADO,
          "La plantilla contiene encabezados duplicados.");
    }
    if (!recibidos.equals(PacienteExcelTemplateGenerator.ENCABEZADOS)) {
      throw global(PacienteImportacionErrorCodigo.PLANTILLA_NO_RECONOCIDA,
          "Los encabezados no coinciden con la plantilla oficial o están desordenados.");
    }
  }

  private PacienteExcelValidationResult leerFilas(Sheet sheet) {
    List<PacienteImportacionFila> filas = new ArrayList<>();
    int filasVacias = 0;
    for (int indice = 1; indice <= sheet.getLastRowNum(); indice++) {
      Row row = sheet.getRow(indice);
      validarColumnasAdicionales(row);
      if (filaVacia(row)) {
        filasVacias++;
        continue;
      }
      rechazarFormulas(row);
      filas.add(leerFila(row));
      if (filas.size() > properties.maxRegistros()) {
        throw global(PacienteImportacionErrorCodigo.MAX_REGISTROS_EXCEDIDO,
            "El archivo supera el máximo permitido de pacientes.");
      }
    }
    return new PacienteExcelValidationResult(List.copyOf(filas), filasVacias);
  }

  private void validarColumnasAdicionales(Row row) {
    if (row == null || row.getLastCellNum() <= properties.maxColumnas()) return;
    for (int columna = properties.maxColumnas(); columna < row.getLastCellNum(); columna++) {
      Cell cell = row.getCell(columna);
      if (cell != null && (cell.getCellType() == CellType.FORMULA
          || !formatter.formatCellValue(cell).trim().isEmpty())) {
        throw global(PacienteImportacionErrorCodigo.PLANTILLA_NO_RECONOCIDA,
            "La hoja Pacientes contiene columnas adicionales no permitidas.");
      }
    }
  }

  private boolean filaVacia(Row row) {
    if (row == null) return true;
    for (int columna = 0; columna < properties.maxColumnas(); columna++) {
      Cell cell = row.getCell(columna);
      if (cell != null && cell.getCellType() != CellType.BLANK
          && !formatter.formatCellValue(cell).trim().isEmpty()) return false;
      if (cell != null && cell.getCellType() == CellType.FORMULA) return false;
    }
    return true;
  }

  private void rechazarFormulas(Row row) {
    for (int columna = 0; columna < properties.maxColumnas(); columna++) {
      Cell cell = row.getCell(columna);
      if (cell != null && cell.getCellType() == CellType.FORMULA) {
        throw global(PacienteImportacionErrorCodigo.FORMULA_NO_PERMITIDA,
            "No se permiten fórmulas en las celdas de datos.");
      }
    }
  }

  private PacienteImportacionFila leerFila(Row row) {
    List<PacienteImportacionError> errores = new ArrayList<>();
    Map<String, String> originales = new LinkedHashMap<>();
    List<String> textos = new ArrayList<>(properties.maxColumnas());
    for (int columna = 0; columna < properties.maxColumnas(); columna++) {
      String original = valorOriginal(row.getCell(columna));
      originales.put(PacienteExcelTemplateGenerator.ENCABEZADOS.get(columna), original);
      textos.add(limpiarTexto(original));
      if (columna != COLUMNA_TRAIDO_POR && textos.get(columna).isBlank()) {
        agregarError(errores, PacienteImportacionErrorCodigo.CAMPO_OBLIGATORIO,
            PacienteExcelTemplateGenerator.ENCABEZADOS.get(columna), "El campo es obligatorio.");
      }
    }

    String dni = leerDni(row.getCell(COLUMNA_DNI), errores);
    LocalDate fecha = leerFecha(row.getCell(COLUMNA_FECHA_NACIMIENTO), errores);
    String estadoCivil = normalizarEstadoCivil(textos.get(COLUMNA_ESTADO_CIVIL), errores);
    String sexo = normalizarSexo(textos.get(COLUMNA_SEXO), errores);
    String educacion = normalizarEducacion(textos.get(COLUMNA_EDUCACION), errores);

    PacienteImportacionDatos paciente = PacienteImportacionDatos.builder()
        .apellidos(textos.get(0)).nombres(textos.get(1)).fechaNacimiento(fecha)
        .estadoCivil(estadoCivil).dni(dni).sexo(sexo).direccion(textos.get(6))
        .distrito(textos.get(7)).traidoPor(textos.get(8)).build();
    PacienteImportacionAntecedentes antecedentes = PacienteImportacionAntecedentes.builder()
        .alimentacion(textos.get(9)).habitos(textos.get(10)).vivienda(textos.get(11))
        .desarrolloPsicomotor(textos.get(12)).vacunas(textos.get(13)).educacion(educacion)
        .enfermedadesPrevias(textos.get(15)).cirugiasPrevias(textos.get(16))
        .alergiasMedicamentos(textos.get(17)).build();

    return PacienteImportacionFila.builder()
        .numeroFila(row.getRowNum() + 1)
        .estado(errores.isEmpty() ? PacienteImportacionFilaEstado.VALIDO : PacienteImportacionFilaEstado.ERROR_DATOS)
        .paciente(paciente).antecedentes(antecedentes).datosOriginales(originales).errores(errores).build();
  }

  private String valorOriginal(Cell cell) {
    if (cell == null || cell.getCellType() == CellType.BLANK) return "";
    return formatter.formatCellValue(cell);
  }

  private String leerDni(Cell cell, List<PacienteImportacionError> errores) {
    if (cell == null || cell.getCellType() == CellType.BLANK) return "";
    String dni;
    if (cell.getCellType() == CellType.NUMERIC) {
      double numero = cell.getNumericCellValue();
      if (!Double.isFinite(numero) || numero != Math.rint(numero)
          || numero < 10_000_000 || numero > 99_999_999) {
        agregarErrorDni(errores);
        return limpiarTexto(formatter.formatCellValue(cell));
      }
      dni = Long.toString((long) numero);
    } else {
      dni = limpiarTexto(formatter.formatCellValue(cell));
    }
    if (!dni.matches("\\d{8}")) agregarErrorDni(errores);
    return dni;
  }

  private void agregarErrorDni(List<PacienteImportacionError> errores) {
    if (errores.stream().noneMatch(error -> error.getCodigo() == PacienteImportacionErrorCodigo.DNI_INVALIDO)) {
      agregarError(errores, PacienteImportacionErrorCodigo.DNI_INVALIDO, "DNI",
          "El DNI debe contener exactamente 8 dígitos.");
    }
  }

  private LocalDate leerFecha(Cell cell, List<PacienteImportacionError> errores) {
    if (cell == null || cell.getCellType() == CellType.BLANK) return null;
    LocalDate fecha;
    try {
      if (cell.getCellType() == CellType.NUMERIC) {
        if (!DateUtil.isCellDateFormatted(cell) || !DateUtil.isValidExcelDate(cell.getNumericCellValue())) {
          throw new DateTimeParseException("Fecha numérica inválida", "", 0);
        }
        fecha = DateUtil.getLocalDateTime(cell.getNumericCellValue()).toLocalDate();
      } else if (cell.getCellType() == CellType.STRING) {
        fecha = LocalDate.parse(limpiarTexto(cell.getStringCellValue()), FECHA_FORMATTER);
      } else {
        throw new DateTimeParseException("Tipo inválido", "", 0);
      }
    } catch (DateTimeParseException | IllegalArgumentException exception) {
      agregarError(errores, PacienteImportacionErrorCodigo.FECHA_INVALIDA, "Fecha de nacimiento",
          "La fecha debe ser válida y tener el formato dd/MM/yyyy.");
      return null;
    }
    if (fecha.isAfter(LocalDate.now(ZONA_HORARIA_LIMA))) {
      agregarError(errores, PacienteImportacionErrorCodigo.FECHA_FUTURA, "Fecha de nacimiento",
          "La fecha de nacimiento no puede ser futura.");
    }
    return fecha;
  }

  private String normalizarSexo(String valor, List<PacienteImportacionError> errores) {
    if (valor.isBlank()) return "";
    return switch (claveComparacion(valor)) {
      case "M", "MASCULINO", "HOMBRE", "VARON" -> "M";
      case "F", "FEMENINO", "MUJER" -> "F";
      default -> {
        agregarError(errores, PacienteImportacionErrorCodigo.SEXO_INVALIDO, "Sexo",
            "El valor de sexo no pertenece al catálogo permitido.");
        yield valor;
      }
    };
  }

  private String normalizarEstadoCivil(String valor, List<PacienteImportacionError> errores) {
    if (valor.isBlank()) return "";
    String clave = claveComparacion(valor).replace("(A)", "");
    if (clave.startsWith("SOLTER")) return "SOLTERO";
    if (clave.startsWith("CASAD")) return "CASADO";
    if (clave.startsWith("DIVORCIAD")) return "DIVORCIADO";
    if (clave.startsWith("VIUD")) return "VIUDO";
    agregarError(errores, PacienteImportacionErrorCodigo.ESTADO_CIVIL_INVALIDO, "Estado civil",
        "El estado civil no pertenece al catálogo permitido.");
    return valor;
  }

  private String normalizarEducacion(String valor, List<PacienteImportacionError> errores) {
    if (valor.isBlank()) return "";
    return switch (claveComparacion(valor)) {
      case "P", "PRIMARIA" -> "P";
      case "S", "SECUNDARIA" -> "S";
      case "T", "TECNICO" -> "T";
      case "S1", "SUPERIOR" -> "S1";
      default -> {
        agregarError(errores, PacienteImportacionErrorCodigo.EDUCACION_INVALIDA, "Educación",
            "La educación no pertenece al catálogo permitido.");
        yield valor;
      }
    };
  }

  private String limpiarTexto(String valor) {
    return valor == null ? "" : valor.trim().replaceAll("\\s+", " ");
  }

  private String claveComparacion(String valor) {
    return Normalizer.normalize(limpiarTexto(valor), Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT);
  }

  private void agregarError(
      List<PacienteImportacionError> errores,
      PacienteImportacionErrorCodigo codigo,
      String campo,
      String mensaje
  ) {
    errores.add(PacienteImportacionError.builder().codigo(codigo).campo(campo).mensaje(mensaje).build());
  }

  private PacienteImportacionException global(PacienteImportacionErrorCodigo codigo, String mensaje) {
    return new PacienteImportacionException(codigo, mensaje, HttpStatus.BAD_REQUEST);
  }
}
