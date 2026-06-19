package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.services.EmpleadoService;
import com.krivi.apihistorialmedico.model.api.*;
import com.krivi.apihistorialmedico.util.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/empleado")
@Slf4j
@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST,RequestMethod.PUT})
public class EmpleadoController {

  @Autowired
  EmpleadoService empleadoService;

  @GetMapping("/getAll")
  public ResponseEntity<ResponseModelGet<EmpleadoResponse>> getAll() {
    ResponseModelGet<EmpleadoResponse> responseModelGet = new ResponseModelGet<>();
    try {
      return ResponseEntity.ok(empleadoService.getAll());
    } catch (Exception e) {
      log.info("getAll() : Error{}", e.getMessage());
      responseModelGet.setMensaje(Constant.MENSAJE_ERROR);
      responseModelGet.setError(e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseModelGet);
    }
  }

  @GetMapping("/getAllActive")
  public ResponseEntity<ResponseModelGet<EmpleadoResponse>> getAllActive() {
    ResponseModelGet<EmpleadoResponse> responseModelGet = new ResponseModelGet<>();
    try {
      return ResponseEntity.ok(empleadoService.getAllActive());
    } catch (Exception e) {
      log.info("getAllActive() : Error{}", e.getMessage());
      responseModelGet.setMensaje(Constant.MENSAJE_ERROR);
      responseModelGet.setError(e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseModelGet);
    }
  }

  @GetMapping("/findById/{idEmpleado}")
  public ResponseEntity<ResponseModelGet<EmpleadoResponse>> findById(@PathVariable("idEmpleado") int idEmpleado) {

    ResponseModelGet<EmpleadoResponse> responseModelGet = new ResponseModelGet<>();
    try {

      return ResponseEntity.status(HttpStatus.OK).body(empleadoService.findById(idEmpleado));
    } catch (Exception e) {
      responseModelGet.setMensaje(Constant.MENSAJE_ERROR);
      responseModelGet.setError(e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseModelGet);
    }
  }

  @PostMapping("/insert/empleado")
  public ResponseEntity<ResponseModelSet> create(@RequestBody EmpleadoRequest empleadoRequest) {

    ResponseModelSet responseModelSet = new ResponseModelSet();
    try {
      return ResponseEntity.status(HttpStatus.CREATED).body(empleadoService.save(empleadoRequest));
    } catch (Exception e) {
      responseModelSet.setMensaje(Constant.MENSAJE_GUARDAR_ERROR);
      responseModelSet.setError("Error al realizar el insert en la base de datos: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseModelSet);
    }
  }

  @PutMapping("/update/{idEmpleado}")
  public ResponseEntity<ResponseModelSet> update(@RequestBody EmpleadoRequest empleadoRequest, @PathVariable int idEmpleado) {


    ResponseModelSet responseModelSet = new ResponseModelSet();
    try {
      empleadoRequest.setIdEmpleado(idEmpleado);
      return ResponseEntity.ok(empleadoService.update(empleadoRequest));
    } catch (Exception e) {
      responseModelSet.setError("Error al realizar el update en la base de datos: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseModelSet);
    }
  }

  @PutMapping("/changeStatus/{idEmpleado}")
  public ResponseEntity<ResponseModelSet> changeStatus(@PathVariable int idEmpleado) {
    ResponseModelSet responseModelSet = new ResponseModelSet();
    try {
      return ResponseEntity.ok(empleadoService.changeStatus(idEmpleado));
    } catch (Exception e) {
      responseModelSet.setError("Error al cambiar el estado del empleado: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseModelSet);
    }
  }

}
