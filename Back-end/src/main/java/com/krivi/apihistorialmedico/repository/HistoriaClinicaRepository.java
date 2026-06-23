package com.krivi.apihistorialmedico.repository;

import com.krivi.apihistorialmedico.model.entity.HistoriaClinica;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface HistoriaClinicaRepository extends CrudRepository<HistoriaClinica, Integer> {
  Optional<HistoriaClinica> findByPacienteIdPaciente(Integer idPaciente);
  boolean existsByPacienteIdPaciente(Integer idPaciente);
  long countByFechaCreacionGreaterThanEqualAndFechaCreacionLessThan(LocalDateTime inicio, LocalDateTime fin);
}
