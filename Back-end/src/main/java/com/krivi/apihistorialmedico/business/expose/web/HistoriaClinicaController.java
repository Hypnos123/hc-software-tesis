package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.services.HistoriaClinicaService;
import com.krivi.apihistorialmedico.model.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/historiaClinica")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT})
public class HistoriaClinicaController {
  @Autowired HistoriaClinicaService historiaClinicaService;

  @GetMapping("/getAll")
  public ResponseEntity<ResponseModelGet<HistoriaClinicaResponse>> getAll() { return ResponseEntity.ok(historiaClinicaService.getAll()); }

  @GetMapping("/findById/{id}")
  public ResponseEntity<ResponseModelGet<HistoriaClinicaResponse>> findById(@PathVariable int id) { return ResponseEntity.ok(historiaClinicaService.findById(id)); }

  @GetMapping("/findByPaciente/{idPaciente}")
  public ResponseEntity<ResponseModelGet<HistoriaClinicaResponse>> findByPaciente(@PathVariable int idPaciente) { return ResponseEntity.ok(historiaClinicaService.findByPaciente(idPaciente)); }

  @PostMapping("/insert")
  public ResponseEntity<ResponseModelSet> insert(@RequestBody HistoriaClinicaRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(historiaClinicaService.save(request)); }

  @PutMapping("/update/{id}")
  public ResponseEntity<ResponseModelSet> update(@RequestBody HistoriaClinicaRequest request, @PathVariable int id) { request.setIdHistoriaClinica(id); return ResponseEntity.ok(historiaClinicaService.update(request)); }
}
