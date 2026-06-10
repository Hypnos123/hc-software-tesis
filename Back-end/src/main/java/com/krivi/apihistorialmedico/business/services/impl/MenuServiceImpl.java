package com.krivi.apihistorialmedico.business.services.impl;


import com.krivi.apihistorialmedico.business.services.MenuService;
import com.krivi.apihistorialmedico.model.api.MenuResponse;
import com.krivi.apihistorialmedico.repository.MenuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MenuServiceImpl implements MenuService {

  @Autowired
  MenuRepository menuRepository;

  @Override
  public List<MenuResponse> getAllActive() {

    List<MenuResponse> menuResponseList = new ArrayList<>();
    menuRepository.findByEstadoOrderByIdMenu(true).forEach(menu -> {
      menuResponseList.add(MenuResponse.builder()
          .idMenu(menu.getIdMenu())
          .nombre(menu.getNombre())
          .ruta(menu.getRuta())
          .imagen(menu.getImagen())
          .estado(menu.getEstado())
          .build());
    });

    return menuResponseList;
  }

}
