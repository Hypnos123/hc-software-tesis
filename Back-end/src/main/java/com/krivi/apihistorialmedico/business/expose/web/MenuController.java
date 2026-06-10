package com.krivi.apihistorialmedico.business.expose.web;


import com.krivi.apihistorialmedico.business.services.MenuService;
import com.krivi.apihistorialmedico.model.api.MenuResponse;
import com.krivi.apihistorialmedico.model.api.ResponseModelGet;
import com.krivi.apihistorialmedico.util.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/menu")
@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST,RequestMethod.PUT})
@Slf4j
public class MenuController {

  @Autowired
  private MenuService menuService;


  @GetMapping("/getAllActive")
  public ResponseEntity<ResponseModelGet<MenuResponse>> getAllActive() {
    ResponseModelGet<MenuResponse> response = new ResponseModelGet<>();
    try {
      response.setData(menuService.getAllActive());
      response.setMensaje(Constant.MENSAJE_CONSULTA_OK);

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.info("getAllActive() : Error" + e.getMessage());
      response.setError(e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }
}
