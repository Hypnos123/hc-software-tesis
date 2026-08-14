package com.krivi.apihistorialmedico.repository;

import com.krivi.apihistorialmedico.model.entity.HistoriaClinica;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface HistoriaClinicaRepository extends CrudRepository<HistoriaClinica, Integer> {
  @Query("select h.paciente.idPaciente, count(h), max(h.ultimaActualizacion) from HistoriaClinica h where h.paciente.idPaciente in :idsPaciente group by h.paciente.idPaciente")
  List<Object[]> resumirPorPacientes(@Param("idsPaciente") Collection<Integer> idsPaciente);
  List<HistoriaClinica> findAllByPacienteEstadoRegistroOrderByIdHistoriaClinicaAsc(com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente estadoRegistro);
  Optional<HistoriaClinica> findByIdHistoriaClinicaAndPacienteEstadoRegistro(Integer idHistoriaClinica, com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente estadoRegistro);
  @Query(value = "select h.* from historiaclinica h join paciente p on p.idpaciente = h.idpaciente where h.idpaciente = :idPaciente and p.estadoregistro = 'ACTIVO' order by h.idhistoriaclinica desc limit 1", nativeQuery = true)
  Optional<HistoriaClinica> findByPacienteIdPaciente(@Param("idPaciente") Integer idPaciente);
  List<HistoriaClinica> findAllByPacienteIdPacienteOrderByIdHistoriaClinicaAsc(Integer idPaciente);
  List<HistoriaClinica> findAllByPacienteIdPacienteAndPacienteEstadoRegistroOrderByIdHistoriaClinicaAsc(Integer idPaciente, com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente estadoRegistro);
  boolean existsByPacienteIdPaciente(Integer idPaciente);
  long countByFechaCreacionGreaterThanEqualAndFechaCreacionLessThan(LocalDateTime inicio, LocalDateTime fin);

  @Query("select h from HistoriaClinica h join fetch h.paciente p where h.idHistoriaClinica = :idHistoriaClinica and p.estadoRegistro = com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente.ACTIVO")
  List<HistoriaClinica> findForIntegracionByIdHistoriaClinica(@Param("idHistoriaClinica") Integer idHistoriaClinica);

  @Query("select h from HistoriaClinica h join fetch h.paciente p where h.paciente.idPaciente = :idPaciente and p.estadoRegistro = com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente.ACTIVO")
  List<HistoriaClinica> findForIntegracionByIdPaciente(@Param("idPaciente") Integer idPaciente);

  @Query("select h from HistoriaClinica h join fetch h.paciente p where trim(p.numDocumento) = :dni and p.estadoRegistro = com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente.ACTIVO")
  List<HistoriaClinica> findForIntegracionByDni(@Param("dni") String dni);

  @Query("select h from HistoriaClinica h join fetch h.paciente p where p.estadoRegistro = com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente.ACTIVO order by h.idHistoriaClinica")
  List<HistoriaClinica> findAllForIntegracion();

  @Query("select h from HistoriaClinica h join fetch h.paciente p where h.idHistoriaClinica in :idsHistoria and p.estadoRegistro = com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente.ACTIVO")
  List<HistoriaClinica> findForAnalisisByIds(@Param("idsHistoria") Collection<Integer> idsHistoria);

  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select h from HistoriaClinica h join fetch h.paciente p where h.idHistoriaClinica = :idHistoria")
  Optional<HistoriaClinica> findForFusionById(@Param("idHistoria") Integer idHistoria);

  @Query(value = "select h.idpaciente from historiaclinica h where h.idpaciente is not null group by h.idpaciente having count(*) > 1 order by h.idpaciente", nativeQuery = true)
  List<Integer> findIdsPacienteConHistoriasDuplicadas();

  @Query(value = "select trim(p.numdocumento) from historiaclinica h join paciente p on p.idpaciente = h.idpaciente where p.estadoregistro = 'ACTIVO' and p.numdocumento is not null and trim(p.numdocumento) regexp '^[0-9]{8}$' group by trim(p.numdocumento) having count(*) > 1 order by trim(p.numdocumento)", nativeQuery = true)
  List<String> findDnisNormalizadosConHistoriasDuplicadas();
}
