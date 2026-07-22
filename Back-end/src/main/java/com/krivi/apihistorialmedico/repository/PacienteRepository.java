package com.krivi.apihistorialmedico.repository;

import com.krivi.apihistorialmedico.model.entity.Paciente;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

  long countByFechaCreacionGreaterThanEqualAndFechaCreacionLessThan(LocalDateTime inicio, LocalDateTime fin);

  List<Paciente> findByFechaCreacionGreaterThanEqualAndFechaCreacionLessThanOrderByFechaCreacionDesc(LocalDateTime inicio, LocalDateTime fin);

  List<Paciente> findTop10ByOrderByFechaCreacionDesc();

  @Query("select p.numDocumento from Paciente p where p.numDocumento is not null and trim(p.numDocumento) <> '' group by p.numDocumento having count(p) > 1 order by p.numDocumento")
  List<String> findDnisDuplicados();

  List<Paciente> findByNumDocumentoOrderByIdPacienteAsc(String numDocumento);
}
