package com.krivi.apihistorialmedico.business.services.impl.importacion;

import com.krivi.apihistorialmedico.business.services.importacion.PacienteImportacionWriter;
import com.krivi.apihistorialmedico.model.entity.Antecedentes;
import com.krivi.apihistorialmedico.model.entity.Paciente;
import com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionAntecedentes;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionDatos;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionFila;
import com.krivi.apihistorialmedico.repository.AntecedentesRepository;
import com.krivi.apihistorialmedico.repository.PacienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;

@Service
public class PacienteImportacionWriterImpl implements PacienteImportacionWriter {
  private final PacienteRepository pacienteRepository;
  private final AntecedentesRepository antecedentesRepository;

  public PacienteImportacionWriterImpl(
      PacienteRepository pacienteRepository,
      AntecedentesRepository antecedentesRepository
  ) {
    this.pacienteRepository = pacienteRepository;
    this.antecedentesRepository = antecedentesRepository;
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Integer registrar(PacienteImportacionFila fila) {
    PacienteImportacionDatos datos = fila.getPaciente();
    Paciente paciente = new Paciente();
    paciente.setEstadoRegistro(EstadoRegistroPaciente.ACTIVO);
    paciente.setApellidos(datos.getApellidos());
    paciente.setNombres(datos.getNombres());
    paciente.setFechaNacimiento(Date.valueOf(datos.getFechaNacimiento()));
    paciente.setFechaIngreso(new java.util.Date());
    paciente.setEstadoCivil(datos.getEstadoCivil());
    paciente.setNumDocumento(datos.getDni());
    paciente.setSexo(datos.getSexo());
    paciente.setDireccion(datos.getDireccion());
    paciente.setDistrito(datos.getDistrito());
    paciente.setTraidoPor(datos.getTraidoPor());
    Paciente guardado = pacienteRepository.save(paciente);

    PacienteImportacionAntecedentes datosAntecedentes = fila.getAntecedentes();
    Antecedentes antecedentes = new Antecedentes();
    antecedentes.setPaciente(guardado);
    antecedentes.setAlimentacion(datosAntecedentes.getAlimentacion());
    antecedentes.setHabitos(datosAntecedentes.getHabitos());
    antecedentes.setVivienda(datosAntecedentes.getVivienda());
    antecedentes.setDesarrolloPsicomotor(datosAntecedentes.getDesarrolloPsicomotor());
    antecedentes.setVacunas(datosAntecedentes.getVacunas());
    antecedentes.setEducacion(datosAntecedentes.getEducacion());
    antecedentes.setEnfermedadesPrevias(datosAntecedentes.getEnfermedadesPrevias());
    antecedentes.setCirugiasPrevias(datosAntecedentes.getCirugiasPrevias());
    antecedentes.setAlergiaMedicamentos(datosAntecedentes.getAlergiasMedicamentos());
    antecedentesRepository.save(antecedentes);
    return guardado.getIdPaciente();
  }
}
