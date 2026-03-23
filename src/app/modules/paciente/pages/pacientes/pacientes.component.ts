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
import { PacienteService } from '../../services/paciente.service';
import { IPaciente } from '../../models/paciente';



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

  customers: IPaciente[] = [];
  selectedCustomers: any[] = [];
  loading = false;
  searchValue = '';
  activityValues: number[] = [0, 100];

  constructor(private router: Router, private pacienteService: PacienteService) {
    this.getAllActives();
  }

  getAllActives() {
    this.pacienteService.getAllActivos().subscribe((response) => {
      if (response) {
        this.customers = response;
      }
    })
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
