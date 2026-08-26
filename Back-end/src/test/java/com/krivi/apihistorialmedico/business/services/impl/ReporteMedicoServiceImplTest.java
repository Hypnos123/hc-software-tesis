package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.model.api.*;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReporteMedicoServiceImplTest {
  private final ReporteMedicoServiceImpl service = new ReporteMedicoServiceImpl();

  @Test
  void pdfIndividualGeneraBytesConFirmaPdf() throws Exception {
    byte[] pdf = service.generarEvaluacionMedica(documento(List.of(consulta(1))));

    assertTrue(pdf.length > 1_000);
    assertArrayEquals("%PDF".getBytes(StandardCharsets.US_ASCII), java.util.Arrays.copyOf(pdf, 4));
    try (PdfReader reader = new PdfReader(pdf)) {
      assertEquals(1, reader.getNumberOfPages());
    }
  }

  @Test
  void consolidadoConVariasConsultasGeneraPdf() throws Exception {
    byte[] pdf = service.generarReporteConsultas(documento(List.of(consulta(1), consulta(2), consulta(3), consulta(4))));

    assertTrue(pdf.length > 1_000);
    try (PdfReader reader = new PdfReader(pdf)) {
      assertTrue(reader.getNumberOfPages() >= 1);
    }
  }

  @Test
  void camposOpcionalesNulosNoFallanYUsanTextosAmigables() throws Exception {
    ReporteMedicoConsulta consulta = consulta(1);
    consulta.setIdHistoriaClinica(null); consulta.setEspecialidad(" "); consulta.setMedicoResponsable(null);
    consulta.setDiagnostico(null); consulta.setExamenesRecetados(" "); consulta.setReceta(null);
    consulta.setTratamiento(""); consulta.setProximaCita(null); consulta.setEdadPaciente(null);
    ReporteMedicoDocumento documento = documento(List.of(consulta));
    documento.getPaciente().setNombreCompleto(null); documento.getPaciente().setDni(" ");
    documento.getPaciente().setFechaNacimiento(null); documento.setIdsHistoriasClinicasIncluidas(List.of());

    byte[] pdf = assertDoesNotThrow(() -> service.generarEvaluacionMedica(documento));
    String texto = extraerTexto(pdf);

    assertTrue(texto.contains("No registrado."));
    assertTrue(texto.contains("No se recetaron exámenes."));
    assertTrue(texto.contains("No se registró receta."));
    assertTrue(texto.contains("No se programó próxima cita."));
  }

  @Test
  void textosLargosSaltosDeLineaYTildesNoProvocanError() {
    ReporteMedicoConsulta consulta = consulta(1);
    String largo = ("Evaluación médica prolongada con hipertensión, afección respiratoria y evolución clínica.\n"
        + "El paciente señala mejoría, pero continuará el tratamiento según prescripción. ").repeat(20);
    consulta.setDiagnostico(largo); consulta.setReceta("Ibuprofeno y solución según indicación.\n" + largo);
    consulta.setTratamiento("Observación, hidratación y control del niño Peña Núñez.\n" + largo);

    byte[] pdf = assertDoesNotThrow(() -> service.generarEvaluacionMedica(documento(List.of(consulta))));

    assertTrue(pdf.length > 1_000);
  }

  @Test
  void helveticaCp1252ConservaCaracteresEspanoles() throws Exception {
    ReporteMedicoConsulta consulta = consulta(1);
    consulta.setMedicoResponsable("José Muñoz Peña");
    consulta.setDiagnostico("Diagnóstico: infección respiratoria");
    consulta.setReceta("Evaluación médica");
    consulta.setTratamiento("Niña con evolución favorable");

    String texto = extraerTexto(service.generarEvaluacionMedica(documento(List.of(consulta))));

    assertTrue(texto.contains("José Muñoz Peña"));
    assertTrue(texto.contains("Evaluación médica"));
    assertTrue(texto.contains("Diagnóstico: infección respiratoria"));
    assertTrue(texto.contains("Niña con evolución favorable"));
  }

  @Test
  void fechaConsultaSeImprimeSinHoraArtificial() throws Exception {
    ReporteMedicoConsulta consulta = consulta(1);
    consulta.setFechaEfectiva(LocalDateTime.of(2026, 5, 10, 0, 0));
    consulta.setOrigenFechaEfectiva(OrigenFechaConsultaReporte.FECHA_CONSULTA);

    String texto = extraerTexto(service.generarEvaluacionMedica(documento(List.of(consulta))));

    assertTrue(texto.contains("10/05/2026"));
    assertFalse(texto.contains("10/05/2026 00:00"));
  }

  @Test
  void consolidadoExtensoGeneraVariasPaginas() throws Exception {
    List<ReporteMedicoConsulta> consultas = new ArrayList<>();
    for (int i = 1; i <= 18; i++) {
      ReporteMedicoConsulta consulta = consulta(i);
      consulta.setTratamiento(("Tratamiento detallado con seguimiento clínico y recomendaciones. ").repeat(8));
      consultas.add(consulta);
    }

    byte[] pdf = service.generarReporteConsultas(documento(consultas));

    try (PdfReader reader = new PdfReader(pdf)) {
      assertTrue(reader.getNumberOfPages() > 1);
    }
  }

  @Test
  void multiplesHistoriasYMedicosDiferentesSeGeneranCorrectamente() throws Exception {
    ReporteMedicoConsulta primera = consulta(1); primera.setIdHistoriaClinica(11); primera.setMedicoResponsable("José Núñez");
    ReporteMedicoConsulta segunda = consulta(2); segunda.setIdHistoriaClinica(22); segunda.setMedicoResponsable("María Peña");
    ReporteMedicoDocumento documento = documento(List.of(primera, segunda));
    documento.setIdsHistoriasClinicasIncluidas(List.of(11, 22));

    String texto = extraerTexto(service.generarReporteConsultas(documento));

    assertTrue(texto.contains("11, 22"));
    assertTrue(texto.contains("José Núñez"));
    assertTrue(texto.contains("María Peña"));
  }

  @Test
  void ausenciaDeLogoSeManejaSinInterrumpirElPdf() {
    ReporteMedicoServiceImpl sinLogo = new ReporteMedicoServiceImpl(
        new ClassPathResource("reportes/logo-inexistente.png"));

    byte[] pdf = assertDoesNotThrow(() -> sinLogo.generarEvaluacionMedica(documento(List.of(consulta(1)))));

    assertTrue(pdf.length > 1_000);
  }

  @Test
  void exigeExactamenteUnaConsultaParaEvaluacionIndividual() {
    assertThrows(IllegalArgumentException.class,
        () -> service.generarEvaluacionMedica(documento(List.of(consulta(1), consulta(2)))));
  }

  @Test
  void generaEjemplosParaVerificacionManual(@TempDir Path temporal) throws Exception {
    ReporteMedicoConsulta completa = consulta(1);
    ReporteMedicoConsulta vacia = consulta(2); vacia.setExamenesRecetados(null); vacia.setReceta(null); vacia.setProximaCita(null);
    ReporteMedicoConsulta larga = consulta(3);
    larga.setDiagnostico(("Diagnóstico extenso con tildes, ñ y recomendaciones clínicas. ").repeat(30));
    List<ReporteMedicoConsulta> cuatro = List.of(consulta(1), consulta(2), consulta(3), consulta(4));
    List<ReporteMedicoConsulta> muchas = new ArrayList<>();
    for (int i = 1; i <= 14; i++) muchas.add(consulta(i));

    Files.write(temporal.resolve("evaluacion-individual.pdf"), service.generarEvaluacionMedica(documento(List.of(completa))));
    Files.write(temporal.resolve("evaluacion-campos-vacios.pdf"), service.generarEvaluacionMedica(documento(List.of(vacia))));
    Files.write(temporal.resolve("evaluacion-texto-largo.pdf"), service.generarEvaluacionMedica(documento(List.of(larga))));
    Files.write(temporal.resolve("reporte-cuatro-consultas.pdf"), service.generarReporteConsultas(documento(cuatro)));
    Files.write(temporal.resolve("reporte-varias-paginas.pdf"), service.generarReporteConsultas(documento(muchas)));

    try (var archivos = Files.list(temporal)) {
      assertEquals(5, archivos.filter(path -> path.toString().endsWith(".pdf")).count());
    }
  }

  private String extraerTexto(byte[] pdf) throws Exception {
    try (PdfReader reader = new PdfReader(pdf)) {
      PdfTextExtractor extractor = new PdfTextExtractor(reader);
      StringBuilder texto = new StringBuilder();
      for (int pagina = 1; pagina <= reader.getNumberOfPages(); pagina++) {
        texto.append(extractor.getTextFromPage(pagina));
      }
      return texto.toString();
    }
  }

  private ReporteMedicoDocumento documento(List<ReporteMedicoConsulta> consultas) {
    return ReporteMedicoDocumento.builder().alcance(ReporteConsultaAlcance.TODAS)
        .fechaDesde(LocalDate.of(2026, 1, 1)).fechaHasta(LocalDate.of(2026, 12, 31))
        .totalConsultasEncontradas(consultas.size()).consultasAtendidasIncluidas(consultas.size())
        .consultasNoAtendidasExcluidas(0)
        .paciente(ReporteMedicoPaciente.builder().idPaciente(7).nombreCompleto("Ana María Peña Núñez")
            .dni("12345678").fechaNacimiento(LocalDate.of(1990, 8, 20)).build())
        .idsHistoriasClinicasIncluidas(consultas.stream().map(ReporteMedicoConsulta::getIdHistoriaClinica)
            .distinct().sorted().toList())
        .consultas(consultas).build();
  }

  private ReporteMedicoConsulta consulta(int id) {
    return ReporteMedicoConsulta.builder().idConsulta(id).idHistoriaClinica(11)
        .fechaEfectiva(LocalDateTime.of(2026, 5, Math.min(id, 28), 10, 30))
        .origenFechaEfectiva(OrigenFechaConsultaReporte.FECHA_ATENCION).edadPaciente(35)
        .especialidad("Medicina general").medicoResponsable("José Núñez Peña")
        .diagnostico("Infección respiratoria aguda con evolución favorable.")
        .examenesRecetados("Hemograma completo.").receta("Paracetamol según indicación médica.")
        .tratamiento("Reposo, hidratación y control médico.").proximaCita(LocalDate.of(2026, 5, 25)).build();
  }
}
