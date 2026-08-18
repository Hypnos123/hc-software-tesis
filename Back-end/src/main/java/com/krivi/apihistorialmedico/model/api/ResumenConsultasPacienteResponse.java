package com.krivi.apihistorialmedico.model.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumenConsultasPacienteResponse {
  private PacienteResumen paciente;
  private AntecedentesResumen antecedentes;
  private ResumenAtencion resumenAtencion;
  private List<CategoriaResumen> tiposEnfermedad;
  private List<CategoriaResumen> especialidades;
  private FuncionesVitalesResumen funcionesVitales;
  private List<EvaluacionRecienteResumen> evaluacionesRecientes;
  private List<ConsultaRecienteResumen> consultasRecientes;
  private CalidadDatosResumen calidadDatos;

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class PacienteResumen {
    private Integer idPaciente;
    private String nombreCompleto;
    private String dni;
    private Date fechaNacimiento;
    private Integer edad;
    private String estado;
    private Long cantidadHistoriasClinicas;
    private List<Integer> idsHistoriasClinicas;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class AntecedentesResumen {
    private String enfermedadesPrevias;
    private String cirugiasPrevias;
    private String alergiaMedicamentos;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class ResumenAtencion {
    private Long totalConsultasAtendidas;
    private LocalDateTime fechaPrimeraConsulta;
    private LocalDateTime fechaUltimaConsulta;
    private String ultimaEspecialidad;
    private String ultimoDoctor;
    private List<Date> proximasCitas;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class CategoriaResumen {
    private Integer id;
    private String nombre;
    private Long cantidad;
    private Double porcentaje;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class FuncionesVitalesResumen {
    private EstadisticaVital presionSistolica;
    private EstadisticaVital presionDiastolica;
    private EstadisticaVital frecuenciaCardiaca;
    private EstadisticaVital frecuenciaRespiratoria;
    private EstadisticaVital talla;
    private EstadisticaVital temperatura;
    private EstadisticaVital peso;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class EstadisticaVital {
    private Double ultimoValor;
    private Double promedio;
    private Double minimo;
    private Double maximo;
    private Long cantidadRegistrosValidos;
    private Long cantidadRegistrosDescartados;
    private String unidad;
    private String tendencia;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class EvaluacionRecienteResumen {
    private Integer idConsulta;
    private String diagnostico;
    private String examenesRecetados;
    private String receta;
    private String tratamiento;
    private Date proximaCita;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class ConsultaRecienteResumen {
    private Integer idConsulta;
    private Integer idHistoriaClinica;
    private LocalDateTime fecha;
    private String especialidad;
    private String doctor;
    private String relatoPaciente;
    private String diagnostico;
    private String tratamiento;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class CalidadDatosResumen {
    private Long consultasSinFecha;
    private Long consultasSinTipoEnfermedad;
    private Long consultasSinEspecialidad;
    private Long valoresVitalesDescartados;
    private Long consultasConRelacionInconsistente;
  }
}
