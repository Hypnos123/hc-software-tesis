package com.krivi.apihistorialmedico.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultaResumenRepositoryTest {
  @Test void conteoDelResumenFiltraDirectamentePorPacienteYSoloAtendido() throws Exception {
    Method method = ConsultaRepository.class.getMethod("resumirAtendidasByPacienteId", Integer.class);
    String jpql = method.getAnnotation(Query.class).value().replaceAll("\\s+", " ").toLowerCase();

    assertTrue(jpql.contains("c.paciente.idpaciente = :idpaciente"));
    assertTrue(jpql.contains("upper(trim(c.estado)) = 'atendido'"));
    assertFalse(jpql.contains("historiaclinica.idhistoriaclinica"));
    assertFalse(jpql.contains("pendiente"));
  }

  @Test void todasLasConsultasDeFaseDosMantienenPacienteYEstadoAtendido() throws Exception {
    List<Method> metodos = List.of(
        ConsultaRepository.class.getMethod("contarTiposAtendidosByPacienteId", Integer.class),
        ConsultaRepository.class.getMethod("contarEspecialidadesAtendidasByPacienteId", Integer.class),
        ConsultaRepository.class.getMethod("findRecientesAtendidasByPacienteId", Integer.class,
            org.springframework.data.domain.Pageable.class),
        ConsultaRepository.class.getMethod("findProximasCitasAtendidasByPacienteId", Integer.class,
            java.util.Date.class, org.springframework.data.domain.Pageable.class),
        ConsultaRepository.class.getMethod("resumirCalidadAtendidasByPacienteId", Integer.class));

    for (Method metodo : metodos) {
      String jpql = metodo.getAnnotation(Query.class).value().replaceAll("\\s+", " ").toLowerCase();
      assertTrue(jpql.contains("c.paciente.idpaciente = :idpaciente"), metodo.getName());
      assertTrue(jpql.contains("upper(trim(c.estado)) = 'atendido'"), metodo.getName());
    }
  }
}
