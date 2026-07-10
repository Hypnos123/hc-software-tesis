package com.krivi.apihistorialmedico.repository;

import com.krivi.apihistorialmedico.model.entity.Paciente;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface PacienteRepository extends CrudRepository<Paciente, Integer> {

  Optional<Paciente> findByNumDocumento(String numDocumento);

  @Query(value = "SELECT * FROM paciente p WHERE LOWER(CONCAT(p.apellidos, ' ', p.nombres)) LIKE LOWER(CONCAT('%', :term, '%')) OR LOWER(CONCAT(p.nombres, ' ', p.apellidos)) LIKE LOWER(CONCAT('%', :term, '%')) LIMIT :limit", nativeQuery = true)
  List<Paciente> searchByNombre(@Param("term") String term, @Param("limit") int limit);

  @Query(value = "SELECT * FROM paciente p WHERE p.numdocumento = :dni LIMIT :limit", nativeQuery = true)
  List<Paciente> searchByDni(@Param("dni") String dni, @Param("limit") int limit);

  long countByFechaIngresoGreaterThanEqualAndFechaIngresoLessThan(Date inicio, Date fin);
}
