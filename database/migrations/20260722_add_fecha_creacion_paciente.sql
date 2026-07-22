-- Migración para instalaciones existentes de historiaclinicadb.
-- Ejecutar una sola vez y en este orden.
-- Para registros históricos, fechacreacion usa fechaingreso cuando existe;
-- en caso contrario usa la fecha y hora de ejecución de esta migración.

ALTER TABLE `historiaclinicadb`.`paciente`
  ADD COLUMN `fechacreacion` DATETIME NULL AFTER `idpaciente`;

UPDATE `historiaclinicadb`.`paciente`
SET `fechacreacion` = COALESCE(`fechaingreso`, NOW())
WHERE `fechacreacion` IS NULL;

ALTER TABLE `historiaclinicadb`.`paciente`
  MODIFY COLUMN `fechacreacion` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE INDEX `idx_paciente_fechacreacion`
  ON `historiaclinicadb`.`paciente` (`fechacreacion`);
