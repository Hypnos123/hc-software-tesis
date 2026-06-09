
import {AbstractControl, ValidationErrors, ValidatorFn} from '@angular/forms';

export function rangeStockValidator(stockActual: string): ValidatorFn {
    return (control:AbstractControl) : ValidationErrors | null => {

        const valueEnvio = control.value;

        if (!valueEnvio) {
            return null;
        }

        const valid = stockActual >= valueEnvio;
        return !valid ? {stockValid:true}: null;
    }
}
