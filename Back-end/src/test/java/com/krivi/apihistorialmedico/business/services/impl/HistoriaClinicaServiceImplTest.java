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
import com.krivi.apihistorialmedico.model.api.HistoriasClinicasFaltantesPreviewResponse;
import com.krivi.apihistorialmedico.model.api.CreacionHistoriaClinicaFaltanteResponse;
import com.krivi.apihistorialmedico.model.api.EstadoCreacionHistoriaClinicaFaltante;
import com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente;
import com.krivi.apihistorialmedico.repository.AntecedentesRepository;
import com.krivi.apihistorialmedico.repository.HistoriaClinicaRepository;
import com.krivi.apihistorialmedico.repository.PacienteRepository;
import com.krivi.apihistorialmedico.repository.ConsultaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class HistoriaClinicaServiceImplTest {
  @Mock private HistoriaClinicaRepository historiaClinicaRepository;
  @Mock private PacienteRepository pacienteRepository;
  @Mock private AntecedentesRepository antecedentesRepository;
  @Mock private ConsultaRepository consultaRepository;
  @InjectMocks private HistoriaClinicaServiceImpl historiaClinicaService;

  @Test
  void previewDevuelveVacioCuandoNoHayPacientesPendientesSinGuardarEntidades() {
    when(pacienteRepository.findByEstadoRegistroAndSinHistoriaClinica(
        com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente.ACTIVO)).thenReturn(List.of());

    HistoriasClinicasFaltantesPreviewResponse response =
        historiaClinicaService.obtenerHistoriasClinicasFaltantes();

    assertEquals(0, response.getCantidad());
    assertTrue(response.getPacientes().isEmpty());
    verify(pacienteRepository).findByEstadoRegistroAndSinHistoriaClinica(
        com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente.ACTIVO);
    verifyNoInteractions(historiaClinicaRepository, antecedentesRepository);
  }

  @Test
  void previewMapeaTodosLosPendientesEnOrdenConDatosMinimosYSeguros() {
    Paciente primero = paciente(3, "  Ana María ", " Pérez ", "12345678");
    Paciente segundo = paciente(7, null, "  ", null);
    Paciente tercero = paciente(11, "Luis", null, "12A4");
    when(pacienteRepository.findByEstadoRegistroAndSinHistoriaClinica(
        com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente.ACTIVO))
        .thenReturn(List.of(primero, segundo, tercero));

    HistoriasClinicasFaltantesPreviewResponse response =
        historiaClinicaService.obtenerHistoriasClinicasFaltantes();

    assertEquals(3, response.getCantidad());
    assertEquals(List.of(3, 7, 11), response.getPacientes().stream()
        .map(item -> item.getIdPaciente()).toList());
    assertEquals("Ana María Pérez", response.getPacientes().get(0).getNombreCompleto());
    assertEquals("******78", response.getPacientes().get(0).getDniEnmascarado());
    assertEquals("Nombre no registrado", response.getPacientes().get(1).getNombreCompleto());
    assertEquals("No registrado", response.getPacientes().get(1).getDniEnmascarado());
    assertEquals("Luis", response.getPacientes().get(2).getNombreCompleto());
    assertEquals("No registrado", response.getPacientes().get(2).getDniEnmascarado());
    assertFalse(response.getPacientes().get(2).getDniEnmascarado().contains("12A4"));
    verify(pacienteRepository, never()).save(any(Paciente.class));
    verifyNoInteractions(historiaClinicaRepository, antecedentesRepository);
  }

  private Paciente paciente(int id, String nombres, String apellidos, String dni) {
    Paciente paciente = new Paciente();
    paciente.setIdPaciente(id);
    paciente.setNombres(nombres);
    paciente.setApellidos(apellidos);
    paciente.setNumDocumento(dni);
    return paciente;
  }

  @Test
  void creaHistoriaFaltanteParaPacienteActivoSinModificarPacienteNiAntecedentes() {
    Paciente paciente = paciente(10, "Ana", "Pérez", "12345678");
    paciente.setFechaNacimiento(java.sql.Date.valueOf("1990-02-03"));
    paciente.setEstadoCivil("SOLTERO");
    when(pacienteRepository.findById(10)).thenReturn(Optional.of(paciente));
    when(historiaClinicaRepository.existsByPacienteIdPaciente(10)).thenReturn(false);
    when(historiaClinicaRepository.save(any(HistoriaClinica.class))).thenAnswer(invocacion -> {
      HistoriaClinica historia = invocacion.getArgument(0);
      historia.setIdHistoriaClinica(101);
      return historia;
    });

    CreacionHistoriaClinicaFaltanteResponse response =
        historiaClinicaService.crearHistoriaClinicaSiFalta(10);

    assertEquals(EstadoCreacionHistoriaClinicaFaltante.CREADA, response.getEstado());
    assertEquals(10, response.getIdPaciente());
    assertEquals(101, response.getIdHistoriaClinica());
    verify(historiaClinicaRepository, times(1)).save(org.mockito.ArgumentMatchers.argThat(historia ->
        historia.getPaciente() == paciente && historia.getIdHistoriaClinica() == null));
    verify(pacienteRepository, never()).save(any(Paciente.class));
    verifyNoInteractions(antecedentesRepository);
    assertEquals("12345678", paciente.getNumDocumento());
    assertEquals("Ana", paciente.getNombres());
    assertEquals("Pérez", paciente.getApellidos());
    assertEquals(java.sql.Date.valueOf("1990-02-03"), paciente.getFechaNacimiento());
    assertEquals("SOLTERO", paciente.getEstadoCivil());
  }

  @Test
  void omitePacienteQueYaTieneUnaOMultiplesHistorias() {
    Paciente paciente = paciente(10, "Ana", "Pérez", "12345678");
    when(pacienteRepository.findById(10)).thenReturn(Optional.of(paciente));
    when(historiaClinicaRepository.existsByPacienteIdPaciente(10)).thenReturn(true);

    CreacionHistoriaClinicaFaltanteResponse conUnaHistoria =
        historiaClinicaService.crearHistoriaClinicaSiFalta(10);
    CreacionHistoriaClinicaFaltanteResponse conMultiplesHistorias =
        historiaClinicaService.crearHistoriaClinicaSiFalta(10);

    assertEquals(EstadoCreacionHistoriaClinicaFaltante.OMITIDA_YA_TIENE_HISTORIA,
        conUnaHistoria.getEstado());
    assertEquals(EstadoCreacionHistoriaClinicaFaltante.OMITIDA_YA_TIENE_HISTORIA,
        conMultiplesHistorias.getEstado());
    verify(historiaClinicaRepository, never()).save(any(HistoriaClinica.class));
    verify(pacienteRepository, never()).save(any(Paciente.class));
    verifyNoInteractions(antecedentesRepository);
  }

  @Test
  void noCreaHistoriaParaPacienteInexistente() {
    when(pacienteRepository.findById(999)).thenReturn(Optional.empty());

    CreacionHistoriaClinicaFaltanteResponse response =
        historiaClinicaService.crearHistoriaClinicaSiFalta(999);

    assertEquals(EstadoCreacionHistoriaClinicaFaltante.PACIENTE_NO_ENCONTRADO, response.getEstado());
    verifyNoInteractions(historiaClinicaRepository, antecedentesRepository);
    verify(pacienteRepository, never()).save(any(Paciente.class));
  }

  @Test
  void noCreaHistoriaParaPacienteArchivado() {
    Paciente paciente = paciente(10, "Ana", "Pérez", "12345678");
    paciente.setEstadoRegistro(EstadoRegistroPaciente.ARCHIVADO);
    when(pacienteRepository.findById(10)).thenReturn(Optional.of(paciente));

    CreacionHistoriaClinicaFaltanteResponse response =
        historiaClinicaService.crearHistoriaClinicaSiFalta(10);

    assertEquals(EstadoCreacionHistoriaClinicaFaltante.PACIENTE_INACTIVO, response.getEstado());
    verifyNoInteractions(historiaClinicaRepository, antecedentesRepository);
    verify(pacienteRepository, never()).save(any(Paciente.class));
  }

  @Test
  void permiteCrearSinDniValidoYSinAntecedentes() {
    Paciente paciente = paciente(10, null, null, "DNI-invalido");
    when(pacienteRepository.findById(10)).thenReturn(Optional.of(paciente));
    when(historiaClinicaRepository.existsByPacienteIdPaciente(10)).thenReturn(false);
    when(historiaClinicaRepository.save(any(HistoriaClinica.class))).thenAnswer(invocacion -> {
      HistoriaClinica historia = invocacion.getArgument(0);
      historia.setIdHistoriaClinica(102);
      return historia;
    });

    CreacionHistoriaClinicaFaltanteResponse response =
        historiaClinicaService.crearHistoriaClinicaSiFalta(10);

    assertEquals(EstadoCreacionHistoriaClinicaFaltante.CREADA, response.getEstado());
    assertEquals(102, response.getIdHistoriaClinica());
    verify(historiaClinicaRepository, times(1)).save(any(HistoriaClinica.class));
    verifyNoInteractions(antecedentesRepository);
  }

  @Test
  void devuelveErrorSiFallaElGuardadoDeLaHistoria() {
    Paciente paciente = paciente(10, "Ana", "Pérez", "12345678");
    when(pacienteRepository.findById(10)).thenReturn(Optional.of(paciente));
    when(historiaClinicaRepository.existsByPacienteIdPaciente(10)).thenReturn(false);
    when(historiaClinicaRepository.save(any(HistoriaClinica.class)))
        .thenThrow(new RuntimeException("fallo simulado"));

    CreacionHistoriaClinicaFaltanteResponse response =
        historiaClinicaService.crearHistoriaClinicaSiFalta(10);

    assertEquals(EstadoCreacionHistoriaClinicaFaltante.ERROR, response.getEstado());
    assertNull(response.getIdHistoriaClinica());
    verify(pacienteRepository, never()).save(any(Paciente.class));
    verifyNoInteractions(antecedentesRepository);
  }

  @Test
  void creacionIndividualUsaTransaccionIndependiente() throws Exception {
    Transactional transactional = HistoriaClinicaServiceImpl.class
        .getMethod("crearHistoriaClinicaSiFalta", Integer.class)
        .getAnnotation(Transactional.class);

    assertNotNull(transactional);
    assertEquals(Propagation.REQUIRES_NEW, transactional.propagation());
  }

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
    when(historiaClinicaRepository.findByIdHistoriaClinicaAndPacienteEstadoRegistro(25, com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente.ACTIVO)).thenReturn(java.util.Optional.of(historia));
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
    when(historiaClinicaRepository.findAllByPacienteIdPacienteAndPacienteEstadoRegistroOrderByIdHistoriaClinicaAsc(10, com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente.ACTIVO))
        .thenReturn(List.of(historia, segundaHistoria));
    assertEquals(List.of("Ana Actualizada", "Ana Actualizada"), historiaClinicaService.findByPaciente(10)
        .getData().stream().map(item -> item.getNombres()).toList());
  }

  @Test
  void rechazaActualizacionDeHistoriaInexistenteODatosInvalidos() {
    when(historiaClinicaRepository.findByIdHistoriaClinicaAndPacienteEstadoRegistro(999, com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente.ACTIVO)).thenReturn(java.util.Optional.empty());
    CreacionHistoriaClinicaException inexistente = assertThrows(CreacionHistoriaClinicaException.class,
        () -> historiaClinicaService.update(999, updateValido()));

    Paciente paciente = new Paciente();
    HistoriaClinica historia = new HistoriaClinica();
    historia.setIdHistoriaClinica(25);
    historia.setPaciente(paciente);
    when(historiaClinicaRepository.findByIdHistoriaClinicaAndPacienteEstadoRegistro(25, com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente.ACTIVO)).thenReturn(java.util.Optional.of(historia));
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
    when(historiaClinicaRepository.findByIdHistoriaClinicaAndPacienteEstadoRegistro(25, com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente.ACTIVO)).thenReturn(java.util.Optional.of(historia));
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
  void noCreaHistoriaCuandoSoloExisteUnPacienteArchivado() {
    // findByDniNormalizado consulta explícitamente solo ACTIVO; el archivado permanece en BD pero no es candidato.
    when(pacienteRepository.findByDniNormalizado("12345678")).thenReturn(List.of());

    CreacionHistoriaClinicaException error = assertThrows(CreacionHistoriaClinicaException.class,
        () -> historiaClinicaService.save(requestValido("12345678")));

    assertEquals("PACIENTE_NO_ENCONTRADO", error.getCodigo());
    verify(historiaClinicaRepository, never()).save(any());
  }

  @Test
  void unActivoYUnArchivadoConElMismoDniNoGeneranAmbiguedad() {
    Paciente activo = new Paciente();
    activo.setIdPaciente(10);
    activo.setNumDocumento("12345678");
    when(pacienteRepository.findByDniNormalizado("12345678")).thenReturn(List.of(activo));
    when(antecedentesRepository.findByPacienteIdPaciente(10)).thenReturn(List.of());
    when(historiaClinicaRepository.save(any(HistoriaClinica.class))).thenAnswer(invocation -> {
      HistoriaClinica historia = invocation.getArgument(0);
      historia.setIdHistoriaClinica(101);
      return historia;
    });

    ResponseModelSet response = historiaClinicaService.save(requestValido("12345678"));

    assertEquals(101, response.getIdGenerado());
    verify(historiaClinicaRepository).save(any(HistoriaClinica.class));
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
    when(historiaClinicaRepository.findAllByPacienteIdPacienteAndPacienteEstadoRegistroOrderByIdHistoriaClinicaAsc(10, com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente.ACTIVO))
        .thenReturn(List.of(primera, segunda));
    when(antecedentesRepository.findByPacienteIdPaciente(10)).thenReturn(List.of());

    assertEquals(List.of(15, 16), historiaClinicaService.findByPaciente(10).getData().stream()
        .map(item -> item.getIdHistoriaClinica()).toList());
  }

  @Test
  void calculaEdadDesdeJavaSqlDate() {
    LocalDate nacimiento = LocalDate.now(ZoneId.of("America/Lima")).minusYears(30).minusDays(1);
    HistoriaClinica historia = historia(15, 10, "12345678", "Ana", "Pérez");
    historia.getPaciente().setFechaNacimiento(java.sql.Date.valueOf(nacimiento));
    when(historiaClinicaRepository.findAllByPacienteIdPacienteAndPacienteEstadoRegistroOrderByIdHistoriaClinicaAsc(10, com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente.ACTIVO))
        .thenReturn(List.of(historia));
    when(antecedentesRepository.findByPacienteIdPaciente(10)).thenReturn(List.of());

    assertEquals(30, historiaClinicaService.findByPaciente(10).getData().getFirst().getEdad());
  }

  @Test
  void calculaEdadDesdeJavaUtilDateUsandoZonaHorariaLima() {
    ZoneId lima = ZoneId.of("America/Lima");
    LocalDate nacimiento = LocalDate.now(lima).minusYears(24);
    java.util.Date fechaNacimiento = java.util.Date.from(nacimiento.atTime(12, 0).atZone(lima).toInstant());
    HistoriaClinica historia = historia(15, 10, "12345678", "Ana", "Pérez");
    historia.getPaciente().setFechaNacimiento(fechaNacimiento);
    when(historiaClinicaRepository.findAllByPacienteIdPacienteAndPacienteEstadoRegistroOrderByIdHistoriaClinicaAsc(10, com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente.ACTIVO))
        .thenReturn(List.of(historia));
    when(antecedentesRepository.findByPacienteIdPaciente(10)).thenReturn(List.of());

    assertEquals(24, historiaClinicaService.findByPaciente(10).getData().getFirst().getEdad());
  }

  @Test
  void conservaEdadNulaCuandoNoExisteFechaDeNacimiento() {
    HistoriaClinica historia = historia(15, 10, "12345678", "Ana", "Pérez");
    historia.getPaciente().setFechaNacimiento(null);
    when(historiaClinicaRepository.findAllByPacienteIdPacienteAndPacienteEstadoRegistroOrderByIdHistoriaClinicaAsc(10, com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente.ACTIVO))
        .thenReturn(List.of(historia));
    when(antecedentesRepository.findByPacienteIdPaciente(10)).thenReturn(List.of());

    assertNull(historiaClinicaService.findByPaciente(10).getData().getFirst().getEdad());
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
  void consultaGeneralDetectaHistoriasDePacientesActivosConElMismoDni() {
    HistoriaClinica primera = historia(1, 10, " 72845292", "Mismo", "Nombre");
    HistoriaClinica segunda = historia(2, 11, "72845292 ", "Mismo", "Nombre");
    when(historiaClinicaRepository.findAllForIntegracion()).thenReturn(List.of(primera, segunda));

    DuplicadosHistoriasClinicasResponse response = historiaClinicaService.obtenerDuplicadosParaIntegracion();

    assertTrue(response.isHayDuplicados());
    assertEquals(1, response.getTotalGrupos());
    assertEquals("dni", response.getDuplicados().getFirst().getTipo());
    assertEquals(List.of(1, 2), response.getDuplicados().getFirst().getHistoriasClinicas().stream()
        .map(item -> item.getIdHistoriaClinica()).toList());
  }

  @Test
  void unaHistoriaPorPacienteNoSeConsideraDuplicadaPeroDosDelMismoPacienteSi() {
    HistoriaClinica unica = historia(1, 10, "11111111", "Ana", "Lima");
    when(historiaClinicaRepository.findAllForIntegracion()).thenReturn(List.of(unica));
    assertFalse(historiaClinicaService.obtenerDuplicadosParaIntegracion().isHayDuplicados());

    HistoriaClinica segunda = historia(2, 10, "11111111", "Ana", "Lima");
    when(historiaClinicaRepository.findAllForIntegracion()).thenReturn(List.of(unica, segunda));
    assertTrue(historiaClinicaService.obtenerDuplicadosParaIntegracion().isHayDuplicados());
  }

  @Test
  void devuelveMensajesEspecificosParaDniInexistenteYUnaSolaHistoria() {
    when(pacienteRepository.findByDniNormalizado("00000000")).thenReturn(List.of());
    DuplicadosHistoriasClinicasResponse inexistente = historiaClinicaService.obtenerDuplicadosParaIntegracion("00000000");
    assertEquals("No se encontró un paciente activo con el DNI ingresado.", inexistente.getMensaje());

    Paciente paciente = new Paciente();
    when(pacienteRepository.findByDniNormalizado("11111111")).thenReturn(List.of(paciente));
    when(historiaClinicaRepository.findForIntegracionByDni("11111111"))
        .thenReturn(List.of(historia(1, 10, "11111111", "Ana", "Lima")));
    DuplicadosHistoriasClinicasResponse unica = historiaClinicaService.obtenerDuplicadosParaIntegracion("11111111");
    assertFalse(unica.isHayDuplicados());
    assertTrue(unica.getMensaje().contains("tiene una sola historia clínica"));
  }

  @Test
  void recomendacionPriorizaConsultasActividadAntiguedadEId() {
    assertEquals(2, recomendada("10000001", historia(1, 1, "10000001", "Ana", "Lima"),
        historia(2, 1, "10000001", "Ana", "Lima"), new Object[]{1, 1L, null}, new Object[]{2, 2L, null}));

    LocalDateTime anterior = LocalDateTime.of(2026, 1, 1, 10, 0);
    LocalDateTime reciente = LocalDateTime.of(2026, 2, 1, 10, 0);
    assertEquals(4, recomendada("10000002", historia(3, 2, "10000002", "Beto", "Lima"),
        historia(4, 2, "10000002", "Beto", "Lima"), new Object[]{3, 2L, anterior}, new Object[]{4, 2L, reciente}));

    HistoriaClinica antigua = historia(5, 3, "10000003", "Cora", "Lima");
    HistoriaClinica nueva = historia(6, 3, "10000003", "Cora", "Lima");
    antigua.setFechaCreacion(LocalDateTime.of(2025, 1, 1, 10, 0));
    nueva.setFechaCreacion(LocalDateTime.of(2026, 1, 1, 10, 0));
    assertEquals(5, recomendada("10000003", antigua, nueva, new Object[]{5, 0L, null}, new Object[]{6, 0L, null}));

    HistoriaClinica idMayor = historia(8, 4, "10000004", "Dora", "Lima");
    HistoriaClinica idMenor = historia(7, 4, "10000004", "Dora", "Lima");
    assertEquals(7, recomendada("10000004", idMayor, idMenor, new Object[]{8, 0L, null}, new Object[]{7, 0L, null}));
  }

  private Integer recomendada(String dni, HistoriaClinica primera, HistoriaClinica segunda, Object[] primerResumen, Object[] segundoResumen) {
    when(pacienteRepository.findByDniNormalizado(dni)).thenReturn(List.of(primera.getPaciente()));
    when(historiaClinicaRepository.findForIntegracionByDni(dni)).thenReturn(List.of(primera, segunda));
    when(consultaRepository.resumirPorHistoriasClinicas(List.of(primera.getIdHistoriaClinica(), segunda.getIdHistoriaClinica())))
        .thenReturn(List.of(primerResumen, segundoResumen));
    return historiaClinicaService.obtenerDuplicadosParaIntegracion(dni).getDuplicados().getFirst().getIdHistoriaClinicaRecomendada();
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
