import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'tipoPagoIcon',
  standalone: true
})
export class TipoPagoIconPipe implements PipeTransform {
  transform(value: string): string {
    const icons: { [key: string]: string } = {
      'efectivo': `assets/images/caja/${value}.svg`,
      'yape': `assets/images/caja/${value}.svg`,
      'plin': `assets/images/caja/${value}.svg`,
      'pos': `assets/images/caja/${value}.svg`
    };

    return icons[value];
  }
}
