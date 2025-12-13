import { Component } from '@angular/core';
import { ButtonComponent, TableComponent } from '@app/shared/components';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { DropdownModule } from 'primeng/dropdown';
import { MultiSelectModule } from 'primeng/multiselect';
import { SliderModule } from 'primeng/slider';
import { ProgressBarModule } from 'primeng/progressbar';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { PaginatorModule } from 'primeng/paginator';
import { TooltipModule } from 'primeng/tooltip';
import { Router } from '@angular/router';



@Component({
  selector: 'app-pacientes',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    DropdownModule,
    MultiSelectModule,
    SliderModule,
    ProgressBarModule,
    TagModule,
    ButtonModule,
    InputTextModule,
    IconFieldModule,
    InputIconModule,
    PaginatorModule,
    TooltipModule,
    ButtonComponent
  ],
  templateUrl: './pacientes.component.html',
  styleUrl: './pacientes.component.scss'
})
export class PacientesComponent {

  customers: any[] = [];
  selectedCustomers: any[] = [];
  loading = false;
  searchValue = '';
  activityValues: number[] = [0, 100];

  constructor(private router: Router) {
    this.customers =[
  {
    id: 1,
    apellidos: 'Mendoza Davalos',
    nombres: 'Josefina Vera',
    edad: 41,
    dni: '74526981',
    fechaRegistro: '2024-11-20T18:00:00'
  },
  {
    id: 2,
    apellidos: 'Quispe Huamán',
    nombres: 'Luis Alberto',
    edad: 29,
    dni: '42831659',
    fechaRegistro: '2024-10-05T09:35:00'
  },
  {
    id: 3,
    apellidos: 'Rojas Salazar',
    nombres: 'María Elena',
    edad: 36,
    dni: '76351844',
    fechaRegistro: '2024-09-12T14:12:00'
  }
];

  }

 clear(table: any) {
    table.clear();
    this.searchValue = '';
    this.activityValues = [0, 100];
  }

  verPaciente() {
    this.router.navigate(['/paciente/mantenimiento-paciente']);
  }

  
  

  

}
