import { Component, OnInit } from '@angular/core';
import { ButtonComponent } from '@app/shared/components';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { IconFieldModule } from 'primeng/iconfield';
import { PaginatorModule } from 'primeng/paginator';
import { TooltipModule } from 'primeng/tooltip';
import { Router } from '@angular/router';
import { HistoriaClinicaService } from '../../services/consultas.service';
import { IHistoriaClinica } from '../../models/historiaClinica';

@Component({
  selector: 'app-historias-clinicas', standalone: true,
  imports: [CommonModule, FormsModule, TableModule, ButtonModule, InputTextModule, IconFieldModule, PaginatorModule, TooltipModule, ButtonComponent],
  templateUrl: './historias-clinicas.component.html', styleUrl: './historias-clinicas.component.scss'
})
export class HistoriasClinicasComponent implements OnInit {
  loading = false; searchValue = ''; rows: IHistoriaClinica[] = []; selected: IHistoriaClinica[] = [];
  globalFields = ['idHistoriaClinica','apellidos','nombres','numDocumento'];
  constructor(private router: Router, private historiaClinicaService: HistoriaClinicaService) {}
  ngOnInit(): void { this.cargarHistorias(); }
  cargarHistorias(): void { this.loading = true; this.historiaClinicaService.getAll().subscribe({ next: r => { this.rows = r; this.loading = false; }, error: () => this.loading = false }); }
  nombreCompleto(r: IHistoriaClinica): string { return [r.apellidos, r.nombres].filter(Boolean).join(' '); }
  verHistoria(id?: number) { if (id) this.router.navigate(['/historiaClinica', 'mantenimiento-historias-clinicas', 'ver', id]); }
  editarHistoria(id?: number) { if (id) this.router.navigate(['/historiaClinica', 'mantenimiento-historias-clinicas', 'editar', id]); }
  consultas(row: IHistoriaClinica) { this.router.navigate(['/historiaClinica/ver-consultas', row.idHistoriaClinica]); }
  clear(dt: any) { dt.clear(); this.searchValue = ''; }
}
