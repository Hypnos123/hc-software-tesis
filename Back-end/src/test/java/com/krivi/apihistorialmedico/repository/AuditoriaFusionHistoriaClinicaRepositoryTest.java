package com.krivi.apihistorialmedico.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditoriaFusionHistoriaClinicaRepositoryTest {
  @Test
  void consultaPaginadaIncluyeFiltrosAdministrativosSinColecciones() throws Exception {
    Method method = AuditoriaFusionHistoriaClinicaRepository.class.getMethod("buscar", String.class, String.class,
        Integer.class, Integer.class, Integer.class, Integer.class, String.class, LocalDateTime.class,
        LocalDateTime.class, Pageable.class);
    String jpql = method.getAnnotation(Query.class).value().replaceAll("\\s+", " ").toLowerCase();
    assertTrue(jpql.contains("select a from auditoriafusionhistoriaclinica a"));
    assertTrue(jpql.contains("trim(a.paciente.numdocumento) = :dni"));
    assertTrue(jpql.contains("a.paciente.idpaciente = :idpaciente"));
    assertTrue(jpql.contains("a.historiaprincipal.idhistoriaclinica = :idhistoriaprincipal"));
    assertTrue(jpql.contains("a.idhistoriaeliminada = :idhistoriaeliminada"));
    assertTrue(jpql.contains("a.fecha >= :desde"));
    assertTrue(jpql.contains("a.fecha <= :hasta"));
    assertTrue(jpql.contains("a.usuario.usuario"));
  }
}
