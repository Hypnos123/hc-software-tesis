USE `historiaclinicadb`;
CREATE TABLE IF NOT EXISTS `auditoriafusionhistoriaclinica` (
  `idauditoria` INT NOT NULL AUTO_INCREMENT,
  `idhistoriaprincipal` INT NOT NULL,
  `idhistoriaeliminada` INT NOT NULL COMMENT 'ID histórico sin FK porque la historia fue eliminada',
  `idpaciente` INT NOT NULL, `idusuario` INT NOT NULL, `idempleado` INT NOT NULL,
  `cargo` VARCHAR(65) NOT NULL, `origen` VARCHAR(20) NOT NULL, `motivo` VARCHAR(45) NOT NULL,
  `detalle` VARCHAR(500) NULL, `consultasantesprincipal` BIGINT NOT NULL,
  `consultasantessecundaria` BIGINT NOT NULL, `consultastransferidas` BIGINT NOT NULL,
  `consultasdespuesprincipal` BIGINT NOT NULL, `resultado` VARCHAR(30) NOT NULL,
  `fecha` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`idauditoria`), INDEX `idx_auditoria_fusion_principal` (`idhistoriaprincipal`),
  INDEX `idx_auditoria_fusion_eliminada` (`idhistoriaeliminada`), INDEX `idx_auditoria_fusion_fecha` (`fecha`),
  CONSTRAINT `fk_auditoria_fusion_principal` FOREIGN KEY (`idhistoriaprincipal`) REFERENCES `historiaclinica` (`idhistoriaclinica`) ON DELETE NO ACTION,
  CONSTRAINT `fk_auditoria_fusion_paciente` FOREIGN KEY (`idpaciente`) REFERENCES `paciente` (`idpaciente`) ON DELETE NO ACTION,
  CONSTRAINT `fk_auditoria_fusion_usuario` FOREIGN KEY (`idusuario`) REFERENCES `usuario` (`idusuario`) ON DELETE NO ACTION,
  CONSTRAINT `fk_auditoria_fusion_empleado` FOREIGN KEY (`idempleado`) REFERENCES `empleado` (`idempleado`) ON DELETE NO ACTION
) ENGINE=InnoDB;
