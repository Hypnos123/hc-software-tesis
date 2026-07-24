package com.krivi.apihistorialmedico.business.services;

import com.krivi.apihistorialmedico.model.api.BusquedaConsultasMedicasResponse;
import com.krivi.apihistorialmedico.model.api.EstadisticasConsultasMedicasResponse;
import com.krivi.apihistorialmedico.model.api.ListadoConsultasMedicasResponse;

public interface ConsultaMedicaIntegracionService {
  BusquedaConsultasMedicasResponse buscar(String criterio);
  EstadisticasConsultasMedicasResponse obtenerEstadisticas();
  ListadoConsultasMedicasResponse obtenerPendientes();
  ListadoConsultasMedicasResponse obtenerUltimas(Integer limite);
}
