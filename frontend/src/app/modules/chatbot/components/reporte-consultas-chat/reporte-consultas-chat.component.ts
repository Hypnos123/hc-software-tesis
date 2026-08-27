import { CommonModule } from '@angular/common';
import { Component, EventEmitter, OnDestroy, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HistoriaClinicaService } from '@app/modules/historiaClinica/services/consultas.service';
import { ReporteConsultaAlcance, ReporteConsultaFiltro, ReporteConsultaSeleccion, ReportePdfArchivo } from '@app/shared/models/reporte-medico';
import { ReporteMedicoService } from '@app/shared/services/reporte-medico.service';
import { finalize, Subscription } from 'rxjs';

interface PacienteReporteChat { idPaciente: number; nombreCompleto: string; dni: string; }
type VistaReporteChat = 'metodo' | 'busqueda' | 'pacientes' | 'alcance' | 'fecha' | 'rango' | 'resultado' | 'error';

@Component({
  selector: 'app-reporte-consultas-chat', standalone: true, imports: [CommonModule, FormsModule],
  templateUrl: './reporte-consultas-chat.component.html', styleUrl: './reporte-consultas-chat.component.scss'
})
export class ReporteConsultasChatComponent implements OnDestroy {
  @Output() pdfGenerado = new EventEmitter<ReportePdfArchivo>();
  @Output() pdfCargando = new EventEmitter<boolean>();
  @Output() pdfError = new EventEmitter<string>();
  @Output() mensajeConversacional = new EventEmitter<string>();
  @Output() volverConsultas = new EventEmitter<void>();

  vista: VistaReporteChat = 'metodo';
  metodo?: 'DNI' | 'NOMBRE';
  criterio = '';
  pacientes: PacienteReporteChat[] = [];
  paciente?: PacienteReporteChat;
  alcance?: ReporteConsultaAlcance;
  fecha = '';
  fechaDesde = '';
  fechaHasta = '';
  seleccion?: ReporteConsultaSeleccion;
  filtroConfirmado?: ReporteConsultaFiltro;
  error?: string;
  cargando = false;
  private solicitud?: Subscription;

  constructor(private historiasService: HistoriaClinicaService, private reportesService: ReporteMedicoService) {}
  ngOnDestroy(): void { this.solicitud?.unsubscribe(); }

  elegirMetodo(metodo: 'DNI' | 'NOMBRE'): void {
    this.metodo = metodo; this.criterio = ''; this.error = undefined; this.vista = 'busqueda';
    this.mensajeConversacional.emit(metodo === 'DNI' ? 'Buscar por DNI' : 'Buscar por nombre');
  }

  buscar(): void {
    const criterio = this.criterio.trim();
    if (!criterio || (this.metodo === 'DNI' && !/^\d{8}$/.test(criterio))) {
      this.error = this.metodo === 'DNI' ? 'El DNI debe contener exactamente ocho dígitos.' : 'Ingresa el nombre del paciente.'; return;
    }
    this.cancelarSolicitud(); this.cargando = true; this.error = undefined;
    this.mensajeConversacional.emit('Buscando al paciente...');
    const consulta = this.metodo === 'DNI'
      ? this.historiasService.buscarPacientesPorDni(criterio)
      : this.historiasService.buscarPacientesPorNombre(criterio);
    this.solicitud = consulta.pipe(finalize(() => this.cargando = false)).subscribe({
      next: resultados => {
        const filtrados = this.metodo === 'DNI'
          ? resultados.filter(item => (item.numDocumento ?? item.dni ?? '').trim() === criterio) : resultados;
        this.pacientes = filtrados.filter(item => !!item.idPaciente).map(item => ({
          idPaciente: item.idPaciente!, nombreCompleto: [item.nombres, item.apellidos].filter(Boolean).join(' ').trim(),
          dni: (item.numDocumento ?? item.dni ?? '').trim()
        }));
        if (!this.pacientes.length) { this.mostrarError('No se encontró un paciente con los datos ingresados.'); return; }
        if (this.pacientes.length === 1) this.seleccionarPaciente(this.pacientes[0]);
        else this.vista = 'pacientes';
      },
      error: () => this.mostrarError('No se pudo consultar al paciente en este momento.')
    });
  }

