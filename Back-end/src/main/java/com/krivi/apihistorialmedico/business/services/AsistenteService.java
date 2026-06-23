package com.krivi.apihistorialmedico.business.services;

import com.krivi.apihistorialmedico.model.api.AsistenteRequest;
import com.krivi.apihistorialmedico.model.api.AsistenteResponse;

public interface AsistenteService {
  AsistenteResponse preguntar(AsistenteRequest request, Integer idUsuario);
}
