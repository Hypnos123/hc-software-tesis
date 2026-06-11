package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.services.PacienteService;
import com.krivi.apihistorialmedico.model.api.*;
import com.krivi.apihistorialmedico.util.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/paciente")
@Slf4j
@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST,RequestMethod.PUT})
public class PacienteController {

  @Autowired
  PacienteService pacienteService;


  @GetMapping("/getAllActive")
  public ResponseEntity<ResponseModelGet<PacienteResponse>> getAllActive() {
    ResponseModelGet<PacienteResponse> responseModelGet = new ResponseModelGet<>();
    try {
      return ResponseEntity.ok(pacienteService.getAllActive());
    } catch (Exception e) {
      log.info("getAllActive() : Error" + e.getMessage());
      responseModelGet.setMensaje(Constant.MENSAJE_ERROR);
      responseModelGet.setError(e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseModelGet);
    }
  }

  @GetMapping("/findById/{idPaciente}")
  public ResponseEntity<ResponseModelGet<PacienteResponse>> findById(@PathVariable("idPaciente") int idPAciente) {

    ResponseModelGet<PacienteResponse > responseModelGet = new ResponseModelGet<>();
    try {

      return ResponseEntity.status(HttpStatus.OK).body(pacienteService.findById(idPAciente));
    } catch (Exception e) {
      responseModelGet.setMensaje(Constant.MENSAJE_ERROR);
      responseModelGet.setError(e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseModelGet);
    }
  }

  @PostMapping("/insert/paciente")
  public ResponseEntity<ResponseModelSet> create(@RequestBody PacienteRequest pacienteRequest) {

    ResponseModelSet responseModelSet = new ResponseModelSet();
    try {
      return ResponseEntity.status(HttpStatus.CREATED).body(pacienteService.save(pacienteRequest));
    } catch (Exception e) {
      responseModelSet.setMensaje(Constant.MENSAJE_GUARDAR_ERROR);
      responseModelSet.setError("Error al realizar el insert en la base de datos: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseModelSet);
    }
  }

  @PutMapping("/update/{idPaciente}")
  public ResponseEntity<ResponseModelSet> update(@RequestBody PacienteRequest pacienteRequest, @PathVariable int idPaciente) {


    ResponseModelSet responseModelSet = new ResponseModelSet();
    try {
      pacienteRequest.setIdPaciente(idPaciente);
      return ResponseEntity.ok(pacienteService.update(pacienteRequest));
    } catch (Exception e) {
      responseModelSet.setError("Error al realizar el update en la base de datos: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseModelSet);
    }
  }


}
