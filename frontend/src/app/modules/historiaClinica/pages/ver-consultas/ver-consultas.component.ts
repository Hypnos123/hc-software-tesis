import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { FormsModule } from '@angular/forms';
import { TooltipModule } from 'primeng/tooltip';
import { HistoriaClinicaService } from '../../services/consultas.service';
import { IDetalleConsulta } from '../../models/historiaClinica';

@Component({
  selector: 'app-consultas-historia-clinica', standalone: true,
  imports: [CommonModule, FormsModule, TableModule, ButtonModule, InputTextModule, TooltipModule],
  templateUrl: './ver-consultas.component.html', styleUrl: './ver-consultas.component.scss'
})
export class ConsultasHistoriaClinicaComponent implements OnInit {
  idHistoriaClinica!: number;
  paciente = '';
  searchValue = '';
  consultas: IDetalleConsulta[] = [];
  loading = false;
  globalFields = ['idConsulta', 'apellidos', 'nombres', 'numDocumento', 'especialidadRequerida', 'doctorResponsable', 'estado'];

  constructor(private route: ActivatedRoute, private router: Router, private service: HistoriaClinicaService) {}

  ngOnInit(): void {
    this.idHistoriaClinica = Number(this.route.snapshot.paramMap.get('id'));
    this.cargarCabecera();
    this.cargarConsultas();
  }

  cargarCabecera(): void {
    this.service.getById(this.idHistoriaClinica).subscribe(h => {
      this.paciente = h ? [h.apellidos, h.nombres].filter(Boolean).join(' ') : 'Paciente no encontrado';
    });
  }

  cargarConsultas(): void {
    this.loading = true;
    this.service.getConsultasByHistoria(this.idHistoriaClinica).subscribe({
      next: data => { this.consultas = data; this.loading = false; },
      error: () => { this.consultas = []; this.loading = false; }
    });
  }

  volver(): void { this.router.navigate(['/historiaClinica']); }
  verConsulta(row: IDetalleConsulta): void { this.router.navigate(['/historiaClinica/ver-consultas', this.idHistoriaClinica, 'ver'], { queryParams: { idConsulta: row.idConsulta } }); }
  editarConsulta(row: IDetalleConsulta): void { this.router.navigate(['/historiaClinica/ver-consultas', this.idHistoriaClinica, 'editar'], { queryParams: { idConsulta: row.idConsulta } }); }
  nuevaConsulta(): void { this.router.navigate(['/historiaClinica/ver-consultas', this.idHistoriaClinica, 'nuevo']); }
}
