package com.krivi.apihistorialmedico.integration.dni;

import com.krivi.apihistorialmedico.business.exception.DniConsultaException;
import com.krivi.apihistorialmedico.model.api.ConsultaDniResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

@Component
public class DemoDniProviderClient implements DniProviderClient {
  private final Map<String, DemoPersona> personas = Map.of(
      "12345678", new DemoPersona("JUAN CARLOS", "PEREZ GOMEZ", LocalDate.of(1998, 5, 10), "M"),
      "87654321", new DemoPersona("MARIA ELENA", "QUISPE ROJAS", LocalDate.of(2001, 3, 22), "F"),
      "45123678", new DemoPersona("JOSEFINA VERA", "MENDOZA DAVALOS", LocalDate.of(1999, 12, 2), "F"),
      "47891234", new DemoPersona("LUIS ALBERTO", "RAMIREZ TORRES", LocalDate.of(1991, 8, 11), "M")
  );

  @Override
  public ConsultaDniResponse consultar(String dni) {
    DemoPersona persona = personas.get(dni);
    if (persona == null) {
      throw new DniConsultaException(
          "DNI_NO_ENCONTRADO",
          "No se encontraron datos para el DNI ingresado.",
          HttpStatus.NOT_FOUND
      );
    }

    return ConsultaDniResponse.builder()
        .dni(dni)
        .nombres(persona.nombres())
        .apellidos(persona.apellidos())
        .fechaNacimiento(Date.from(persona.fechaNacimiento().atStartOfDay(ZoneId.systemDefault()).toInstant()))
        .edad(Period.between(persona.fechaNacimiento(), LocalDate.now()).getYears())
        .sexo(persona.sexo())
        .fuente("DEMO")
        .build();
  }

  private record DemoPersona(String nombres, String apellidos, LocalDate fechaNacimiento, String sexo) { }
}
