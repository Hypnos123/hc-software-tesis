package com.krivi.apihistorialmedico.business.services;

import com.krivi.apihistorialmedico.model.api.ReporteMedicoDocumento;

public interface ReporteMedicoService {
  byte[] generarEvaluacionMedica(ReporteMedicoDocumento documento);

  byte[] generarReporteConsultas(ReporteMedicoDocumento documento);
}
