import { CommonModule } from '@angular/common';
import {
  Component, EventEmitter,
  Input,
  OnInit,
  Output
} from '@angular/core';
import { RouterModule } from '@angular/router';
import { ButtonModule } from 'primeng/button';

@Component({
  selector: 'app-button',
  templateUrl: './button.component.html',
  styleUrls: ['./button.component.scss'],
  standalone: true,
  imports: [CommonModule, ButtonModule, RouterModule]
})
export class ButtonComponent implements OnInit {
  @Output() eventoClick = new EventEmitter<any>();
  @Input() label: string = '';
  @Input() icono: string = '';
  @Input() tipo: string = 'button';
  @Input() router: string = '/';
  @Input() isDisabled?: boolean = false;
  @Input() isVisible: boolean = true;
  @Input() estiloBoton: string = 'p-button';
  @Input() tooltip: string = '';
  clase: string = 'p-button-rounded';
  isRouter: boolean = false;

  ngOnInit(): void {
    if (this.router != '/') {
      this.isRouter = true;
    }
      console.log(this.estiloBoton);

    switch (this.estiloBoton) {
      case 'rounded':
        this.clase = 'p-button-rounded p-button-text';
        break;

      case 'text':
        this.clase = 'p-button-text';
        break;

      case 'outlined':
        this.clase = 'p-button-outlined';
        break;

      case 'raised ':
        this.clase = 'p-button-raised';
        break;

      case 'rippled ':
        this.clase = 'p-button-rippled';
        break;

      case 'rounded-danger':
        this.clase = 'p-button-rounded p-button-danger';
        break;

      case 'rounded-help':
        this.clase = 'p-button-rounded p-button-help';
        break;

      case 'rounded-success':
        this.clase = 'p-button-rounded p-button-success';
        break;

      case 'outlined-rounded-success':
        this.clase = 'p-button-outlined p-button-rounded p-button-success';
        break;


      case 'outlined-rounded-danger':
        this.clase = 'p-button-outlined p-button-rounded p-button-danger';
        break;

      case '':
        this.clase = 'p-button';
        break;

      default:
        this.clase = this.estiloBoton;
        break;
    }
  }

  ejecutarEventoClick() {
    this.eventoClick.emit();
  }
}
