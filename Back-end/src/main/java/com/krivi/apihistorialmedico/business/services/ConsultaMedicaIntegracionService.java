package com.krivi.apihistorialmedico.business.services;

import com.krivi.apihistorialmedico.model.api.BusquedaConsultasMedicasResponse;
import com.krivi.apihistorialmedico.model.api.EstadisticasConsultasMedicasResponse;
import com.krivi.apihistorialmedico.model.api.ListadoConsultasMedicasResponse;
import com.krivi.apihistorialmedico.model.api.ResumenConsultasPacienteResponse;
import java.time.LocalDate;

public interface ConsultaMedicaIntegracionService {
  BusquedaConsultasMedicasResponse buscar(String criterio);
  EstadisticasConsultasMedicasResponse obtenerEstadisticas();
  ListadoConsultasMedicasResponse obtenerPendientes();
  ListadoConsultasMedicasResponse obtenerUltimas(Integer limite);
  ListadoConsultasMedicasResponse obtenerPorEstado(String estado);
  ListadoConsultasMedicasResponse obtenerPorFecha(LocalDate fechaInicio, LocalDate fechaFin);
  ListadoConsultasMedicasResponse obtenerPorPaciente(Integer idPaciente);
  ListadoConsultasMedicasResponse obtenerUltimaPorPaciente(Integer idPaciente);
  ResumenConsultasPacienteResponse obtenerResumenPaciente(Integer idPaciente, Integer idUsuario);
}
