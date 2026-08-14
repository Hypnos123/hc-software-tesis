package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.model.api.AnalisisHistoriasClinicasDuplicadasResponse;
import com.krivi.apihistorialmedico.model.entity.Consulta;
import com.krivi.apihistorialmedico.model.entity.Empleado;
import com.krivi.apihistorialmedico.model.entity.HistoriaClinica;
import com.krivi.apihistorialmedico.model.entity.Paciente;
import com.krivi.apihistorialmedico.model.entity.TipoEnfermedad;
import com.krivi.apihistorialmedico.repository.ConsultaRepository;
import com.krivi.apihistorialmedico.repository.HistoriaClinicaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalisisHistoriasClinicasDuplicadasServiceImplTest {
  @Mock HistoriaClinicaRepository historiaRepository;
  @Mock ConsultaRepository consultaRepository;
  AnalisisHistoriasClinicasDuplicadasServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new AnalisisHistoriasClinicasDuplicadasServiceImpl(historiaRepository, consultaRepository);
  }

  @Test
  void operacionEstaDeclaradaComoSoloLectura() throws Exception {
    Transactional transactional = AnalisisHistoriasClinicasDuplicadasServiceImpl.class
        .getMethod("analizar", List.class).getAnnotation(Transactional.class);

    assertNotNull(transactional);
    assertTrue(transactional.readOnly());
  }

  @Test
  void analizaDosHistoriasSinConsultasYUsaAntiguedad() {
    Paciente paciente = paciente(12, "74281635");
    HistoriaClinica antigua = historia(7, paciente, LocalDateTime.of(2026, 1, 1, 9, 0));
    HistoriaClinica nueva = historia(8, paciente, LocalDateTime.of(2026, 2, 1, 9, 0));
    preparar(List.of(antigua, nueva), List.of(), List.of());

    AnalisisHistoriasClinicasDuplicadasResponse respuesta = service.analizar(List.of(7, 8));

    assertEquals("MISMO_PACIENTE", respuesta.getTipoDuplicidad());
    assertEquals(7, respuesta.getIdHistoriaClinicaRecomendada());
    assertTrue(respuesta.isFuturaFusionPermitida());
    assertTrue(respuesta.getMensaje().contains("No existen consultas para transferir"));
    assertEquals(0, respuesta.getHistoriasComparadas().getFirst().getCantidadConsultasExclusivas());
  }

  @Test
  void recomiendaPrincipalConConsultasFrenteASecundariaVacia() {
    Paciente paciente = paciente(1, "12345678");
    HistoriaClinica principal = historia(10, paciente, LocalDateTime.of(2026, 2, 1, 0, 0));
    HistoriaClinica vacia = historia(11, paciente, LocalDateTime.of(2026, 1, 1, 0, 0));
    preparar(List.of(principal, vacia), List.of(consulta(1, principal, paciente, 1)), List.of());

    var respuesta = service.analizar(List.of(10, 11));

    assertEquals(10, respuesta.getIdHistoriaClinicaRecomendada());
    assertEquals(1, respuesta.getHistoriasComparadas().getFirst().getCantidadConsultasExclusivas());
  }

  @Test
  void recomiendaSecundariaConConsultasFrenteAPrincipalVacia() {
    Paciente paciente = paciente(1, "12345678");
    HistoriaClinica vacia = historia(10, paciente, LocalDateTime.of(2026, 1, 1, 0, 0));
    HistoriaClinica completa = historia(11, paciente, LocalDateTime.of(2026, 2, 1, 0, 0));
    preparar(List.of(vacia, completa), List.of(), List.of(consulta(1, completa, paciente, 1)));

    assertEquals(11, service.analizar(List.of(10, 11)).getIdHistoriaClinicaRecomendada());
  }

  @Test
  void comparaAmbasConConsultasYPriorizaCantidad() {
    Paciente paciente = paciente(1, "12345678");
    HistoriaClinica a = historia(10, paciente, LocalDateTime.of(2026, 2, 1, 0, 0));
    HistoriaClinica b = historia(11, paciente, LocalDateTime.of(2026, 1, 1, 0, 0));
    preparar(List.of(a, b), List.of(consulta(1, a, paciente, 1), consulta(2, a, paciente, 2)),
        List.of(consulta(3, b, paciente, 3)));

    var respuesta = service.analizar(List.of(10, 11));

    assertEquals(10, respuesta.getIdHistoriaClinicaRecomendada());
    assertEquals(3, respuesta.getHistoriasComparadas().stream().mapToLong(h -> h.getCantidadConsultasExclusivas()).sum());
  }

  @Test
  void priorizaRiquezaCuandoCantidadEmpata() {
    Paciente paciente = paciente(1, "12345678");
    HistoriaClinica a = historia(10, paciente, LocalDateTime.of(2026, 1, 1, 0, 0));
    HistoriaClinica b = historia(11, paciente, LocalDateTime.of(2026, 2, 1, 0, 0));
    Consulta pobre = consulta(1, a, paciente, 1);
    Consulta rica = consulta(2, b, paciente, 1);
    rica.setDiagnostico("Neumonía"); rica.setTratamiento("Antibiótico"); rica.setReceta("Receta A");
    rica.setExamenesRecetados("Radiografía"); rica.setRelatoPaciente("Dolor persistente");
    preparar(List.of(a, b), List.of(pobre), List.of(rica));

    var respuesta = service.analizar(List.of(10, 11));

    assertEquals(11, respuesta.getIdHistoriaClinicaRecomendada());
    assertTrue(respuesta.getHistoriasComparadas().getFirst().getPuntajeRiquezaClinica() > 10);
  }

  @Test
  void priorizaActividadMasRecienteTrasEmpateClinico() {
    Paciente paciente = paciente(1, "12345678");
    HistoriaClinica a = historia(10, paciente, LocalDateTime.of(2026, 1, 1, 0, 0));
    HistoriaClinica b = historia(11, paciente, LocalDateTime.of(2026, 2, 1, 0, 0));
    Consulta antigua = consulta(1, a, paciente, 1); antigua.setFechaAtencion(LocalDateTime.of(2026, 3, 1, 10, 0));
    Consulta reciente = consulta(2, b, paciente, 1); reciente.setFechaAtencion(LocalDateTime.of(2026, 4, 1, 10, 0));
    preparar(List.of(a, b), List.of(antigua), List.of(reciente));

    assertEquals(11, service.analizar(List.of(10, 11)).getIdHistoriaClinicaRecomendada());
  }

  @Test
  void usaIdMenorEnEmpateCompleto() {
    Paciente paciente = paciente(1, "12345678");
    LocalDateTime fecha = LocalDateTime.of(2026, 1, 1, 0, 0);
    HistoriaClinica a = historia(10, paciente, fecha); HistoriaClinica b = historia(11, paciente, fecha);
    preparar(List.of(a, b), List.of(), List.of());

    assertEquals(10, service.analizar(List.of(10, 11)).getIdHistoriaClinicaRecomendada());
  }

  @Test
  void soportaMasDeDosHistorias() {
    Paciente paciente = paciente(1, "12345678");
    HistoriaClinica a = historia(10, paciente, LocalDateTime.of(2026, 1, 1, 0, 0));
    HistoriaClinica b = historia(11, paciente, LocalDateTime.of(2026, 2, 1, 0, 0));
    HistoriaClinica c = historia(12, paciente, LocalDateTime.of(2026, 3, 1, 0, 0));
    when(historiaRepository.findForAnalisisByIds(List.of(10, 11, 12))).thenReturn(List.of(a, b, c));
    when(consultaRepository.findByHistoriaClinicaIdHistoriaClinica(10)).thenReturn(List.of());
    when(consultaRepository.findByHistoriaClinicaIdHistoriaClinica(11)).thenReturn(List.of());
    when(consultaRepository.findByHistoriaClinicaIdHistoriaClinica(12)).thenReturn(List.of());

    var respuesta = service.analizar(List.of(10, 11, 12));

    assertEquals(3, respuesta.getHistoriasComparadas().size());
    assertEquals(10, respuesta.getIdHistoriaClinicaRecomendada());
  }

  @Test
  void bloqueaMismoDniConPacientesDistintos() {
    Paciente a = paciente(1, "12345678"); Paciente b = paciente(2, "12345678");
    HistoriaClinica ha = historia(10, a, LocalDateTime.now()); HistoriaClinica hb = historia(11, b, LocalDateTime.now());
    preparar(List.of(ha, hb), List.of(), List.of());

    var respuesta = service.analizar(List.of(10, 11));

    assertEquals("MISMO_DNI_DIFERENTE_PACIENTE", respuesta.getTipoDuplicidad());
    assertFalse(respuesta.isFuturaFusionPermitida());
    assertTrue(respuesta.getMotivoBloqueo().contains("pacientes duplicados"));
  }

  @Test
  void bloqueaConsultaInconsistenteSinOcultarla() {
    Paciente propietario = paciente(1, "12345678"); Paciente incorrecto = paciente(2, "12345678");
    HistoriaClinica a = historia(10, propietario, LocalDateTime.now()); HistoriaClinica b = historia(11, propietario, LocalDateTime.now());
    preparar(List.of(a, b), List.of(consulta(1, a, incorrecto, 1)), List.of());

    var respuesta = service.analizar(List.of(10, 11));

    assertFalse(respuesta.isFuturaFusionPermitida());
    assertEquals(1, respuesta.getAdvertenciasIntegridad().size());
    assertTrue(respuesta.getAdvertenciasIntegridad().getFirst().contains("consulta ID 1"));
  }

  @Test
  void informaPosiblesCoincidenciasSinExcluirConsultas() {
    Paciente paciente = paciente(1, "12345678");
    HistoriaClinica a = historia(10, paciente, LocalDateTime.now()); HistoriaClinica b = historia(11, paciente, LocalDateTime.now());
    Consulta ca = consulta(1, a, paciente, 1); Consulta cb = consulta(2, b, paciente, 1);
    ca.setDiagnostico("Gripe aguda"); cb.setDiagnostico("Gripe aguda");
    ca.setTratamiento("Reposo"); cb.setTratamiento("Reposo");
    preparar(List.of(a, b), List.of(ca), List.of(cb));

    var respuesta = service.analizar(List.of(10, 11));

    assertEquals(1, respuesta.getPosiblesCoincidencias().size());
    assertEquals("POSIBLE_COINCIDENCIA", respuesta.getPosiblesCoincidencias().getFirst().getClasificacion());
    assertEquals(2, respuesta.getHistoriasComparadas().stream().mapToLong(h -> h.getCantidadConsultasExclusivas()).sum());
  }

  private void preparar(List<HistoriaClinica> historias, List<Consulta> consultasA, List<Consulta> consultasB) {
    List<Integer> ids = historias.stream().map(HistoriaClinica::getIdHistoriaClinica).toList();
    when(historiaRepository.findForAnalisisByIds(ids)).thenReturn(historias);
    when(consultaRepository.findByHistoriaClinicaIdHistoriaClinica(historias.get(0).getIdHistoriaClinica())).thenReturn(consultasA);
    when(consultaRepository.findByHistoriaClinicaIdHistoriaClinica(historias.get(1).getIdHistoriaClinica())).thenReturn(consultasB);
  }

  private Paciente paciente(int id, String dni) {
    Paciente p = new Paciente(); p.setIdPaciente(id); p.setNumDocumento(dni); p.setNombres("Ana"); p.setApellidos("Lima"); return p;
  }
  private HistoriaClinica historia(int id, Paciente paciente, LocalDateTime fecha) {
    HistoriaClinica h = new HistoriaClinica(); h.setIdHistoriaClinica(id); h.setPaciente(paciente); h.setFechaCreacion(fecha); h.setUltimaActualizacion(fecha); return h;
  }
  private Consulta consulta(int id, HistoriaClinica historia, Paciente paciente, int dia) {
    Consulta c = new Consulta(); c.setIdConsulta(id); c.setHistoriaClinica(historia); c.setPaciente(paciente);
    c.setEstado("ATENDIDO"); c.setFechaAtencion(LocalDateTime.of(2026, 5, dia, 10, 0));
    c.setDoctorResponsable(new Empleado(5)); c.setTipoEnfermedad(new TipoEnfermedad(3)); return c;
  }
}
