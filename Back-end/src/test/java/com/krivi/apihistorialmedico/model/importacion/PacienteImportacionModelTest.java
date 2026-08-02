package com.krivi.apihistorialmedico.model.importacion;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

class PacienteImportacionModelTest {

  @Test
  void defineLosEstadosGeneralesYPorFilaRequeridos() {
    assertThat(EnumSet.allOf(PacienteImportacionEstado.class)).containsExactlyInAnyOrder(
        PacienteImportacionEstado.PREVISUALIZADA,
        PacienteImportacionEstado.CONFIRMANDO,
        PacienteImportacionEstado.CONFIRMADA,
        PacienteImportacionEstado.CANCELADA,
        PacienteImportacionEstado.EXPIRADA
    );
    assertThat(EnumSet.allOf(PacienteImportacionFilaEstado.class)).containsExactlyInAnyOrder(
        PacienteImportacionFilaEstado.VALIDO,
        PacienteImportacionFilaEstado.ERROR_DATOS,
        PacienteImportacionFilaEstado.DNI_EXISTENTE,
        PacienteImportacionFilaEstado.DNI_ARCHIVADO_EXISTENTE,
        PacienteImportacionFilaEstado.DNI_DUPLICADO_ARCHIVO,
        PacienteImportacionFilaEstado.REGISTRADO,
        PacienteImportacionFilaEstado.NO_REGISTRADO
    );
  }

  @Test
  void conservaFechaDeNacimientoComoLocalDateYLosDieciochoCampos() {
    LocalDate fechaNacimiento = LocalDate.of(1990, 5, 18);
    PacienteImportacionDatos paciente = PacienteImportacionDatos.builder()
        .apellidos("Pérez").nombres("Ana").fechaNacimiento(fechaNacimiento)
        .estadoCivil("SOLTERO").dni("01234567").sexo("F")
        .direccion("Dirección").distrito("Distrito").traidoPor("Familiar")
        .build();
    PacienteImportacionAntecedentes antecedentes = PacienteImportacionAntecedentes.builder()
        .alimentacion("Balanceada").habitos("No refiere").vivienda("Casa")
        .desarrolloPsicomotor("Normal").vacunas("Completas").educacion("S1")
        .enfermedadesPrevias("Ninguna").cirugiasPrevias("Ninguna")
        .alergiasMedicamentos("No refiere").build();

    PacienteImportacionFila fila = PacienteImportacionFila.builder()
        .numeroFila(4).estado(PacienteImportacionFilaEstado.VALIDO)
        .paciente(paciente).antecedentes(antecedentes).build();

    assertThat(fila.getPaciente().getFechaNacimiento()).isEqualTo(fechaNacimiento);
    assertThat(fila.getPaciente().getDni()).isEqualTo("01234567");
    assertThat(fila.getAntecedentes().getAlergiasMedicamentos()).isEqualTo("No refiere");
    assertThat(fila.getErrores()).isEmpty();
    assertThat(fila.getAdvertencias()).isEmpty();
  }

  @Test
  void incluyeElCatalogoEstableDeErrores() {
    assertThat(EnumSet.allOf(PacienteImportacionErrorCodigo.class)).contains(
        PacienteImportacionErrorCodigo.ARCHIVO_VACIO,
        PacienteImportacionErrorCodigo.ARCHIVO_DEMASIADO_GRANDE,
        PacienteImportacionErrorCodigo.FORMATO_NO_PERMITIDO,
        PacienteImportacionErrorCodigo.ARCHIVO_CORRUPTO,
        PacienteImportacionErrorCodigo.PLANTILLA_NO_RECONOCIDA,
        PacienteImportacionErrorCodigo.VERSION_INCOMPATIBLE,
        PacienteImportacionErrorCodigo.HOJA_PRINCIPAL_FALTANTE,
        PacienteImportacionErrorCodigo.COLUMNA_FALTANTE,
        PacienteImportacionErrorCodigo.ENCABEZADO_DUPLICADO,
        PacienteImportacionErrorCodigo.MAX_REGISTROS_EXCEDIDO,
        PacienteImportacionErrorCodigo.FORMULA_NO_PERMITIDA,
        PacienteImportacionErrorCodigo.CAMPO_OBLIGATORIO,
        PacienteImportacionErrorCodigo.DNI_INVALIDO,
        PacienteImportacionErrorCodigo.DNI_EXISTENTE,
        PacienteImportacionErrorCodigo.DNI_DUPLICADO_ARCHIVO,
        PacienteImportacionErrorCodigo.FECHA_INVALIDA,
        PacienteImportacionErrorCodigo.FECHA_FUTURA,
        PacienteImportacionErrorCodigo.SEXO_INVALIDO,
        PacienteImportacionErrorCodigo.ESTADO_CIVIL_INVALIDO,
        PacienteImportacionErrorCodigo.EDUCACION_INVALIDA,
        PacienteImportacionErrorCodigo.IMPORTACION_NO_ENCONTRADA,
        PacienteImportacionErrorCodigo.IMPORTACION_EXPIRADA,
        PacienteImportacionErrorCodigo.IMPORTACION_YA_CONFIRMADA,
        PacienteImportacionErrorCodigo.IMPORTACION_NO_AUTORIZADA
    );
  }
}
