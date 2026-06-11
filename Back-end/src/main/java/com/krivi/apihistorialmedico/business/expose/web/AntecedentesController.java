package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.services.AntecedentesService;
import com.krivi.apihistorialmedico.model.api.AntecedentesRequest;
import com.krivi.apihistorialmedico.model.api.AntecedentesResponse;
import com.krivi.apihistorialmedico.model.api.ResponseModelGet;
import com.krivi.apihistorialmedico.model.api.ResponseModelSet;

import com.krivi.apihistorialmedico.util.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/antecedentes")
@Slf4j
@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST,RequestMethod.PUT})
public class AntecedentesController {


  @Autowired
  private AntecedentesService antecedentesService;

  @GetMapping("/getAllActive")
  public ResponseEntity<ResponseModelGet<AntecedentesResponse>> getAllActive() {
    ResponseModelGet<AntecedentesResponse> responseModelGet = new ResponseModelGet<>();
    try {
      return ResponseEntity.ok(antecedentesService.getAllActive());
    } catch (Exception e) {
      log.info("getAllActive() : Error" + e.getMessage());
      responseModelGet.setMensaje(Constant.MENSAJE_ERROR);
      responseModelGet.setError(e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseModelGet);
    }
  }

  @GetMapping("/findById/{idAntecedente}")
  public ResponseEntity<ResponseModelGet<AntecedentesResponse>> findById(@PathVariable("idAntecedente") int idAntecedente) {

    ResponseModelGet<AntecedentesResponse> responseModelGet = new ResponseModelGet<>();
    try {

      return ResponseEntity.status(HttpStatus.OK).body(antecedentesService.findById(idAntecedente));
    } catch (Exception e) {
      responseModelGet.setMensaje(Constant.MENSAJE_ERROR);
      responseModelGet.setError(e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseModelGet);
    }
  }

  @GetMapping("/findByPaciente/{idPaciente}")
  public ResponseEntity<ResponseModelGet<AntecedentesResponse>> findByPaciente(@PathVariable("idPaciente") int idPaciente) {

    ResponseModelGet<AntecedentesResponse> responseModelGet = new ResponseModelGet<>();
    try {
      return ResponseEntity.status(HttpStatus.OK).body(antecedentesService.findByPaciente(idPaciente));
    } catch (Exception e) {
      responseModelGet.setMensaje(Constant.MENSAJE_ERROR);
      responseModelGet.setError(e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseModelGet);
    }
  }

  @PostMapping("/insert/antecedente")
  public ResponseEntity<ResponseModelSet> create(@RequestBody AntecedentesRequest antecedentesRequest) {

    ResponseModelSet responseModelSet = new ResponseModelSet();
    try {
      return ResponseEntity.status(HttpStatus.CREATED).body(antecedentesService.save(antecedentesRequest));
    } catch (Exception e) {
      responseModelSet.setMensaje(Constant.MENSAJE_GUARDAR_ERROR);
      responseModelSet.setError("Error al realizar el insert en la base de datos: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseModelSet);
    }
  }

  @PutMapping("/update/{idAntecedente}")
  public ResponseEntity<ResponseModelSet> update(@RequestBody AntecedentesRequest antecedentesRequest, @PathVariable int idAntecedente) {


    ResponseModelSet responseModelSet = new ResponseModelSet();
    try {
      antecedentesRequest.setIdAntecedentes(idAntecedente);
      return ResponseEntity.ok(antecedentesService.update(antecedentesRequest));
    } catch (Exception e) {
      responseModelSet.setError("Error al realizar el update en la base de datos: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseModelSet);
    }
  }




}
