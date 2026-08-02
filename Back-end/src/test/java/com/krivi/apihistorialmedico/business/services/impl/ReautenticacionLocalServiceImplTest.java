package com.krivi.apihistorialmedico.business.services.impl;

import com.krivi.apihistorialmedico.business.exception.ReautenticacionException;
import com.krivi.apihistorialmedico.model.api.ReautenticacionRequest;
import com.krivi.apihistorialmedico.model.entity.Empleado;
import com.krivi.apihistorialmedico.model.entity.Usuario;
import com.krivi.apihistorialmedico.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReautenticacionLocalServiceImplTest {
  private static final String CLAVE = "clave-local";

  @Mock UsuarioRepository usuarioRepository;
  @InjectMocks ReautenticacionLocalServiceImpl service;

  @Test
  void autorizaAdministradorActivoConContrasenaCorrecta() {
    prepararUsuario(usuarioActivo("ADMINISTRADOR", CLAVE));

    var response = service.reautenticar(7, request(CLAVE));

    assertTrue(response.isAutorizado());
    assertTrue(response.isPuedeArchivarPacientes());
    assertEquals("ADMINISTRADOR", response.getCargo());
    assertEquals("AUTORIZADO", response.getResultado());
  }

  @Test
  void autorizaEnfermeroActivoConContrasenaCorrecta() {
    prepararUsuario(usuarioActivo("ENFERMERO", CLAVE));

    var response = service.reautenticar(7, request(CLAVE));

    assertTrue(response.isAutorizado());
    assertEquals("ENFERMERO", response.getCargo());
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "ENFERMERO",
      "ENFERMERA",
      "ENFERMERA(O)",
      "ENFERMERO(A)",
      "ENFERMERIA",
      "  enfermera(o)  ",
      " enfermero(a) ",
      "  enfermería  "
  })
  void autorizaAliasDeEnfermeriaYLosNormalizaComoEnfermero(String cargo) {
    prepararUsuario(usuarioActivo(cargo, CLAVE));

    var response = service.reautenticar(7, request(CLAVE));

    assertTrue(response.isAutorizado());
    assertTrue(response.isPuedeArchivarPacientes());
    assertEquals("ENFERMERO", response.getCargo());
  }

  @Test
  void normalizaCargoPermitidoEnMinusculasConEspacios() {
    prepararUsuario(usuarioActivo("  administrador  ", CLAVE));

    var response = service.reautenticar(7, request(CLAVE));

    assertEquals("ADMINISTRADOR", response.getCargo());
    assertTrue(response.isAutorizado());
  }

  @Test
  void rechazaDoctorAunqueLaContrasenaSeaCorrecta() {
    prepararUsuario(usuarioActivo("Doctor", CLAVE));

    ReautenticacionException error = assertThrows(ReautenticacionException.class,
        () -> service.reautenticar(7, request(CLAVE)));

    assertEquals("CARGO_NO_AUTORIZADO", error.getResultado());
    assertEquals("DOCTOR", error.getCargo());
  }

  @Test
  void rechazaMedicoConTildeAunqueLaContrasenaSeaCorrecta() {
    prepararUsuario(usuarioActivo("Médico", CLAVE));

    ReautenticacionException error = assertThrows(ReautenticacionException.class,
        () -> service.reautenticar(7, request(CLAVE)));

    assertEquals("CARGO_NO_AUTORIZADO", error.getResultado());
    assertEquals("MEDICO", error.getCargo());
  }

  @ParameterizedTest
  @ValueSource(strings = {"DOCTOR", "doctor", "MEDICO", "MÉDICO", " médico "})
  void mantieneSinPermisoLosCargosMedicos(String cargo) {
    prepararUsuario(usuarioActivo(cargo, CLAVE));

    ReautenticacionException error = assertThrows(ReautenticacionException.class,
        () -> service.reautenticar(7, request(CLAVE)));

    assertEquals("CARGO_NO_AUTORIZADO", error.getResultado());
  }

  @Test
  void rechazaIdentificadorAusenteSinConsultarRepositorio() {
    ReautenticacionException error = assertThrows(ReautenticacionException.class,
        () -> service.reautenticar(null, request(CLAVE)));

    assertEquals("USUARIO_REQUERIDO", error.getResultado());
    verifyNoInteractions(usuarioRepository);
  }

  @Test
  void rechazaUsuarioInexistente() {
    when(usuarioRepository.findById(7)).thenReturn(Optional.empty());

    ReautenticacionException error = assertThrows(ReautenticacionException.class,
        () -> service.reautenticar(7, request(CLAVE)));

    assertEquals("USUARIO_NO_ENCONTRADO", error.getResultado());
  }

  @Test
  void rechazaUsuarioInactivo() {
    Usuario usuario = usuarioActivo("ADMINISTRADOR", CLAVE);
    usuario.setEstado(false);
    prepararUsuario(usuario);

    ReautenticacionException error = assertThrows(ReautenticacionException.class,
        () -> service.reautenticar(7, request(CLAVE)));

    assertEquals("USUARIO_INACTIVO", error.getResultado());
  }

  @Test
  void rechazaEmpleadoInexistente() {
    Usuario usuario = usuarioActivo("ADMINISTRADOR", CLAVE);
    usuario.setEmpleado(null);
    prepararUsuario(usuario);

    ReautenticacionException error = assertThrows(ReautenticacionException.class,
        () -> service.reautenticar(7, request(CLAVE)));

    assertEquals("EMPLEADO_NO_ENCONTRADO", error.getResultado());
  }

  @Test
  void rechazaEmpleadoInactivo() {
    Usuario usuario = usuarioActivo("ADMINISTRADOR", CLAVE);
    usuario.getEmpleado().setEstado(false);
    prepararUsuario(usuario);

    ReautenticacionException error = assertThrows(ReautenticacionException.class,
        () -> service.reautenticar(7, request(CLAVE)));

    assertEquals("EMPLEADO_INACTIVO", error.getResultado());
  }

  @Test
  void rechazaCargoVacioODesconocido() {
    prepararUsuario(usuarioActivo("   ", CLAVE));
    assertEquals("CARGO_NO_AUTORIZADO", assertThrows(ReautenticacionException.class,
        () -> service.reautenticar(7, request(CLAVE))).getResultado());

    prepararUsuario(usuarioActivo("RECEPCIONISTA", CLAVE));
    assertEquals("CARGO_NO_AUTORIZADO", assertThrows(ReautenticacionException.class,
        () -> service.reautenticar(7, request(CLAVE))).getResultado());
  }

  @Test
  void rechazaContrasenaNulaOVacia() {
    prepararUsuario(usuarioActivo("ADMINISTRADOR", CLAVE));

    assertEquals("CONTRASENA_REQUERIDA", assertThrows(ReautenticacionException.class,
        () -> service.reautenticar(7, request(null))).getResultado());
    assertEquals("CONTRASENA_REQUERIDA", assertThrows(ReautenticacionException.class,
        () -> service.reautenticar(7, request(""))).getResultado());
    assertEquals("CONTRASENA_REQUERIDA", assertThrows(ReautenticacionException.class,
        () -> service.reautenticar(7, request("   "))).getResultado());
  }

  @Test
  void rechazaContrasenaIncorrectaSinExponerla() {
    prepararUsuario(usuarioActivo("ADMINISTRADOR", CLAVE));
    String incorrecta = "secreto-incorrecto";

    ReautenticacionException error = assertThrows(ReautenticacionException.class,
        () -> service.reautenticar(7, request(incorrecta)));

    assertEquals("CONTRASENA_INCORRECTA", error.getResultado());
    assertFalse(error.getMessage().contains(incorrecta));
    assertFalse(error.toString().contains(incorrecta));
  }

  @Test
  void noModificaEntidadesNiDependeDeArchivadoOAuditoria() {
    Usuario usuario = usuarioActivo("ENFERMERO", CLAVE);
    prepararUsuario(usuario);

    service.reautenticar(7, request(CLAVE));

    assertEquals(CLAVE, usuario.getContrasena());
    assertTrue(usuario.getEstado());
    assertTrue(usuario.getEmpleado().getEstado());
    verify(usuarioRepository, never()).save(any());
    assertArrayEquals(new Class<?>[]{UsuarioRepository.class},
        ReautenticacionLocalServiceImpl.class.getConstructors()[0].getParameterTypes());
  }

  private void prepararUsuario(Usuario usuario) {
    when(usuarioRepository.findById(7)).thenReturn(Optional.of(usuario));
  }

  private Usuario usuarioActivo(String cargo, String contrasena) {
    Empleado empleado = new Empleado();
    empleado.setIdEmpleado(3);
    empleado.setEstado(true);
    empleado.setCargo(cargo);
    Usuario usuario = new Usuario();
    usuario.setIdUsuario(7);
    usuario.setEstado(true);
    usuario.setContrasena(contrasena);
    usuario.setEmpleado(empleado);
    return usuario;
  }

  private ReautenticacionRequest request(String contrasena) {
    ReautenticacionRequest request = new ReautenticacionRequest();
    request.setContrasena(contrasena);
    return request;
  }
}
