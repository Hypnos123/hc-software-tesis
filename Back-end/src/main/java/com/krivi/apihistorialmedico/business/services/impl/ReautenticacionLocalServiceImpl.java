package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.exception.ReautenticacionException;
import com.krivi.apihistorialmedico.business.services.ReautenticacionLocalService;
import com.krivi.apihistorialmedico.model.api.ReautenticacionRequest;
import com.krivi.apihistorialmedico.model.api.ReautenticacionResponse;
import com.krivi.apihistorialmedico.model.entity.Empleado;
import com.krivi.apihistorialmedico.model.entity.Usuario;
import com.krivi.apihistorialmedico.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Objects;
import java.util.Set;

@Service
public class ReautenticacionLocalServiceImpl implements ReautenticacionLocalService {
  private static final Set<String> CARGOS_PERMITIDOS = Set.of("ADMINISTRADOR", "ENFERMERO");

  private final UsuarioRepository usuarioRepository;

  public ReautenticacionLocalServiceImpl(UsuarioRepository usuarioRepository) {
    this.usuarioRepository = usuarioRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public ReautenticacionResponse reautenticar(Integer idUsuarioActual, ReautenticacionRequest request) {
    if (idUsuarioActual == null) {
      throw error("USUARIO_REQUERIDO", "Debe indicar el usuario actual.", null, HttpStatus.BAD_REQUEST);
    }

    Usuario usuario = usuarioRepository.findById(idUsuarioActual)
        .orElseThrow(() -> error("USUARIO_NO_ENCONTRADO", "El usuario actual no existe.", null, HttpStatus.NOT_FOUND));
    if (!Boolean.TRUE.equals(usuario.getEstado())) {
      throw error("USUARIO_INACTIVO", "El usuario actual está inactivo.", null, HttpStatus.FORBIDDEN);
    }

    Empleado empleado = usuario.getEmpleado();
    if (empleado == null || empleado.getIdEmpleado() == null) {
      throw error("EMPLEADO_NO_ENCONTRADO", "El usuario no tiene un empleado asociado.", null, HttpStatus.NOT_FOUND);
    }
    if (!Boolean.TRUE.equals(empleado.getEstado())) {
      throw error("EMPLEADO_INACTIVO", "El empleado asociado está inactivo.", null, HttpStatus.FORBIDDEN);
    }

    String cargo = normalizarCargo(empleado.getCargo());
    if (cargo.isEmpty() || !CARGOS_PERMITIDOS.contains(cargo)) {
      throw error("CARGO_NO_AUTORIZADO", "El cargo del usuario no permite archivar pacientes.",
          cargo.isEmpty() ? null : cargo, HttpStatus.FORBIDDEN);
    }

    String contrasena = request == null ? null : request.getContrasena();
    if (contrasena == null || contrasena.isBlank()) {
      throw error("CONTRASENA_REQUERIDA", "Debe ingresar la contraseña.", cargo, HttpStatus.BAD_REQUEST);
    }
    if (!Objects.equals(contrasena, usuario.getContrasena())) {
      throw error("CONTRASENA_INCORRECTA", "La contraseña ingresada no es correcta.", cargo, HttpStatus.UNAUTHORIZED);
    }

    return ReautenticacionResponse.builder()
        .autorizado(true)
        .cargo(cargo)
        .puedeArchivarPacientes(true)
        .resultado("AUTORIZADO")
        .mensaje("Identidad validada correctamente.")
        .build();
  }

  static String normalizarCargo(String cargo) {
    if (cargo == null) return "";
    return Normalizer.normalize(cargo.trim(), Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .replaceAll("\\s+", " ")
        .toUpperCase();
  }

  private ReautenticacionException error(String resultado, String mensaje, String cargo, HttpStatus status) {
    return new ReautenticacionException(resultado, mensaje, cargo, status);
  }
}
