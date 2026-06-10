package com.krivi.apihistorialmedico.repository;


import com.krivi.apihistorialmedico.model.api.DetallePermisoResponse;
import com.krivi.apihistorialmedico.model.entity.DetallePermiso;
import com.krivi.apihistorialmedico.model.projection.DetallePermisoLoginProjection;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetallePermisoRepository extends CrudRepository<DetallePermiso, Integer> {

  List<DetallePermiso> findByIdUsuario(int idUsuario);

  @Modifying
  @Transactional
  @Query(value = "delete from detallepermiso where idusuario=?1", nativeQuery = true)
  void deleteByIdUsuario(int idUsuario);

  @Query(value = "select dp.iddetallepermiso as idDetallePermiso,dp.idusuario as idUsuario, dp.idmenu as idMenu, m.nombre as nombre, u.usuario as usuario from detallepermiso dp " +
      "inner join usuario u on u.idusuario=dp.idusuario " +
      "inner join menu m on m.idmenu=dp.idmenu " +
      "where dp.idusuario=?1", nativeQuery = true)
  List<DetallePermisoResponse> detallePermisosListar(String idUsuario);

  @Query(value = " select dp.idmenu as idMenu, m.nombre as nombre, m.ruta as ruta, m.imagen as imagen from detallepermiso dp "
      + "inner join menu m on m.idmenu=dp.idmenu "
      + "where dp.idusuario = ?1  and m.estado = true", nativeQuery = true)
  List<DetallePermisoLoginProjection> getDetallePermisoLogin(Integer idUsuario);
}
