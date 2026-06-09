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

interface HCRow {
  id: number;
  paciente: { apellidos: string; nombres: string };
  dni: string;
  edad: number;
  fechaCreacion: string;       
  ultimaActualizacion: string; 
}

@Component({
  selector: 'app-historias-clinicas',
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
  templateUrl: './historias-clinicas.component.html',
  styleUrl: './historias-clinicas.component.scss'
})
export class HistoriasClinicasComponent {

  constructor(private router: Router) {}

 verHistoria(id: number) {
  this.router.navigate(['/historiaClinica', 'mantenimiento-historias-clinicas', 'ver', id]);
}
  loading = false;
  searchValue = '';
  selected: HCRow[] = [];

  rows: HCRow[] = [
    { id: 1, paciente: { apellidos: 'Mendoza Davalos', nombres: 'Josefina Vera' },
     dni: '74526981', edad: 41, fechaCreacion: '2024-11-20', ultimaActualizacion: '2024-11-20' },
    { id: 2, paciente: { apellidos: 'Quispe Huamán', nombres: 'Luis Alberto' },
     dni: '42831659', edad: 29, fechaCreacion: '2024-10-05', ultimaActualizacion: '2024-11-15' },
    { id: 3, paciente: { apellidos: 'Rojas Salazar', nombres: 'María Elena' },
     dni: '76351844', edad: 36, fechaCreacion: '2024-09-12', ultimaActualizacion: '2024-10-01' }
  ];

  globalFields = ['paciente.apellidos','paciente.nombres','dni'];

  clear(dt: any) { dt.clear(); this.searchValue = ''; }

  ver(row: HCRow) {}
  
  consultas(row: HCRow) {
  this.router.navigate(['/historiaClinica/ver-consultas', row.id]);
}
}
