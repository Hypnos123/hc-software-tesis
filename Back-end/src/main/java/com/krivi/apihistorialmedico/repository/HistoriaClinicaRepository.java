package com.krivi.apihistorialmedico.repository;

import com.krivi.apihistorialmedico.model.entity.HistoriaClinica;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HistoriaClinicaRepository extends CrudRepository<HistoriaClinica, Integer> {
  Optional<HistoriaClinica> findByPacienteIdPaciente(Integer idPaciente);
  boolean existsByPacienteIdPaciente(Integer idPaciente);
  long countByFechaCreacionGreaterThanEqualAndFechaCreacionLessThan(LocalDateTime inicio, LocalDateTime fin);

  @Query("select h from HistoriaClinica h join fetch h.paciente where h.idHistoriaClinica = :idHistoriaClinica")
  List<HistoriaClinica> findForIntegracionByIdHistoriaClinica(@Param("idHistoriaClinica") Integer idHistoriaClinica);

  @Query("select h from HistoriaClinica h join fetch h.paciente where h.paciente.idPaciente = :idPaciente")
  List<HistoriaClinica> findForIntegracionByIdPaciente(@Param("idPaciente") Integer idPaciente);

  @Query("select h from HistoriaClinica h join fetch h.paciente p where trim(p.numDocumento) = :dni")
  List<HistoriaClinica> findForIntegracionByDni(@Param("dni") String dni);

  @Query("select h from HistoriaClinica h join fetch h.paciente order by h.idHistoriaClinica")
  List<HistoriaClinica> findAllForIntegracion();

  @Query(value = "select h.idpaciente from historiaclinica h where h.idpaciente is not null group by h.idpaciente having count(*) > 1 order by h.idpaciente", nativeQuery = true)
  List<Integer> findIdsPacienteConHistoriasDuplicadas();

  @Query(value = "select trim(p.numdocumento) from historiaclinica h join paciente p on p.idpaciente = h.idpaciente where p.numdocumento is not null and trim(p.numdocumento) regexp '^[0-9]{8}$' group by trim(p.numdocumento) having count(*) > 1 order by trim(p.numdocumento)", nativeQuery = true)
  List<String> findDnisNormalizadosConHistoriasDuplicadas();
}
