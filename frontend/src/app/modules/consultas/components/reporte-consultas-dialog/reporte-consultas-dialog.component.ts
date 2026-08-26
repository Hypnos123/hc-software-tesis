import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PdfPreviewComponent } from '@app/shared/components';
import {
  ReporteConsultaAlcance, ReporteConsultaFiltro, ReporteConsultaSeleccion,
  ReportePacienteOpcion, ReportePdfArchivo
} from '@app/shared/models/reporte-medico';
import { ReporteMedicoService } from '@app/shared/services/reporte-medico.service';
import { finalize, Subscription } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

@Component({
  selector: 'app-reporte-consultas-dialog', standalone: true,
  imports: [CommonModule, FormsModule, DialogModule, DropdownModule, CalendarModule, ButtonModule,
    ProgressSpinnerModule, PdfPreviewComponent],
  templateUrl: './reporte-consultas-dialog.component.html',
  styleUrl: './reporte-consultas-dialog.component.scss'
})
export class ReporteConsultasDialogComponent implements OnChanges, OnDestroy {
  @Input() visible = false;
  @Input() pacientes: ReportePacienteOpcion[] = [];
  @Output() cerrarDialog = new EventEmitter<void>();

  dialogVisible = false;
  idPaciente?: number;
  alcance: ReporteConsultaAlcance = 'TODAS';
  fecha?: Date;
  fechaDesde?: Date;
  fechaHasta?: Date;
  seleccion?: ReporteConsultaSeleccion;
  cargandoSeleccion = false;
  errorSeleccion?: string;
  mostrarPdf = false;
  cargandoPdf = false;
  errorPdf?: string;
  reportePdf?: ReportePdfArchivo;
  readonly alcanceOptions = [
    { label: 'Última consulta atendida', value: 'ULTIMA' as ReporteConsultaAlcance },
    { label: 'Todas las consultas', value: 'TODAS' as ReporteConsultaAlcance },
    { label: 'Consultas de una fecha', value: 'FECHA' as ReporteConsultaAlcance },
    { label: 'Consultas por rango de fechas', value: 'RANGO_FECHAS' as ReporteConsultaAlcance }
  ];
  private filtroSeleccionado?: ReporteConsultaFiltro;
  private seleccionRequest?: Subscription;
  private pdfRequest?: Subscription;
  private secuencia = 0;
  private cierreEmitido = false;

  constructor(private reporteService: ReporteMedicoService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['visible']) {
      this.dialogVisible = this.visible;
      if (this.visible) { this.cierreEmitido = false; this.reiniciar(); }
    }
  }

  ngOnDestroy(): void { this.cancelarRequests(); }

  get requiereFecha(): boolean { return this.alcance === 'FECHA'; }
  get requiereRango(): boolean { return this.alcance === 'RANGO_FECHAS'; }
  get puedeGenerar(): boolean { return !!this.seleccion?.puedeGenerar && !!this.filtroSeleccionado && !this.cargandoPdf; }

  criterioCambiado(): void {
    this.seleccionRequest?.unsubscribe();
    this.limpiarResultadoSeleccion();
    this.cerrarVistaPrevia();
  }

  consultarSeleccion(): void {
    const filtro = this.construirFiltro();
    if (!this.idPaciente || !filtro) return;
    this.limpiarResultadoSeleccion();
    const secuencia = ++this.secuencia;
    this.cargandoSeleccion = true;
    this.seleccionRequest = this.reporteService.obtenerSeleccion(this.idPaciente, filtro)
      .pipe(finalize(() => { if (secuencia === this.secuencia) this.cargandoSeleccion = false; }))
      .subscribe({
        next: seleccion => {
          if (secuencia !== this.secuencia) return;
          this.seleccion = seleccion;
          this.filtroSeleccionado = { ...filtro };
        },
        error: error => this.reporteService.obtenerMensajeError(error).then(mensaje => {
          if (secuencia === this.secuencia) this.errorSeleccion = mensaje;
        })
      });
  }

  generarVistaPrevia(): void {
    if (!this.idPaciente || !this.puedeGenerar || !this.filtroSeleccionado) return;
    const secuencia = ++this.secuencia;
    const filtro = { ...this.filtroSeleccionado };
    this.reportePdf = undefined;
    this.errorPdf = undefined;
    this.cargandoPdf = true;
    this.mostrarPdf = true;
    this.pdfRequest = this.reporteService.obtenerReporteConsolidado(this.idPaciente, filtro)
      .pipe(finalize(() => { if (secuencia === this.secuencia) this.cargandoPdf = false; }))
      .subscribe({
        next: reporte => { if (secuencia === this.secuencia) this.reportePdf = reporte; },
        error: error => this.reporteService.obtenerMensajeError(error).then(mensaje => {
          if (secuencia === this.secuencia && this.mostrarPdf) this.errorPdf = mensaje;
        })
      });
  }

  cerrarVistaPrevia(): void {
    this.secuencia++;
    this.pdfRequest?.unsubscribe();
    this.pdfRequest = undefined;
    this.mostrarPdf = false;
    this.cargandoPdf = false;
    this.errorPdf = undefined;
    this.reportePdf = undefined;
  }

  cerrar(): void {
    this.dialogVisible = false;
    this.cancelarRequests();
    this.reiniciar();
    if (!this.cierreEmitido) { this.cierreEmitido = true; this.cerrarDialog.emit(); }
  }

  private construirFiltro(): ReporteConsultaFiltro | undefined {
    this.errorSeleccion = undefined;
    if (!this.idPaciente) {
      this.errorSeleccion = 'Seleccione un paciente.';
      return undefined;
    }
    if (this.requiereFecha && !this.fecha) {
      this.errorSeleccion = 'Seleccione la fecha que desea consultar.';
      return undefined;
    }
    if (this.requiereRango && (!this.fechaDesde || !this.fechaHasta)) {
      this.errorSeleccion = 'Seleccione la fecha inicial y final.';
      return undefined;
    }
    if (this.requiereRango && this.fechaDesde!.getTime() > this.fechaHasta!.getTime()) {
      this.errorSeleccion = 'La fecha inicial no puede ser posterior a la fecha final.';
      return undefined;
    }
    return {
      alcance: this.alcance,
      fecha: this.requiereFecha ? this.formatearFecha(this.fecha!) : undefined,
      fechaDesde: this.requiereRango ? this.formatearFecha(this.fechaDesde!) : undefined,
      fechaHasta: this.requiereRango ? this.formatearFecha(this.fechaHasta!) : undefined
    };
  }

  private formatearFecha(fecha: Date): string {
    const anio = fecha.getFullYear();
    const mes = String(fecha.getMonth() + 1).padStart(2, '0');
    const dia = String(fecha.getDate()).padStart(2, '0');
    return `${anio}-${mes}-${dia}`;
  }

  private limpiarResultadoSeleccion(): void {
    this.secuencia++;
    this.seleccion = undefined;
    this.filtroSeleccionado = undefined;
    this.errorSeleccion = undefined;
    this.cargandoSeleccion = false;
  }

  private reiniciar(): void {
    this.idPaciente = undefined;
    this.alcance = 'TODAS';
    this.fecha = undefined;
    this.fechaDesde = undefined;
    this.fechaHasta = undefined;
    this.limpiarResultadoSeleccion();
    this.cerrarVistaPrevia();
  }

  private cancelarRequests(): void {
    this.seleccionRequest?.unsubscribe();
    this.pdfRequest?.unsubscribe();
    this.seleccionRequest = undefined;
    this.pdfRequest = undefined;
  }
}
