package com.krivi.apihistorialmedico.business.importacion;

import com.krivi.apihistorialmedico.config.PacienteImportacionProperties;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

@Component
public class PacienteExcelTemplateGenerator {
  public static final String HOJA_PACIENTES = "Pacientes";
  public static final String HOJA_INSTRUCCIONES = "Instrucciones";
  public static final String HOJA_CATALOGOS = "Catalogos";
  public static final String PROPIEDAD_VERSION = "versionPlantilla";

  public static final List<String> ENCABEZADOS = List.of(
      "Apellidos", "Nombres", "Fecha de nacimiento", "Estado civil", "DNI", "Sexo",
      "Dirección", "Distrito", "Traído por", "Alimentación", "Hábitos", "Vivienda",
      "Desarrollo psicomotor", "Vacunas", "Educación", "Enfermedades previas",
      "Cirugías previas", "Alergias a medicamentos"
  );

  private static final List<String> SEXOS = List.of("Masculino", "Femenino");
  private static final List<String> ESTADOS_CIVILES = List.of(
      "Soltero(a)", "Casado(a)", "Divorciado(a)", "Viudo(a)"
  );
  private static final List<String> EDUCACIONES = List.of(
      "Primaria", "Secundaria", "Tecnico", "Superior"
  );
  private static final String CLAVE_PROTECCION = "plantilla-pacientes";

  private final PacienteImportacionProperties properties;

  public PacienteExcelTemplateGenerator(PacienteImportacionProperties properties) {
    this.properties = properties;
  }

  public byte[] generar() {
    validarConfiguracion();
    try (XSSFWorkbook workbook = new XSSFWorkbook();
         ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      workbook.getProperties().getCustomProperties()
          .addProperty(PROPIEDAD_VERSION, properties.versionPlantilla());

      Sheet pacientes = workbook.createSheet(HOJA_PACIENTES);
      Sheet instrucciones = workbook.createSheet(HOJA_INSTRUCCIONES);
      Sheet catalogos = workbook.createSheet(HOJA_CATALOGOS);

      crearCatalogos(workbook, catalogos);
      crearHojaPacientes(workbook, pacientes);
      crearInstrucciones(workbook, instrucciones);
      workbook.setSheetHidden(workbook.getSheetIndex(catalogos), true);

      workbook.setActiveSheet(workbook.getSheetIndex(pacientes));
      workbook.write(output);
      return output.toByteArray();
    } catch (IOException exception) {
      throw new UncheckedIOException("No se pudo generar la plantilla de pacientes.", exception);
    }
  }

  private void validarConfiguracion() {
    if (properties.maxColumnas() != ENCABEZADOS.size()) {
      throw new IllegalStateException("La cantidad configurada de columnas no coincide con la plantilla.");
    }
  }

  private void crearHojaPacientes(XSSFWorkbook workbook, Sheet sheet) {
    CellStyle encabezadoStyle = crearEstiloEncabezado(workbook);
    CellStyle textoEditable = crearEstiloEditable(workbook, "@");
    CellStyle fechaEditable = crearEstiloEditable(workbook, "dd/MM/yyyy");

    Row encabezado = sheet.createRow(0);
    for (int columna = 0; columna < ENCABEZADOS.size(); columna++) {
      Cell cell = encabezado.createCell(columna);
      cell.setCellValue(ENCABEZADOS.get(columna));
      cell.setCellStyle(encabezadoStyle);
      sheet.setColumnWidth(columna, anchoColumna(columna));
    }
    encabezado.setHeightInPoints(30);

    for (int fila = 1; fila <= properties.maxRegistros(); fila++) {
      Row row = sheet.createRow(fila);
      for (int columna = 0; columna < properties.maxColumnas(); columna++) {
        Cell cell = row.createCell(columna);
        cell.setCellStyle(columna == 2 ? fechaEditable : textoEditable);
      }
    }

    sheet.createFreezePane(0, 1);
    sheet.setAutoFilter(new CellRangeAddress(0, properties.maxRegistros(), 0, properties.maxColumnas() - 1));
    agregarValidacionLista(sheet, 5, "CatalogoSexo");
    agregarValidacionLista(sheet, 3, "CatalogoEstadoCivil");
    agregarValidacionLista(sheet, 14, "CatalogoEducacion");
    sheet.protectSheet(CLAVE_PROTECCION);
  }

  private void crearCatalogos(XSSFWorkbook workbook, Sheet sheet) {
    crearColumnaCatalogo(sheet, 0, "Sexo", SEXOS);
    crearColumnaCatalogo(sheet, 1, "Estado civil", ESTADOS_CIVILES);
    crearColumnaCatalogo(sheet, 2, "Educación", EDUCACIONES);
    crearNombre(workbook, "CatalogoSexo", "$A$2:$A$3");
    crearNombre(workbook, "CatalogoEstadoCivil", "$B$2:$B$5");
    crearNombre(workbook, "CatalogoEducacion", "$C$2:$C$5");
    sheet.protectSheet(CLAVE_PROTECCION);
  }

  private void crearColumnaCatalogo(Sheet sheet, int columna, String titulo, List<String> valores) {
    Row tituloRow = obtenerOCrearFila(sheet, 0);
    tituloRow.createCell(columna).setCellValue(titulo);
    for (int indice = 0; indice < valores.size(); indice++) {
      obtenerOCrearFila(sheet, indice + 1).createCell(columna).setCellValue(valores.get(indice));
    }
  }

