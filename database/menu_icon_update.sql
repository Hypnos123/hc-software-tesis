-- Normaliza los iconos del menú para PrimeIcons.
-- Ejecutar en la base historiaclinicadb si la columna menu.imagen contiene NULL,
-- valores vacíos o clases de otra librería como Font Awesome.
UPDATE historiaclinicadb.menu
SET imagen = CASE
  WHEN LOWER(nombre) LIKE '%usuario%' THEN 'pi pi-user'
  WHEN LOWER(nombre) LIKE '%consulta%' THEN 'pi pi-address-book'
  WHEN LOWER(nombre) LIKE '%historia%' THEN 'pi pi-file-check'
  WHEN LOWER(nombre) LIKE '%paciente%' THEN 'pi pi-users'
  WHEN LOWER(nombre) LIKE '%empleado%' THEN 'pi pi-user-edit'
  ELSE 'pi pi-circle'
END
WHERE imagen IS NULL
   OR TRIM(imagen) = ''
   OR imagen NOT LIKE 'pi pi-%';
