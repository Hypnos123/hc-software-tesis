import { Component, OnDestroy } from '@angular/core';
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
import { Subscription } from 'rxjs';
import { PacienteListRefreshService } from '../../services/paciente-list-refresh.service';



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
export class PacientesComponent implements OnDestroy {

  customers: IPaciente[] = [];
  selectedCustomers: any[] = [];
  loading = false;
  searchValue = '';
  activityValues: number[] = [0, 100];
  private readonly refreshSubscription: Subscription;

  constructor(
    private router: Router,
    private pacienteService: PacienteService,
    refreshService: PacienteListRefreshService
  ) {
    this.getAllActives();
    this.refreshSubscription = refreshService.refresh$.subscribe(() => this.getAllActives());
  }

  ngOnDestroy(): void { this.refreshSubscription.unsubscribe(); }

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

  nuevoPaciente() {
  this.router.navigate(['/paciente/mantenimiento-paciente/nuevo']);
}

  verPaciente(id: number) {
  this.router.navigate(['/paciente/mantenimiento-paciente/ver', id]);
}

  editarPaciente(id: number) {
  this.router.navigate(['/paciente/mantenimiento-paciente/editar', id]);
}

  
  

  

}