  private Row obtenerOCrearFila(Sheet sheet, int indice) {
    Row row = sheet.getRow(indice);
    return row == null ? sheet.createRow(indice) : row;
  }

  private void crearNombre(XSSFWorkbook workbook, String nombre, String rango) {
    Name namedRange = workbook.createName();
    namedRange.setNameName(nombre);
    namedRange.setRefersToFormula("'" + HOJA_CATALOGOS + "'!" + rango);
  }

  private void agregarValidacionLista(Sheet sheet, int columna, String nombreCatalogo) {
    DataValidationHelper helper = sheet.getDataValidationHelper();
    DataValidationConstraint constraint = helper.createFormulaListConstraint(nombreCatalogo);
    CellRangeAddressList rango = new CellRangeAddressList(1, properties.maxRegistros(), columna, columna);
    DataValidation validation = helper.createValidation(constraint, rango);
    validation.setShowErrorBox(true);
    validation.setSuppressDropDownArrow(true);
    sheet.addValidationData(validation);
  }

  private void crearInstrucciones(XSSFWorkbook workbook, Sheet sheet) {
    CellStyle tituloStyle = crearEstiloTitulo(workbook);
    CellStyle encabezadoStyle = crearEstiloEncabezado(workbook);
    Row version = sheet.createRow(0);
    version.createCell(0).setCellValue("Versión de plantilla");
    version.createCell(1).setCellValue(properties.versionPlantilla());

    Row titulo = sheet.createRow(2);
    titulo.createCell(0).setCellValue("Instrucciones de importación masiva de pacientes");
    titulo.getCell(0).setCellStyle(tituloStyle);

    List<String> instrucciones = List.of(
        "Cada fila de la hoja Pacientes representa un paciente.",
        "Se permiten como máximo " + properties.maxRegistros() + " pacientes por archivo.",
        "El DNI es obligatorio y debe contener exactamente 8 dígitos.",
        "La columna DNI está configurada como texto para conservar los ceros iniciales.",
        "La fecha de nacimiento es obligatoria y debe ingresarse como dd/MM/yyyy.",
        "Son obligatorios: Apellidos, Nombres, Fecha de nacimiento, Estado civil, DNI, Sexo, Dirección, Distrito, Alimentación, Hábitos, Vivienda, Desarrollo psicomotor, Vacunas, Educación, Enfermedades previas, Cirugías previas y Alergias a medicamentos.",
        "Traído por conserva su comportamiento opcional.",
        "No modifique los encabezados ni agregue columnas.",
        "No se permiten fórmulas en las celdas de datos.",
        "Las celdas vacías no se convertirán automáticamente en «No refiere».",
        "Los datos serán validados antes de registrarse. La descarga o carga no registra pacientes."
    );
    for (int indice = 0; indice < instrucciones.size(); indice++) {
      sheet.createRow(indice + 4).createCell(0).setCellValue("• " + instrucciones.get(indice));
    }

    int filaEncabezadoEjemplo = 17;
    Row subtitulo = sheet.createRow(filaEncabezadoEjemplo - 1);
    subtitulo.createCell(0).setCellValue("Ejemplo completo (solo referencial; no se importa)");
    Row encabezadoEjemplo = sheet.createRow(filaEncabezadoEjemplo);
    Row datosEjemplo = sheet.createRow(filaEncabezadoEjemplo + 1);
    List<String> ejemplo = List.of(
        "Pérez Díaz", "Ana María", "10/05/1995", "Soltero(a)", "01234567", "Femenino",
        "Av. Ejemplo 123", "Lima", "Familiar", "Balanceada", "No refiere", "Material noble",
        "Normal", "Completas", "Superior", "Ninguna", "Ninguna", "No refiere"
    );
    for (int columna = 0; columna < ENCABEZADOS.size(); columna++) {
      Cell cell = encabezadoEjemplo.createCell(columna);
      cell.setCellValue(ENCABEZADOS.get(columna));
      cell.setCellStyle(encabezadoStyle);
      datosEjemplo.createCell(columna).setCellValue(ejemplo.get(columna));
    }
    sheet.setColumnWidth(0, 10000);
    for (int columna = 1; columna < ENCABEZADOS.size(); columna++) {
      sheet.setColumnWidth(columna, 5000);
    }
  }

  private CellStyle crearEstiloEncabezado(XSSFWorkbook workbook) {
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setBold(true);
    font.setColor(IndexedColors.WHITE.getIndex());
    style.setFont(font);
    style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    style.setAlignment(HorizontalAlignment.CENTER);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    style.setWrapText(true);
    style.setBorderBottom(BorderStyle.THIN);
    style.setLocked(true);
    return style;
  }

  private CellStyle crearEstiloEditable(XSSFWorkbook workbook, String formato) {
    CellStyle style = workbook.createCellStyle();
    DataFormat dataFormat = workbook.createDataFormat();
    style.setDataFormat(dataFormat.getFormat(formato));
    style.setLocked(false);
    return style;
  }

  private CellStyle crearEstiloTitulo(XSSFWorkbook workbook) {
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setBold(true);
    font.setFontHeightInPoints((short) 14);
    style.setFont(font);
    return style;
  }

  private int anchoColumna(int columna) {
    return switch (columna) {
      case 2, 3, 4, 5, 8, 14 -> 18 * 256;
      case 6, 12, 15, 16, 17 -> 28 * 256;
      default -> 22 * 256;
    };
  }
}
