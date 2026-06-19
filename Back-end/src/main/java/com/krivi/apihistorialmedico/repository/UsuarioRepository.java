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
  @Query(value = "update usuario set estado=0 where idusuario=?1", nativeQuery = true)
  void setInactive(int idUsuario);

  @Query(value = "select idUsuario as Id from usuario order by idusuario desc limit 1", nativeQuery = true)
  List<Id> getUltimoRegistro();

  @Query(value = "select u.idusuario as idUsuario, u.usuario as usuario, u.tipousuario as tipoUsuario, "
      + "u.estado as estadoUsuario, e.idempleado as idEmpleado, e.nombres as nombres, "
      + "e.apellidos as apellidos, e.cargo as cargo, e.estado as estadoEmpleado from usuario u "
      + "inner join empleado e on u.idempleado=e.idempleado "
      + "where u.usuario = ?1 and u.contrasena = ?2 and u.estado = true and e.estado = true", nativeQuery = true)
  List<UsuarioLoginProjection> getLogin(String usuario, String contrasena);

  Optional<Usuario> findByUsuarioAndEstado(String usuario, Boolean estado);





}
