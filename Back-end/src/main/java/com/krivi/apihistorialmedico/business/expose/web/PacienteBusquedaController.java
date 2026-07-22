package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.exception.BusquedaPacienteException;
import com.krivi.apihistorialmedico.business.services.PacienteService;
import com.krivi.apihistorialmedico.model.api.ApiErrorResponse;
import com.krivi.apihistorialmedico.model.api.BusquedaPacienteResponse;
import com.krivi.apihistorialmedico.model.api.DuplicadosPacientesResponse;
import com.krivi.apihistorialmedico.model.api.EstadisticasPacientesResponse;
import com.krivi.apihistorialmedico.model.api.PacientesRegistradosHoyResponse;
import com.krivi.apihistorialmedico.model.api.UltimosPacientesResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pacientes")
@Slf4j
public class PacienteBusquedaController {
  private final PacienteService pacienteService;

  public PacienteBusquedaController(PacienteService pacienteService) {
    this.pacienteService = pacienteService;
  }

  @Operation(
      summary = "Buscar pacientes para integración",
      description = "Busca por DNI (8 dígitos), ID de paciente (entero positivo de menos de 8 dígitos) o nombre completo. La respuesta contiene únicamente datos mínimos de identificación.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Búsqueda procesada", content = @Content(schema = @Schema(implementation = BusquedaPacienteResponse.class), examples = @ExampleObject(value = "{\"encontrado\":true,\"tipoResultado\":\"unico\",\"pacientes\":[{\"idPaciente\":6,\"dni\":\"78451268\",\"nombreCompleto\":\"Patricia Elena Cárdenas Torres\",\"tieneHistoriaClinica\":true}]}"))),
      @ApiResponse(responseCode = "400", description = "Criterio ausente, vacío o inválido", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "Error interno", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  })
  @GetMapping("/buscar")
  public ResponseEntity<BusquedaPacienteResponse> buscarParaIntegracion(
      @Parameter(in = ParameterIn.QUERY, required = true, description = "DNI, ID de paciente o nombre completo", example = "78451268")
      @RequestParam(required = false) String criterio) {
    return ResponseEntity.ok(pacienteService.buscarParaIntegracion(criterio));
  }

  @Operation(summary = "Obtener estadísticas de pacientes", description = "Devuelve el total de pacientes y los creados hoy según la zona horaria America/Lima.")
  @ApiResponse(responseCode = "200", description = "Estadísticas obtenidas", content = @Content(schema = @Schema(implementation = EstadisticasPacientesResponse.class), examples = @ExampleObject(value = "{\"totalPacientes\":25,\"registradosHoy\":3}")))
  @GetMapping("/estadisticas")
  public ResponseEntity<EstadisticasPacientesResponse> estadisticas() {
    return ResponseEntity.ok(pacienteService.obtenerEstadisticasParaIntegracion());
  }

  @Operation(summary = "Obtener últimos pacientes registrados", description = "Ordena por fecha técnica de creación descendente. El límite predeterminado es 5 y debe estar entre 1 y 10.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Pacientes obtenidos", content = @Content(schema = @Schema(implementation = UltimosPacientesResponse.class))),
      @ApiResponse(responseCode = "400", description = "Límite inválido", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  })
  @GetMapping("/ultimos")
  public ResponseEntity<UltimosPacientesResponse> ultimos(
      @Parameter(in = ParameterIn.QUERY, description = "Cantidad de resultados, entre 1 y 10", example = "5")
      @RequestParam(required = false) Integer limite) {
    return ResponseEntity.ok(pacienteService.obtenerUltimosParaIntegracion(limite));
  }

  @Operation(summary = "Obtener pacientes registrados hoy", description = "Calcula hoy en la zona horaria America/Lima y devuelve solo datos mínimos de registro.")
  @ApiResponse(responseCode = "200", description = "Pacientes obtenidos", content = @Content(schema = @Schema(implementation = PacientesRegistradosHoyResponse.class)))
  @GetMapping("/registrados-hoy")
  public ResponseEntity<PacientesRegistradosHoyResponse> registradosHoy() {
    return ResponseEntity.ok(pacienteService.obtenerRegistradosHoyParaIntegracion());
  }

  @Operation(summary = "Detectar pacientes duplicados por DNI", description = "Incluye solo DNI exactos no nulos ni vacíos con más de un paciente asociado.")
  @ApiResponse(responseCode = "200", description = "Duplicados obtenidos", content = @Content(schema = @Schema(implementation = DuplicadosPacientesResponse.class)))
  @GetMapping("/duplicados")
  public ResponseEntity<DuplicadosPacientesResponse> duplicados() {
    return ResponseEntity.ok(pacienteService.obtenerDuplicadosParaIntegracion());
  }

  @ExceptionHandler(BusquedaPacienteException.class)
  public ResponseEntity<ApiErrorResponse> handleBusquedaPacienteException(BusquedaPacienteException exception) {
    return ResponseEntity.status(exception.getStatus())
        .body(ApiErrorResponse.builder().codigo(exception.getCodigo()).mensaje(exception.getMessage()).build());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception exception) {
    log.error("buscarParaIntegracion(): error inesperado", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiErrorResponse.builder().codigo("ERROR_INTERNO").mensaje("No se pudo realizar la búsqueda de pacientes.").build());
  }
}
