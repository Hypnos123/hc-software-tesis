package com.krivi.apihistorialmedico.business.services;

import com.krivi.apihistorialmedico.model.api.ReporteConsultaFiltroRequest;
import com.krivi.apihistorialmedico.model.api.ReporteMedicoDocumento;

public interface ConsultaReporteQueryService {
  ReporteMedicoDocumento seleccionarConsultaIndividual(Integer idConsulta);

  ReporteMedicoDocumento seleccionarConsultasPaciente(Integer idPaciente, ReporteConsultaFiltroRequest filtro);
}
