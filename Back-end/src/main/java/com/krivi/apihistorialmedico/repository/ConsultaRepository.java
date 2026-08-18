package com.krivi.apihistorialmedico.repository;

import com.krivi.apihistorialmedico.model.entity.Consulta;
import com.krivi.apihistorialmedico.model.projection.ConsultaResumenRecienteProjection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Collection;
import java.util.Optional;

public interface ConsultaRepository extends CrudRepository<Consulta, Integer> {
  @Query("select c.paciente.idPaciente, count(c), max(coalesce(c.fechaAtencion, c.fechaCreacion)) from Consulta c where c.paciente.idPaciente in :idsPaciente group by c.paciente.idPaciente")
  List<Object[]> resumirPorPacientes(@Param("idsPaciente") Collection<Integer> idsPaciente);
  @Query("select c.historiaClinica.idHistoriaClinica, count(c), max(coalesce(c.fechaAtencion, c.fechaCreacion)) from Consulta c where c.historiaClinica.idHistoriaClinica in :idsHistoria group by c.historiaClinica.idHistoriaClinica")
  List<Object[]> resumirPorHistoriasClinicas(@Param("idsHistoria") Collection<Integer> idsHistoria);
  List<Consulta> findByHistoriaClinicaIdHistoriaClinica(Integer idHistoriaClinica);
  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from Consulta c join fetch c.paciente left join fetch c.doctorResponsable left join fetch c.tipoEnfermedad where c.historiaClinica.idHistoriaClinica = :idHistoria order by c.idConsulta")
  List<Consulta> findForFusionByHistoriaId(@Param("idHistoria") Integer idHistoria);
  long countByHistoriaClinicaIdHistoriaClinica(Integer idHistoriaClinica);
  @Query(value = "select * from consulta c where c.idhistoriaclinica = :idHistoriaClinica order by coalesce(c.fechaconsulta, c.fechacreacion) desc", nativeQuery = true)
  List<Consulta> findByHistoriaClinicaOrdenadasPorFecha(@Param("idHistoriaClinica") Integer idHistoriaClinica);
  List<Consulta> findByHistoriaClinicaIdHistoriaClinicaAndEstadoIgnoreCaseOrderByFechaCreacionAsc(Integer idHistoriaClinica, String estado);
  @Query(value = "select * from consulta c where c.idhistoriaclinica = :idHistoriaClinica order by coalesce(c.fechaconsulta, c.fechacreacion) desc limit 1", nativeQuery = true)
  Optional<Consulta> findUltimaByHistoriaClinica(@Param("idHistoriaClinica") Integer idHistoriaClinica);
  List<Consulta> findByDoctorResponsableIdEmpleado(Integer idEmpleado);
  long countByFechaCreacionGreaterThanEqualAndFechaCreacionLessThan(LocalDateTime inicio, LocalDateTime fin);
  long countByEstado(String estado);
  long countByEstadoAndFechaCreacionGreaterThanEqualAndFechaCreacionLessThan(String estado, LocalDateTime inicio, LocalDateTime fin);
  @Query("select count(c) from Consulta c where upper(c.estado) = upper(:estado) and c.fechaAtencion >= :inicio and c.fechaAtencion < :fin")
  long countByEstadoAndFechaAtencionGreaterThanEqualAndFechaAtencionLessThan(@Param("estado") String estado, @Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
  @Query("select c from Consulta c join fetch c.paciente left join fetch c.doctorResponsable where upper(c.estado) = upper(:estado) and c.fechaAtencion >= :inicio and c.fechaAtencion < :fin order by c.fechaAtencion desc")
  List<Consulta> findAtendidasPorFechaAtencion(@Param("estado") String estado, @Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
  long countByDoctorResponsableIdEmpleado(Integer idEmpleado);
  long countByDoctorResponsableIdEmpleadoAndEstado(Integer idEmpleado, String estado);
  long countByPacienteIdPaciente(Integer idPaciente);
  long countByPacienteIdPacienteAndEstado(Integer idPaciente, String estado);
  @Query("select count(c), min(c.fechaAtencion), max(c.fechaAtencion) from Consulta c where c.paciente.idPaciente = :idPaciente and upper(trim(c.estado)) = 'ATENDIDO'")
  List<Object[]> resumirAtendidasByPacienteId(@Param("idPaciente") Integer idPaciente);
  @Query("select c.tipoEnfermedad.idTipoEnfermedad, c.tipoEnfermedad.descripcion, count(c) from Consulta c where c.paciente.idPaciente = :idPaciente and upper(trim(c.estado)) = 'ATENDIDO' and c.tipoEnfermedad is not null group by c.tipoEnfermedad.idTipoEnfermedad, c.tipoEnfermedad.descripcion order by count(c) desc, c.tipoEnfermedad.descripcion")
  List<Object[]> contarTiposAtendidosByPacienteId(@Param("idPaciente") Integer idPaciente);
  @Query("select c.especialidadRequerida, count(c) from Consulta c where c.paciente.idPaciente = :idPaciente and upper(trim(c.estado)) = 'ATENDIDO' and c.especialidadRequerida is not null and trim(c.especialidadRequerida) <> '' group by c.especialidadRequerida order by count(c) desc, c.especialidadRequerida")
  List<Object[]> contarEspecialidadesAtendidasByPacienteId(@Param("idPaciente") Integer idPaciente);
  @Query("""
      select c.idConsulta as idConsulta,
             c.historiaClinica.idHistoriaClinica as idHistoriaClinica,
             c.fechaAtencion as fechaAtencion,
             c.especialidadRequerida as especialidad,
             concat(coalesce(c.doctorResponsable.nombres, ''), ' ', coalesce(c.doctorResponsable.apellidos, '')) as doctor,
             c.relatoPaciente as relatoPaciente,
             c.diagnostico as diagnostico,
             c.examenesRecetados as examenesRecetados,
             c.receta as receta,
             c.tratamiento as tratamiento,
             c.proximaCita as proximaCita
        from Consulta c
       where c.paciente.idPaciente = :idPaciente
         and upper(trim(c.estado)) = 'ATENDIDO'
       order by c.fechaAtencion desc, c.idConsulta desc
      """)
  List<ConsultaResumenRecienteProjection> findRecientesAtendidasByPacienteId(
      @Param("idPaciente") Integer idPaciente, Pageable pageable);
  @Query("select distinct c.proximaCita from Consulta c where c.paciente.idPaciente = :idPaciente and upper(trim(c.estado)) = 'ATENDIDO' and c.proximaCita >= :hoy order by c.proximaCita")
  List<java.util.Date> findProximasCitasAtendidasByPacienteId(@Param("idPaciente") Integer idPaciente,
      @Param("hoy") java.util.Date hoy, Pageable pageable);
  @Query("""
      select sum(case when c.fechaAtencion is null and c.fechaConsulta is null and c.fechaCreacion is null then 1 else 0 end),
             sum(case when c.tipoEnfermedad is null then 1 else 0 end),
             sum(case when c.especialidadRequerida is null or trim(c.especialidadRequerida) = '' then 1 else 0 end),
             sum(case when c.historiaClinica is null or c.historiaClinica.paciente.idPaciente <> c.paciente.idPaciente then 1 else 0 end)
        from Consulta c
       where c.paciente.idPaciente = :idPaciente
         and upper(trim(c.estado)) = 'ATENDIDO'
      """)
  List<Object[]> resumirCalidadAtendidasByPacienteId(@Param("idPaciente") Integer idPaciente);
  long countByFechaAtencionGreaterThanEqualAndFechaAtencionLessThan(LocalDateTime inicio, LocalDateTime fin);
  @Query("select c from Consulta c join fetch c.paciente left join fetch c.historiaClinica left join fetch c.doctorResponsable where c.paciente.idPaciente = :idPaciente order by c.fechaCreacion desc")
  List<Consulta> findAdministrativasRecientesByPacienteId(@Param("idPaciente") Integer idPaciente, Pageable pageable);
  @Query("select c from Consulta c join fetch c.paciente left join fetch c.historiaClinica left join fetch c.doctorResponsable where c.estado = 'PENDIENTE' order by c.fechaCreacion asc")
  List<Consulta> findPendientesAdministrativas();
  @Query("select c from Consulta c join fetch c.paciente left join fetch c.historiaClinica left join fetch c.doctorResponsable order by c.fechaCreacion desc")
  List<Consulta> findUltimasAdministrativas(Pageable pageable);
  @Query("select count(c) from Consulta c where c.estado = 'PENDIENTE' or c.diagnostico is null or trim(c.diagnostico) = '' or c.tratamiento is null or trim(c.tratamiento) = ''") long countIncompletas();
  @Query("select c from Consulta c where c.estado = 'PENDIENTE' or c.diagnostico is null or trim(c.diagnostico) = '' or c.tratamiento is null or trim(c.tratamiento) = '' order by c.fechaCreacion desc") List<Consulta> findIncompletas();
  @Query("select c.especialidadRequerida, count(c) from Consulta c where c.especialidadRequerida is not null and trim(c.especialidadRequerida) <> '' group by c.especialidadRequerida order by count(c) desc") List<Object[]> rankingEspecialidades();
  @Query("select c.especialidadRequerida, count(c) from Consulta c where c.especialidadRequerida is not null and trim(c.especialidadRequerida) <> '' and c.fechaCreacion >= :inicio and c.fechaCreacion < :fin group by c.especialidadRequerida order by count(c) desc") List<Object[]> rankingEspecialidades(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
  @Query("select c.tipoEnfermedad.descripcion, count(c) from Consulta c where c.tipoEnfermedad is not null group by c.tipoEnfermedad.descripcion order by count(c) desc") List<Object[]> rankingTiposEnfermedad();
  @Query("select c.tipoEnfermedad.descripcion, count(c) from Consulta c where c.tipoEnfermedad is not null and c.fechaCreacion >= :inicio and c.fechaCreacion < :fin group by c.tipoEnfermedad.descripcion order by count(c) desc") List<Object[]> rankingTiposEnfermedad(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
  @Query("select concat(c.doctorResponsable.nombres, ' ', c.doctorResponsable.apellidos), count(c) from Consulta c where c.estado = 'ATENDIDO' group by c.doctorResponsable.idEmpleado, c.doctorResponsable.nombres, c.doctorResponsable.apellidos order by count(c) desc") List<Object[]> rankingDoctoresAtenciones();
}
