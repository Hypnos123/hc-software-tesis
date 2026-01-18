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
import { MensajesSwalService } from '@app/shared/services/mensajes-swal.service';
import { Router } from '@angular/router';

interface ConsultaRow {
  id: number;
  paciente: { apellidos: string; nombres: string };
  edad: number;
  motivo: string;
  fechaRegistro: string;
  estado: 'Por atender' | 'Atendido';
}

@Component({
  selector: 'app-consultas',
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
  templateUrl: './consultas.component.html',
  styleUrl: './consultas.component.scss'
})
export class ConsultasComponent {

  rows: ConsultaRow[] = [
    { id: 1, paciente: { apellidos: 'Herrera Muñoz', nombres: 'Juan Pablo' }, edad: 41, motivo: 'Dolor abdominal', fechaRegistro: '2024-11-20T18:00:00', estado: 'Por atender' },
    { id: 2, paciente: { apellidos: 'Mendoza Davalos', nombres: 'Josefina Vera' }, edad: 41, motivo: 'Dolor abdominal', fechaRegistro: '2024-11-20T18:00:00', estado: 'Atendido' },
    { id: 3, paciente: { apellidos: 'Rojas Salazar', nombres: 'María Elena' }, edad: 36, motivo: 'Dolor abdominal', fechaRegistro: '2024-11-20T18:00:00', estado: 'Atendido' },
    { id: 4, paciente: { apellidos: 'Herrera Muñoz', nombres: 'Juan Pablo' }, edad: 41, motivo: 'Dolor abdominal', fechaRegistro: '2024-11-20T18:00:00', estado: 'Por atender' },
  ];

  estadoOptions = [
    { label: 'Todos', value: null },
    { label: 'Por atender', value: 'Por atender' },
    { label: 'Atendido', value: 'Atendido' }
  ];
  estadoSeleccionado: string | null = null;

  loading = false;

  constructor(
    private router: Router,
    private readonly servicioMensajesSwal: MensajesSwalService
  ) { }

  getSeverity(estado: ConsultaRow['estado']) {
    return estado === 'Por atender' ? 'warning' : 'success';
  }

  // para filtro por estado usando p-table
  onFilterEstado(dt: any) {
    if (this.estadoSeleccionado) dt.filter(this.estadoSeleccionado, 'estado', 'equals');
    else dt.clear();
  }

  ver(row: ConsultaRow) {
    const id = row.id;
    this.router.navigateByUrl(`consultas/mantenimiento-consultas/${id}`);
  }

  asignar(row: ConsultaRow) {
  }

}
