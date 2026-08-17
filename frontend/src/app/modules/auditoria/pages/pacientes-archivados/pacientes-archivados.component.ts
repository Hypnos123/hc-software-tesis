import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule, TableLazyLoadEvent } from 'primeng/table';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { DialogModule } from 'primeng/dialog';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { AuditoriaAdminService } from '../../services/auditoria-admin.service';
import { PacienteArchivadoDetalle, PacienteArchivadoResumen } from '../../models/paciente-archivado-admin';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-pacientes-archivados',
  standalone: true,
  imports: [CommonModule, FormsModule, TableModule, InputTextModule, ButtonModule, TagModule, DialogModule, ProgressSpinnerModule],
  templateUrl: './pacientes-archivados.component.html',
  styleUrl: '../auditoria-placeholder.scss',
})
export class PacientesArchivadosComponent implements OnInit {
  searchValue = '';
  dni = '';
  idPaciente: number | null = null;
  desde = '';
  hasta = '';
  loading = true;
  detailLoading = false;
  errorMessage = '';
  records: PacienteArchivadoResumen[] = [];
  detail: PacienteArchivadoDetalle | null = null;
  detailVisible = false;
  page = 0;
  size = 10;
  totalRecords = 0;
  sort = 'fechaArchivado,desc';

  constructor(private readonly service: AuditoriaAdminService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.errorMessage = '';
    this.service.listarPacientesArchivados({ page: this.page, size: this.size, sort: this.sort,
      search: this.searchValue.trim(), dni: this.dni.trim(), idPaciente: this.idPaciente ?? undefined,
      desde: this.desde ? `${this.desde}T00:00:00` : undefined, hasta: this.hasta ? `${this.hasta}T23:59:59` : undefined })
      .pipe(finalize(() => this.loading = false))
      .subscribe({ next: (response) => { this.records = response.content; this.totalRecords = response.totalElements; },
        error: (error) => { this.records = []; this.totalRecords = 0; this.errorMessage = error?.error?.mensaje ?? 'No fue posible cargar los pacientes archivados.'; } });
  }

  applyFilters(): void { this.page = 0; this.load(); }

  clearFilters(): void {
    this.searchValue = ''; this.dni = ''; this.idPaciente = null; this.desde = ''; this.hasta = ''; this.page = 0; this.load();
  }

  onLazyLoad(event: TableLazyLoadEvent): void {
    const nextSize = event.rows ?? this.size;
    const nextPage = Math.floor((event.first ?? 0) / nextSize);
    if (nextPage === this.page && nextSize === this.size) return;
    this.page = nextPage; this.size = nextSize; this.load();
  }

  openDetail(idPaciente: number): void {
    this.detailVisible = true; this.detailLoading = true; this.detail = null;
    this.service.obtenerPacienteArchivado(idPaciente).pipe(finalize(() => this.detailLoading = false)).subscribe({
      next: (detail) => this.detail = detail,
      error: (error) => this.errorMessage = error?.error?.mensaje ?? 'No fue posible cargar el detalle del paciente.',
    });
  }

  get hasFilters(): boolean { return !!(this.searchValue.trim() || this.dni.trim() || this.idPaciente || this.desde || this.hasta); }
}
