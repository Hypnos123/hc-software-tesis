package com.krivi.apihistorialmedico.business.services;

import com.krivi.apihistorialmedico.model.api.BusquedaConsultasMedicasResponse;
import com.krivi.apihistorialmedico.model.api.EstadisticasConsultasMedicasResponse;
import com.krivi.apihistorialmedico.model.api.ListadoConsultasMedicasResponse;
import com.krivi.apihistorialmedico.model.api.ResumenConsultasPacienteResponse;

public interface ConsultaMedicaIntegracionService {
  BusquedaConsultasMedicasResponse buscar(String criterio);
  EstadisticasConsultasMedicasResponse obtenerEstadisticas();
  ListadoConsultasMedicasResponse obtenerPendientes();
  ListadoConsultasMedicasResponse obtenerUltimas(Integer limite);
  ResumenConsultasPacienteResponse obtenerResumenPaciente(Integer idPaciente, Integer idUsuario);
}
