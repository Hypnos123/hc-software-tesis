# Resultados de auditoría — resumen histórico de consultas

La auditoría de Fase 0 se ejecutó contra la base local usada para la demostración. No se modificaron
datos históricos ni fue necesaria una migración.

- La columna física es `frecuenciarespiratoria`.
- Hay 15 consultas: 5 `ATENDIDO` y 10 `PENDIENTE`; no hay otros estados.
- No hay pacientes con más de un registro de antecedentes.
- No hay consultas cuyo paciente difiera del paciente de su historia clínica.
- No hay tipos de enfermedad duplicados según la clave normalizada.
- Tipos existentes: `ALERGICA`, `DERMATOLOGICA`, `DIGESTIVA`, `INFECCIOSA`, `NEUROLOGICA` y
  `RESPIRATORIA`.
- Especialidades: `DERMATOLOGIA` (5), `GASTROENTEROLOGIA` (5), `MEDICINA_GENERAL` (2) y
  `NEUROLOGIA` (3).
- Presiones en consultas atendidas: `120`, `30`, `50`, `148/94` y `120/80`. Solo las dos últimas
  cumplen el formato sistólica/diastólica previsto. Los otros valores se conservarán y se descartarán
  únicamente del cálculo futuro de presión.
- Las demás funciones vitales pudieron recuperarse correctamente.
