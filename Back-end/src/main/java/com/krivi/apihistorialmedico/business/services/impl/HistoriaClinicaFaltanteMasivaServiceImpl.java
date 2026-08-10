package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.services.HistoriaClinicaFaltanteMasivaService;
import com.krivi.apihistorialmedico.business.services.HistoriaClinicaService;
import com.krivi.apihistorialmedico.model.api.CreacionHistoriaClinicaFaltanteResponse;
import com.krivi.apihistorialmedico.model.api.CrearHistoriasClinicasFaltantesResponse;
import com.krivi.apihistorialmedico.model.api.EstadoCreacionHistoriaClinicaFaltante;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class HistoriaClinicaFaltanteMasivaServiceImpl implements HistoriaClinicaFaltanteMasivaService {
  private final HistoriaClinicaService historiaClinicaService;

  public HistoriaClinicaFaltanteMasivaServiceImpl(HistoriaClinicaService historiaClinicaService) {
    this.historiaClinicaService = historiaClinicaService;
  }

  @Override
  public CrearHistoriasClinicasFaltantesResponse crearHistoriasClinicasFaltantes(List<Integer> idsPacientes) {
    int totalSolicitados = idsPacientes == null ? 0 : idsPacientes.size();
    LinkedHashSet<Integer> idsUnicos = new LinkedHashSet<>();
    if (idsPacientes != null) {
      idsPacientes.stream().filter(java.util.Objects::nonNull).forEach(idsUnicos::add);
    }

    List<CreacionHistoriaClinicaFaltanteResponse> resultados = new ArrayList<>();
    idsUnicos.forEach(idPaciente -> resultados.add(procesarPaciente(idPaciente)));
    return construirResumen(totalSolicitados, resultados);
  }

  private CreacionHistoriaClinicaFaltanteResponse procesarPaciente(Integer idPaciente) {
    try {
      CreacionHistoriaClinicaFaltanteResponse resultado =
          historiaClinicaService.crearHistoriaClinicaSiFalta(idPaciente);
      return resultado == null ? resultadoError(idPaciente) : resultado;
    } catch (RuntimeException exception) {
      return resultadoError(idPaciente);
    }
  }

  private CreacionHistoriaClinicaFaltanteResponse resultadoError(Integer idPaciente) {
    return CreacionHistoriaClinicaFaltanteResponse.builder()
        .idPaciente(idPaciente)
        .estado(EstadoCreacionHistoriaClinicaFaltante.ERROR)
        .build();
  }

  private CrearHistoriasClinicasFaltantesResponse construirResumen(int totalSolicitados,
      List<CreacionHistoriaClinicaFaltanteResponse> resultados) {
    return CrearHistoriasClinicasFaltantesResponse.builder()
        .totalSolicitados(totalSolicitados)
        .totalProcesados(resultados.size())
        .creadas(contar(resultados, EstadoCreacionHistoriaClinicaFaltante.CREADA))
        .omitidas(contar(resultados, EstadoCreacionHistoriaClinicaFaltante.OMITIDA_YA_TIENE_HISTORIA))
        .noEncontrados(contar(resultados, EstadoCreacionHistoriaClinicaFaltante.PACIENTE_NO_ENCONTRADO))
        .inactivos(contar(resultados, EstadoCreacionHistoriaClinicaFaltante.PACIENTE_INACTIVO))
        .errores(contar(resultados, EstadoCreacionHistoriaClinicaFaltante.ERROR))
        .resultados(List.copyOf(resultados))
        .build();
  }

  private int contar(List<CreacionHistoriaClinicaFaltanteResponse> resultados,
      EstadoCreacionHistoriaClinicaFaltante estado) {
    return (int) resultados.stream().filter(resultado -> resultado.getEstado() == estado).count();
  }
}
