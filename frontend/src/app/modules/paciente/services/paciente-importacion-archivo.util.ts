export const MAX_TAMANO_IMPORTACION_BYTES = 2 * 1024 * 1024;

export function validarArchivoImportacion(archivo?: File): string {
  if (!archivo) return 'Selecciona un archivo Excel.';
  if (archivo.size === 0) return 'El archivo seleccionado está vacío.';
  if (!archivo.name.toLowerCase().endsWith('.xlsx')) return 'Solo se permiten archivos con extensión .xlsx.';
  if (archivo.size > MAX_TAMANO_IMPORTACION_BYTES) return 'El archivo supera el tamaño permitido de 2 MB.';
  return '';
}
