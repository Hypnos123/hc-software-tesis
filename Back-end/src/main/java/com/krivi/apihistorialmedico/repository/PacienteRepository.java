package com.krivi.apihistorialmedico.repository;

import com.krivi.apihistorialmedico.model.entity.Paciente;
import org.springframework.data.repository.CrudRepository;

public interface PacienteRepository extends CrudRepository<Paciente, Integer> {
}
