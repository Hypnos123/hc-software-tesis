import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { ButtonComponent, TableComponent } from '@app/shared/components';
import { IPaciente } from '../../models/paciente';
import { IColumnasTabla } from '@app/shared/models/columnas';
import { PacienteService } from '../../services/paciente.service';
import { MensajesSwalService } from '@app/shared/services/mensajes-swal.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-mantenimiento-paciente',
  standalone: true,
  imports: [CommonModule, ButtonComponent, TableComponent],
  templateUrl: './mantenimiento-paciente.component.html',
  styleUrl: './mantenimiento-paciente.component.scss'
})
export class MantenimientoPacienteComponent {
  listaPacientes: IPaciente[] = [];
  cols: IColumnasTabla[] = [];
  colsVisibles: IColumnasTabla[] = [];
  isCargado: boolean = false;

  constructor(
    private pacienteService:PacienteService,
    private router: Router,
    private readonly servicioMensajesSwal: MensajesSwalService
  ) { }

  ngOnInit(): void {
    this.getAllActivosElementos();
  }
  getAllActivosElementos() {
  }



}
