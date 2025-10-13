import { Directive, ElementRef, Input, OnInit } from '@angular/core';

@Directive({
  selector: '[appStyleEstado]',
  standalone: true
})
export class StyleEstadoDirective implements OnInit {
  @Input() value = '';

  constructor(private el: ElementRef) { }

  ngOnInit() {
    switch (this.value.toString()) {
      case 'A CUENTA':
        this.el.nativeElement.classList.add('estadoAmarillo');
        break;

      case 'PAGADO':
        this.el.nativeElement.classList.add('estadoVerde');

        break;

      case 'AGOTANDOSE':
        this.el.nativeElement.classList.add('estadoAmarillo');
        break;

      case 'EN STOCK':
        this.el.nativeElement.classList.add('estadoAmarillo');
        break;

      case "PENDIENTE":
        this.el.nativeElement.classList.add('estadoRojo');
        break;

      case 'COMPLETADO':
        this.el.nativeElement.classList.add('estadoAzul');
        break;

      case 'APROBADO':
        this.el.nativeElement.classList.add('estadoVerde');
        break;

      case 'ANULADO':
        this.el.nativeElement.classList.add('estadoRojo');
        break;

      case '0':
        this.el.nativeElement.classList.add('estadoRojo');
        break;

      case '1':
        this.el.nativeElement.classList.add('estadoVerde');
        break;

      case 'false':
        this.el.nativeElement.classList.add('estadoRojo');
        break;

      case 'true':
        this.el.nativeElement.classList.add('estadoVerde');
        break;
      default:
        break;
    }
  }
}
