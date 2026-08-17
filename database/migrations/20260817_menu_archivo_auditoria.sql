-- Fase 1: opción administrativa "Archivo y auditoría".
-- Crea o actualiza el menú y lo asigna exclusivamente a usuarios Administradores.
USE `historiaclinicadb`;

INSERT INTO `menu` (`nombre`, `ruta`, `imagen`, `estado`)
SELECT 'Archivo y auditoría', '/auditoria', 'pi pi-history', 1
WHERE NOT EXISTS (SELECT 1 FROM `menu` WHERE `ruta` = '/auditoria');

UPDATE `menu`
SET `nombre` = 'Archivo y auditoría', `imagen` = 'pi pi-history', `estado` = 1
WHERE `ruta` = '/auditoria';

-- Elimina asignaciones incorrectas previas antes de aplicar la regla por cargo.
DELETE dp
FROM `detallepermiso` dp
INNER JOIN `menu` m ON m.`idmenu` = dp.`idmenu`
INNER JOIN `usuario` u ON u.`idusuario` = dp.`idusuario`
INNER JOIN `empleado` e ON e.`idempleado` = u.`idempleado`
WHERE m.`ruta` = '/auditoria'
  AND UPPER(TRIM(e.`cargo`)) <> 'ADMINISTRADOR';

INSERT INTO `detallepermiso` (`idmenu`, `idusuario`)
SELECT m.`idmenu`, u.`idusuario`
FROM `menu` m
INNER JOIN `usuario` u ON u.`estado` = 1
INNER JOIN `empleado` e ON e.`idempleado` = u.`idempleado` AND e.`estado` = 1
WHERE m.`ruta` = '/auditoria'
  AND UPPER(TRIM(e.`cargo`)) = 'ADMINISTRADOR'
  AND NOT EXISTS (
    SELECT 1 FROM `detallepermiso` dp
    WHERE dp.`idmenu` = m.`idmenu` AND dp.`idusuario` = u.`idusuario`
  );
