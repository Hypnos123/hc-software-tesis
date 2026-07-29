package com.krivi.apihistorialmedico.business.importacion.store;

import com.krivi.apihistorialmedico.model.api.importacion.PacienteImportacionConfirmacionResponse;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacion;

import java.util.UUID;

public interface PacienteImportacionStore {
  void guardar(PacienteImportacion importacion);
  PacienteImportacion obtener(UUID importacionId);
  PacienteImportacion iniciarConfirmacion(UUID importacionId);
  void marcarConfirmada(UUID importacionId, PacienteImportacionConfirmacionResponse resultado);
  void restaurarPrevisualizada(UUID importacionId);
  int limpiarExpiradas();
}
