package com.krivi.apihistorialmedico.business.services;

import com.krivi.apihistorialmedico.model.api.ArchivarPacienteDuplicadoRequest;
import com.krivi.apihistorialmedico.model.api.ArchivarPacienteDuplicadoResponse;
import com.krivi.apihistorialmedico.model.api.AuditoriaArchivadoPacienteResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface ArchivadoPacienteDuplicadoService {
  ArchivarPacienteDuplicadoResponse archivar(Integer idUsuarioActual, Integer idPacienteArchivado,
                                             ArchivarPacienteDuplicadoRequest request);

  List<AuditoriaArchivadoPacienteResponse> consultarAuditoria(Integer idUsuarioActual, String dni,
                                                               Integer idPaciente, LocalDateTime desde,
                                                               LocalDateTime hasta);
}
