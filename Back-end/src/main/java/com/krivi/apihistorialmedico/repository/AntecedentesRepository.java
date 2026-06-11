package com.krivi.apihistorialmedico.repository;

import com.krivi.apihistorialmedico.model.entity.Antecedentes;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface AntecedentesRepository extends CrudRepository<Antecedentes, Integer> {

  List<Antecedentes> findByPacienteIdPaciente(Integer idPaciente);
}
