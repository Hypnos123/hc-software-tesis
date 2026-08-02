package com.krivi.apihistorialmedico.repository;

import com.krivi.apihistorialmedico.model.entity.Antecedentes;
import com.krivi.apihistorialmedico.model.entity.EstadoRegistroPaciente;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Collection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AntecedentesRepository extends CrudRepository<Antecedentes, Integer> {

  @Query("""
      select a.paciente.idPaciente,
             count(a),
             sum(case when a.alimentacion is not null and trim(a.alimentacion) <> '' then 1 else 0 end
               + case when a.habitos is not null and trim(a.habitos) <> '' then 1 else 0 end
               + case when a.vivienda is not null and trim(a.vivienda) <> '' then 1 else 0 end
               + case when a.desarrolloPsicomotor is not null and trim(a.desarrolloPsicomotor) <> '' then 1 else 0 end
               + case when a.vacunas is not null and trim(a.vacunas) <> '' then 1 else 0 end
               + case when a.educacion is not null and trim(a.educacion) <> '' then 1 else 0 end
               + case when a.enfermedadesPrevias is not null and trim(a.enfermedadesPrevias) <> '' then 1 else 0 end
               + case when a.cirugiasPrevias is not null and trim(a.cirugiasPrevias) <> '' then 1 else 0 end
               + case when a.alergiaMedicamentos is not null and trim(a.alergiaMedicamentos) <> '' then 1 else 0 end)
        from Antecedentes a
       where a.paciente.idPaciente in :idsPaciente
       group by a.paciente.idPaciente
      """)
  List<Object[]> resumirPorPacientes(@Param("idsPaciente") Collection<Integer> idsPaciente);

  List<Antecedentes> findByPacienteIdPaciente(Integer idPaciente);
  List<Antecedentes> findAllByPacienteEstadoRegistro(EstadoRegistroPaciente estadoRegistro);
  List<Antecedentes> findByPacienteIdPacienteAndPacienteEstadoRegistro(Integer idPaciente, EstadoRegistroPaciente estadoRegistro);
  java.util.Optional<Antecedentes> findByIdAntecedentesAndPacienteEstadoRegistro(Integer idAntecedentes, EstadoRegistroPaciente estadoRegistro);
}
