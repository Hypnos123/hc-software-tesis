package com.krivi.apihistorialmedico.repository;

import com.krivi.apihistorialmedico.model.entity.AuditoriaFusionHistoriaClinica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AuditoriaFusionHistoriaClinicaRepository extends JpaRepository<AuditoriaFusionHistoriaClinica, Integer> {
  @EntityGraph(attributePaths = {"historiaPrincipal", "paciente", "usuario", "empleado"})
  @Query("""
      select a from AuditoriaFusionHistoriaClinica a
      where (:dni is null or trim(a.paciente.numDocumento) = :dni)
        and (:idPaciente is null or a.paciente.idPaciente = :idPaciente)
        and (:idHistoriaPrincipal is null or a.historiaPrincipal.idHistoriaClinica = :idHistoriaPrincipal)
        and (:idHistoriaEliminada is null or a.idHistoriaEliminada = :idHistoriaEliminada)
        and (:idUsuario is null or a.usuario.idUsuario = :idUsuario)
        and (:resultado is null or upper(a.resultado) = upper(:resultado))
        and (:desde is null or a.fecha >= :desde)
        and (:hasta is null or a.fecha <= :hasta)
        and (:search is null
          or lower(concat(coalesce(a.paciente.nombres, ''), ' ', coalesce(a.paciente.apellidos, ''))) like lower(concat('%', :search, '%'))
          or lower(concat(coalesce(a.paciente.apellidos, ''), ' ', coalesce(a.paciente.nombres, ''))) like lower(concat('%', :search, '%'))
          or trim(a.paciente.numDocumento) like concat('%', :search, '%')
          or lower(a.usuario.usuario) like lower(concat('%', :search, '%')))
      """)
  Page<AuditoriaFusionHistoriaClinica> buscar(@Param("search") String search, @Param("dni") String dni,
      @Param("idPaciente") Integer idPaciente, @Param("idHistoriaPrincipal") Integer idHistoriaPrincipal,
      @Param("idHistoriaEliminada") Integer idHistoriaEliminada, @Param("idUsuario") Integer idUsuario,
      @Param("resultado") String resultado, @Param("desde") LocalDateTime desde,
      @Param("hasta") LocalDateTime hasta, Pageable pageable);

  @EntityGraph(attributePaths = {"historiaPrincipal", "paciente", "usuario", "empleado"})
  @Query("select a from AuditoriaFusionHistoriaClinica a where a.idAuditoria = :idAuditoria")
  Optional<AuditoriaFusionHistoriaClinica> buscarDetalle(@Param("idAuditoria") Integer idAuditoria);
}
