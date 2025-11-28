import { Component } from '@angular/core';
import { ButtonComponent, TableComponent } from '@app/shared/components';

@Component({
  selector: 'app-consultas',
  standalone: true,
  imports: [
    ButtonComponent,
  ],
  templateUrl: './consultas.component.html',
  styleUrl: './consultas.component.scss'
})
export class ConsultasComponent {

}
