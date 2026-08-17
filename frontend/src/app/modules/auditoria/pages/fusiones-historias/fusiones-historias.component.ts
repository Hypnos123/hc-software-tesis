import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { FusionHistoriaAuditoriaDetalle, FusionHistoriaAuditoriaResumen } from '../../models/fusion-historia-auditoria';
import { AuditoriaAdminService } from '../../services/auditoria-admin.service';

@Component({
  selector: 'app-fusiones-historias',
  standalone: true,
  imports: [CommonModule, FormsModule, TableModule, ButtonModule, InputTextModule, TagModule, DialogModule, ProgressSpinnerModule],
  templateUrl: './fusiones-historias.component.html',
  styleUrl: '../auditoria-placeholder.scss',
})
export class FusionesHistoriasComponent implements OnInit {
  searchValue = '';
  dni = '';
  idPaciente: number | null = null;
  idHistoriaPrincipal: number | null = null;
  idHistoriaEliminada: number | null = null;
  desde = '';
  hasta = '';
  loading = true;
  detailLoading = false;
  errorMessage = '';
  records: FusionHistoriaAuditoriaResumen[] = [];
  detail: FusionHistoriaAuditoriaDetalle | null = null;
  detailVisible = false;
  page = 0;
  size = 10;
  totalRecords = 0;
  sort = 'fecha,desc';

  constructor(private readonly service: AuditoriaAdminService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true; this.errorMessage = '';
    this.service.listarFusionesHistorias({ page: this.page, size: this.size, sort: this.sort,
      search: this.searchValue.trim(), dni: this.dni.trim(), idPaciente: this.idPaciente ?? undefined,
      idHistoriaPrincipal: this.idHistoriaPrincipal ?? undefined, idHistoriaEliminada: this.idHistoriaEliminada ?? undefined,
      desde: this.desde ? `${this.desde}T00:00:00` : undefined, hasta: this.hasta ? `${this.hasta}T23:59:59` : undefined })
      .pipe(finalize(() => this.loading = false)).subscribe({
        next: (response) => { this.records = response.content; this.totalRecords = response.totalElements; },
        error: (error) => { this.records = []; this.totalRecords = 0; this.errorMessage = error?.error?.mensaje ?? 'No fue posible cargar el historial de fusiones.'; },
      });
  }

  applyFilters(): void { this.page = 0; this.load(); }
  clearFilters(): void {
    this.searchValue = ''; this.dni = ''; this.idPaciente = null; this.idHistoriaPrincipal = null;
    this.idHistoriaEliminada = null; this.desde = ''; this.hasta = ''; this.page = 0; this.load();
  }
  onLazyLoad(event: TableLazyLoadEvent): void {
    const nextSize = event.rows ?? this.size; const nextPage = Math.floor((event.first ?? 0) / nextSize);
    if (nextPage === this.page && nextSize === this.size) return;
    this.page = nextPage; this.size = nextSize; this.load();
  }
  openDetail(idAuditoria: number): void {
    this.detailVisible = true; this.detailLoading = true; this.detail = null;
    this.service.obtenerFusionHistoria(idAuditoria).pipe(finalize(() => this.detailLoading = false)).subscribe({
      next: (detail) => this.detail = detail,
      error: (error) => this.errorMessage = error?.error?.mensaje ?? 'No fue posible cargar el detalle de la fusión.',
    });
  }
  get hasFilters(): boolean {
    return !!(this.searchValue.trim() || this.dni.trim() || this.idPaciente || this.idHistoriaPrincipal
      || this.idHistoriaEliminada || this.desde || this.hasta);
  }
}
