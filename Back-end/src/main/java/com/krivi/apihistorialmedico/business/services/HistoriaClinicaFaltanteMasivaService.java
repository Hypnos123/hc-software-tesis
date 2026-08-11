package com.krivi.apihistorialmedico.business.services;

import com.krivi.apihistorialmedico.model.api.CrearHistoriasClinicasFaltantesResponse;

import java.util.List;

public interface HistoriaClinicaFaltanteMasivaService {
  CrearHistoriasClinicasFaltantesResponse crearHistoriasClinicasFaltantes(List<Integer> idsPacientes);
}
