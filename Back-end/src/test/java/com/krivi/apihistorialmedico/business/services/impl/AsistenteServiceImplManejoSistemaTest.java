package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.services.HistoriaClinicaService;
import com.krivi.apihistorialmedico.model.api.AsistenteRequest;
import com.krivi.apihistorialmedico.model.api.AsistenteResponse;
import com.krivi.apihistorialmedico.repository.ConsultaRepository;
import com.krivi.apihistorialmedico.repository.HistoriaClinicaRepository;
import com.krivi.apihistorialmedico.repository.PacienteRepository;
import com.krivi.apihistorialmedico.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class AsistenteServiceImplManejoSistemaTest {
  @Mock private PacienteRepository pacienteRepository;
  @Mock private HistoriaClinicaRepository historiaClinicaRepository;
  @Mock private ConsultaRepository consultaRepository;
  @Mock private UsuarioRepository usuarioRepository;
  @Mock private HistoriaClinicaService historiaClinicaService;
  @InjectMocks private AsistenteServiceImpl asistenteService;

  @Test
  void respondeLasPreguntasDeManejoDelSistema() {
    Map<String, String> casos = new LinkedHashMap<>();
    casos.put("¿Cómo registro un paciente?", "Ingresa a la sección Pacientes y haz clic en el botón Agregar Pacientes. Completa los datos personales y los antecedentes del paciente. Finalmente, haz clic en Guardar para registrar la información.");
    casos.put("¿Cómo edito los datos de un paciente?", "Ingresa a la sección Pacientes, busca al paciente que deseas modificar y haz clic en el ícono del lápiz ubicado en la columna Opciones. Actualiza los datos necesarios y guarda los cambios.");
    casos.put("¿Cómo visualizo los datos de un paciente?", "Ingresa a la sección Pacientes, busca al paciente que deseas consultar y haz clic en el ícono del ojo ubicado en la columna Opciones. El sistema mostrará todos los datos registrados del paciente.");
    casos.put("¿Cómo creo una historia clínica?", "Ingresa a la sección Historia Clínica y haz clic en el botón Agregar HC. Selecciona al paciente previamente registrado, completa los datos de la historia clínica y sus antecedentes patológicos. Finalmente, haz clic en Guardar. Recuerda que cada historia clínica debe estar asociada a un paciente registrado en el sistema.");
    casos.put("¿Cómo edito una historia clínica?", "Ingresa a la sección Historia Clínica, busca la historia que deseas modificar y haz clic en el ícono del lápiz ubicado en la columna Opciones. Actualiza los datos necesarios y guarda los cambios.");
    casos.put("¿Cómo visualizo una historia clínica?", "Ingresa a la sección Historia Clínica, busca la historia que deseas consultar y haz clic en el ícono del ojo ubicado en la columna Opciones. El sistema mostrará toda la información registrada.");
    casos.put("¿Cómo agrego una consulta médica?", "Ingresa a la sección Historia Clínica y busca la historia clínica del paciente. En la columna Opciones, haz clic en el ícono de documento para acceder a sus consultas médicas. Luego, selecciona Agregar consulta, completa los campos requeridos y guarda la información.");
    casos.put("¿Cómo comienzo la atención de una consulta médica?", "Ingresa a la sección Consultas y busca una consulta que se encuentre en estado Por atender. En la columna Opciones, haz clic en Comenzar atención, completa la evaluación médica y guarda la información para finalizar la atención.");
    casos.put("¿Cómo visualizo una consulta médica antes de atenderla?", "Ingresa a la sección Consultas, busca la consulta que deseas revisar y haz clic en el ícono del ojo ubicado en la columna Opciones. El sistema mostrará los datos de la consulta sin necesidad de comenzar la atención.");

    casos.forEach((pregunta, respuestaEsperada) -> {
      AsistenteResponse response = asistenteService.preguntar(request(pregunta), null);
      assertEquals("AYUDA_USO_SISTEMA", response.getIntencion());
      assertEquals(respuestaEsperada, response.getRespuesta());
    });
  }

  @Test
  void noExponeAyudaDeFuncionesAdministrativas() {
    assertEquals("NO_RECONOCIDA", asistenteService.preguntar(request("¿Cómo gestiono empleados?"), null).getIntencion());
    assertEquals("NO_RECONOCIDA", asistenteService.preguntar(request("¿Cómo gestiono usuarios y permisos?"), null).getIntencion());
  }

  @Test
  void muestraAyudaConMarcadoresGenericosSinPromoverBusquedaPorId() {
    AsistenteResponse response = asistenteService.preguntar(request("¿Qué preguntas puedo hacer?"), null);

    assertEquals("AYUDA_USO_SISTEMA", response.getIntencion());
    assertTrue(response.getRespuesta().contains("(PONER DNI)"));
    assertTrue(response.getRespuesta().contains("(AGREGAR NOMBRE Y DOS APELLIDOS)"));
    assertFalse(response.getRespuesta().matches("(?s).*\\b\\d{8}\\b.*"));
    assertFalse(response.getRespuesta().matches("(?is).*\\bID\\s*\\d+\\b.*"));
  }

  @Test
  void solicitaDatosDelPacienteSinPromoverId() {
    AsistenteResponse consultas = asistenteService.preguntar(request("¿El paciente tiene consultas médicas?"), null);
    AsistenteResponse historia = asistenteService.preguntar(request("¿El paciente tiene historia clínica?"), null);
    AsistenteResponse busquedaDni = asistenteService.preguntar(request("Buscar paciente por DNI"), null);
    AsistenteResponse busquedaNombre = asistenteService.preguntar(request("Buscar paciente por nombre"), null);

    assertEquals("CONSULTAS_MEDICAS_REQUIERE_PACIENTE", consultas.getIntencion());
    assertEquals("HISTORIA_CLINICA_REQUIERE_PACIENTE", historia.getIntencion());
    assertFalse(consultas.getRespuesta().toLowerCase().contains(" id"));
    assertFalse(historia.getRespuesta().toLowerCase().contains(" id"));
    assertTrue(busquedaDni.getRespuesta().contains("(PONER DNI)"));
    assertFalse(busquedaDni.getRespuesta().matches("(?s).*\\b\\d{8}\\b.*"));
    assertEquals("Ingresa el nombre y los dos apellidos del paciente.", busquedaNombre.getRespuesta());
  }

  private AsistenteRequest request(String pregunta) {
    AsistenteRequest request = new AsistenteRequest();
    request.setPregunta(pregunta);
    return request;
  }
}
