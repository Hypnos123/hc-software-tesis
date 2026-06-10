package com.krivi.apihistorialmedico.repository;

import com.krivi.apihistorialmedico.model.entity.Antecedentes;
import com.krivi.apihistorialmedico.model.entity.Consulta;
import org.springframework.data.repository.CrudRepository;

public interface ConsultaRepository extends CrudRepository<Consulta, Integer> {
}
