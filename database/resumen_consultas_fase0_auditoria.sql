-- Auditoría de solo lectura para la Fase 0 del resumen histórico de consultas.
-- Este archivo no modifica estructura ni datos.

-- 1. Nombre físico de las columnas de consulta.
SELECT column_name, column_type, is_nullable
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'consulta'
ORDER BY ordinal_position;

-- 2. Formatos reales de funciones vitales en consultas atendidas.
SET @col_frecuencia_respiratoria = (
  SELECT column_name
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'consulta'
    AND column_name IN ('frecuenciarespiratoria', 'fecuenciarespiratoria')
  ORDER BY column_name = 'frecuenciarespiratoria' DESC
  LIMIT 1
);
SET @sql_vitales = CONCAT(
  'SELECT idconsulta, presionarterial, frecuenciacardiaca, ',
  IFNULL(@col_frecuencia_respiratoria, 'NULL'),
  ' AS frecuenciarespiratoria, talla, temperatura, peso ',
  'FROM consulta WHERE UPPER(TRIM(estado)) = ''ATENDIDO'' ORDER BY idconsulta'
);
PREPARE auditoria_vitales FROM @sql_vitales;
EXECUTE auditoria_vitales;
DEALLOCATE PREPARE auditoria_vitales;

-- 3. Especialidades y su clave comparable (tildes requieren revisión visual).
SELECT especialidadrequerida,
       UPPER(REPLACE(TRIM(especialidadrequerida), ' ', '_')) AS clave_normalizada,
       COUNT(*) AS cantidad
FROM consulta
WHERE especialidadrequerida IS NOT NULL
  AND TRIM(especialidadrequerida) <> ''
GROUP BY especialidadrequerida,
         UPPER(REPLACE(TRIM(especialidadrequerida), ' ', '_'))
ORDER BY clave_normalizada, especialidadrequerida;

-- 4. Catálogo de tipos de enfermedad y posibles duplicados exactos normalizados.
SELECT idtipoenfermedad, descripcion,
       UPPER(REPLACE(TRIM(descripcion), ' ', '_')) AS clave_normalizada
FROM tipoenfermedad
ORDER BY clave_normalizada, idtipoenfermedad;

SELECT UPPER(REPLACE(TRIM(descripcion), ' ', '_')) AS clave_normalizada,
       COUNT(*) AS cantidad,
       GROUP_CONCAT(idtipoenfermedad ORDER BY idtipoenfermedad) AS ids
FROM tipoenfermedad
GROUP BY UPPER(REPLACE(TRIM(descripcion), ' ', '_'))
HAVING COUNT(*) > 1;

-- 5. Pacientes con más de un registro de antecedentes.
SELECT idpaciente, COUNT(*) AS cantidad_antecedentes,
       GROUP_CONCAT(idantecedentes ORDER BY idantecedentes) AS ids_antecedentes
FROM antecedentes
GROUP BY idpaciente
HAVING COUNT(*) > 1
ORDER BY idpaciente;

-- 6. Consultas cuyo paciente no coincide con el paciente de la historia clínica.
SELECT c.idconsulta, c.idpaciente AS paciente_consulta,
       c.idhistoriaclinica, h.idpaciente AS paciente_historia
FROM consulta c
JOIN historiaclinica h ON h.idhistoriaclinica = c.idhistoriaclinica
WHERE c.idpaciente <> h.idpaciente
ORDER BY c.idconsulta;

-- 7. Estados reales, preservando primero el valor almacenado.
SELECT estado, UPPER(TRIM(estado)) AS estado_normalizado, COUNT(*) AS cantidad
FROM consulta
GROUP BY estado, UPPER(TRIM(estado))
ORDER BY estado_normalizado, estado;
