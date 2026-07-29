package com.krivi.apihistorialmedico.business.importacion;

import com.krivi.apihistorialmedico.business.exception.PacienteImportacionException;
import com.krivi.apihistorialmedico.business.services.impl.importacion.PacienteImportacionValidacionServiceImpl;
import com.krivi.apihistorialmedico.config.PacienteImportacionProperties;
import com.krivi.apihistorialmedico.model.api.importacion.PacienteImportacionValidacionResponse;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionErrorCodigo;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionFilaEstado;
import com.krivi.apihistorialmedico.repository.PacienteRepository;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class PacienteImportacionValidacionServiceImplTest {
  private PacienteImportacionProperties properties;
  private PacienteExcelTemplateGenerator generator;
  private PacienteRepository repository;
  private PacienteImportacionValidacionServiceImpl service;

  @BeforeEach
  void setUp() {
    properties = new PacienteImportacionProperties(50, 2, 15, 18, "1.0");
    generator = new PacienteExcelTemplateGenerator(properties);
    repository = mock(PacienteRepository.class);
    service = new PacienteImportacionValidacionServiceImpl(
        new PacienteExcelReader(properties), repository, properties
    );
    when(repository.findDnisExistentes(anyCollection())).thenReturn(Set.of());
  }

  @Test
  void validaArchivoYNormalizaDatosConNumeroRealDeFila() throws Exception {
    MockMultipartFile archivo = archivoConFilas(filaValida("01234567"));

    PacienteImportacionValidacionResponse response = service.validar(archivo);

    assertThat(response.getResumen().getRegistrosAnalizados()).isEqualTo(1);
    assertThat(response.getResumen().getValidos()).isEqualTo(1);
    assertThat(response.getFilas().getFirst().getNumeroFila()).isEqualTo(2);
    assertThat(response.getFilas().getFirst().getDni()).isEqualTo("01234567");
    assertThat(response.getFilas().getFirst().getPaciente().getSexo()).isEqualTo("F");
    assertThat(response.getFilas().getFirst().getPaciente().getEstadoCivil()).isEqualTo("SOLTERO");
    assertThat(response.getFilas().getFirst().getAntecedentes().getEducacion()).isEqualTo("S1");
    verify(repository).findDnisExistentes(anyCollection());
    verifyNoMoreInteractions(repository);
  }

  @Test
  void aceptaFechaExcelRealYVariantesDeCatalogos() throws Exception {
    byte[] contenido = workbook(workbook -> {
      Row row = workbook.getSheet("Pacientes").getRow(1);
      escribirFila(row, filaValida("12345678"));
      row.getCell(2).setCellValue(java.sql.Date.valueOf(LocalDate.of(1990, 5, 10)));
      row.getCell(2).getCellStyle().setDataFormat(workbook.createDataFormat().getFormat("dd/MM/yyyy"));
      row.getCell(3).setCellValue("  casada  ");
      row.getCell(5).setCellValue("  mujer ");
      row.getCell(14).setCellValue(" tecnico ");
    });

    var fila = service.validar(archivo(contenido, "pacientes.xlsx")).getFilas().getFirst();

    assertThat(fila.getPaciente().getFechaNacimiento()).isEqualTo(LocalDate.of(1990, 5, 10));
    assertThat(fila.getPaciente().getEstadoCivil()).isEqualTo("CASADO");
    assertThat(fila.getPaciente().getSexo()).isEqualTo("F");
    assertThat(fila.getAntecedentes().getEducacion()).isEqualTo("T");
  }

  @ParameterizedTest
  @ValueSource(strings = {"1234567", "123456789", "1234A678", "1234.678", "1.234E7"})
  void rechazaDniTextualInvalido(String dni) throws Exception {
    var response = service.validar(archivoConFilas(filaValida(dni)));

    assertThat(response.getFilas().getFirst().getEstado()).isEqualTo(PacienteImportacionFilaEstado.ERROR_DATOS);
    assertThat(response.getFilas().getFirst().getErrores())
        .extracting(error -> error.getCodigo()).contains(PacienteImportacionErrorCodigo.DNI_INVALIDO);
  }

  @Test
  void aceptaDniNumericoDeOchoDigitosPeroNoInventaCero() throws Exception {
    byte[] valido = workbook(workbook -> {
      Row row = workbook.getSheet("Pacientes").getRow(1);
      escribirFila(row, filaValida("12345678"));
      row.getCell(4).setCellValue(12345678d);
    });
    assertThat(service.validar(archivo(valido, "valido.xlsx")).getFilas().getFirst().getDni())
        .isEqualTo("12345678");

    byte[] perdido = workbook(workbook -> {
      Row row = workbook.getSheet("Pacientes").getRow(1);
      escribirFila(row, filaValida("12345678"));
      row.getCell(4).setCellValue(1234567d);
    });
    assertThat(service.validar(archivo(perdido, "perdido.xlsx")).getFilas().getFirst().getErrores())
        .extracting(error -> error.getCodigo()).contains(PacienteImportacionErrorCodigo.DNI_INVALIDO);
  }

  @Test
  void marcaTodasLasRepeticionesYContinuaConLasDemasFilas() throws Exception {
    var response = service.validar(archivoConFilas(
        filaValida("12345678"), filaValida("12345678"), filaValida("12345678"), filaValida("87654321")
    ));

    assertThat(response.getFilas()).extracting(fila -> fila.getEstado()).containsExactly(
        PacienteImportacionFilaEstado.DNI_DUPLICADO_ARCHIVO,
        PacienteImportacionFilaEstado.DNI_DUPLICADO_ARCHIVO,
        PacienteImportacionFilaEstado.DNI_DUPLICADO_ARCHIVO,
        PacienteImportacionFilaEstado.VALIDO
    );
    assertThat(response.getResumen().getFilasConDniDuplicado()).isEqualTo(3);
    assertThat(response.getResumen().getValidos()).isEqualTo(1);
  }

  @Test
  void consultaDnisUnaSolaVezYMarcaLosExistentesSinAsumirUnicidad() throws Exception {
    when(repository.findDnisExistentes(anyCollection())).thenReturn(Set.of("12345678"));

    var response = service.validar(archivoConFilas(filaValida("12345678"), filaValida("87654321")));

    assertThat(response.getFilas()).extracting(fila -> fila.getEstado()).containsExactly(
        PacienteImportacionFilaEstado.DNI_EXISTENTE, PacienteImportacionFilaEstado.VALIDO
    );
    verify(repository, times(1)).findDnisExistentes(anyCollection());
  }

  @ParameterizedTest
  @ValueSource(strings = {"31/02/2020", "2020-02-20", "02/03/04"})
  void rechazaFechasInvalidasOAmbiguas(String fecha) throws Exception {
    String[] fila = filaValida("12345678");
    fila[2] = fecha;
    var response = service.validar(archivoConFilas(fila));
    assertThat(response.getFilas().getFirst().getErrores()).extracting(error -> error.getCodigo())
        .contains(PacienteImportacionErrorCodigo.FECHA_INVALIDA);
  }

  @Test
  void rechazaFechaFuturaYFechaObligatoriaVacia() throws Exception {
    String[] futura = filaValida("12345678");
    futura[2] = LocalDate.now(java.time.ZoneId.of("America/Lima")).plusDays(1)
        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    assertThat(service.validar(archivoConFilas(futura)).getFilas().getFirst().getErrores())
        .extracting(error -> error.getCodigo()).contains(PacienteImportacionErrorCodigo.FECHA_FUTURA);

    String[] vacia = filaValida("87654321");
    vacia[2] = "";
    assertThat(service.validar(archivoConFilas(vacia)).getFilas().getFirst().getErrores())
        .extracting(error -> error.getCodigo()).contains(PacienteImportacionErrorCodigo.CAMPO_OBLIGATORIO);
  }

  @Test
  void rechazaCatalogosInvalidosYConservaTextoClinico() throws Exception {
    String[] fila = filaValida("12345678");
    fila[3] = "acompañado";
    fila[5] = "X";
    fila[10] = "  No   refiere ";
    fila[14] = "Doctorado";

    var resultado = service.validar(archivoConFilas(fila)).getFilas().getFirst();

    assertThat(resultado.getErrores()).extracting(error -> error.getCodigo()).contains(
        PacienteImportacionErrorCodigo.SEXO_INVALIDO,
        PacienteImportacionErrorCodigo.ESTADO_CIVIL_INVALIDO,
        PacienteImportacionErrorCodigo.EDUCACION_INVALIDA
    );
    assertThat(resultado.getAntecedentes().getHabitos()).isEqualTo("No refiere");
  }

  @Test
  void ignoraFilasVaciasPeroProcesaFilasParciales() throws Exception {
    byte[] contenido = workbook(workbook -> {
      escribirFila(workbook.getSheet("Pacientes").getRow(2), new String[]{"Solo apellido"});
    });

    var response = service.validar(archivo(contenido, "pacientes.xlsx"));

    assertThat(response.getResumen().getRegistrosAnalizados()).isEqualTo(1);
    assertThat(response.getFilas().getFirst().getNumeroFila()).isEqualTo(3);
    assertThat(response.getFilas().getFirst().getEstado()).isEqualTo(PacienteImportacionFilaEstado.ERROR_DATOS);
  }

  @Test
  void validaErroresGlobalesDeArchivo() {
    assertGlobal(null, PacienteImportacionErrorCodigo.ARCHIVO_VACIO, HttpStatus.BAD_REQUEST);
    assertGlobal(new MockMultipartFile("archivo", "vacio.xlsx", "application/octet-stream", new byte[0]),
        PacienteImportacionErrorCodigo.ARCHIVO_VACIO, HttpStatus.BAD_REQUEST);
    assertGlobal(new MockMultipartFile("archivo", "pacientes.csv", "text/csv", "x".getBytes()),
        PacienteImportacionErrorCodigo.FORMATO_NO_PERMITIDO, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    assertGlobal(new MockMultipartFile("archivo", "falso.xlsx", "application/zip", "no-es-ooxml".getBytes()),
        PacienteImportacionErrorCodigo.ARCHIVO_CORRUPTO, HttpStatus.BAD_REQUEST);
    assertGlobal(new MockMultipartFile("archivo", "grande.xlsx", "application/octet-stream", new byte[2 * 1024 * 1024 + 1]),
        PacienteImportacionErrorCodigo.ARCHIVO_DEMASIADO_GRANDE, HttpStatus.PAYLOAD_TOO_LARGE);
  }

  @Test
  void rechazaVersionHojaEncabezadosColumnasFormulasYLimite() throws Exception {
    assertWorkbookGlobal(workbook -> workbook.getProperties().getCustomProperties()
        .getProperty(PacienteExcelTemplateGenerator.PROPIEDAD_VERSION).setLpwstr("2.0"),
        PacienteImportacionErrorCodigo.VERSION_INCOMPATIBLE);
    assertWorkbookGlobal(workbook -> workbook.setSheetName(workbook.getSheetIndex("Pacientes"), "Otra"),
        PacienteImportacionErrorCodigo.HOJA_PRINCIPAL_FALTANTE);
    assertWorkbookGlobal(workbook -> workbook.getSheet("Pacientes").getRow(0).getCell(17).setBlank(),
        PacienteImportacionErrorCodigo.COLUMNA_FALTANTE);
    assertWorkbookGlobal(workbook -> workbook.getSheet("Pacientes").getRow(0).createCell(18).setCellValue("Extra"),
        PacienteImportacionErrorCodigo.PLANTILLA_NO_RECONOCIDA);
    assertWorkbookGlobal(workbook -> workbook.getSheet("Pacientes").getRow(0).getCell(1).setCellValue("Apellidos"),
        PacienteImportacionErrorCodigo.ENCABEZADO_DUPLICADO);
    assertWorkbookGlobal(workbook -> {
      var row = workbook.getSheet("Pacientes").getRow(0);
      String primero = row.getCell(0).getStringCellValue();
      row.getCell(0).setCellValue(row.getCell(1).getStringCellValue());
      row.getCell(1).setCellValue(primero);
    }, PacienteImportacionErrorCodigo.PLANTILLA_NO_RECONOCIDA);
    assertWorkbookGlobal(workbook -> {
      Row row = workbook.getSheet("Pacientes").getRow(1);
      escribirFila(row, filaValida("12345678"));
      row.getCell(0).setCellFormula("1+1");
    }, PacienteImportacionErrorCodigo.FORMULA_NO_PERMITIDA);
    assertWorkbookGlobal(workbook -> {
      var sheet = workbook.getSheet("Pacientes");
      for (int i = 1; i <= 51; i++) escribirFila(sheet.getRow(i) == null ? sheet.createRow(i) : sheet.getRow(i), filaValida(String.format("%08d", i)));
    }, PacienteImportacionErrorCodigo.MAX_REGISTROS_EXCEDIDO);
  }

  private void assertWorkbookGlobal(WorkbookChange change, PacienteImportacionErrorCodigo codigo) throws Exception {
    assertGlobal(archivo(workbook(change), "pacientes.xlsx"), codigo, HttpStatus.BAD_REQUEST);
  }

  private void assertGlobal(MockMultipartFile archivo, PacienteImportacionErrorCodigo codigo, HttpStatus status) {
    assertThatThrownBy(() -> service.validar(archivo))
        .isInstanceOfSatisfying(PacienteImportacionException.class, exception -> {
          assertThat(exception.getCodigo()).isEqualTo(codigo);
          assertThat(exception.getEstadoHttp()).isEqualTo(status);
        });
  }

  private MockMultipartFile archivoConFilas(String[]... filas) throws Exception {
    return archivo(workbook(workbook -> {
      for (int indice = 0; indice < filas.length; indice++) {
        escribirFila(workbook.getSheet("Pacientes").getRow(indice + 1), filas[indice]);
      }
    }), "pacientes.xlsx");
  }

  private byte[] workbook(WorkbookChange change) throws Exception {
    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(generator.generar()));
         ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      change.apply(workbook);
      workbook.write(output);
      return output.toByteArray();
    }
  }

  private MockMultipartFile archivo(byte[] contenido, String nombre) {
    return new MockMultipartFile("archivo", nombre,
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", contenido);
  }

  private void escribirFila(Row row, String[] valores) {
    for (int indice = 0; indice < valores.length; indice++) {
      var cell = row.getCell(indice) == null ? row.createCell(indice, CellType.STRING) : row.getCell(indice);
      cell.setCellValue(valores[indice]);
    }
  }

  private String[] filaValida(String dni) {
    return new String[]{"Pérez Ramos", "Juan", "10/05/1990", "Soltero(a)", dni, "Femenino",
        "Av. Uno 123", "Lima", "", "Balanceada", "No refiere", "Material noble", "Normal",
        "Completas", "Superior", "Ninguna", "Ninguna", "No refiere"};
  }

  @FunctionalInterface
  private interface WorkbookChange {
    void apply(XSSFWorkbook workbook) throws Exception;
  }
}
