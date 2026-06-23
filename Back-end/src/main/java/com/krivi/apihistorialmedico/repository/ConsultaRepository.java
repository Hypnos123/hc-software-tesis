package com.krivi.apihistorialmedico.repository;

import com.krivi.apihistorialmedico.model.entity.Consulta;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ConsultaRepository extends CrudRepository<Consulta, Integer> {
  List<Consulta> findByHistoriaClinicaIdHistoriaClinica(Integer idHistoriaClinica);
  List<Consulta> findByDoctorResponsableIdEmpleado(Integer idEmpleado);
}
