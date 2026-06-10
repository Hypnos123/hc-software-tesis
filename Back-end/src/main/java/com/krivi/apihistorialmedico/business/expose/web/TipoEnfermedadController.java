package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.services.TipoEnfermedadService;
import com.krivi.apihistorialmedico.model.api.*;
import com.krivi.apihistorialmedico.util.Constant;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tipoEnfermedad")
@Slf4j
@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST,RequestMethod.PUT})
public class TipoEnfermedadController {

  @Autowired
  TipoEnfermedadService tipoEnfermedadService;

  @GetMapping("/getAllActive")
  public ResponseEntity<ResponseModelGet<TipoEnfermedadResponse>> getAllActive() {
    ResponseModelGet<TipoEnfermedadResponse> responseModelGet = new ResponseModelGet<>();
    try {
      return ResponseEntity.ok(tipoEnfermedadService.getAllActive());
    } catch (Exception e) {
      log.info("getAllActive() : Error" + e.getMessage());
      responseModelGet.setMensaje(Constant.MENSAJE_ERROR);
      responseModelGet.setError(e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseModelGet);
    }
  }

  @GetMapping("/findById/{idTipoEnfermedad}")
  public ResponseEntity<ResponseModelGet<TipoEnfermedadResponse>> findById(@PathVariable("idTipoEnfermedad") int idTipoEnfermedad) {

    ResponseModelGet<TipoEnfermedadResponse> responseModelGet = new ResponseModelGet<>();
    try {

      return ResponseEntity.status(HttpStatus.OK).body(tipoEnfermedadService.findById(idTipoEnfermedad));
    } catch (Exception e) {
      responseModelGet.setMensaje(Constant.MENSAJE_ERROR);
      responseModelGet.setError(e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseModelGet);
    }
  }

  @PostMapping("/insert/tipoEnfermedad")
  public ResponseEntity<ResponseModelSet> create(@RequestBody TipoEnfermedadRequest tipoEnfermedadRequest) {

    ResponseModelSet responseModelSet = new ResponseModelSet();
    try {
      return ResponseEntity.status(HttpStatus.CREATED).body(tipoEnfermedadService.save(tipoEnfermedadRequest));
    } catch (Exception e) {
      responseModelSet.setMensaje(Constant.MENSAJE_GUARDAR_ERROR);
      responseModelSet.setError("Error al realizar el insert en la base de datos: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseModelSet);
    }
  }

  @PutMapping("/update/{idTipoEnfermedad}")
  public ResponseEntity<ResponseModelSet> update(@RequestBody TipoEnfermedadRequest tipoEnfermedadRequest, @PathVariable int idTipoEnfermedad) {


    ResponseModelSet responseModelSet = new ResponseModelSet();
    try {
      tipoEnfermedadRequest.setIdTipoEnfermedad(idTipoEnfermedad);
      return ResponseEntity.ok(tipoEnfermedadService.save(tipoEnfermedadRequest));
    } catch (Exception e) {
      responseModelSet.setError("Error al realizar el update en la base de datos: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseModelSet);
    }
  }

}
