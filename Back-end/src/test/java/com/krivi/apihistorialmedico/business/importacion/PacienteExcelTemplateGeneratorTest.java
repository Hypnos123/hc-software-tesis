package com.krivi.apihistorialmedico.business.importacion;

import com.krivi.apihistorialmedico.config.PacienteImportacionProperties;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class PacienteExcelTemplateGeneratorTest {
  private static final int MAX_REGISTROS = 50;
  private static final int MAX_COLUMNAS = 18;
  private static final String VERSION = "1.0";

  private PacienteExcelTemplateGenerator generator;

  @BeforeEach
  void setUp() {
    generator = new PacienteExcelTemplateGenerator(
        new PacienteImportacionProperties(MAX_REGISTROS, 2, 15, MAX_COLUMNAS, VERSION)
    );
  }

  @Test
  void generaWorkbookConHojasEncabezadosYVersionOficiales() throws Exception {
    byte[] contenido = generator.generar();

    try (XSSFWorkbook workbook = abrir(contenido)) {
      assertThat(workbook.getNumberOfSheets()).isEqualTo(3);
      assertThat(java.util.stream.IntStream.range(0, workbook.getNumberOfSheets())
          .mapToObj(workbook::getSheetName).toList())
          .containsExactly("Pacientes", "Instrucciones", "Catalogos");

      Sheet pacientes = workbook.getSheet("Pacientes");
      assertThat(celdasComoTexto(pacientes.getRow(0)))
          .containsExactlyElementsOf(PacienteExcelTemplateGenerator.ENCABEZADOS);
      assertThat(workbook.getSheet("Instrucciones").getRow(0).getCell(1).getStringCellValue())
          .isEqualTo(VERSION);
      assertThat(workbook.isSheetHidden(workbook.getSheetIndex("Catalogos"))).isTrue();
    }
  }

  @Test
  void configuraFormatoProteccionFiltroYListasSinBloquearDatos() throws Exception {
    try (XSSFWorkbook workbook = abrir(generator.generar())) {
      XSSFSheet pacientes = workbook.getSheet("Pacientes");

      assertThat(pacientes.getPaneInformation()).isNotNull();
      assertThat((int) pacientes.getPaneInformation().getHorizontalSplitPosition()).isEqualTo(1);
      assertThat(pacientes.getCTWorksheet().isSetAutoFilter()).isTrue();
      assertThat(pacientes.getCTWorksheet().isSetSheetProtection()).isTrue();
      assertThat(pacientes.getRow(0).getCell(0).getCellStyle().getLocked()).isTrue();
      assertThat(pacientes.getRow(1).getCell(0).getCellStyle().getLocked()).isFalse();
      assertThat(pacientes.getRow(MAX_REGISTROS).getCell(MAX_COLUMNAS - 1).getCellStyle().getLocked())
          .isFalse();
      assertThat(pacientes.getRow(1).getCell(4).getCellStyle().getDataFormatString()).isEqualTo("@");
      assertThat(pacientes.getRow(1).getCell(2).getCellStyle().getDataFormatString())
          .isEqualTo("dd/MM/yyyy");

      List<? extends DataValidation> validations = pacientes.getDataValidations();
      assertThat(validations).hasSize(3);
      assertThat(validations.stream()
          .map(validation -> validation.getValidationConstraint().getFormula1()).toList())
          .containsExactlyInAnyOrder("CatalogoSexo", "CatalogoEstadoCivil", "CatalogoEducacion");
    }
  }

  @Test
  void contieneCatalogosCompatiblesYEjemploSoloEnInstrucciones() throws Exception {
    try (XSSFWorkbook workbook = abrir(generator.generar())) {
      Sheet catalogos = workbook.getSheet("Catalogos");
      assertThat(valoresColumna(catalogos, 0, 0, 2)).containsExactly("Sexo", "Masculino", "Femenino");
      assertThat(valoresColumna(catalogos, 1, 0, 4))
          .containsExactly("Estado civil", "Soltero(a)", "Casado(a)", "Divorciado(a)", "Viudo(a)");
      assertThat(valoresColumna(catalogos, 2, 0, 4))
          .containsExactly("Educación", "Primaria", "Secundaria", "Tecnico", "Superior");

      assertThat(contieneTexto(workbook.getSheet("Instrucciones"), "Pérez Díaz")).isTrue();
      assertThat(contieneTexto(workbook.getSheet("Pacientes"), "Pérez Díaz")).isFalse();
      assertThat(workbook.getSheet("Pacientes").getLastRowNum()).isEqualTo(MAX_REGISTROS);
    }
  }

  @Test
  void noGeneraFormulasMacrosImagenesNiDatosEnLaTabla() throws Exception {
    byte[] contenido = generator.generar();
    try (XSSFWorkbook workbook = abrir(contenido)) {
      assertThat(workbook.getAllPictures()).isEmpty();
      for (Sheet sheet : workbook) {
        for (Row row : sheet) {
          for (Cell cell : row) {
            assertThat(cell.getCellType()).isNotEqualTo(CellType.FORMULA);
          }
        }
      }
      Sheet pacientes = workbook.getSheet("Pacientes");
      for (int fila = 1; fila <= MAX_REGISTROS; fila++) {
        for (Cell cell : pacientes.getRow(fila)) {
          assertThat(cell.getCellType()).isEqualTo(CellType.BLANK);
        }
      }
    }
    assertThat(contieneEntradaZip(contenido, "vbaProject.bin")).isFalse();
  }

  private XSSFWorkbook abrir(byte[] contenido) throws Exception {
    return new XSSFWorkbook(new ByteArrayInputStream(contenido));
  }

  private List<String> celdasComoTexto(Row row) {
    return java.util.stream.IntStream.range(0, row.getLastCellNum())
        .mapToObj(indice -> row.getCell(indice).getStringCellValue()).toList();
  }

  private List<String> valoresColumna(Sheet sheet, int columna, int inicio, int fin) {
    return java.util.stream.IntStream.rangeClosed(inicio, fin)
        .mapToObj(fila -> sheet.getRow(fila).getCell(columna).getStringCellValue()).toList();
  }

  private boolean contieneTexto(Sheet sheet, String texto) {
    for (Row row : sheet) {
      for (Cell cell : row) {
        if (cell.getCellType() == CellType.STRING && cell.getStringCellValue().contains(texto)) return true;
      }
    }
    return false;
  }

  private boolean contieneEntradaZip(byte[] contenido, String fragmento) throws Exception {
    try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(contenido))) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        if (entry.getName().contains(fragmento)) return true;
      }
      return false;
    }
  }
}
