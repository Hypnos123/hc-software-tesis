package com.krivi.apihistorialmedico.business.expose.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krivi.apihistorialmedico.business.exception.CreacionHistoriaClinicaException;
import com.krivi.apihistorialmedico.business.services.HistoriaClinicaService;
import com.krivi.apihistorialmedico.model.api.HistoriaClinicaRequest;
import com.krivi.apihistorialmedico.model.api.HistoriaClinicaUpdateRequest;
import com.krivi.apihistorialmedico.model.api.ResponseModelSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HistoriaClinicaControllerTest {
  private HistoriaClinicaService service;
  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @BeforeEach
  void setUp() {
    service = mock(HistoriaClinicaService.class);
    HistoriaClinicaController controller = new HistoriaClinicaController();
    controller.historiaClinicaService = service;
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
  }

  @Test
  void respondeCreatedSoloCuandoLaHistoriaFueCreada() throws Exception {
    ResponseModelSet response = new ResponseModelSet("Registro guardado correctamente.", null, 101);
    when(service.save(any())).thenReturn(response);

    mockMvc.perform(post("/historiaClinica/insert").contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(requestValido())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.idGenerado").value(101));
  }

  @Test
  void respondeBadRequestParaDniInvalido() throws Exception {
    verificarError(HttpStatus.BAD_REQUEST, "DNI_INVALIDO", "El DNI debe contener exactamente ocho dígitos.");
  }

  @Test
  void respondeNotFoundParaPacienteInexistente() throws Exception {
    verificarError(HttpStatus.NOT_FOUND, "PACIENTE_NO_ENCONTRADO", "No existe un paciente registrado con el DNI ingresado.");
  }

  @Test
  void respondeConflictParaDniAmbiguo() throws Exception {
    verificarError(HttpStatus.CONFLICT, "DNI_AMBIGUO", "El DNI está asociado a varios pacientes.");
  }

  @Test
  void actualizaUsandoElIdDeLaRutaYUnBodySinIdentificadores() throws Exception {
    ResponseModelSet response = new ResponseModelSet("Registro actualizado correctamente.", null, 25);
    when(service.update(eq(25), any())).thenReturn(response);

    mockMvc.perform(put("/historiaClinica/update/25").contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateValido())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.idGenerado").value(25));
  }

  @Test
  void respondeNotFoundCuandoLaHistoriaAEditarNoExiste() throws Exception {
    when(service.update(eq(999), any())).thenThrow(new CreacionHistoriaClinicaException(
        "HISTORIA_NO_ENCONTRADA", "La historia clínica no existe.", HttpStatus.NOT_FOUND));

    mockMvc.perform(put("/historiaClinica/update/999").contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateValido())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.codigo").value("HISTORIA_NO_ENCONTRADA"));
  }

  private void verificarError(HttpStatus status, String codigo, String mensaje) throws Exception {
    when(service.save(any())).thenThrow(new CreacionHistoriaClinicaException(codigo, mensaje, status));

    mockMvc.perform(post("/historiaClinica/insert").contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(requestValido())))
        .andExpect(status().is(status.value()))
        .andExpect(jsonPath("$.codigo").value(codigo))
        .andExpect(jsonPath("$.mensaje").value(mensaje));
  }

  private HistoriaClinicaRequest requestValido() {
    HistoriaClinicaRequest request = new HistoriaClinicaRequest();
    request.setFechaIngreso(LocalDate.of(2026, 7, 27));
    request.setFechaNacimiento(LocalDate.of(1996, 1, 1));
    request.setApellidos("Pérez");
    request.setNombres("Ana");
    request.setEstadoCivil("SOLTERO");
    request.setDni("12345678");
    return request;
  }

  private HistoriaClinicaUpdateRequest updateValido() {
    HistoriaClinicaUpdateRequest request = new HistoriaClinicaUpdateRequest();
    request.setFechaIngreso(LocalDate.of(2026, 7, 27));
    request.setFechaNacimiento(LocalDate.of(1995, 5, 10));
    request.setApellidos("Pérez Actualizado");
    request.setNombres("Ana Actualizada");
    request.setEstadoCivil("CASADO");
    request.setEnfermedadesPrevias("Asma controlada");
    return request;
  }
}
