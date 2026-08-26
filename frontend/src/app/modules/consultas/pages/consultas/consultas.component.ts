import { Component, OnDestroy, OnInit } from '@angular/core';
import { ButtonComponent, PdfPreviewComponent } from '@app/shared/components';
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
import { AuthService } from '@app/auth/services/auth.service';
import { ChatbotNavigationService } from '@app/modules/chatbot/services/chatbot-navigation.service';
import { ReporteMedicoService } from '@app/shared/services/reporte-medico.service';
import { ReportePdfArchivo } from '@app/shared/models/reporte-medico';
import { finalize, Subscription } from 'rxjs';
import { ReporteConsultasDialogComponent } from '../../components/reporte-consultas-dialog/reporte-consultas-dialog.component';
import { ReportePacienteOpcion } from '@app/shared/models/reporte-medico';

interface ConsultaRow { id: number; idPaciente?: number; paciente: { apellidos: string; nombres: string }; dni?: string; edad?: number | string; consultasAtendidas: number; especialidadRequerida?: string; fechaRegistro?: string | Date; estado: 'Por atender' | 'Atendido'; estadoBD?: string; }

@Component({ selector: 'app-consultas', standalone: true, imports: [CommonModule, FormsModule, TableModule, DropdownModule, TagModule, ButtonModule, InputTextModule, TooltipModule, ButtonComponent, DialogModule, PdfPreviewComponent, ReporteConsultasDialogComponent], templateUrl: './consultas.component.html', styleUrl: './consultas.component.scss' })
export class ConsultasComponent implements OnInit, OnDestroy {
  mostrarConfirmacionAtencion = false;
  consultaSeleccionada: ConsultaRow | null = null;
  cantidadConsultasAtendidas = 0;
  errorConsultaHistorial = false;
  rows: ConsultaRow[] = [];
  estadoOptions = [{ label: 'Todos', value: null }, { label: 'Por atender', value: 'Por atender' }, { label: 'Atendido', value: 'Atendido' }];
  estadoSeleccionado: string | null = null;
  loading = false;
  mostrarVistaPreviaReporte = false;
  cargandoReporte = false;
  errorReporte?: string;
  reportePdf?: ReportePdfArchivo;
  private reporteRequest?: Subscription;
  private secuenciaReporte = 0;
  mostrarReporteConsolidado = false;
  pacientesReporte: ReportePacienteOpcion[] = [];

  constructor(private router: Router, private consultaService: ConsultaService,
    private readonly servicioMensajesSwal: MensajesSwalService, private authService: AuthService,
    private chatbotNavigation: ChatbotNavigationService, private reporteMedicoService: ReporteMedicoService) {}
  ngOnInit(): void { this.cargarConsultas(); }
  ngOnDestroy(): void { this.reporteRequest?.unsubscribe(); }

