package com.krivi.apihistorialmedico.business.importacion;

import com.krivi.apihistorialmedico.business.services.impl.importacion.PacienteImportacionWriterImpl;
import com.krivi.apihistorialmedico.model.entity.Antecedentes;
import com.krivi.apihistorialmedico.model.entity.Paciente;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionAntecedentes;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionDatos;
import com.krivi.apihistorialmedico.model.importacion.PacienteImportacionFila;
import com.krivi.apihistorialmedico.repository.AntecedentesRepository;
import com.krivi.apihistorialmedico.repository.PacienteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PacienteImportacionWriterImplTest {
  private final PacienteRepository pacienteRepository = mock(PacienteRepository.class);
  private final AntecedentesRepository antecedentesRepository = mock(AntecedentesRepository.class);
  private final PacienteImportacionWriterImpl writer =
      new PacienteImportacionWriterImpl(pacienteRepository, antecedentesRepository);

  @Test
  void registraPacienteYAntecedentesConLosDatosNormalizados() {
    when(pacienteRepository.save(any())).thenAnswer(invocacion -> {
      Paciente paciente = invocacion.getArgument(0);
      paciente.setIdPaciente(25);
      return paciente;
    });

    assertThat(writer.registrar(fila())).isEqualTo(25);

    verify(pacienteRepository).save(org.mockito.ArgumentMatchers.argThat(paciente ->
        "01234567".equals(paciente.getNumDocumento())
            && "SOLTERO".equals(paciente.getEstadoCivil())
            && paciente.getFechaIngreso() != null));
    verify(antecedentesRepository).save(org.mockito.ArgumentMatchers.argThat(antecedentes ->
        antecedentes.getPaciente().getIdPaciente() == 25
            && "No refiere".equals(antecedentes.getAlergiaMedicamentos())));
  }

  @Test
  void usaUnaTransaccionNuevaPorFila() throws Exception {
    Transactional transactional = PacienteImportacionWriterImpl.class
        .getMethod("registrar", PacienteImportacionFila.class)
        .getAnnotation(Transactional.class);

    assertThat(transactional).isNotNull();
    assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
  }

  @Test
  void propagaElErrorDeAntecedentesParaQueLaTransaccionRevierta() {
    when(pacienteRepository.save(any())).thenAnswer(invocacion -> {
      Paciente paciente = invocacion.getArgument(0);
      paciente.setIdPaciente(25);
      return paciente;
    });
    when(antecedentesRepository.save(any(Antecedentes.class)))
        .thenThrow(new RuntimeException("fallo simulado"));

    assertThatThrownBy(() -> writer.registrar(fila())).isInstanceOf(RuntimeException.class);
  }

  private PacienteImportacionFila fila() {
    return PacienteImportacionFila.builder()
        .paciente(PacienteImportacionDatos.builder().apellidos("Pérez").nombres("Ana")
            .fechaNacimiento(LocalDate.of(1990, 5, 10)).estadoCivil("SOLTERO")
            .dni("01234567").sexo("F").direccion("Dirección").distrito("Lima").traidoPor("").build())
        .antecedentes(PacienteImportacionAntecedentes.builder().alimentacion("Balanceada")
            .habitos("No refiere").vivienda("Casa").desarrolloPsicomotor("Normal")
            .vacunas("Completas").educacion("S1").enfermedadesPrevias("Ninguna")
            .cirugiasPrevias("Ninguna").alergiasMedicamentos("No refiere").build())
        .build();
  }
}
