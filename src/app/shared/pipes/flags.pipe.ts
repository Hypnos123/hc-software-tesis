import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'flags',
  standalone: true
})
export class FlagsPipe implements PipeTransform {

  transform(value: any, tipoFlag: string): string {
    let resultado: string;
    switch (tipoFlag) {
      case 'estado':
        if (value) {
          resultado = 'ACTIVO';
        } else {
          resultado = 'INACTIVO';
        }
        break;

      case 'estadoCompra':
        if (value) {
          resultado = 'EMITIDO';
        } else {
          resultado = 'RECEPCIONADO / RECHAZADO';
        }
        break;

      case 'boolean':
        if (value) {
          resultado = 'Si';
        } else {
          resultado = 'No';
        }
        break;

        case 'isparavender':
        if (value == '1') {
          resultado = 'EN VENTA';
        } else {
          resultado = 'POR VENDER';
        }
        break;

        case 'isCaja':
          if (value == true) {
            resultado = 'CREADO';
          } else {
            resultado = 'SIN CREAR';
          }
          break;

      default:
        if (value) {
          resultado = 'Verdadero';
        } else {
          resultado = 'Falso';
        }
    }
    return resultado;
  }

}
