package com.krivi.apihistorialmedico.repository;

import com.krivi.apihistorialmedico.model.entity.Empleado;
import org.springframework.data.repository.CrudRepository;

public interface EmpleadoRepository extends CrudRepository<Empleado, Integer> {

  Iterable<Empleado> findByEstadoTrue();
}
