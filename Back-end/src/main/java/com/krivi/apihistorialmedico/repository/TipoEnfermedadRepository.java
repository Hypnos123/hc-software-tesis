package com.krivi.apihistorialmedico.repository;

import com.krivi.apihistorialmedico.model.entity.TipoEnfermedad;
import org.springframework.data.repository.CrudRepository;

public interface TipoEnfermedadRepository extends CrudRepository<TipoEnfermedad, Integer> {
  java.util.Optional<TipoEnfermedad> findByDescripcionIgnoreCase(String descripcion);
}
