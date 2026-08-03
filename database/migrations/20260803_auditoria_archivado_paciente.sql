-- Fase 5: auditoría transaccional del archivado lógico de pacientes duplicados.
-- Migración manual, defensiva e idempotente para MySQL 8.0.41.
-- No archiva pacientes ni genera registros de auditoría automáticamente.

USE `historiaclinicadb`;

CREATE TABLE IF NOT EXISTS `auditoriaarchivadopaciente` (
  `idauditoria` INT NOT NULL AUTO_INCREMENT,
  `idpacientearchivado` INT NOT NULL,
  `idpacienteprincipal` INT NOT NULL,
  `idusuario` INT NOT NULL,
  `idempleado` INT NOT NULL,
  `cargo` VARCHAR(65) NOT NULL,
  `dni` VARCHAR(15) NOT NULL,
  `motivo` VARCHAR(45) NOT NULL,
  `detalle` VARCHAR(500) NULL,
  `estadoanterior` VARCHAR(20) NOT NULL,
  `estadonuevo` VARCHAR(20) NOT NULL,
  `requiriorevisionclinica` TINYINT(1) NOT NULL DEFAULT 0,
  `confirmorevisionclinica` TINYINT(1) NOT NULL DEFAULT 0,
  `origen` VARCHAR(20) NOT NULL,
  `fecha` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `nombrepacientearchivado` VARCHAR(250) NOT NULL,
  `nombrepacienteprincipal` VARCHAR(250) NOT NULL,
  `usuarioresponsable` VARCHAR(120) NOT NULL,
  PRIMARY KEY (`idauditoria`)
) ENGINE = InnoDB;

DELIMITER $$

DROP PROCEDURE IF EXISTS `migrar_auditoria_archivado_paciente`$$
CREATE PROCEDURE `migrar_auditoria_archivado_paciente`()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'auditoriaarchivadopaciente' AND INDEX_NAME = 'idx_auditoria_archivado_dni_fecha') THEN
    CREATE INDEX `idx_auditoria_archivado_dni_fecha` ON `auditoriaarchivadopaciente` (`dni`, `fecha` DESC);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'auditoriaarchivadopaciente' AND INDEX_NAME = 'idx_auditoria_archivado_paciente') THEN
    CREATE INDEX `idx_auditoria_archivado_paciente` ON `auditoriaarchivadopaciente` (`idpacientearchivado`);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'auditoriaarchivadopaciente' AND INDEX_NAME = 'idx_auditoria_principal') THEN
    CREATE INDEX `idx_auditoria_principal` ON `auditoriaarchivadopaciente` (`idpacienteprincipal`);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'auditoriaarchivadopaciente' AND INDEX_NAME = 'idx_auditoria_usuario') THEN
    CREATE INDEX `idx_auditoria_usuario` ON `auditoriaarchivadopaciente` (`idusuario`);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'auditoriaarchivadopaciente' AND INDEX_NAME = 'idx_auditoria_empleado') THEN
    CREATE INDEX `idx_auditoria_empleado` ON `auditoriaarchivadopaciente` (`idempleado`);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'auditoriaarchivadopaciente' AND CONSTRAINT_NAME = 'fk_auditoria_paciente_archivado') THEN
    ALTER TABLE `auditoriaarchivadopaciente` ADD CONSTRAINT `fk_auditoria_paciente_archivado`
      FOREIGN KEY (`idpacientearchivado`) REFERENCES `paciente` (`idpaciente`) ON DELETE NO ACTION ON UPDATE NO ACTION;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'auditoriaarchivadopaciente' AND CONSTRAINT_NAME = 'fk_auditoria_paciente_principal') THEN
    ALTER TABLE `auditoriaarchivadopaciente` ADD CONSTRAINT `fk_auditoria_paciente_principal`
      FOREIGN KEY (`idpacienteprincipal`) REFERENCES `paciente` (`idpaciente`) ON DELETE NO ACTION ON UPDATE NO ACTION;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'auditoriaarchivadopaciente' AND CONSTRAINT_NAME = 'fk_auditoria_usuario') THEN
    ALTER TABLE `auditoriaarchivadopaciente` ADD CONSTRAINT `fk_auditoria_usuario`
      FOREIGN KEY (`idusuario`) REFERENCES `usuario` (`idusuario`) ON DELETE NO ACTION ON UPDATE NO ACTION;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'auditoriaarchivadopaciente' AND CONSTRAINT_NAME = 'fk_auditoria_empleado') THEN
    ALTER TABLE `auditoriaarchivadopaciente` ADD CONSTRAINT `fk_auditoria_empleado`
      FOREIGN KEY (`idempleado`) REFERENCES `empleado` (`idempleado`) ON DELETE NO ACTION ON UPDATE NO ACTION;
  END IF;
END$$

CALL `migrar_auditoria_archivado_paciente`()$$
DROP PROCEDURE `migrar_auditoria_archivado_paciente`$$

DELIMITER ;
