package com.krivi.apihistorialmedico.business.services;



import com.krivi.apihistorialmedico.model.api.MenuResponse;

import java.util.List;

public interface MenuService {

  List<MenuResponse> getAllActive();

}
