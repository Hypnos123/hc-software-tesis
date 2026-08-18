package com.krivi.apihistorialmedico.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultaResumenRepositoryTest {
  @Test void conteoDelResumenFiltraDirectamentePorPacienteYSoloAtendido() throws Exception {
    Method method = ConsultaRepository.class.getMethod("countAtendidasByPacienteId", Integer.class);
    String jpql = method.getAnnotation(Query.class).value().replaceAll("\\s+", " ").toLowerCase();

    assertTrue(jpql.contains("c.paciente.idpaciente = :idpaciente"));
    assertTrue(jpql.contains("upper(trim(c.estado)) = 'atendido'"));
    assertFalse(jpql.contains("historiaclinica.idhistoriaclinica"));
    assertFalse(jpql.contains("pendiente"));
  }
}
