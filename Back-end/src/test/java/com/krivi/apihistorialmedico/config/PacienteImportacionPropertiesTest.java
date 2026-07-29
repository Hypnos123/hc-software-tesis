package com.krivi.apihistorialmedico.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class PacienteImportacionPropertiesTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withUserConfiguration(TestConfiguration.class)
      .withPropertyValues(
          "paciente.importacion.max-registros=50",
          "paciente.importacion.max-tamano-mb=2",
          "paciente.importacion.tiempo-expiracion-minutos=15",
          "paciente.importacion.max-columnas=18",
          "paciente.importacion.version-plantilla=1.0"
      );

  @Test
  void cargaLasPropiedadesTipadasDeImportacion() {
    contextRunner.run(context -> {
      assertThat(context).hasSingleBean(PacienteImportacionProperties.class);
      PacienteImportacionProperties properties = context.getBean(PacienteImportacionProperties.class);

      assertThat(properties.maxRegistros()).isEqualTo(50);
      assertThat(properties.maxTamanoMb()).isEqualTo(2);
      assertThat(properties.tiempoExpiracionMinutos()).isEqualTo(15);
      assertThat(properties.maxColumnas()).isEqualTo(18);
      assertThat(properties.versionPlantilla()).isEqualTo("1.0");
    });
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(PacienteImportacionProperties.class)
  static class TestConfiguration {
  }
}
