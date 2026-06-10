package com.krivi.apihistorialmedico.util;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Date;


public class Util {


  public static String formatearFechaHora(Date fecha) {

    SimpleDateFormat formatoDeseado = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    return formatoDeseado.format(fecha);
  }

  public static String formatearFecha(Date fecha) {

    SimpleDateFormat formatoDeseado = new SimpleDateFormat("dd/MM/yyyy");

    return formatoDeseado.format(fecha);
  }

  public static String formatearFechaYMD(Date fecha) {

    SimpleDateFormat formatoDeseado = new SimpleDateFormat("yyyy-MM-dd");

    return formatoDeseado.format(fecha);
  }

  public static String formatearHora(Date fecha) {

    SimpleDateFormat formatoDeseado = new SimpleDateFormat("HH:mm:ss");

    return formatoDeseado.format(fecha);
  }


  public static String completarConCeros(int codigo) {
    return String.format("%06d", codigo);
  }

  public static String calcularEdad(String fechaNacimientoStr) {

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    LocalDate fechaNacimiento = LocalDate.parse(fechaNacimientoStr, formatter);
    LocalDate fechaHoy = LocalDate.now();
    Period periodo = Period.between(fechaNacimiento, fechaHoy);
    int años = periodo.getYears();
    int meses = periodo.getMonths();
    return años + " años y " + meses + " meses";
  }

  public static String formatearDouble(Double valor) {

    if (valor == null){
      valor = 0.0;
    }

    DecimalFormat formato = new DecimalFormat("#.00");

    return formato.format(valor);
  }

  public static String getFechafile() {


    SimpleDateFormat formatoDeseado = new SimpleDateFormat("ddMMyyyyHHmmss");

    return formatoDeseado.format(new Date());
  }


}
