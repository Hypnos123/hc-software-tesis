package com.krivi.apihistorialmedico.model.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "paciente")
public class Paciente {

  private static final ZoneId ZONA_HORARIA_LIMA = ZoneId.of("America/Lima");

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "idpaciente")
  private Integer idPaciente;
  @Column(name = "fechacreacion", nullable = false, updatable = false)
  private LocalDateTime fechaCreacion;
  @Column(name = "ultimaactualizacion", nullable = false)
  private LocalDateTime ultimaActualizacion;
  @Enumerated(EnumType.STRING)
  @Column(name = "estadoregistro", nullable = false, length = 20)
  private EstadoRegistroPaciente estadoRegistro = EstadoRegistroPaciente.ACTIVO;
  @Column(name = "fechaarchivado")
  private LocalDateTime fechaArchivado;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "idusuarioarchivado")
  private Usuario archivadoPor;
  @Column(name = "motivoarchivado", length = 45)
  private String motivoArchivado;
  @Column(name = "detallemotivoarchivado", length = 500)
  private String detalleMotivoArchivado;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "idpacienteprincipal")
  private Paciente pacientePrincipal;
  @Version
  @Column(name = "version", nullable = false)
  private Long version;
  private String nombres;
  private String apellidos;
  @Column(name = "fechaingreso")
  private Date fechaIngreso;
  @Column(name = "fechanacimiento")
  private Date fechaNacimiento;
  @Column(name = "estadocivil")
  private String estadoCivil;
  @Column(name = "numdocumento")
  private String numDocumento;
  private String sexo;
  private String direccion;
  private String distrito;
  @Column(name = "traidopor")
  private String traidoPor;

  public Paciente(Integer idPaciente) {
    this.idPaciente = idPaciente;
  }

  @PrePersist
  void asignarFechaCreacion() {
    LocalDateTime ahora = LocalDateTime.now(ZONA_HORARIA_LIMA);
    if (fechaCreacion == null) {
      fechaCreacion = ahora;
    }
    ultimaActualizacion = ahora;
    if (estadoRegistro == null) estadoRegistro = EstadoRegistroPaciente.ACTIVO;
    validarEstadoRegistro();
  }

  @PreUpdate
  void asignarFechaActualizacion() {
    ultimaActualizacion = LocalDateTime.now(ZONA_HORARIA_LIMA);
    validarEstadoRegistro();
  }

  private void validarEstadoRegistro() {
    if (estadoRegistro == EstadoRegistroPaciente.ACTIVO) {
      fechaArchivado = null;
      archivadoPor = null;
      motivoArchivado = null;
      detalleMotivoArchivado = null;
      pacientePrincipal = null;
      return;
    }
    if (pacientePrincipal == this
        || (idPaciente != null && pacientePrincipal != null && idPaciente.equals(pacientePrincipal.getIdPaciente()))) {
      throw new IllegalStateException("El paciente principal debe ser distinto del paciente archivado.");
    }
  }

  @OneToMany(mappedBy = "paciente" , cascade = CascadeType.ALL, orphanRemoval = false)
  @JsonManagedReference
  private List<Antecedentes> antecedentes;

  @OneToMany(mappedBy = "paciente" , cascade = CascadeType.ALL, orphanRemoval = false)
  @JsonManagedReference
  private List<Consulta> consultas;





}
