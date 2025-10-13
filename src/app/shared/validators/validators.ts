import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export function documentoValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const formGroup = control.parent;
    if (!formGroup) return null; // Evita errores si el control aún no tiene padre

    const tipoDocumento = formGroup.get('tipoDocumento')?.value;
    const numeroDocumento = control.value;

    if (!numeroDocumento) return null; // Permitir vacío (otras validaciones pueden manejarlo)

    const regexDNI = /^\d{8}$/;
    const regexCE = /^\d{9}$/;

    if (tipoDocumento?.tipo === 'DNI' && !regexDNI.test(numeroDocumento)) {
      return { dniInvalido: 'El DNI debe tener 8 dígitos' };
    }

    if (tipoDocumento?.tipo === 'CE' && !regexCE.test(numeroDocumento)) {
      return { ceInvalido: 'El CE debe tener 9 dígitos' };
    }

    return null;
  };
}
