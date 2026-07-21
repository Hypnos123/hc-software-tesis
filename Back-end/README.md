# Backend — búsqueda segura de pacientes

## Endpoint para integración

`GET /api/pacientes/buscar?criterio={valor}` permite localizar pacientes sin exponer información clínica ni datos personales adicionales.

### Clasificación del criterio

- **DNI:** exactamente 8 dígitos.
- **ID de paciente:** entero positivo de menos de 8 dígitos.
- **Nombre completo:** texto no numérico; la búsqueda es parcial, no distingue mayúsculas/minúsculas y usa la normalización existente para resolver coincidencias aproximadas cuando es necesario.
- **Límite:** las búsquedas por nombre devuelven como máximo 10 resultados.

### Respuestas

La respuesta `200 OK` siempre contiene únicamente `idPaciente`, `dni`, `nombreCompleto` y `tieneHistoriaClinica` por paciente.

```json
{
  "encontrado": true,
  "tipoResultado": "unico",
  "pacientes": [
    {
      "idPaciente": 6,
      "dni": "78451268",
      "nombreCompleto": "Patricia Elena Cárdenas Torres",
      "tieneHistoriaClinica": true
    }
  ]
}
```

Para una búsqueda válida sin coincidencias se responde `200 OK`, con `encontrado: false`, `tipoResultado: "sin_resultados"`, una lista vacía y un mensaje explicativo. Las coincidencias por nombre múltiples se indican con `tipoResultado: "multiple"`.

```json
{
  "encontrado": false,
  "tipoResultado": "sin_resultados",
  "pacientes": [],
  "mensaje": "No se encontró ningún paciente con el criterio indicado."
}
```

Un criterio ausente, vacío o numérico inválido devuelve `400 Bad Request`. Los errores inesperados devuelven `500 Internal Server Error` sin detalles internos.

### Ejemplos cURL

```bash
curl -i "http://localhost:8080/api/pacientes/buscar?criterio=78451268"
curl -i "http://localhost:8080/api/pacientes/buscar?criterio=15"
curl -G -i "http://localhost:8080/api/pacientes/buscar" --data-urlencode "criterio=Patricia Elena Cárdenas Torres"
curl -i "http://localhost:8080/api/pacientes/buscar?criterio=%20%20%20"
```

También se puede probar desde Swagger UI en `http://localhost:8080/swagger-ui.html`.

### Postman

1. Cree una solicitud `GET` a `http://localhost:8080/api/pacientes/buscar`.
2. En la pestaña **Params**, agregue `criterio` con el DNI, ID o nombre que desea consultar.
3. Envíe la solicitud y verifique `encontrado`, `tipoResultado` y la lista `pacientes`.

## Alcance de seguridad

Esta integración es local y temporal para la demostración. No incorpora Botpress, Webchat, Spring Security, JWT, API keys ni cambios globales de CORS. Para cualquier despliegue fuera de ese contexto se recomienda añadir autenticación de servicio, autorización, limitación de solicitudes, auditoría y una política CORS restrictiva.
