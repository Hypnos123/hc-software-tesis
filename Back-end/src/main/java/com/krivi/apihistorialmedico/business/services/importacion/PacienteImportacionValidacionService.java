package com.krivi.apihistorialmedico.business.services.importacion;

import com.krivi.apihistorialmedico.model.api.importacion.PacienteImportacionValidacionResponse;
import org.springframework.web.multipart.MultipartFile;

public interface PacienteImportacionValidacionService {
  PacienteImportacionValidacionResponse validar(MultipartFile archivo);
}
