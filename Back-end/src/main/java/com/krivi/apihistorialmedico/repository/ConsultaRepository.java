package com.krivi.apihistorialmedico.repository;

import com.krivi.apihistorialmedico.model.entity.Consulta;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ConsultaRepository extends CrudRepository<Consulta, Integer> {
  List<Consulta> findByHistoriaClinicaIdHistoriaClinica(Integer idHistoriaClinica);
  List<Consulta> findByDoctorResponsableIdEmpleado(Integer idEmpleado);
  long countByFechaCreacionGreaterThanEqualAndFechaCreacionLessThan(LocalDateTime inicio, LocalDateTime fin);
  long countByEstado(String estado);
  long countByEstadoAndFechaCreacionGreaterThanEqualAndFechaCreacionLessThan(String estado, LocalDateTime inicio, LocalDateTime fin);
  long countByDoctorResponsableIdEmpleado(Integer idEmpleado);
  long countByDoctorResponsableIdEmpleadoAndEstado(Integer idEmpleado, String estado);
  @Query("select count(c) from Consulta c where c.estado = 'PENDIENTE' or c.diagnostico is null or trim(c.diagnostico) = '' or c.tratamiento is null or trim(c.tratamiento) = ''") long countIncompletas();
  @Query("select c from Consulta c where c.estado = 'PENDIENTE' or c.diagnostico is null or trim(c.diagnostico) = '' or c.tratamiento is null or trim(c.tratamiento) = '' order by c.fechaCreacion desc") List<Consulta> findIncompletas();
  @Query("select c.especialidadRequerida, count(c) from Consulta c where c.especialidadRequerida is not null and trim(c.especialidadRequerida) <> '' group by c.especialidadRequerida order by count(c) desc") List<Object[]> rankingEspecialidades();
  @Query("select c.especialidadRequerida, count(c) from Consulta c where c.especialidadRequerida is not null and trim(c.especialidadRequerida) <> '' and c.fechaCreacion >= :inicio and c.fechaCreacion < :fin group by c.especialidadRequerida order by count(c) desc") List<Object[]> rankingEspecialidades(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
  @Query("select c.tipoEnfermedad.descripcion, count(c) from Consulta c where c.tipoEnfermedad is not null group by c.tipoEnfermedad.descripcion order by count(c) desc") List<Object[]> rankingTiposEnfermedad();
  @Query("select c.tipoEnfermedad.descripcion, count(c) from Consulta c where c.tipoEnfermedad is not null and c.fechaCreacion >= :inicio and c.fechaCreacion < :fin group by c.tipoEnfermedad.descripcion order by count(c) desc") List<Object[]> rankingTiposEnfermedad(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
  @Query("select concat(c.doctorResponsable.nombres, ' ', c.doctorResponsable.apellidos), count(c) from Consulta c where c.estado = 'ATENDIDO' group by c.doctorResponsable.idEmpleado, c.doctorResponsable.nombres, c.doctorResponsable.apellidos order by count(c) desc") List<Object[]> rankingDoctoresAtenciones();
}
