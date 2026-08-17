package com.krivi.apihistorialmedico.repository;

import com.krivi.apihistorialmedico.model.entity.AuditoriaArchivadoPaciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Collection;

public interface AuditoriaArchivadoPacienteRepository extends JpaRepository<AuditoriaArchivadoPaciente, Integer> {
  @Query("""
      select a from AuditoriaArchivadoPaciente a
      join fetch a.pacienteArchivado
      join fetch a.pacientePrincipal
      join fetch a.usuario
      join fetch a.empleado
      where (:dni is null or a.dni = :dni)
        and (:idPaciente is null or a.pacienteArchivado.idPaciente = :idPaciente
             or a.pacientePrincipal.idPaciente = :idPaciente)
        and (:desde is null or a.fecha >= :desde)
        and (:hasta is null or a.fecha <= :hasta)
      order by a.fecha desc, a.idAuditoria desc
      """)
  List<AuditoriaArchivadoPaciente> buscar(
      @Param("dni") String dni,
      @Param("idPaciente") Integer idPaciente,
      @Param("desde") LocalDateTime desde,
      @Param("hasta") LocalDateTime hasta);

  @Query("""
      select a from AuditoriaArchivadoPaciente a
      join fetch a.usuario
      join fetch a.empleado
      where a.pacienteArchivado.idPaciente in :idsPaciente
        and not exists (
          select a2.idAuditoria from AuditoriaArchivadoPaciente a2
          where a2.pacienteArchivado.idPaciente = a.pacienteArchivado.idPaciente
            and (a2.fecha > a.fecha or (a2.fecha = a.fecha and a2.idAuditoria > a.idAuditoria))
        )
      """)
  List<AuditoriaArchivadoPaciente> buscarUltimasPorPacientes(@Param("idsPaciente") Collection<Integer> idsPaciente);

  @Query("""
      select a from AuditoriaArchivadoPaciente a
      join fetch a.usuario
      join fetch a.empleado
      join fetch a.pacientePrincipal
      where a.pacienteArchivado.idPaciente = :idPaciente
      order by a.fecha desc, a.idAuditoria desc
      """)
  List<AuditoriaArchivadoPaciente> buscarPorPacienteMasReciente(
      @Param("idPaciente") Integer idPaciente, Pageable pageable);
}
