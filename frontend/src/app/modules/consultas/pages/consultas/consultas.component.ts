import { Component, OnInit } from '@angular/core';
import { ButtonComponent } from '@app/shared/components';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { DropdownModule } from 'primeng/dropdown';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';
import { MensajesSwalService } from '@app/shared/services/mensajes-swal.service';
import { Router } from '@angular/router';
import { DialogModule } from 'primeng/dialog';
import { ConsultaService } from '../../services/consultas.service';
import { IDetalleConsulta } from '@app/modules/historiaClinica/models/historiaClinica';

interface ConsultaRow { id: number; paciente: { apellidos: string; nombres: string }; dni?: string; edad?: number | string; especialidadRequerida?: string; fechaRegistro?: string | Date; estado: 'Por atender' | 'Atendido'; estadoBD?: string; }

@Component({ selector: 'app-consultas', standalone: true, imports: [CommonModule, FormsModule, TableModule, DropdownModule, TagModule, ButtonModule, InputTextModule, TooltipModule, ButtonComponent, DialogModule], templateUrl: './consultas.component.html', styleUrl: './consultas.component.scss' })
export class ConsultasComponent implements OnInit {
  mostrarConfirmacionAtencion = false;
  consultaSeleccionada: ConsultaRow | null = null;
  rows: ConsultaRow[] = [];
  estadoOptions = [{ label: 'Todos', value: null }, { label: 'Por atender', value: 'Por atender' }, { label: 'Atendido', value: 'Atendido' }];
  estadoSeleccionado: string | null = null;
  loading = false;

  constructor(private router: Router, private consultaService: ConsultaService, private readonly servicioMensajesSwal: MensajesSwalService) {}
  ngOnInit(): void { this.cargarConsultas(); }

  cargarConsultas(): void { this.loading = true; this.consultaService.getAllActivos().subscribe({ next: data => { this.rows = data.map(c => this.toRow(c)); this.loading = false; }, error: e => { this.loading = false; this.servicioMensajesSwal.mensajeError(e?.error?.error || 'No se pudieron cargar las consultas.'); } }); }
  private toRow(c: IDetalleConsulta): ConsultaRow { const estado = this.normalizarEstado(c.estado) === 'ATENDIDO' ? 'Atendido' : 'Por atender'; return { id: c.idConsulta!, paciente: { apellidos: c.apellidos ?? '', nombres: c.nombres ?? '' }, dni: c.numDocumento, edad: c.edad, especialidadRequerida: c.especialidadRequerida, fechaRegistro: c.fechaCreacion, estado, estadoBD: c.estado }; }
  private normalizarEstado(e?: string): string { return (e ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim().toUpperCase().replace(/ /g, '_'); }
  getSeverity(estado: ConsultaRow['estado']) { return estado === 'Por atender' ? 'warning' : 'success'; }
  onFilterEstado(dt: any) { this.estadoSeleccionado ? dt.filter(this.estadoSeleccionado, 'estado', 'equals') : dt.clear(); }
  ver(row: ConsultaRow) { this.router.navigate(['consultas/lista-consultas/detalle', row.id], { queryParams: { modo: 'ver' } }); }
  abrirConfirmacionAtencion(row: ConsultaRow) { this.consultaSeleccionada = row; this.mostrarConfirmacionAtencion = true; }
  confirmarAtencion() { if (!this.consultaSeleccionada) return; const id = this.consultaSeleccionada.id; this.mostrarConfirmacionAtencion = false; this.router.navigate(['consultas/lista-consultas/detalle', id], { queryParams: { modo: 'atender' } }); }
  cancelarAtencion() { this.mostrarConfirmacionAtencion = false; this.consultaSeleccionada = null; }
}
