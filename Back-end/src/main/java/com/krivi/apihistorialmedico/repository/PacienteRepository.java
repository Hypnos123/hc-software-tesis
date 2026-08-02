package com.krivi.apihistorialmedico.repository;

import com.krivi.apihistorialmedico.model.entity.Paciente;
import com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PacienteRepository extends CrudRepository<Paciente, Integer> {

  List<Paciente> findByNumDocumentoAndEstadoRegistroOrderByIdPacienteAsc(String numDocumento, EstadoRegistroPaciente estadoRegistro);

  Optional<Paciente> findByIdPacienteAndEstadoRegistro(Integer idPaciente, EstadoRegistroPaciente estadoRegistro);

  List<Paciente> findAllByEstadoRegistroOrderByIdPacienteAsc(EstadoRegistroPaciente estadoRegistro);

  @Query(value = "SELECT * FROM paciente p WHERE p.estadoregistro = 'ACTIVO' AND (LOWER(CONCAT(p.apellidos, ' ', p.nombres)) LIKE LOWER(CONCAT('%', :term, '%')) OR LOWER(CONCAT(p.nombres, ' ', p.apellidos)) LIKE LOWER(CONCAT('%', :term, '%'))) LIMIT :limit", nativeQuery = true)
  List<Paciente> searchByNombre(@Param("term") String term, @Param("limit") int limit);

  @Query("select p from Paciente p where p.estadoRegistro = com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente.ACTIVO and lower(concat(coalesce(p.nombres, ''), ' ', coalesce(p.apellidos, ''))) like lower(concat('%', :term, '%'))")
  List<Paciente> searchByNombreToken(@Param("term") String term);

  @Query(value = "SELECT * FROM paciente p WHERE p.estadoregistro = 'ACTIVO' AND p.numdocumento = :dni LIMIT :limit", nativeQuery = true)
  List<Paciente> searchByDni(@Param("dni") String dni, @Param("limit") int limit);

  long countByFechaIngresoGreaterThanEqualAndFechaIngresoLessThan(Date inicio, Date fin);
  long countByEstadoRegistroAndFechaIngresoGreaterThanEqualAndFechaIngresoLessThan(EstadoRegistroPaciente estadoRegistro, Date inicio, Date fin);

  long countByEstadoRegistroAndFechaCreacionGreaterThanEqualAndFechaCreacionLessThan(EstadoRegistroPaciente estadoRegistro, LocalDateTime inicio, LocalDateTime fin);

  List<Paciente> findByEstadoRegistroAndFechaCreacionGreaterThanEqualAndFechaCreacionLessThanOrderByFechaCreacionDesc(EstadoRegistroPaciente estadoRegistro, LocalDateTime inicio, LocalDateTime fin);

  List<Paciente> findTop10ByEstadoRegistroOrderByFechaCreacionDesc(EstadoRegistroPaciente estadoRegistro);

  long countByEstadoRegistro(EstadoRegistroPaciente estadoRegistro);

  @Query("select p.numDocumento from Paciente p where p.estadoRegistro = com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente.ACTIVO and p.numDocumento is not null and trim(p.numDocumento) <> '' group by p.numDocumento having count(p) > 1 order by p.numDocumento")
  List<String> findDnisDuplicados();

  @Query("select p from Paciente p where p.estadoRegistro = com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente.ACTIVO and trim(p.numDocumento) = :dni order by p.idPaciente")
  List<Paciente> findByDniNormalizado(@Param("dni") String dni);

  @Query("select distinct trim(p.numDocumento) from Paciente p where trim(p.numDocumento) in :dnis")
  Set<String> findDnisExistentes(@Param("dnis") java.util.Collection<String> dnis);

  @Query("select distinct trim(p.numDocumento) from Paciente p where p.estadoRegistro = com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente.ACTIVO and trim(p.numDocumento) in :dnis")
  Set<String> findDnisActivos(@Param("dnis") java.util.Collection<String> dnis);

  @Query("select distinct trim(p.numDocumento) from Paciente p where p.estadoRegistro = com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente.ARCHIVADO and trim(p.numDocumento) in :dnis")
  Set<String> findDnisArchivados(@Param("dnis") java.util.Collection<String> dnis);
}
