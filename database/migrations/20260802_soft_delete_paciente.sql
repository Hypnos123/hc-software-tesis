-- Fase 2: prepara el archivado lógico de pacientes sin eliminar datos relacionados.
-- Migración manual, defensiva e idempotente para instalaciones existentes.
-- Ejecutar una sola vez sobre una copia de respaldo verificada.

USE `historiaclinicadb`;
SET time_zone = '-05:00';

DELIMITER $$

DROP PROCEDURE IF EXISTS `migrar_soft_delete_paciente`$$
CREATE PROCEDURE `migrar_soft_delete_paciente`()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'paciente' AND COLUMN_NAME = 'ultimaactualizacion') THEN
    ALTER TABLE `paciente` ADD COLUMN `ultimaactualizacion` DATETIME NULL AFTER `fechacreacion`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'paciente' AND COLUMN_NAME = 'estadoregistro') THEN
    ALTER TABLE `paciente` ADD COLUMN `estadoregistro` VARCHAR(20) NULL AFTER `ultimaactualizacion`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'paciente' AND COLUMN_NAME = 'fechaarchivado') THEN
    ALTER TABLE `paciente` ADD COLUMN `fechaarchivado` DATETIME NULL AFTER `estadoregistro`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'paciente' AND COLUMN_NAME = 'idusuarioarchivado') THEN
    ALTER TABLE `paciente` ADD COLUMN `idusuarioarchivado` INT NULL AFTER `fechaarchivado`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'paciente' AND COLUMN_NAME = 'motivoarchivado') THEN
    ALTER TABLE `paciente` ADD COLUMN `motivoarchivado` VARCHAR(45) NULL AFTER `idusuarioarchivado`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'paciente' AND COLUMN_NAME = 'detallemotivoarchivado') THEN
    ALTER TABLE `paciente` ADD COLUMN `detallemotivoarchivado` VARCHAR(500) NULL AFTER `motivoarchivado`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'paciente' AND COLUMN_NAME = 'idpacienteprincipal') THEN
    ALTER TABLE `paciente` ADD COLUMN `idpacienteprincipal` INT NULL AFTER `detallemotivoarchivado`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'paciente' AND COLUMN_NAME = 'version') THEN
    ALTER TABLE `paciente` ADD COLUMN `version` BIGINT NULL AFTER `idpacienteprincipal`;
  END IF;

  UPDATE `paciente`
     SET `estadoregistro` = 'ACTIVO'
   WHERE `estadoregistro` IS NULL OR TRIM(`estadoregistro`) = '';
  UPDATE `paciente`
     SET `ultimaactualizacion` = COALESCE(`ultimaactualizacion`, `fechacreacion`, NOW())
   WHERE `ultimaactualizacion` IS NULL;
  UPDATE `paciente` SET `version` = 0 WHERE `version` IS NULL;

  ALTER TABLE `paciente`
    MODIFY COLUMN `ultimaactualizacion` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    MODIFY COLUMN `estadoregistro` VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    MODIFY COLUMN `version` BIGINT NOT NULL DEFAULT 0;

  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'paciente' AND INDEX_NAME = 'idx_paciente_estado_dni') THEN
    CREATE INDEX `idx_paciente_estado_dni` ON `paciente` (`estadoregistro`, `numdocumento`);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'paciente' AND INDEX_NAME = 'idx_paciente_principal') THEN
    CREATE INDEX `idx_paciente_principal` ON `paciente` (`idpacienteprincipal`);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'paciente' AND INDEX_NAME = 'idx_paciente_fechaarchivado') THEN
    CREATE INDEX `idx_paciente_fechaarchivado` ON `paciente` (`fechaarchivado`);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'paciente' AND CONSTRAINT_NAME = 'fk_paciente_usuario_archivado') THEN
    ALTER TABLE `paciente` ADD CONSTRAINT `fk_paciente_usuario_archivado`
      FOREIGN KEY (`idusuarioarchivado`) REFERENCES `usuario` (`idusuario`) ON DELETE NO ACTION ON UPDATE NO ACTION;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'paciente' AND CONSTRAINT_NAME = 'fk_paciente_principal') THEN
    ALTER TABLE `paciente` ADD CONSTRAINT `fk_paciente_principal`
      FOREIGN KEY (`idpacienteprincipal`) REFERENCES `paciente` (`idpaciente`) ON DELETE NO ACTION ON UPDATE NO ACTION;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'paciente' AND CONSTRAINT_NAME = 'chk_paciente_principal_distinto') THEN
    ALTER TABLE `paciente` ADD CONSTRAINT `chk_paciente_principal_distinto`
      CHECK (`idpacienteprincipal` IS NULL OR `idpacienteprincipal` <> `idpaciente`);
  END IF;
END$$

CALL `migrar_soft_delete_paciente`()$$
DROP PROCEDURE `migrar_soft_delete_paciente`$$

DELIMITER ;
