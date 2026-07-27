package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.exception.BusquedaHistoriaClinicaException;
import com.krivi.apihistorialmedico.business.exception.CreacionHistoriaClinicaException;
import com.krivi.apihistorialmedico.model.api.BusquedaHistoriasClinicasResponse;
import com.krivi.apihistorialmedico.model.api.DuplicadosHistoriasClinicasResponse;
import com.krivi.apihistorialmedico.model.api.EstadisticasHistoriasClinicasResponse;
import com.krivi.apihistorialmedico.model.entity.HistoriaClinica;
import com.krivi.apihistorialmedico.model.entity.Paciente;
import com.krivi.apihistorialmedico.model.entity.Antecedentes;
import com.krivi.apihistorialmedico.model.api.HistoriaClinicaRequest;
import com.krivi.apihistorialmedico.model.api.HistoriaClinicaUpdateRequest;
import com.krivi.apihistorialmedico.model.api.ResponseModelSet;
import com.krivi.apihistorialmedico.repository.AntecedentesRepository;
import com.krivi.apihistorialmedico.repository.HistoriaClinicaRepository;
import com.krivi.apihistorialmedico.repository.PacienteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class HistoriaClinicaServiceImplTest {
  @Mock private HistoriaClinicaRepository historiaClinicaRepository;
  @Mock private PacienteRepository pacienteRepository;
  @Mock private AntecedentesRepository antecedentesRepository;
  @InjectMocks private HistoriaClinicaServiceImpl historiaClinicaService;

  @Test
  void actualizaPacienteYAntecedentesSinCambiarRelacionNiCrearHistoria() {
    Paciente paciente = new Paciente();
    paciente.setIdPaciente(10);
    paciente.setNumDocumento("12345678");
    HistoriaClinica historia = new HistoriaClinica();
    historia.setIdHistoriaClinica(25);
    historia.setPaciente(paciente);
    Antecedentes antecedentes = new Antecedentes();
    antecedentes.setIdAntecedentes(8);
    antecedentes.setPaciente(paciente);
    when(historiaClinicaRepository.findById(25)).thenReturn(java.util.Optional.of(historia));
    when(antecedentesRepository.findByPacienteIdPaciente(10)).thenReturn(List.of(antecedentes));
    when(historiaClinicaRepository.save(historia)).thenReturn(historia);

    ResponseModelSet response = historiaClinicaService.update(25, updateValido());

    assertEquals(25, response.getIdGenerado());
    assertSame(paciente, historia.getPaciente());
    assertEquals(10, historia.getPaciente().getIdPaciente());
    assertEquals("12345678", paciente.getNumDocumento());
    assertEquals("Ana Actualizada", paciente.getNombres());
    assertEquals(java.sql.Date.valueOf("1995-05-10"), paciente.getFechaNacimiento());
    assertEquals("Asma controlada", antecedentes.getEnfermedadesPrevias());
    verify(historiaClinicaRepository).save(historia);

    HistoriaClinica segundaHistoria = new HistoriaClinica();
    segundaHistoria.setIdHistoriaClinica(26);
    segundaHistoria.setPaciente(paciente);
    when(historiaClinicaRepository.findAllByPacienteIdPacienteOrderByIdHistoriaClinicaAsc(10))
        .thenReturn(List.of(historia, segundaHistoria));
    assertEquals(List.of("Ana Actualizada", "Ana Actualizada"), historiaClinicaService.findByPaciente(10)
        .getData().stream().map(item -> item.getNombres()).toList());
  }

  @Test
  void rechazaActualizacionDeHistoriaInexistenteODatosInvalidos() {
    when(historiaClinicaRepository.findById(999)).thenReturn(java.util.Optional.empty());
    CreacionHistoriaClinicaException inexistente = assertThrows(CreacionHistoriaClinicaException.class,
        () -> historiaClinicaService.update(999, updateValido()));

    Paciente paciente = new Paciente();
    HistoriaClinica historia = new HistoriaClinica();
    historia.setIdHistoriaClinica(25);
    historia.setPaciente(paciente);
    when(historiaClinicaRepository.findById(25)).thenReturn(java.util.Optional.of(historia));
    HistoriaClinicaUpdateRequest futura = updateValido();
    futura.setFechaNacimiento(LocalDate.now(ZoneId.of("America/Lima")).plusDays(1));
    CreacionHistoriaClinicaException fechaInvalida = assertThrows(CreacionHistoriaClinicaException.class,
        () -> historiaClinicaService.update(25, futura));
    HistoriaClinicaUpdateRequest sinNombre = updateValido();
    sinNombre.setNombres("   ");
    CreacionHistoriaClinicaException datosInvalidos = assertThrows(CreacionHistoriaClinicaException.class,
        () -> historiaClinicaService.update(25, sinNombre));

    assertEquals(404, inexistente.getStatus().value());
    assertEquals(400, fechaInvalida.getStatus().value());
    assertEquals(400, datosInvalidos.getStatus().value());
  }

  @Test
  void actualizacionEsTransaccionalYNoGuardaHistoriaSiFallaAntecedente() throws Exception {
    assertNotNull(HistoriaClinicaServiceImpl.class
        .getMethod("update", int.class, HistoriaClinicaUpdateRequest.class)
        .getAnnotation(Transactional.class));
    Paciente paciente = new Paciente();
    paciente.setIdPaciente(10);
    HistoriaClinica historia = new HistoriaClinica();
    historia.setIdHistoriaClinica(25);
    historia.setPaciente(paciente);
    when(historiaClinicaRepository.findById(25)).thenReturn(java.util.Optional.of(historia));
    when(antecedentesRepository.findByPacienteIdPaciente(10)).thenReturn(List.of());
    when(antecedentesRepository.save(any(Antecedentes.class))).thenThrow(new RuntimeException("fallo simulado"));

    assertThrows(RuntimeException.class, () -> historiaClinicaService.update(25, updateValido()));
    verify(historiaClinicaRepository, never()).save(any(HistoriaClinica.class));
  }

  @Test
  void creaDosHistoriasParaElMismoPacienteYActualizaDatosYAntecedentes() {
    Paciente paciente = new Paciente();
    paciente.setIdPaciente(10);
    paciente.setNumDocumento(" 12345678 ");
    Antecedentes antecedentes = new Antecedentes();
    antecedentes.setIdAntecedentes(5);
    AtomicInteger secuenciaHistorias = new AtomicInteger(100);
    when(pacienteRepository.findByDniNormalizado("12345678")).thenReturn(List.of(paciente));
    when(pacienteRepository.save(any(Paciente.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
    when(antecedentesRepository.findByPacienteIdPaciente(10)).thenReturn(List.of(antecedentes));
    when(antecedentesRepository.save(any(Antecedentes.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
    when(historiaClinicaRepository.save(any(HistoriaClinica.class))).thenAnswer(invocacion -> {
      HistoriaClinica historia = invocacion.getArgument(0);
      historia.setIdHistoriaClinica(secuenciaHistorias.incrementAndGet());
      return historia;
    });

    HistoriaClinicaRequest request = requestValido("12345678");
    ResponseModelSet primera = historiaClinicaService.save(request);
    ResponseModelSet segunda = historiaClinicaService.save(request);

    assertNotEquals(primera.getIdGenerado(), segunda.getIdGenerado());
    assertEquals("Ana María", paciente.getNombres());
    assertEquals("Pérez Díaz", paciente.getApellidos());
    assertEquals("12345678", paciente.getNumDocumento());
    assertEquals(java.sql.Date.valueOf(request.getFechaIngreso()), paciente.getFechaIngreso());
    assertEquals(java.sql.Date.valueOf(request.getFechaNacimiento()), paciente.getFechaNacimiento());
    assertSame(paciente, antecedentes.getPaciente());
    assertEquals("Asma", antecedentes.getEnfermedadesPrevias());
    assertEquals("Ninguna", antecedentes.getCirugiasPrevias());
    assertEquals("Penicilina", antecedentes.getAlergiaMedicamentos());
    verify(historiaClinicaRepository, times(2)).save(any(HistoriaClinica.class));
  }

  @Test
  void creaAntecedentesCuandoElPacienteAunNoTieneRegistro() {
    Paciente paciente = new Paciente();
    paciente.setIdPaciente(10);
    when(pacienteRepository.findByDniNormalizado("12345678")).thenReturn(List.of(paciente));
    when(antecedentesRepository.findByPacienteIdPaciente(10)).thenReturn(List.of());
    when(historiaClinicaRepository.save(any(HistoriaClinica.class))).thenAnswer(invocacion -> {
      HistoriaClinica historia = invocacion.getArgument(0);
      historia.setIdHistoriaClinica(101);
      return historia;
    });

    historiaClinicaService.save(requestValido("12345678"));

    verify(antecedentesRepository).save(org.mockito.ArgumentMatchers.argThat(antecedentes ->
        antecedentes.getIdAntecedentes() == null
            && antecedentes.getPaciente() == paciente
            && "Asma".equals(antecedentes.getEnfermedadesPrevias())));
  }

  @Test
  void rechazaDniVacioOInvalidoConBadRequest() {
    CreacionHistoriaClinicaException vacio = assertThrows(CreacionHistoriaClinicaException.class,
        () -> historiaClinicaService.save(requestValido("   ")));
    CreacionHistoriaClinicaException invalido = assertThrows(CreacionHistoriaClinicaException.class,
        () -> historiaClinicaService.save(requestValido("1234ABCD")));

    assertEquals(400, vacio.getStatus().value());
    assertEquals("DNI_REQUERIDO", vacio.getCodigo());
    assertEquals(400, invalido.getStatus().value());
    assertEquals("DNI_INVALIDO", invalido.getCodigo());
    verify(historiaClinicaRepository, never()).save(any());
  }

  @Test
  void rechazaFechaNacimientoFutura() {
    HistoriaClinicaRequest futura = requestValido("12345678");
    futura.setFechaNacimiento(LocalDate.now(ZoneId.of("America/Lima")).plusDays(1));

    CreacionHistoriaClinicaException errorFecha = assertThrows(CreacionHistoriaClinicaException.class,
        () -> historiaClinicaService.save(futura));

    assertEquals("FECHA_NACIMIENTO_INVALIDA", errorFecha.getCodigo());
    verify(historiaClinicaRepository, never()).save(any());
  }

  @Test
  void rechazaPacienteInexistenteConNotFound() {
    when(pacienteRepository.findByDniNormalizado("12345678")).thenReturn(List.of());

    CreacionHistoriaClinicaException error = assertThrows(CreacionHistoriaClinicaException.class,
        () -> historiaClinicaService.save(requestValido(" 12345678 ")));

    assertEquals(404, error.getStatus().value());
    assertEquals("PACIENTE_NO_ENCONTRADO", error.getCodigo());
    verify(historiaClinicaRepository, never()).save(any());
  }

  @Test
  void rechazaDniAsociadoAVariosPacientesConConflict() {
    Paciente primero = new Paciente();
    Paciente segundo = new Paciente();
    when(pacienteRepository.findByDniNormalizado("12345678")).thenReturn(List.of(primero, segundo));

    CreacionHistoriaClinicaException error = assertThrows(CreacionHistoriaClinicaException.class,
        () -> historiaClinicaService.save(requestValido("12345678")));

    assertEquals(409, error.getStatus().value());
    assertEquals("DNI_AMBIGUO", error.getCodigo());
    verify(pacienteRepository, never()).save(any());
    verify(historiaClinicaRepository, never()).save(any());
  }

  @Test
  void listaTodasLasHistoriasDelPaciente() {
    HistoriaClinica primera = historia(15, 10, "12345678", "Ana", "Pérez");
    HistoriaClinica segunda = historia(16, 10, "12345678", "Ana", "Pérez");
    when(historiaClinicaRepository.findAllByPacienteIdPacienteOrderByIdHistoriaClinicaAsc(10))
        .thenReturn(List.of(primera, segunda));
    when(antecedentesRepository.findByPacienteIdPaciente(10)).thenReturn(List.of());

    assertEquals(List.of(15, 16), historiaClinicaService.findByPaciente(10).getData().stream()
        .map(item -> item.getIdHistoriaClinica()).toList());
  }

  @Test
  void buscaNumeroCortoPorHistoriaYPacienteYEliminaRepetidos() {
    HistoriaClinica historiaPorId = historia(5, 10, "72845292", "Ana", "Lima");
    HistoriaClinica historiaPorPaciente = historia(7, 5, "12345678", "Bruno", "Paz");
    when(historiaClinicaRepository.findForIntegracionByIdHistoriaClinica(5)).thenReturn(List.of(historiaPorId));
    when(historiaClinicaRepository.findForIntegracionByIdPaciente(5)).thenReturn(List.of(historiaPorId, historiaPorPaciente));

    BusquedaHistoriasClinicasResponse response = historiaClinicaService.buscarParaIntegracion("5");

    assertTrue(response.isEncontrado());
    assertEquals("multiple", response.getTipoResultado());
    assertEquals(List.of(5, 7), response.getHistoriasClinicas().stream().map(h -> h.getIdHistoriaClinica()).toList());
  }

  @Test
  void admitePrefijosExplicitosYNormalizaDniSoloConTrim() {
    HistoriaClinica historia = historia(8, 4, " 72845292 ", "Carmen", "Ríos");
    when(historiaClinicaRepository.findForIntegracionByIdHistoriaClinica(8)).thenReturn(List.of(historia));
    when(historiaClinicaRepository.findForIntegracionByDni("72845292")).thenReturn(List.of(historia));

    assertEquals(8, historiaClinicaService.buscarParaIntegracion("historia:8").getHistoriasClinicas().getFirst().getIdHistoriaClinica());
    assertEquals("72845292", historiaClinicaService.buscarParaIntegracion(" dni: 72845292 ").getHistoriasClinicas().getFirst().getDni());
    verify(historiaClinicaRepository).findForIntegracionByDni("72845292");
  }

  @Test
  void rechazaDnisInvalidosYNumerosDeMasDeOchoDigitos() {
    assertThrows(BusquedaHistoriaClinicaException.class, () -> historiaClinicaService.buscarParaIntegracion("dni: ABC12345"));
    assertThrows(BusquedaHistoriaClinicaException.class, () -> historiaClinicaService.buscarParaIntegracion("123456789"));
  }

  @Test
  void buscaPorNombreSinMayusculasNiTildesYPermiteOmitirSegundoNombre() {
    HistoriaClinica patricia = historia(1, 2, "12345678", "Patricia Elena", "Cárdenas Torres");
    when(historiaClinicaRepository.findAllForIntegracion()).thenReturn(List.of(patricia));

    assertEquals("Patricia Elena Cárdenas Torres", historiaClinicaService.buscarParaIntegracion("Patricia").getHistoriasClinicas().getFirst().getNombreCompleto());
    assertEquals("Patricia Elena Cárdenas Torres", historiaClinicaService.buscarParaIntegracion("Patricia Cardenas Torres").getHistoriasClinicas().getFirst().getNombreCompleto());
    assertEquals("Patricia Elena Cárdenas Torres", historiaClinicaService.buscarParaIntegracion("Cárdenas Patricia").getHistoriasClinicas().getFirst().getNombreCompleto());
  }

  @Test
  void devuelveSinResultadosParaNombreInexistente() {
    when(historiaClinicaRepository.findAllForIntegracion()).thenReturn(List.of(historia(1, 2, "12345678", "Patricia Elena", "Cárdenas Torres")));

    BusquedaHistoriasClinicasResponse response = historiaClinicaService.buscarParaIntegracion("Inexistente");

    assertFalse(response.isEncontrado());
    assertEquals("sin_resultados", response.getTipoResultado());
    assertTrue(response.getHistoriasClinicas().isEmpty());
  }

  @Test
  void calculaEstadisticasConRangoDeLima() {
    LocalDateTime inicio = LocalDate.now(ZoneId.of("America/Lima")).atStartOfDay();
    when(historiaClinicaRepository.count()).thenReturn(12L);
    when(historiaClinicaRepository.countByFechaCreacionGreaterThanEqualAndFechaCreacionLessThan(inicio, inicio.plusDays(1))).thenReturn(3L);

    EstadisticasHistoriasClinicasResponse response = historiaClinicaService.obtenerEstadisticasParaIntegracion();

    assertEquals(12, response.getTotalHistoriasClinicas());
    assertEquals(3, response.getCreadasHoy());
  }

  @Test
  void detectaDuplicadosPorDniNormalizadoYPorPacienteSinNombres() {
    HistoriaClinica primera = historia(1, 10, " 72845292", "Mismo", "Nombre");
    HistoriaClinica segunda = historia(2, 11, "72845292 ", "Mismo", "Nombre");
    when(historiaClinicaRepository.findIdsPacienteConHistoriasDuplicadas()).thenReturn(List.of(20));
    when(historiaClinicaRepository.findForIntegracionByIdPaciente(20)).thenReturn(List.of(historia(3, 20, "11111111", "Otro", "Paciente"), historia(4, 20, "22222222", "Otro", "Paciente")));
    when(historiaClinicaRepository.findDnisNormalizadosConHistoriasDuplicadas()).thenReturn(List.of("72845292"));
    when(historiaClinicaRepository.findForIntegracionByDni("72845292")).thenReturn(List.of(primera, segunda));

    DuplicadosHistoriasClinicasResponse response = historiaClinicaService.obtenerDuplicadosParaIntegracion();

    assertTrue(response.isHayDuplicados());
    assertEquals(2, response.getTotalGrupos());
    assertEquals("idPaciente", response.getDuplicados().getFirst().getTipo());
    assertEquals("dni", response.getDuplicados().get(1).getTipo());
  }

  private HistoriaClinica historia(int idHistoria, int idPaciente, String dni, String nombres, String apellidos) {
    Paciente paciente = new Paciente();
    paciente.setIdPaciente(idPaciente); paciente.setNumDocumento(dni); paciente.setNombres(nombres); paciente.setApellidos(apellidos);
    HistoriaClinica historia = new HistoriaClinica();
    historia.setIdHistoriaClinica(idHistoria); historia.setPaciente(paciente); historia.setFechaCreacion(LocalDateTime.of(2026, 7, 23, 9, 0));
    return historia;
  }

  private HistoriaClinicaRequest requestValido(String dni) {
    HistoriaClinicaRequest request = new HistoriaClinicaRequest();
    request.setFechaIngreso(LocalDate.now(ZoneId.of("America/Lima")));
    request.setFechaNacimiento(LocalDate.of(1996, 1, 1));
    request.setApellidos(" Pérez Díaz ");
    request.setNombres(" Ana María ");
    request.setEstadoCivil("SOLTERO");
    request.setDni(dni);
    request.setEnfermedadesPrevias(" Asma ");
    request.setCirugiasPrevias(" Ninguna ");
    request.setAlergiaMedicamentos(" Penicilina ");
    return request;
  }

  private HistoriaClinicaUpdateRequest updateValido() {
    HistoriaClinicaUpdateRequest request = new HistoriaClinicaUpdateRequest();
    request.setFechaIngreso(LocalDate.of(2026, 7, 27));
    request.setFechaNacimiento(LocalDate.of(1995, 5, 10));
    request.setApellidos(" Pérez Actualizado ");
    request.setNombres(" Ana Actualizada ");
    request.setEstadoCivil("CASADO");
    request.setEnfermedadesPrevias(" Asma controlada ");
    request.setCirugiasPrevias(" Ninguna ");
    request.setAlergiaMedicamentos(" Penicilina ");
    return request;
  }
}
