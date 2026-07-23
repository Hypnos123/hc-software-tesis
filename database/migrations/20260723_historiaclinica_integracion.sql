-- Migración manual, defensiva e idempotente para instalaciones existentes.
-- No elimina ni corrige historias clínicas duplicadas automáticamente.
-- La sesión se fija explícitamente a UTC-05:00 para que NOW()/CURRENT_TIMESTAMP
-- sea coherente con America/Lima (Lima no usa horario de verano).

USE `historiaclinicadb`;
SET time_zone = '-05:00';

DELIMITER $$

DROP PROCEDURE IF EXISTS `migrar_historiaclinica_integracion`$$
CREATE PROCEDURE `migrar_historiaclinica_integracion`()
BEGIN
  DECLARE columna_fecha_existe INT DEFAULT 0;
  DECLARE columna_actualizacion_existe INT DEFAULT 0;
  DECLARE indice_fecha_existe INT DEFAULT 0;
  DECLARE unique_paciente_existe INT DEFAULT 0;
  DECLARE duplicados_por_paciente INT DEFAULT 0;

  SELECT COUNT(*) INTO columna_fecha_existe FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'historiaclinica' AND COLUMN_NAME = 'fechacreacion';
  IF columna_fecha_existe = 0 THEN
    ALTER TABLE `historiaclinica` ADD COLUMN `fechacreacion` DATETIME NULL AFTER `idhistoriaclinica`;
  END IF;

  SELECT COUNT(*) INTO columna_actualizacion_existe FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'historiaclinica' AND COLUMN_NAME = 'ultimaactualizacion';
  IF columna_actualizacion_existe = 0 THEN
    ALTER TABLE `historiaclinica` ADD COLUMN `ultimaactualizacion` DATETIME NULL AFTER `fechacreacion`;
  END IF;

  UPDATE `historiaclinica`
  SET `fechacreacion` = COALESCE(`ultimaactualizacion`, NOW())
  WHERE `fechacreacion` IS NULL;
  UPDATE `historiaclinica`
  SET `ultimaactualizacion` = `fechacreacion`
  WHERE `ultimaactualizacion` IS NULL;

  ALTER TABLE `historiaclinica`
    MODIFY COLUMN `fechacreacion` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    MODIFY COLUMN `ultimaactualizacion` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

  SELECT COUNT(*) INTO indice_fecha_existe FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'historiaclinica' AND INDEX_NAME = 'idx_historiaclinica_fechacreacion';
  IF indice_fecha_existe = 0 THEN
    CREATE INDEX `idx_historiaclinica_fechacreacion` ON `historiaclinica` (`fechacreacion`);
  END IF;

  SELECT COUNT(*) INTO unique_paciente_existe FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'historiaclinica' AND COLUMN_NAME = 'idpaciente' AND NON_UNIQUE = 0;
  IF unique_paciente_existe = 0 THEN
    SELECT COUNT(*) INTO duplicados_por_paciente FROM (
      SELECT `idpaciente` FROM `historiaclinica` WHERE `idpaciente` IS NOT NULL GROUP BY `idpaciente` HAVING COUNT(*) > 1
    ) duplicados;
    IF duplicados_por_paciente > 0 THEN
      SELECT `idpaciente`, COUNT(*) AS cantidad
      FROM `historiaclinica` WHERE `idpaciente` IS NOT NULL GROUP BY `idpaciente` HAVING COUNT(*) > 1 ORDER BY `idpaciente`;
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No se agregó UNIQUE(idpaciente): revise manualmente los duplicados listados.';
    END IF;
    ALTER TABLE `historiaclinica` ADD CONSTRAINT `uk_historiaclinica_paciente` UNIQUE (`idpaciente`);
  END IF;
END$$

CALL `migrar_historiaclinica_integracion`()$$
DROP PROCEDURE `migrar_historiaclinica_integracion`$$

DELIMITER ;
