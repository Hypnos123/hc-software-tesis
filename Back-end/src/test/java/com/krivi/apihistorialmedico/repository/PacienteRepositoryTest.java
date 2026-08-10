package com.krivi.apihistorialmedico.repository;

import com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;

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
}
