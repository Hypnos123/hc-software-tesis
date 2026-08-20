package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.model.api.ResumenConsultasPacienteResponse.EstadisticaVital;
import com.krivi.apihistorialmedico.model.projection.ConsultaFuncionesVitalesProjection;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FuncionesVitalesResumenCalculatorTest {
  private final FuncionesVitalesResumenCalculator calculator = new FuncionesVitalesResumenCalculator();

  @Test void calculaEstadisticasPresionNumericosDescartesYUltimosValores() {
    var resultado = calculator.calcular(List.of(
        registro(1, "2026-01-01", "120/80", "60", "12", "1.70", "36", 60D),
        registro(2, "2026-02-01", "120", "", "texto", "1.80", "37", 70D),
        registro(3, "2026-03-01", "148/94", "80", "16", "no-numero", "38", 80D)));

    EstadisticaVital sistolica = resultado.funcionesVitales().getPresionSistolica();
    assertEquals(148D, sistolica.getUltimoValor());
    assertEquals(134D, sistolica.getPromedio());
    assertEquals(120D, sistolica.getMinimo());
    assertEquals(148D, sistolica.getMaximo());
    assertEquals(2L, sistolica.getCantidadRegistrosValidos());
    assertEquals(1L, sistolica.getCantidadRegistrosDescartados());
    assertEquals("ASCENDENTE", sistolica.getTendencia());
    assertEquals("mmHg", sistolica.getUnidad());

    EstadisticaVital diastolica = resultado.funcionesVitales().getPresionDiastolica();
    assertEquals(94D, diastolica.getUltimoValor());
    assertEquals(87D, diastolica.getPromedio());
    assertEquals(80D, diastolica.getMinimo());
    assertEquals(94D, diastolica.getMaximo());

    EstadisticaVital cardiaca = resultado.funcionesVitales().getFrecuenciaCardiaca();
    assertEquals(70D, cardiaca.getPromedio());
    assertEquals(60D, cardiaca.getMinimo());
    assertEquals(80D, cardiaca.getMaximo());
    assertEquals(80D, cardiaca.getUltimoValor());
    assertEquals(2L, cardiaca.getCantidadRegistrosValidos());
    assertEquals(0L, cardiaca.getCantidadRegistrosDescartados());
    assertEquals("ASCENDENTE", cardiaca.getTendencia());

    assertEquals(1L, resultado.funcionesVitales().getFrecuenciaRespiratoria().getCantidadRegistrosDescartados());
    assertEquals(1L, resultado.funcionesVitales().getTalla().getCantidadRegistrosDescartados());
    assertEquals(37D, resultado.funcionesVitales().getTemperatura().getPromedio());
    assertEquals(70D, resultado.funcionesVitales().getPeso().getPromedio());
    assertEquals(3L, resultado.valoresDescartados());
  }

  @Test void ordenaPorFechaEIdYCalculaTendenciasDescendenteYEstable() {
    var descendente = calculator.calcular(List.of(
        registro(20, "2026-03-01", "120/80", "60", null, null, null, null),
        registro(10, "2026-01-01", "120/80", "90", null, null, null, null)));
    assertEquals(60D, descendente.funcionesVitales().getFrecuenciaCardiaca().getUltimoValor());
    assertEquals("DESCENDENTE", descendente.funcionesVitales().getFrecuenciaCardiaca().getTendencia());

    var mismaFecha = calculator.calcular(List.of(
        registro(2, "2026-01-01", "120/80", "80", null, null, null, null),
        registro(1, "2026-01-01", "120/80", "70", null, null, null, null)));
    assertEquals(80D, mismaFecha.funcionesVitales().getFrecuenciaCardiaca().getUltimoValor());

    var estable = calculator.calcular(List.of(
        registro(1, "2026-01-01", "120/80", "70", null, null, null, null),
        registro(2, "2026-02-01", "120/80", "70", null, null, null, null)));
    assertEquals("ESTABLE", estable.funcionesVitales().getFrecuenciaCardiaca().getTendencia());
  }

  @Test void pacienteSinValoresValidosDistingueAusenciasDeDatosInvalidos() {
    var resultado = calculator.calcular(List.of(
        registro(1, "2026-01-01", "", null, "texto", "", null, null),
        registro(2, "2026-02-01", "120", "no-numero", null, null, "", null)));

    EstadisticaVital presion = resultado.funcionesVitales().getPresionSistolica();
    assertNull(presion.getUltimoValor());
    assertNull(presion.getPromedio());
    assertNull(presion.getMinimo());
    assertNull(presion.getMaximo());
    assertEquals(0L, presion.getCantidadRegistrosValidos());
    assertEquals(1L, presion.getCantidadRegistrosDescartados());
    assertEquals("SIN_DATOS_SUFICIENTES", presion.getTendencia());
    assertEquals(3L, resultado.valoresDescartados());
  }

  private ConsultaFuncionesVitalesProjection registro(Integer id, String fecha, String presion,
      String cardiaca, String respiratoria, String talla, String temperatura, Double peso) {
    return new RegistroVital(id, Date.valueOf(fecha), presion, cardiaca, respiratoria, talla, temperatura, peso);
  }

  private record RegistroVital(Integer idConsulta, java.util.Date fechaConsulta, String presionArterial,
      String frecuenciaCardiaca, String frecuenciaRespiratoria, String talla, String temperatura,
      Double peso) implements ConsultaFuncionesVitalesProjection {
    @Override public Integer getIdConsulta() { return idConsulta; }
    @Override public java.util.Date getFechaConsulta() { return fechaConsulta; }
    @Override public String getPresionArterial() { return presionArterial; }
    @Override public String getFrecuenciaCardiaca() { return frecuenciaCardiaca; }
    @Override public String getFrecuenciaRespiratoria() { return frecuenciaRespiratoria; }
    @Override public String getTalla() { return talla; }
    @Override public String getTemperatura() { return temperatura; }
    @Override public Double getPeso() { return peso; }
  }
}
