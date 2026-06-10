package com.krivi.apihistorialmedico.repository;


import com.krivi.apihistorialmedico.model.entity.Menu;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuRepository extends CrudRepository<Menu,Integer> {

  List<Menu> findByEstadoOrderByIdMenu(Boolean estado);
}
