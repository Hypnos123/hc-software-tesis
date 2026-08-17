package com.krivi.apihistorialmedico.repository;

import com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PacienteRepositoryTest {

  @Test
  void consultaPendientesExigeEstadoUsaNotExistsYOrdenaPorIdPaciente() throws Exception {
    Method method = PacienteRepository.class.getMethod(
        "findByEstadoRegistroAndSinHistoriaClinica", EstadoRegistroPaciente.class);
    String jpql = method.getAnnotation(Query.class).value()
        .replaceAll("\\s+", " ")
        .trim()
        .toLowerCase();

    assertTrue(jpql.contains("p.estadoregistro = :estado"));
    assertTrue(jpql.contains("not exists"));
    assertTrue(jpql.contains("from historiaclinica h"));
    assertTrue(jpql.contains("h.paciente.idpaciente = p.idpaciente"));
    assertTrue(jpql.endsWith("order by p.idpaciente"));
  }

  @Test
  void consultaAdministrativaParteDePacienteYExcluyeActivos() throws Exception {
    Method method = PacienteRepository.class.getMethod("buscarArchivados", String.class, String.class,
        Integer.class, LocalDateTime.class, LocalDateTime.class, Pageable.class);
    String jpql = method.getAnnotation(Query.class).value().replaceAll("\\s+", " ").toLowerCase();
    assertTrue(jpql.contains("select p from paciente p"));
    assertTrue(jpql.contains("p.estadoregistro = com.krivi.apihistorialmedico.model.entity.estadoregistropaciente.archivado"));
    assertTrue(jpql.contains("p.idpaciente = :idpaciente"));
    assertTrue(jpql.contains("trim(p.numdocumento) = :dni"));
    assertTrue(jpql.contains("p.fechaarchivado >= :desde"));
    assertTrue(jpql.contains("p.fechaarchivado <= :hasta"));
    assertTrue(jpql.contains("p.nombres"));
    assertTrue(jpql.contains("p.apellidos"));
  }
}
