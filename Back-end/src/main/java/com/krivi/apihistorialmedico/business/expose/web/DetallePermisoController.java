package com.krivi.apihistorialmedico.business.expose.web;


import com.krivi.apihistorialmedico.business.services.DetallePermisoService;
import com.krivi.apihistorialmedico.model.api.DetallePermisoRequest;
import com.krivi.apihistorialmedico.model.api.DetallePermisoResponse;
import com.krivi.apihistorialmedico.model.api.ResponseModelGet;
import com.krivi.apihistorialmedico.model.api.ResponseModelSet;
import com.krivi.apihistorialmedico.util.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/detallepermiso")
@Slf4j
@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST,RequestMethod.PUT})
public class DetallePermisoController {

  @Autowired
  private DetallePermisoService detallePermisoService;

  @GetMapping("/findById/{idUsuario}")
  public ResponseEntity<ResponseModelGet<DetallePermisoResponse>> findById(@PathVariable("idUsuario") String idUsuario) {
    ResponseModelGet<DetallePermisoResponse> response = new ResponseModelGet<>();
    try {
      if(detallePermisoService.detallePermisosListar(idUsuario).isEmpty()){
        response.setMensaje(Constant.MENSAJE_NO_CONTENIDO);
        response.setData(Arrays.asList(new DetallePermisoResponse()));
        return ResponseEntity.status(HttpStatus.OK).body(response);}
      else{
      response.setData(detallePermisoService.detallePermisosListar(idUsuario));
      response.setMensaje(Constant.MENSAJE_CONSULTA_OK);
      return ResponseEntity.status(HttpStatus.OK).body(response);}
    } catch (Exception e) {

      response.setError(e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  @PostMapping("/insert/detallepermisos")
  public ResponseEntity<ResponseModelSet> create(@RequestBody List<DetallePermisoRequest> detallePermisoRequestList) {
    ResponseModelSet responseModelSet = new ResponseModelSet();

    try {
      detallePermisoService.save(detallePermisoRequestList);
      responseModelSet.setMensaje(Constant.MENSAJE_GUARDAR_OK);
      return ResponseEntity.status(HttpStatus.CREATED).body(responseModelSet);
    } catch (Exception e) {
      responseModelSet.setError("Error al realizar el insert en la base de datos: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseModelSet);
    }

  }

  @PutMapping("/update/{idUsuario}")
  public ResponseEntity<ResponseModelSet> update(@RequestBody List<DetallePermisoRequest> detallePermisoRequestList,
                                                 @PathVariable("idUsuario") int idUsuario) {
    ResponseModelSet responseModelSet = new ResponseModelSet();

    try {
      detallePermisoService.deleteByIdUsuario(idUsuario);
      detallePermisoService.save(detallePermisoRequestList);

      responseModelSet.setMensaje(Constant.MENSAJE_EDITAR_OK);
      return ResponseEntity.status(HttpStatus.CREATED).body(responseModelSet);
    } catch (Exception e) {
      responseModelSet.setError("Error al realizar el update en la base de datos: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseModelSet);
    }

  }


}
