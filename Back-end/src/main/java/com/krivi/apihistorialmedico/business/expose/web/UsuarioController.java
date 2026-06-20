package com.krivi.apihistorialmedico.business.expose.web;


import com.krivi.apihistorialmedico.business.services.UsuarioService;
import com.krivi.apihistorialmedico.model.api.*;
import com.krivi.apihistorialmedico.util.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/usuario")
@Slf4j
@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST,RequestMethod.PUT})
public class UsuarioController {

  @Autowired
  private UsuarioService usuarioService;

  @GetMapping("/getAllActive")
  public ResponseEntity<ResponseModelGet<UsuarioResponse>> getAllActive() {
    ResponseModelGet<UsuarioResponse> response = new ResponseModelGet<>();
    try {
      response.setData(usuarioService.getAllActive());
      response.setMensaje(Constant.MENSAJE_CONSULTA_OK);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.info("getAllActive() : Error " + e.getMessage());
      response.setError(e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  @GetMapping("/findById/{idUsuario}")
  public ResponseEntity<ResponseModelGet<UsuarioResponse>> findById(@PathVariable("idUsuario") int idUsuario) {

    ResponseModelGet<UsuarioResponse> response = new ResponseModelGet<>();
    try {
      response.setData(Collections.singletonList(usuarioService.findById(idUsuario)));
      response.setMensaje(Constant.MENSAJE_CONSULTA_OK);
      log.info("findById() : Error");
      return ResponseEntity.status(HttpStatus.OK).body(response);
    } catch (Exception e) {


      response.setError(e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  @PostMapping("/getLogin")
  public ResponseEntity<ResponseModelGet<LoginResponse>> getLogin(@RequestBody LoginRequest loginRequest) {

    ResponseModelGet<LoginResponse> response = new ResponseModelGet<>();
    try {

     return ResponseEntity.status(HttpStatus.OK).body(usuarioService.login(loginRequest.getUsuario(), loginRequest.getContrasena()));
    } catch (Exception e) {

      response.setError(e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  @PostMapping("/insert/usuario")
  public ResponseEntity<ResponseModelSet> create(@RequestBody UsuarioRequest usuarioRequest) {

    ResponseModelSet responseModelSet = new ResponseModelSet();
    try {
      responseModelSet = usuarioService.save(usuarioRequest);

      return ResponseEntity.status(HttpStatus.CREATED).body(responseModelSet);
    } catch (Exception e) {
      responseModelSet.setError("Error al realizar el insert en la base de datos: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseModelSet);
    }
  }

  @PutMapping("/update/{idUsuario}")
  public ResponseEntity<ResponseModelSet> update(@RequestBody UsuarioRequest usuarioRequest, @PathVariable int idUsuario) {
    ResponseModelSet responseModelSet = new ResponseModelSet();
    try {
      usuarioRequest.setIdUsuario(idUsuario);
      responseModelSet = usuarioService.update(usuarioRequest);
      return ResponseEntity.ok(responseModelSet);
    } catch (Exception e) {
      responseModelSet.setError("Error al realizar el update en la base de datos: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseModelSet);
    }
  }

  @PutMapping("/setInactive/{idUsuario}")
  public ResponseEntity<ResponseModelSet> setInactive(@PathVariable("idUsuario") int idUsuario) {
    ResponseModelSet responseModelSet = new ResponseModelSet();
    try {
      usuarioService.setInactive(idUsuario);
      responseModelSet.setMensaje(Constant.MENSAJE_ELIMINADO_OK);
      return ResponseEntity.ok(responseModelSet);
    } catch (Exception e) {
      responseModelSet.setError("Error al realizar el update en la base de datos: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseModelSet);
    }
  }



}
