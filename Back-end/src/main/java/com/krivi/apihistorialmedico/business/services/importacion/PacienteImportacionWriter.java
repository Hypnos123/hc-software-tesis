package com.krivi.apihistorialmedico.business.services.importacion;

import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionFila;

public interface PacienteImportacionWriter {
  Integer registrar(PacienteImportacionFila fila);
}
