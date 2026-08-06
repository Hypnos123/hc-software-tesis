package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.services.AsistenteService;
import com.krivi.apihistorialmedico.model.api.AsistenteRequest;
import com.krivi.apihistorialmedico.model.api.AsistenteResponse;
import com.krivi.apihistorialmedico.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/asistente")
public class AsistenteController {
  @Autowired AsistenteService asistenteService;
  @Autowired UsuarioRepository usuarioRepository;

  @PostMapping("/preguntar")
  public ResponseEntity<AsistenteResponse> preguntar(@RequestBody AsistenteRequest request, @RequestHeader(value = "X-Usuario-Id", required = false) Integer idUsuario) {
    if (idUsuario == null || idUsuario <= 0 || usuarioRepository.findById(idUsuario).isEmpty()) {
      return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
          .body(AsistenteResponse.builder().intencion("NO_AUTENTICADO")
              .respuesta("Debes iniciar sesión para utilizar el asistente.").datos(java.util.Map.of()).build());
    }
    return ResponseEntity.ok(asistenteService.preguntar(request, idUsuario));
  }
}