  cargarConsultas(): void { this.loading = true; this.consultaService.getAllActivos().subscribe({ next: data => { this.rows = data.map(c => this.toRow(c)); this.pacientesReporte = this.construirPacientesReporte(this.rows); this.loading = false; }, error: e => { this.loading = false; this.servicioMensajesSwal.mensajeError(e?.error?.error || 'No se pudieron cargar las consultas.'); } }); }
  private toRow(c: IDetalleConsulta): ConsultaRow { const estado = this.normalizarEstado(c.estado) === 'ATENDIDO' ? 'Atendido' : 'Por atender'; return { id: c.idConsulta!, idPaciente: c.idPaciente, paciente: { apellidos: c.apellidos ?? '', nombres: c.nombres ?? '' }, dni: c.numDocumento, edad: c.edad, consultasAtendidas: c.consultasAtendidas ?? 0, especialidadRequerida: c.especialidadRequerida, fechaRegistro: c.fechaCreacion, estado, estadoBD: c.estado }; }
  private normalizarEstado(e?: string): string { return (e ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim().toUpperCase().replace(/ /g, '_'); }
  getSeverity(estado: ConsultaRow['estado']) { return estado === 'Por atender' ? 'warning' : 'success'; }
  onFilterEstado(dt: any) { this.estadoSeleccionado ? dt.filter(this.estadoSeleccionado, 'estado', 'equals') : dt.clear(); }
  ver(row: ConsultaRow) { this.router.navigate(['consultas/lista-consultas/detalle', row.id], { queryParams: { modo: 'ver' } }); }
  abrirReporteConsolidado(): void { this.mostrarReporteConsolidado = true; }
  cerrarReporteConsolidado(): void { this.mostrarReporteConsolidado = false; }
  imprimirEvaluacion(row: ConsultaRow): void {
    if (row.estado !== 'Atendido' || this.cargandoReporte) return;
    const secuencia = ++this.secuenciaReporte;
    this.reportePdf = undefined;
    this.errorReporte = undefined;
    this.cargandoReporte = true;
    this.mostrarVistaPreviaReporte = true;
    this.reporteRequest = this.reporteMedicoService.obtenerEvaluacionMedica(row.id)
      .pipe(finalize(() => {
        if (secuencia === this.secuenciaReporte) {
          this.cargandoReporte = false;
          this.reporteRequest = undefined;
        }
      }))
      .subscribe({
        next: archivo => { if (secuencia === this.secuenciaReporte) this.reportePdf = archivo; },
        error: error => this.reporteMedicoService.obtenerMensajeError(error).then(mensaje => {
          if (secuencia === this.secuenciaReporte && this.mostrarVistaPreviaReporte) {
            this.reportePdf = undefined;
            this.errorReporte = mensaje;
          }
        })
      });
  }
  cerrarVistaPreviaReporte(): void {
    this.secuenciaReporte++;
    this.reporteRequest?.unsubscribe();
    this.reporteRequest = undefined;
    this.mostrarVistaPreviaReporte = false;
    this.cargandoReporte = false;
    this.errorReporte = undefined;
    this.reportePdf = undefined;
  }
  abrirConfirmacionAtencion(row: ConsultaRow) {
    this.consultaSeleccionada = row;
    this.cantidadConsultasAtendidas = this.puedeConsultarResumen() ? row.consultasAtendidas : 0;
    this.errorConsultaHistorial = false;
    this.mostrarConfirmacionAtencion = true;
  }
  cancelarAtencion() { this.mostrarConfirmacionAtencion = false; this.consultaSeleccionada = null; }
  continuarAtencion() { if (!this.consultaSeleccionada) return; const id = this.consultaSeleccionada.id; this.mostrarConfirmacionAtencion = false; this.router.navigate(['consultas/lista-consultas/detalle', id], { queryParams: { modo: 'atender' } }); }
  verResumenAsistente() {
    if (!this.consultaSeleccionada?.idPaciente) return;
    const { idPaciente, paciente, dni } = this.consultaSeleccionada;
    this.mostrarConfirmacionAtencion = false;
    this.chatbotNavigation.abrirResumenConsultas({
      idPaciente,
      nombreCompleto: [paciente.nombres, paciente.apellidos].filter(Boolean).join(' '),
      dni,
      cantidadConsultasAtendidas: this.cantidadConsultasAtendidas
    });
  }
  private puedeConsultarResumen(): boolean { const tipo = this.normalizarRol(this.authService.usuario?.tipoUsuario); const cargo = this.normalizarRol(this.authService.usuario?.cargo); return tipo === 'ADMINISTRADOR' || tipo === 'DOCTOR' || cargo === 'ADMINISTRADOR' || cargo === 'DOCTOR'; }
  private normalizarRol(rol?: string): string { return (rol ?? '').trim().normalize('NFD').replace(/[\u0300-\u036f]/g, '').toUpperCase(); }
  private construirPacientesReporte(rows: ConsultaRow[]): ReportePacienteOpcion[] {
    const pacientes = new Map<number, ReportePacienteOpcion>();
    rows.filter(row => !!row.idPaciente).forEach(row => {
      const nombreCompleto = [row.paciente.nombres, row.paciente.apellidos].filter(Boolean).join(' ').trim();
      pacientes.set(row.idPaciente!, { idPaciente: row.idPaciente!, nombreCompleto, dni: row.dni,
        etiqueta: `${nombreCompleto || 'Paciente sin nombre'}${row.dni ? ` · DNI ${row.dni}` : ''}` });
    });
    return [...pacientes.values()].sort((a, b) => a.nombreCompleto.localeCompare(b.nombreCompleto, 'es'));
  }
}
