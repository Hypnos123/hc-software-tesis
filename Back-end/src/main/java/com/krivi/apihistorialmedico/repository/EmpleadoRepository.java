package com.krivi.apihistorialmedico.repository;

import com.krivi.apihistorialmedico.model.entity.Empleado;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface EmpleadoRepository extends CrudRepository<Empleado, Integer> {

  Iterable<Empleado> findByEstadoTrue();

  @Query("select distinct e from Empleado e left join e.usuarios u where e.estado = true and (upper(e.cargo) in ('DOCTOR', 'MEDICO', 'MÉDICO') or upper(u.tipoUsuario) = 'DOCTOR')")
  List<Empleado> findDoctoresActivos();
}
