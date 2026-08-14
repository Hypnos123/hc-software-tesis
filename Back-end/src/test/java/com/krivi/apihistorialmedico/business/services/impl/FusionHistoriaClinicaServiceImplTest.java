package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.exception.FusionHistoriaClinicaException;
import com.krivi.apihistorialmedico.business.services.*;
import com.krivi.apihistorialmedico.model.api.*;
import com.krivi.apihistorialmedico.model.entity.*;
import com.krivi.apihistorialmedico.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FusionHistoriaClinicaServiceImplTest {
  @Mock ReautenticacionLocalService reautenticacion; @Mock AnalisisHistoriasClinicasDuplicadasService analisis;
  @Mock HistoriaClinicaRepository historias; @Mock ConsultaRepository consultas; @Mock UsuarioRepository usuarios;
  @Mock AuditoriaFusionHistoriaClinicaRepository auditorias; @Mock EntityManager entityManager;
  FusionHistoriaClinicaServiceImpl service; Paciente paciente; HistoriaClinica principal; HistoriaClinica secundaria; Usuario usuario;

  @BeforeEach void setup() {
    service = new FusionHistoriaClinicaServiceImpl(reautenticacion, analisis, historias, consultas, usuarios, auditorias, entityManager);
    paciente = new Paciente(); paciente.setIdPaciente(12); paciente.setEstadoRegistro(EstadoRegistroPaciente.ACTIVO);
    principal = historia(7, paciente); secundaria = historia(8, paciente);
    usuario = new Usuario(); usuario.setIdUsuario(3); usuario.setEmpleado(new Empleado(4));
  }

  @Test void fusionaHistoriasVaciasYAudita() {
    preparar(List.of(), List.of());
    when(auditorias.saveAndFlush(any())).thenAnswer(i -> { AuditoriaFusionHistoriaClinica a=i.getArgument(0); a.setIdAuditoria(9); return a; });
    FusionarHistoriasClinicasResponse response = service.fusionar(3, 8, request(0, 0, List.of(), List.of()));
    assertTrue(response.isFusionada()); assertEquals(0, response.getCantidadConsultasTransferidas()); assertEquals(9, response.getIdAuditoria());
    verify(historias).delete(secundaria); verify(auditorias).saveAndFlush(any());
  }

  @Test void transfiereConsultasSinModificarPaciente() {
    Consulta consulta = consulta(20, secundaria, paciente); preparar(List.of(), List.of(consulta));
    when(consultas.countByHistoriaClinicaIdHistoriaClinica(7)).thenReturn(1L);
    when(auditorias.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    service.fusionar(3, 8, request(0, 1, List.of(), List.of(20)));
    assertSame(principal, consulta.getHistoriaClinica()); assertSame(paciente, consulta.getPaciente());
    verify(consultas).saveAll(List.of(consulta));
  }

  @Test void bloqueaPacientesDiferentesAntesDeTransferir() {
    Paciente otro = new Paciente(); otro.setIdPaciente(13); otro.setEstadoRegistro(EstadoRegistroPaciente.ACTIVO); secundaria.setPaciente(otro);
    prepararLocks();
    FusionHistoriaClinicaException error = assertThrows(FusionHistoriaClinicaException.class, () -> service.fusionar(3, 8, request(0,0,List.of(),List.of())));
    assertEquals("HISTORIAS_DE_PACIENTES_DIFERENTES", error.getResultado()); verify(consultas, never()).saveAll(any());
  }

  @Test void rechazaSnapshotDesactualizadoYConsultaInconsistente() {
    Consulta c = consulta(20, secundaria, paciente); preparar(List.of(), List.of(c));
    FusionHistoriaClinicaException desactualizado = assertThrows(FusionHistoriaClinicaException.class,
        () -> service.fusionar(3, 8, request(0,0,List.of(),List.of())));
    assertEquals("ANALISIS_DESACTUALIZADO", desactualizado.getResultado()); verify(historias, never()).delete(any());
  }

  @Test void falloDuranteTransferenciaNoEliminaNiAudita() {
    Consulta c = consulta(20, secundaria, paciente); preparar(List.of(), List.of(c));
    when(consultas.saveAll(any())).thenThrow(new RuntimeException("fallo"));
    FusionHistoriaClinicaException error = assertThrows(FusionHistoriaClinicaException.class,
        () -> service.fusionar(3, 8, request(0,1,List.of(),List.of(20))));
    assertEquals("ERROR_FUSION", error.getResultado()); verify(historias, never()).delete(any()); verifyNoInteractions(auditorias);
  }

  @Test void falloDuranteEliminacionNoAuditaYPropagaErrorTransaccional() {
    preparar(List.of(), List.of()); doThrow(new RuntimeException("fallo delete")).when(historias).delete(secundaria);
    FusionHistoriaClinicaException error=assertThrows(FusionHistoriaClinicaException.class,()->service.fusionar(3,8,request(0,0,List.of(),List.of())));
    assertEquals("ERROR_FUSION",error.getResultado()); verifyNoInteractions(auditorias);
  }

  private void preparar(List<Consulta> p, List<Consulta> s) {
    prepararLocks(); when(consultas.findForFusionByHistoriaId(7)).thenReturn(p); when(consultas.findForFusionByHistoriaId(8)).thenReturn(s);
    lenient().when(analisis.analizar(List.of(7,8))).thenReturn(AnalisisHistoriasClinicasDuplicadasResponse.builder().posiblesCoincidencias(List.of()).tokenAnalisis("token").build());
    lenient().when(consultas.countByHistoriaClinicaIdHistoriaClinica(8)).thenReturn(0L);
    lenient().when(consultas.countByHistoriaClinicaIdHistoriaClinica(7)).thenReturn((long)(p.size()+s.size()));
  }
  private void prepararLocks() {
    when(reautenticacion.reautenticar(eq(3), any())).thenReturn(ReautenticacionResponse.builder().cargo("ADMINISTRADOR").build());
    when(usuarios.findById(3)).thenReturn(Optional.of(usuario)); when(historias.findForFusionById(7)).thenReturn(Optional.of(principal)); when(historias.findForFusionById(8)).thenReturn(Optional.of(secundaria));
  }
  private FusionarHistoriasClinicasRequest request(long p,long s,List<Integer> ip,List<Integer> is) { FusionarHistoriasClinicasRequest r=new FusionarHistoriasClinicasRequest(); r.setIdHistoriaPrincipal(7);r.setContrasena("clave");r.setConfirmacion(true);r.setMotivo("DUPLICADA");r.setOrigen("CHATBOT");r.setCantidadEsperadaPrincipal(p);r.setCantidadEsperadaSecundaria(s);r.setIdsConsultasEsperadasPrincipal(ip);r.setIdsConsultasEsperadasSecundaria(is);r.setTokenAnalisis("token");return r; }
  private HistoriaClinica historia(int id, Paciente p){HistoriaClinica h=new HistoriaClinica();h.setIdHistoriaClinica(id);h.setPaciente(p);return h;}
  private Consulta consulta(int id,HistoriaClinica h,Paciente p){Consulta c=new Consulta();c.setIdConsulta(id);c.setHistoriaClinica(h);c.setPaciente(p);return c;}
}