  seleccionarPaciente(paciente: PacienteReporteChat): void {
    this.paciente = paciente; this.seleccion = undefined; this.filtroConfirmado = undefined; this.vista = 'alcance';
    this.mensajeConversacional.emit(`Paciente encontrado: ${paciente.nombreCompleto}${paciente.dni ? ` · DNI: ${paciente.dni}` : ''}`);
  }

  elegirAlcance(alcance: ReporteConsultaAlcance): void {
    this.alcance = alcance; this.limpiarSeleccion();
    if (alcance === 'FECHA') { this.vista = 'fecha'; return; }
    if (alcance === 'RANGO_FECHAS') { this.vista = 'rango'; return; }
    this.consultarSeleccion({ alcance });
  }

  consultarFecha(): void {
    if (!this.fecha) { this.error = 'Selecciona la fecha que deseas consultar.'; return; }
    this.consultarSeleccion({ alcance: 'FECHA', fecha: this.fecha });
  }

  consultarRango(): void {
    if (!this.fechaDesde || !this.fechaHasta) { this.error = 'Selecciona la fecha inicial y final.'; return; }
    if (this.fechaDesde > this.fechaHasta) { this.error = 'La fecha inicial no puede ser posterior a la fecha final.'; return; }
    this.consultarSeleccion({ alcance: 'RANGO_FECHAS', fechaDesde: this.fechaDesde, fechaHasta: this.fechaHasta });
  }

  generarPdf(): void {
    if (!this.paciente || !this.seleccion?.puedeGenerar || !this.filtroConfirmado || this.cargando) return;
    const filtro = { ...this.filtroConfirmado };
    this.cancelarSolicitud(); this.cargando = true; this.pdfCargando.emit(true); this.mensajeConversacional.emit('Generando reporte de consultas...');
    this.solicitud = this.reportesService.obtenerReporteConsolidado(this.paciente.idPaciente, filtro)
      .pipe(finalize(() => { this.cargando = false; this.pdfCargando.emit(false); }))
      .subscribe({ next: pdf => this.pdfGenerado.emit(pdf), error: error => this.reportesService.obtenerMensajeError(error)
        .then(mensaje => { this.pdfError.emit(mensaje); this.mostrarError(mensaje, false); }) });
  }

  cambiarCriterio(): void { this.limpiarSeleccion(); this.alcance = undefined; this.vista = 'alcance'; }
  buscarOtroPaciente(): void { this.cancelarSolicitud(); this.reiniciar(); }
  reintentar(): void { this.error = undefined; this.vista = this.paciente ? 'alcance' : (this.metodo ? 'busqueda' : 'metodo'); }
  cancelar(): void { this.cancelarSolicitud(); this.volverConsultas.emit(); }

  private consultarSeleccion(filtro: ReporteConsultaFiltro): void {
    if (!this.paciente || this.cargando) return;
    this.cancelarSolicitud(); this.cargando = true; this.error = undefined; this.seleccion = undefined;
    this.mensajeConversacional.emit('Preparando información del reporte...');
    const filtroExacto = { ...filtro };
    this.solicitud = this.reportesService.obtenerSeleccion(this.paciente.idPaciente, filtroExacto)
      .pipe(finalize(() => this.cargando = false)).subscribe({
        next: seleccion => { this.seleccion = seleccion; this.filtroConfirmado = filtroExacto; this.vista = 'resultado'; },
        error: error => this.reportesService.obtenerMensajeError(error).then(mensaje => this.mostrarError(mensaje))
      });
  }

  private mostrarError(mensaje: string, cambiarVista = true): void { this.error = mensaje; if (cambiarVista) this.vista = 'error'; }
  private limpiarSeleccion(): void { this.seleccion = undefined; this.filtroConfirmado = undefined; this.error = undefined; }
  private reiniciar(): void { this.vista = 'metodo'; this.metodo = undefined; this.criterio = ''; this.pacientes = []; this.paciente = undefined; this.alcance = undefined; this.fecha = ''; this.fechaDesde = ''; this.fechaHasta = ''; this.limpiarSeleccion(); }
  private cancelarSolicitud(): void { this.solicitud?.unsubscribe(); this.solicitud = undefined; this.cargando = false; }
}
