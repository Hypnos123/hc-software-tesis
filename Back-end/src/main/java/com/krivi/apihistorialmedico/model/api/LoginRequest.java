package com.krivi.apihistorialmedico.model.api;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class LoginRequest {

  String usuario;
  String contrasena;

}
