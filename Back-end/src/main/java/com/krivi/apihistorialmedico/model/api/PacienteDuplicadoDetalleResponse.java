package com.krivi.apihistorialmedico.model.api;

import com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PacienteDuplicadoDetalleResponse {
  private Integer idPaciente;
  private String nombres;
  private String apellidos;
  private String nombreCompleto;
  private String tipoDocumento;
  private String dni;
  private LocalDateTime fechaCreacion;
  private LocalDateTime ultimaActualizacion;
  private EstadoRegistroPaciente estadoRegistro;
  private long cantidadHistoriasClinicas;
  private long cantidadConsultas;
  private long cantidadAntecedentes;
  private int cantidadCamposPersonalesCompletos;
  private long cantidadGruposClinicosCompletos;
  private LocalDateTime ultimaActividadClinica;
  private boolean tieneInformacionClinicaRelevante;
}
