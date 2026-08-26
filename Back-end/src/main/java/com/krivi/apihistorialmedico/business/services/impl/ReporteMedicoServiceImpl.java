package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.exception.ReporteMedicoGeneracionException;
import com.krivi.apihistorialmedico.business.services.ReporteMedicoService;
import com.krivi.apihistorialmedico.model.api.*;
import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ReporteMedicoServiceImpl implements ReporteMedicoService {
  static final String LOGO_CLASSPATH = "reportes/HSJ-LOGO.png";
  private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
  private static final Color AZUL = new Color(9, 42, 91);
  private static final Color AZUL_CLARO = new Color(231, 238, 248);
  private static final Color GRIS_BORDE = new Color(190, 198, 209);
  private static final float MARGEN_HORIZONTAL = 42F;
  private static final float MARGEN_SUPERIOR = 38F;
  private static final float MARGEN_INFERIOR = 48F;

  private final Resource logo;
  private final BaseFont baseRegular;
  private final BaseFont baseNegrita;

  public ReporteMedicoServiceImpl() {
    this(new ClassPathResource(LOGO_CLASSPATH));
  }

  ReporteMedicoServiceImpl(Resource logo) {
    this.logo = logo;
    this.baseRegular = crearFuenteEstandar(BaseFont.HELVETICA);
    this.baseNegrita = crearFuenteEstandar(BaseFont.HELVETICA_BOLD);
  }

  @Override
  public byte[] generarEvaluacionMedica(ReporteMedicoDocumento documento) {
    validarDocumento(documento);
    if (documento.getConsultas().size() != 1) {
      throw new IllegalArgumentException("La evaluación médica individual requiere exactamente una consulta.");
    }
    return generar(documento, true);
  }

  @Override
  public byte[] generarReporteConsultas(ReporteMedicoDocumento documento) {
    validarDocumento(documento);
    return generar(documento, false);
  }

  private byte[] generar(ReporteMedicoDocumento modelo, boolean individual) {
    try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
      Document pdf = new Document(PageSize.A4, MARGEN_HORIZONTAL, MARGEN_HORIZONTAL,
          MARGEN_SUPERIOR, MARGEN_INFERIOR);
      PdfWriter writer = PdfWriter.getInstance(pdf, salida);
      writer.setPageEvent(new ReporteMedicoPaginaEvento(baseRegular));
      agregarMetadatos(pdf, individual);
      pdf.open();
      agregarEncabezado(pdf, individual ? "EVALUACIÓN MÉDICA" : "REPORTE DE CONSULTAS DEL PACIENTE");
      if (individual) agregarIndividual(pdf, modelo);
      else agregarConsolidado(pdf, modelo);
      pdf.close();
      return salida.toByteArray();
    } catch (DocumentException | IOException e) {
      throw new ReporteMedicoGeneracionException("No se pudo generar el reporte médico en PDF.", e);
    }
  }

  private void agregarMetadatos(Document pdf, boolean individual) {
    pdf.addTitle(individual ? "Evaluación Médica" : "Reporte de Consultas del Paciente");
    pdf.addAuthor("Hospital San José de Chincha");
    pdf.addCreator("Sistema de Historias Clínicas");
  }

  private void agregarEncabezado(Document pdf, String titulo) throws DocumentException {
    PdfPTable tabla = new PdfPTable(new float[]{1.15F, 3.85F});
    tabla.setWidthPercentage(100);
    PdfPCell logoCelda = celdaSinBorde();
    Image imagen = cargarLogo();
    if (imagen != null) {
      imagen.scaleToFit(112F, 48F);
      imagen.setAlignment(Image.ALIGN_LEFT);
      logoCelda.addElement(imagen);
    }
    tabla.addCell(logoCelda);
    PdfPCell tituloCelda = celdaSinBorde();
    tituloCelda.setVerticalAlignment(Element.ALIGN_MIDDLE);
    Paragraph hospital = parrafo("Hospital San José de Chincha", 15, true, AZUL);
    hospital.setAlignment(Element.ALIGN_RIGHT);
    Paragraph nombreDocumento = parrafo(titulo, 12, true, AZUL);
    nombreDocumento.setAlignment(Element.ALIGN_RIGHT);
    nombreDocumento.setSpacingBefore(5);
    tituloCelda.addElement(hospital);
    tituloCelda.addElement(nombreDocumento);
    tabla.addCell(tituloCelda);
    pdf.add(tabla);
    LineSeparator linea = new LineSeparator(1F, 100F, AZUL, Element.ALIGN_CENTER, -2F);
    pdf.add(new Chunk(linea));
    agregarEspacio(pdf, 9);
  }

  private Image cargarLogo() {
    try {
      return logo != null && logo.exists() ? Image.getInstance(logo.getContentAsByteArray()) : null;
    } catch (Exception e) {
      return null;
    }
  }

  private void agregarIndividual(Document pdf, ReporteMedicoDocumento modelo) throws DocumentException {
    ReporteMedicoConsulta consulta = modelo.getConsultas().getFirst();
    Paragraph identificacion = parrafo("Consulta N.º " + valor(consulta.getIdConsulta()), 11, true, AZUL);
    identificacion.setSpacingAfter(9);
    pdf.add(identificacion);
    agregarTituloSeccion(pdf, "DATOS DEL PACIENTE");
    agregarTablaDatos(pdf, List.of(
        dato("Nombre completo", texto(modelo.getPaciente() == null ? null : modelo.getPaciente().getNombreCompleto())),
        dato("DNI", texto(modelo.getPaciente() == null ? null : modelo.getPaciente().getDni())),
        dato("Fecha de nacimiento", fecha(modelo.getPaciente() == null ? null : modelo.getPaciente().getFechaNacimiento())),
        dato("Edad", edad(consulta.getEdadPaciente())),
        dato("Historia clínica", valor(consulta.getIdHistoriaClinica())),
        dato("Fecha de atención", fechaEfectiva(consulta))));
    agregarTituloSeccion(pdf, "DATOS DE ATENCIÓN");
    agregarTablaDatos(pdf, List.of(
        dato("Especialidad", texto(consulta.getEspecialidad())),
        dato("Médico responsable", texto(consulta.getMedicoResponsable()))));
    agregarTituloSeccion(pdf, "EVALUACIÓN MÉDICA");
    agregarEvaluacion(pdf, consulta);
    agregarFirma(pdf, consulta);
  }

  private void agregarConsolidado(Document pdf, ReporteMedicoDocumento modelo) throws DocumentException {
    agregarTituloSeccion(pdf, "DATOS GENERALES");
    ReporteMedicoPaciente paciente = modelo.getPaciente();
    agregarTablaDatos(pdf, List.of(
        dato("Paciente", texto(paciente == null ? null : paciente.getNombreCompleto())),
        dato("DNI", texto(paciente == null ? null : paciente.getDni())),
        dato("Fecha de nacimiento", fecha(paciente == null ? null : paciente.getFechaNacimiento())),
        dato("Historias clínicas incluidas", historias(modelo.getIdsHistoriasClinicasIncluidas())),
        dato("Periodo consultado", periodo(modelo)),
        dato("Total de consultas encontradas", String.valueOf(modelo.getTotalConsultasEncontradas())),
        dato("Consultas incluidas", String.valueOf(modelo.getConsultasAtendidasIncluidas())),
        dato("Consultas excluidas", String.valueOf(modelo.getConsultasNoAtendidasExcluidas()))));

    int numero = 1;
    for (ReporteMedicoConsulta consulta : modelo.getConsultas()) {
      agregarTituloConsulta(pdf, numero++);
      agregarTablaDatos(pdf, List.of(
          dato("ID de consulta", valor(consulta.getIdConsulta())),
          dato("Historia clínica", valor(consulta.getIdHistoriaClinica())),
          dato("Fecha de atención", fechaEfectiva(consulta)),
          dato("Edad del paciente", edad(consulta.getEdadPaciente())),
          dato("Especialidad", texto(consulta.getEspecialidad())),
          dato("Médico responsable", texto(consulta.getMedicoResponsable()))));
      agregarEvaluacion(pdf, consulta);
      agregarFirma(pdf, consulta);
    }
  }

  private void agregarTituloSeccion(Document pdf, String titulo) throws DocumentException {
    PdfPTable tabla = new PdfPTable(1);
    tabla.setWidthPercentage(100);
    tabla.setSpacingBefore(6);
    tabla.setSpacingAfter(5);
    PdfPCell celda = new PdfPCell(new Phrase(titulo, fuente(10, true, AZUL)));
    celda.setBackgroundColor(AZUL_CLARO);
    celda.setBorderColor(GRIS_BORDE);
    celda.setPadding(6);
    tabla.addCell(celda);
    pdf.add(tabla);
  }

  private void agregarTituloConsulta(Document pdf, int numero) throws DocumentException {
    PdfPTable tabla = new PdfPTable(1);
    tabla.setWidthPercentage(100);
    tabla.setSpacingBefore(12);
    tabla.setSpacingAfter(6);
    tabla.setKeepTogether(true);
    PdfPCell celda = new PdfPCell(new Phrase("CONSULTA " + numero, fuente(11, true, Color.WHITE)));
    celda.setBackgroundColor(AZUL);
    celda.setBorder(Rectangle.NO_BORDER);
    celda.setPadding(7);
    tabla.addCell(celda);
    pdf.add(tabla);
  }

  private void agregarTablaDatos(Document pdf, List<Dato> datos) throws DocumentException {
    PdfPTable tabla = new PdfPTable(new float[]{1.6F, 3.4F});
    tabla.setWidthPercentage(100);
    tabla.setSpacingAfter(7);
    tabla.setKeepTogether(true);
    for (Dato dato : datos) {
      PdfPCell etiqueta = new PdfPCell(new Phrase(dato.etiqueta(), fuente(9, true, AZUL)));
      PdfPCell valor = new PdfPCell(new Phrase(dato.valor(), fuente(9, false, Color.BLACK)));
      for (PdfPCell celda : List.of(etiqueta, valor)) {
        celda.setBorderColor(GRIS_BORDE);
        celda.setPadding(5);
        celda.setVerticalAlignment(Element.ALIGN_TOP);
      }
      tabla.addCell(etiqueta);
      tabla.addCell(valor);
    }
    pdf.add(tabla);
  }

  private void agregarEvaluacion(Document pdf, ReporteMedicoConsulta consulta) throws DocumentException {
    agregarCampoClinico(pdf, "Diagnóstico", texto(consulta.getDiagnostico()));
    agregarCampoClinico(pdf, "Exámenes recetados", texto(consulta.getExamenesRecetados(), "No se recetaron exámenes."));
    agregarCampoClinico(pdf, "Receta", texto(consulta.getReceta(), "No se registró receta."));
    agregarCampoClinico(pdf, "Tratamiento", texto(consulta.getTratamiento()));
    agregarCampoClinico(pdf, "Próxima cita", fecha(consulta.getProximaCita(), "No se programó próxima cita."));
  }

  private void agregarCampoClinico(Document pdf, String etiqueta, String contenido) throws DocumentException {
    PdfPTable bloque = new PdfPTable(1);
    bloque.setWidthPercentage(100);
    bloque.setKeepTogether(true);
    PdfPCell celda = celdaSinBorde();
    Paragraph titulo = parrafo(etiqueta + ":", 9, true, AZUL);
    titulo.setSpacingBefore(3);
    titulo.setSpacingAfter(2);
    celda.addElement(titulo);
    Paragraph valor = parrafo(contenido, 9, false, Color.BLACK);
    valor.setLeading(13F);
    valor.setSpacingAfter(5);
    celda.addElement(valor);
    bloque.addCell(celda);
    pdf.add(bloque);
  }

  private void agregarFirma(Document pdf, ReporteMedicoConsulta consulta) throws DocumentException {
    PdfPTable firma = new PdfPTable(new float[]{1F, 0.25F, 1F});
    firma.setWidthPercentage(84);
    firma.setHorizontalAlignment(Element.ALIGN_CENTER);
    firma.setSpacingBefore(22);
    firma.setSpacingAfter(8);
    firma.setKeepTogether(true);
    firma.addCell(celdaFirma("Firma del médico", "Dr./Dra. " + texto(consulta.getMedicoResponsable()),
        texto(consulta.getEspecialidad())));
    firma.addCell(celdaSinBorde());
    firma.addCell(celdaFirma("Sello", " ", " "));
    pdf.add(firma);
  }

  private PdfPCell celdaFirma(String titulo, String lineaDos, String lineaTres) {
    PdfPCell celda = celdaSinBorde();
    celda.setBorderWidthTop(0.8F);
    celda.setBorderColorTop(AZUL);
    celda.setPaddingTop(5);
    Paragraph encabezado = parrafo(titulo, 8, true, AZUL);
    encabezado.setAlignment(Element.ALIGN_CENTER);
    celda.addElement(encabezado);
    Paragraph detalle = parrafo(lineaDos, 8, false, Color.BLACK);
    detalle.setAlignment(Element.ALIGN_CENTER);
    celda.addElement(detalle);
    Paragraph especialidad = parrafo(lineaTres, 8, false, Color.BLACK);
    especialidad.setAlignment(Element.ALIGN_CENTER);
    celda.addElement(especialidad);
    return celda;
  }

  private String periodo(ReporteMedicoDocumento modelo) {
    if (modelo.getAlcance() == null) return "No registrado.";
    return switch (modelo.getAlcance()) {
      case ULTIMA -> "Última consulta atendida";
      case TODAS -> "Todas las consultas registradas";
      case FECHA -> modelo.getFecha() == null ? "No registrado." : FECHA.format(modelo.getFecha());
      case RANGO_FECHAS -> modelo.getFechaDesde() == null || modelo.getFechaHasta() == null
          ? "No registrado."
          : "Del " + FECHA.format(modelo.getFechaDesde()) + " al " + FECHA.format(modelo.getFechaHasta());
    };
  }

  private String historias(List<Integer> ids) {
    if (ids == null || ids.isEmpty()) return "No registrado.";
    return ids.stream().filter(Objects::nonNull).map(String::valueOf).collect(Collectors.joining(", "));
  }

  private String fechaEfectiva(ReporteMedicoConsulta consulta) {
    LocalDateTime fecha = consulta.getFechaEfectiva();
    if (fecha == null) return "No registrado.";
    return consulta.getOrigenFechaEfectiva() == OrigenFechaConsultaReporte.FECHA_CONSULTA
        ? FECHA.format(fecha.toLocalDate()) : FECHA_HORA.format(fecha);
  }

  private String fecha(LocalDate fecha) {
    return fecha(fecha, "No registrado.");
  }

  private String fecha(LocalDate fecha, String ausente) {
    return fecha == null ? ausente : FECHA.format(fecha);
  }

  private String edad(Integer edad) {
    return edad == null ? "No registrado." : edad + " años";
  }

  private String valor(Object valor) {
    return valor == null ? "No registrado." : texto(String.valueOf(valor));
  }

  private String texto(String valor) {
    return texto(valor, "No registrado.");
  }

  private String texto(String valor, String ausente) {
    return valor == null || valor.trim().isEmpty() ? ausente : valor.trim();
  }

  private Font fuente(float tamanio, boolean negrita, Color color) {
    return new Font(negrita ? baseNegrita : baseRegular, tamanio, Font.NORMAL, color);
  }

  private Paragraph parrafo(String contenido, float tamanio, boolean negrita, Color color) {
    return new Paragraph(contenido, fuente(tamanio, negrita, color));
  }

  private PdfPCell celdaSinBorde() {
    PdfPCell celda = new PdfPCell();
    celda.setBorder(Rectangle.NO_BORDER);
    celda.setPadding(0);
    return celda;
  }

  private void agregarEspacio(Document pdf, float puntos) throws DocumentException {
    Paragraph espacio = new Paragraph(" ", fuente(1, false, Color.WHITE));
    espacio.setLeading(puntos);
    pdf.add(espacio);
  }

  private Dato dato(String etiqueta, String valor) {
    return new Dato(etiqueta, valor);
  }

  private BaseFont crearFuenteEstandar(String nombre) {
    try {
      return BaseFont.createFont(nombre, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
    } catch (Exception e) {
      throw new IllegalStateException("No se pudo cargar la fuente estándar " + nombre
          + " para los reportes médicos.", e);
    }
  }

  private void validarDocumento(ReporteMedicoDocumento documento) {
    if (documento == null) throw new IllegalArgumentException("El modelo del reporte médico es obligatorio.");
    if (documento.getConsultas() == null) throw new IllegalArgumentException("La colección de consultas es obligatoria.");
  }

  private record Dato(String etiqueta, String valor) {}
}
