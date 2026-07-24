-- Migración manual, defensiva e idempotente para la integración administrativa de consultas médicas.
-- No rellena fechaatencion histórica, no altera estados y no elimina datos.
USE `historiaclinicadb`;
SET time_zone = '-05:00';

DELIMITER $$

DROP PROCEDURE IF EXISTS `migrar_consulta_medica_integracion`$$
CREATE PROCEDURE `migrar_consulta_medica_integracion`()
BEGIN
  DECLARE columna_fecha_atencion_existe INT DEFAULT 0;
  DECLARE indice_fecha_atencion_existe INT DEFAULT 0;
  DECLARE indice_estado_fecha_existe INT DEFAULT 0;
  DECLARE indice_fecha_creacion_existe INT DEFAULT 0;

  SELECT COUNT(*) INTO columna_fecha_atencion_existe
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'consulta' AND COLUMN_NAME = 'fechaatencion';
  IF columna_fecha_atencion_existe = 0 THEN
    ALTER TABLE `consulta` ADD COLUMN `fechaatencion` DATETIME NULL AFTER `fechacreacion`;
  END IF;

  SELECT COUNT(*) INTO indice_fecha_atencion_existe
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'consulta' AND INDEX_NAME = 'idx_consulta_fechaatencion';
  IF indice_fecha_atencion_existe = 0 THEN
    CREATE INDEX `idx_consulta_fechaatencion` ON `consulta` (`fechaatencion`);
  END IF;

  SELECT COUNT(*) INTO indice_estado_fecha_existe
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'consulta' AND INDEX_NAME = 'idx_consulta_estado_fechacreacion';
  IF indice_estado_fecha_existe = 0 THEN
    CREATE INDEX `idx_consulta_estado_fechacreacion` ON `consulta` (`estado`, `fechacreacion`);
  END IF;

  SELECT COUNT(*) INTO indice_fecha_creacion_existe
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'consulta' AND INDEX_NAME = 'idx_consulta_fechacreacion';
  IF indice_fecha_creacion_existe = 0 THEN
    CREATE INDEX `idx_consulta_fechacreacion` ON `consulta` (`fechacreacion`);
  END IF;
END$$

CALL `migrar_consulta_medica_integracion`()$$
DROP PROCEDURE `migrar_consulta_medica_integracion`$$

DELIMITER ;
