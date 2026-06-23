package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.services.ConsultaService;
import com.krivi.apihistorialmedico.model.api.*;
import com.krivi.apihistorialmedico.util.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/consulta")
@Slf4j
@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST,RequestMethod.PUT})
public class ConsultaController {


  @Autowired
  private ConsultaService consultaService;

  @GetMapping("/getAllActive")
  public ResponseEntity<ResponseModelGet<ConsultaResponse>> getAllActive(@RequestHeader(value = "X-Usuario-Id", required = false) Integer idUsuario) {
    ResponseModelGet<ConsultaResponse> responseModelGet = new ResponseModelGet<>();
    try {
      return ResponseEntity.ok(consultaService.getAllActive(idUsuario));
    } catch (Exception e) {
      log.info("getAllActive() : Error" + e.getMessage());
      responseModelGet.setMensaje(Constant.MENSAJE_ERROR);
      responseModelGet.setError(e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseModelGet);
    }
  }

  @GetMapping("/findById/{idConsulta}")
  public ResponseEntity<ResponseModelGet<ConsultaResponse>> findById(@PathVariable("idConsulta") int idConsulta, @RequestHeader(value = "X-Usuario-Id", required = false) Integer idUsuario) {

    ResponseModelGet<ConsultaResponse> responseModelGet = new ResponseModelGet<>();
    try {

      return ResponseEntity.status(HttpStatus.OK).body(consultaService.findById(idConsulta, idUsuario));
    } catch (Exception e) {
      responseModelGet.setMensaje(Constant.MENSAJE_ERROR);
      responseModelGet.setError(e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseModelGet);
    }
  }

  @GetMapping("/findByHistoriaClinica/{idHistoriaClinica}")
  public ResponseEntity<ResponseModelGet<ConsultaResponse>> findByHistoriaClinica(@PathVariable int idHistoriaClinica) {
    return ResponseEntity.ok(consultaService.findByHistoriaClinica(idHistoriaClinica));
  }

  @PostMapping("/insert/consulta")
  public ResponseEntity<ResponseModelSet> create(@RequestBody ConsultaRequest consultaRequest) {

    ResponseModelSet responseModelSet = new ResponseModelSet();
    try {
      return ResponseEntity.status(HttpStatus.CREATED).body(consultaService.save(consultaRequest));
    } catch (Exception e) {
      responseModelSet.setMensaje(Constant.MENSAJE_GUARDAR_ERROR);
      responseModelSet.setError("Error al realizar el insert en la base de datos: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseModelSet);
    }
  }

  @PutMapping("/update/{idConsulta}")
  public ResponseEntity<ResponseModelSet> update(@RequestBody ConsultaRequest consultaRequest, @PathVariable int idConsulta) {


    ResponseModelSet responseModelSet = new ResponseModelSet();
    try {
      consultaRequest.setIdConsulta(idConsulta);
      return ResponseEntity.ok(consultaService.update(consultaRequest));
    } catch (Exception e) {
      responseModelSet.setError("Error al realizar el update en la base de datos: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseModelSet);
    }
  }


  @PutMapping("/finalizar-atencion/{idConsulta}")
  public ResponseEntity<ResponseModelSet> finalizarAtencion(@RequestBody ConsultaRequest consultaRequest, @PathVariable int idConsulta, @RequestHeader(value = "X-Usuario-Id", required = false) Integer idUsuario) {
    ResponseModelSet responseModelSet = new ResponseModelSet();
    try {
      return ResponseEntity.ok(consultaService.finalizarAtencion(idConsulta, consultaRequest, idUsuario));
    } catch (Exception e) {
      responseModelSet.setMensaje(Constant.MENSAJE_GUARDAR_ERROR);
      responseModelSet.setError(e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseModelSet);
    }
  }

}
