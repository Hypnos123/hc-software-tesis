package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.model.api.ResumenConsultasPacienteResponse;
import com.krivi.apihistorialmedico.model.projection.ConsultaFuncionesVitalesProjection;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Calcula estadísticas matemáticas sin interpretación clínica. Los rangos aceptados replican las
 * validaciones técnicas del formulario de consulta. Los valores nulos o vacíos son ausencias y no
 * descartes; únicamente un dato presente que no puede parsearse o queda fuera de esos rangos se
 * contabiliza como descartado.
 */
@Component
public class FuncionesVitalesResumenCalculator {
  private static final Pattern PRESION_PATTERN = Pattern.compile("^\\s*(\\d+(?:\\.\\d+)?)\\s*/\\s*(\\d+(?:\\.\\d+)?)\\s*$");

  public ResultadoFuncionesVitales calcular(List<ConsultaFuncionesVitalesProjection> registros) {
    List<ConsultaFuncionesVitalesProjection> ordenados = new ArrayList<>(registros == null ? List.of() : registros);
    ordenados.sort(Comparator.comparing(ConsultaFuncionesVitalesProjection::getFechaConsulta,
            Comparator.nullsFirst(Date::compareTo))
        .thenComparing(ConsultaFuncionesVitalesProjection::getIdConsulta,
            Comparator.nullsFirst(Integer::compareTo)));

    PresionResultado presion = calcularPresion(ordenados);
    ResultadoIndicador frecuenciaCardiaca = calcularTexto(ordenados,
        ConsultaFuncionesVitalesProjection::getFrecuenciaCardiaca, 20D, 250D);
    ResultadoIndicador frecuenciaRespiratoria = calcularTexto(ordenados,
        ConsultaFuncionesVitalesProjection::getFrecuenciaRespiratoria, 5D, 80D);
    ResultadoIndicador talla = calcularTexto(ordenados, ConsultaFuncionesVitalesProjection::getTalla,
        0.3D, 2.5D);
    ResultadoIndicador temperatura = calcularTexto(ordenados,
        ConsultaFuncionesVitalesProjection::getTemperatura, 30D, 45D);
    ResultadoIndicador peso = calcularPeso(ordenados, 1D, 400D);

    long descartados = presion.descartados()
        + frecuenciaCardiaca.descartados() + frecuenciaRespiratoria.descartados()
        + talla.descartados() + temperatura.descartados() + peso.descartados();
    return new ResultadoFuncionesVitales(ResumenConsultasPacienteResponse.FuncionesVitalesResumen.builder()
        .presionSistolica(estadistica(presion.sistolica(), presion.descartados(), "mmHg"))
        .presionDiastolica(estadistica(presion.diastolica(), presion.descartados(), "mmHg"))
        .frecuenciaCardiaca(estadistica(frecuenciaCardiaca.valores(), frecuenciaCardiaca.descartados(), "lpm"))
        .frecuenciaRespiratoria(estadistica(frecuenciaRespiratoria.valores(), frecuenciaRespiratoria.descartados(), "rpm"))
        .talla(estadistica(talla.valores(), talla.descartados(), "m"))
        .temperatura(estadistica(temperatura.valores(), temperatura.descartados(), "°C"))
        .peso(estadistica(peso.valores(), peso.descartados(), "kg"))
        .build(), descartados);
  }

  private PresionResultado calcularPresion(List<ConsultaFuncionesVitalesProjection> registros) {
    List<Double> sistolica = new ArrayList<>();
    List<Double> diastolica = new ArrayList<>();
    long descartados = 0;
    for (ConsultaFuncionesVitalesProjection registro : registros) {
      String original = registro.getPresionArterial();
      if (ausente(original)) continue;
      Matcher matcher = PRESION_PATTERN.matcher(original);
      if (!matcher.matches()) {
        descartados++;
        continue;
      }
      Double sistolicaValor = parsearFinito(matcher.group(1));
      Double diastolicaValor = parsearFinito(matcher.group(2));
      if (sistolicaValor == null || diastolicaValor == null) {
        descartados++;
        continue;
      }
      sistolica.add(sistolicaValor);
      diastolica.add(diastolicaValor);
    }
    return new PresionResultado(sistolica, diastolica, descartados);
  }

  private ResultadoIndicador calcularTexto(List<ConsultaFuncionesVitalesProjection> registros,
      Function<ConsultaFuncionesVitalesProjection, String> extractor, double minimo, double maximo) {
    List<Double> valores = new ArrayList<>();
    long descartados = 0;
    for (ConsultaFuncionesVitalesProjection registro : registros) {
      String original = extractor.apply(registro);
      if (ausente(original)) continue;
      Double valor = parsearFinito(original.trim());
      if (valor == null || valor < minimo || valor > maximo) descartados++;
      else valores.add(valor);
    }
    return new ResultadoIndicador(valores, descartados);
  }

  private ResultadoIndicador calcularPeso(List<ConsultaFuncionesVitalesProjection> registros,
      double minimo, double maximo) {
    List<Double> valores = new ArrayList<>();
    long descartados = 0;
    for (ConsultaFuncionesVitalesProjection registro : registros) {
      Double valor = registro.getPeso();
      if (valor == null) continue;
      if (!Double.isFinite(valor) || valor < minimo || valor > maximo) descartados++;
      else valores.add(valor);
    }
    return new ResultadoIndicador(valores, descartados);
  }

  private ResumenConsultasPacienteResponse.EstadisticaVital estadistica(List<Double> valores,
      long descartados, String unidad) {
    if (valores.isEmpty()) {
      return ResumenConsultasPacienteResponse.EstadisticaVital.builder()
          .cantidadRegistrosValidos(0L).cantidadRegistrosDescartados(descartados)
          .unidad(unidad).tendencia("SIN_DATOS_SUFICIENTES").build();
    }
    double suma = valores.stream().mapToDouble(Double::doubleValue).sum();
    return ResumenConsultasPacienteResponse.EstadisticaVital.builder()
        .ultimoValor(valores.getLast()).promedio(redondear(suma / valores.size()))
        .minimo(valores.stream().mapToDouble(Double::doubleValue).min().orElseThrow())
        .maximo(valores.stream().mapToDouble(Double::doubleValue).max().orElseThrow())
        .cantidadRegistrosValidos((long) valores.size()).cantidadRegistrosDescartados(descartados)
        .unidad(unidad).tendencia(tendencia(valores)).build();
  }

  /** Compara el primer y último valor cronológicos; con menos de dos valores no hay tendencia. */
  private String tendencia(List<Double> valores) {
    if (valores.size() < 2) return "SIN_DATOS_SUFICIENTES";
    int comparacion = Double.compare(valores.getLast(), valores.getFirst());
    if (comparacion > 0) return "ASCENDENTE";
    if (comparacion < 0) return "DESCENDENTE";
    return "ESTABLE";
  }

  private Double parsearFinito(String valor) {
    try {
      double numero = Double.parseDouble(valor);
      return Double.isFinite(numero) ? numero : null;
    } catch (NumberFormatException exception) {
      return null;
    }
  }

  private boolean ausente(String valor) {
    return valor == null || valor.trim().isEmpty();
  }

  private double redondear(double valor) {
    return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
  }

  public record ResultadoFuncionesVitales(
      ResumenConsultasPacienteResponse.FuncionesVitalesResumen funcionesVitales,
      long valoresDescartados) {}

  private record ResultadoIndicador(List<Double> valores, long descartados) {}
  private record PresionResultado(List<Double> sistolica, List<Double> diastolica, long descartados) {}
}
