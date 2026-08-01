package com.krivi.apihistorialmedico.business.services.importacion;

import com.krivi.apihistorialmedico.model.api.importacion.PacienteImportacionConfirmacionResponse;
import com.krivi.apihistorialmedico.model.api.importacion.PacienteImportacionValidacionResponse;

import java.util.UUID;

public interface PacienteImportacionConfirmacionService {
  PacienteImportacionValidacionResponse obtener(UUID importacionId);
  PacienteImportacionConfirmacionResponse confirmar(UUID importacionId);
}
