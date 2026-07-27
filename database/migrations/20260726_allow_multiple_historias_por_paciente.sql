-- Permite varias historias clínicas por paciente sin alterar datos existentes.
-- La migración es defensiva e idempotente y conserva la clave foránea.

USE `historiaclinicadb`;

DELIMITER $$

DROP PROCEDURE IF EXISTS `permitir_multiples_historias_por_paciente`$$
CREATE PROCEDURE `permitir_multiples_historias_por_paciente`()
BEGIN
  DECLARE indice_unique_existe INT DEFAULT 0;
  DECLARE indice_normal_existe INT DEFAULT 0;

  SELECT COUNT(*) INTO indice_unique_existe
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'historiaclinica'
    AND INDEX_NAME = 'uk_historiaclinica_paciente'
    AND NON_UNIQUE = 0;

  SELECT COUNT(*) INTO indice_normal_existe
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'historiaclinica'
    AND COLUMN_NAME = 'idpaciente'
    AND SEQ_IN_INDEX = 1
    AND NON_UNIQUE = 1;

  -- Se crea primero el índice normal para que la FK nunca quede sin índice de soporte.
  IF indice_normal_existe = 0 THEN
    CREATE INDEX `idx_historiaclinica_paciente` ON `historiaclinica` (`idpaciente`);
  END IF;

  IF indice_unique_existe > 0 THEN
    ALTER TABLE `historiaclinica` DROP INDEX `uk_historiaclinica_paciente`;
  END IF;
END$$

CALL `permitir_multiples_historias_por_paciente`()$$
DROP PROCEDURE `permitir_multiples_historias_por_paciente`$$

DELIMITER ;
