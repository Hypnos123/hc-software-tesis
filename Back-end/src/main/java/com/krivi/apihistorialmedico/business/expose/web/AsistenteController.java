package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.services.AsistenteService;
import com.krivi.apihistorialmedico.model.api.AsistenteRequest;
import com.krivi.apihistorialmedico.model.api.AsistenteResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/asistente")
public class AsistenteController {
  @Autowired AsistenteService asistenteService;

  @PostMapping("/preguntar")
  public ResponseEntity<AsistenteResponse> preguntar(@RequestBody AsistenteRequest request, @RequestHeader(value = "X-Usuario-Id", required = false) Integer idUsuario) {
    return ResponseEntity.ok(asistenteService.preguntar(request, idUsuario));
  }
}
