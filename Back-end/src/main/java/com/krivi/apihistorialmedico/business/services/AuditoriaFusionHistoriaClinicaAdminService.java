package com.krivi.apihistorialmedico.business.services;

import com.krivi.apihistorialmedico.model.api.FusionHistoriaClinicaAuditoriaDetalleResponse;
import com.krivi.apihistorialmedico.model.api.FusionHistoriaClinicaAuditoriaResumenResponse;
import com.krivi.apihistorialmedico.model.api.PaginaResponse;

import java.time.LocalDateTime;

public interface AuditoriaFusionHistoriaClinicaAdminService {
  PaginaResponse<FusionHistoriaClinicaAuditoriaResumenResponse> listar(Integer idUsuarioActual, int page, int size,
      String sort, String search, String dni, Integer idPaciente, Integer idHistoriaPrincipal,
      Integer idHistoriaEliminada, Integer idUsuario, String resultado, LocalDateTime desde, LocalDateTime hasta);

  FusionHistoriaClinicaAuditoriaDetalleResponse obtenerDetalle(Integer idUsuarioActual, Integer idAuditoria);
}
