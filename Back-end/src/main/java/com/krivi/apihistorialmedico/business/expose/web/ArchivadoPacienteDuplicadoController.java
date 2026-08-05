package com.krivi.apihistorialmedico.business.expose.web;

import com.krivi.apihistorialmedico.business.exception.ArchivadoPacienteDuplicadoException;
import com.krivi.apihistorialmedico.business.exception.ReautenticacionException;
import com.krivi.apihistorialmedico.business.services.ArchivadoPacienteDuplicadoService;
import com.krivi.apihistorialmedico.model.api.ArchivarPacienteDuplicadoRequest;
import com.krivi.apihistorialmedico.model.api.ArchivarPacienteDuplicadoResponse;
import com.krivi.apihistorialmedico.model.api.AuditoriaArchivadoPacienteResponse;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.OptimisticLockException;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ArchivadoPacienteDuplicadoController {
  private final ArchivadoPacienteDuplicadoService archivadoService;

  public ArchivadoPacienteDuplicadoController(ArchivadoPacienteDuplicadoService archivadoService) {
    this.archivadoService = archivadoService;
  }

  @PostMapping("/pacientes/{idPacienteArchivado}/archivar-duplicado")
  public ResponseEntity<ArchivarPacienteDuplicadoResponse> archivar(
      @RequestHeader(value = "X-Usuario-Id", required = false) Integer idUsuarioActual,
      @PathVariable Integer idPacienteArchivado,
      @RequestBody(required = false) ArchivarPacienteDuplicadoRequest request) {
    return ResponseEntity.ok(archivadoService.archivar(idUsuarioActual, idPacienteArchivado, request));
  }

  @GetMapping("/auditoria/pacientes-archivados")
  public ResponseEntity<List<AuditoriaArchivadoPacienteResponse>> consultarAuditoria(
      @RequestHeader(value = "X-Usuario-Id", required = false) Integer idUsuarioActual,
      @RequestParam(required = false) String dni,
      @RequestParam(required = false) Integer idPaciente,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
    return ResponseEntity.ok(archivadoService.consultarAuditoria(idUsuarioActual, dni, idPaciente, desde, hasta));
  }

  @ExceptionHandler(ArchivadoPacienteDuplicadoException.class)
  public ResponseEntity<ArchivarPacienteDuplicadoResponse> manejarArchivado(ArchivadoPacienteDuplicadoException exception) {
    return error(exception.getStatus(), exception.getResultado(), exception.getMessage());
  }

  @ExceptionHandler(ReautenticacionException.class)
  public ResponseEntity<ArchivarPacienteDuplicadoResponse> manejarReautenticacion(ReautenticacionException exception) {
    return error(exception.getStatus(), exception.getResultado(), exception.getMessage());
  }

  @ExceptionHandler({OptimisticLockException.class, OptimisticLockingFailureException.class})
  public ResponseEntity<ArchivarPacienteDuplicadoResponse> manejarConflictoVersion() {
    return error(HttpStatus.CONFLICT, "CONFLICTO_VERSION",
        "El paciente fue modificado por otro proceso. Vuelva a consultar los duplicados.");
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ArchivarPacienteDuplicadoResponse> manejarCuerpoInvalido() {
    return error(HttpStatus.BAD_REQUEST, "CUERPO_INVALIDO", "El cuerpo de la solicitud no es válido.");
  }

  private ResponseEntity<ArchivarPacienteDuplicadoResponse> error(HttpStatus status, String resultado, String mensaje) {
    return ResponseEntity.status(status).body(ArchivarPacienteDuplicadoResponse.builder()
        .archivado(false)
        .resultado(resultado)
        .mensaje(mensaje)
        .build());
  }
}
