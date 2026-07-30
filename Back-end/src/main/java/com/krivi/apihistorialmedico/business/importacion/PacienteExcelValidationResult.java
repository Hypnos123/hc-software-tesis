package com.krivi.apihistorialmedico.business.importacion;

import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionFila;

import java.util.List;

public record PacienteExcelValidationResult(
    List<PacienteImportacionFila> filas,
    int filasVaciasIgnoradas
) {
}
