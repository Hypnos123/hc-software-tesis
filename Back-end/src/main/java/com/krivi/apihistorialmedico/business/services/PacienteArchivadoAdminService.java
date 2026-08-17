package com.krivi.apihistorialmedico.business.services;

import com.krivi.apihistorialmedico.model.api.PaginaResponse;
import com.krivi.apihistorialmedico.model.api.PacienteArchivadoDetalleResponse;
import com.krivi.apihistorialmedico.model.api.PacienteArchivadoResumenResponse;

import java.time.LocalDateTime;

public interface PacienteArchivadoAdminService {
  PaginaResponse<PacienteArchivadoResumenResponse> listar(Integer idUsuario, int page, int size, String sort,
      String search, String dni, Integer idPaciente, LocalDateTime desde, LocalDateTime hasta);

  PacienteArchivadoDetalleResponse obtenerDetalle(Integer idUsuario, Integer idPaciente);
}
