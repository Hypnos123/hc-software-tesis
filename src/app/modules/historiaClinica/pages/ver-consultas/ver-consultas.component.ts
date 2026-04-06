import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { FormsModule } from '@angular/forms';


interface ConsultaRow {
  id: number;
  paciente: string;
  dni: string;
  especialidad: string;
  doctor: string;
  fechaCreacion: string;
}

@Component({
  selector: 'app-consultas-historia-clinica',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    InputTextModule
  ],
  templateUrl: './ver-consultas.component.html',
  styleUrl: './ver-consultas.component.scss'
})
export class ConsultasHistoriaClinicaComponent implements OnInit {

  idHistoriaClinica!: number;
  paciente = '';
  searchValue = '';

  consultas: ConsultaRow[] = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.idHistoriaClinica = Number(this.route.snapshot.paramMap.get('id'));

    this.cargarConsultas(this.idHistoriaClinica);
  }

  cargarConsultas(idHistoria: number): void {
    const dataMock: Record<number, { paciente: string; consultas: ConsultaRow[] }> = {
      1: {
        paciente: 'Mendoza Davalos Josefina Vera',
        consultas: [
          {
            id: 1,
            paciente: 'Mendoza Davalos Josefina Vera',
            dni: '74526981',
            especialidad: 'Medicina General',
            doctor: 'Marcos Chavez',
            fechaCreacion: '2024-11-20T18:00:00'
          }
        ]
      },
      2: {
        paciente: 'Quispe Huamán Luis Alberto',
        consultas: [
          {
            id: 1,
            paciente: 'Quispe Huamán Luis Alberto',
            dni: '42831659',
            especialidad: 'Cardiología',
            doctor: 'Ana Torres',
            fechaCreacion: '2024-10-10T10:30:00'
          }
        ]
      },
      3: {
        paciente: 'Rojas Salazar María Elena',
        consultas: [
          {
            id: 1,
            paciente: 'Rojas Salazar María Elena',
            dni: '76351844',
            especialidad: 'Dermatología',
            doctor: 'Luis Pérez',
            fechaCreacion: '2024-09-15T09:00:00'
          }
        ]
      }
    };

    const historia = dataMock[idHistoria];

    if (historia) {
      this.paciente = historia.paciente;
      this.consultas = historia.consultas;
    } else {
      this.paciente = 'Paciente no encontrado';
      this.consultas = [];
    }
  }

  volver(): void {
    this.router.navigate(['/historiaClinica']);
  }

 verConsulta(row: any): void {
  this.router.navigate([
    '/historiaClinica/ver-consultas',
    this.idHistoriaClinica,
    'ver'
  ]);
}

  nuevaConsulta(): void {
  this.router.navigate([
    '/historiaClinica/ver-consultas',
    this.idHistoriaClinica,
    'nuevo'
  ]);
}
}