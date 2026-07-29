package com.krivi.apihistorialmedico.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "paciente.importacion")
public record PacienteImportacionProperties(
    int maxRegistros,
    int maxTamanoMb,
    int tiempoExpiracionMinutos,
    int maxColumnas,
    String versionPlantilla
) {
}
