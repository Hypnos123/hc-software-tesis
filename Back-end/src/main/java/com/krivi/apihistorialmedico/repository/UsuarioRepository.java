package com.krivi.apihistorialmedico.repository;

import com.krivi.apihistorialmedico.model.entity.Usuario;
import com.krivi.apihistorialmedico.model.projection.Id;
import com.krivi.apihistorialmedico.model.projection.UsuarioLoginProjection;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends CrudRepository<Usuario, Integer> {

  List<Usuario> findByEstadoOrderByIdUsuarioDesc(Boolean estado);

  @Modifying
  @Transactional
  @Query(value = "UPDATE usuario SET estado = 0 WHERE idusuario = ?1",
          nativeQuery = true)
  void setInactive(int idUsuario);

  @Query(value = """
        SELECT idusuario AS id
        FROM usuario
        ORDER BY idusuario DESC
        LIMIT 1
        """, nativeQuery = true)
  List<Id> getUltimoRegistro();

  @Query(value = """
        SELECT
            u.idusuario AS idUsuario,
            u.usuario AS usuario,
            u.tipousuario AS tipoUsuario,
            CAST(u.estado AS SIGNED) AS estadoUsuario,
            e.idempleado AS idEmpleado,
            e.nombres AS nombres,
            e.apellidos AS apellidos,
            e.cargo AS cargo,
            CAST(e.estado AS SIGNED) AS estadoEmpleado
        FROM usuario u
        INNER JOIN empleado e
            ON u.idempleado = e.idempleado
        WHERE u.usuario = ?1
          AND u.contrasena = ?2
          AND u.estado = 1
          AND e.estado = 1
        """, nativeQuery = true)
  List<UsuarioLoginProjection> getLogin(
          String usuario,
          String contrasena
  );

  Optional<Usuario> findByUsuarioAndEstado(
          String usuario,
          Boolean estado
  );
}