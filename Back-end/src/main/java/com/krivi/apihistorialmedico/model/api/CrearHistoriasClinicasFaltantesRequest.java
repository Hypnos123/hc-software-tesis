package com.krivi.apihistorialmedico.model.api;

import lombok.Data;

import java.util.List;

@Data
public class CrearHistoriasClinicasFaltantesRequest {
  private List<Integer> idsPacientes;
}
