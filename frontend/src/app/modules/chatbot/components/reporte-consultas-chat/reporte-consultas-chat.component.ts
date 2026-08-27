import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnDestroy, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HistoriaClinicaService } from '@app/modules/historiaClinica/services/consultas.service';
import { ReporteConsultaAlcance, ReporteConsultaFiltro, ReporteConsultaSeleccion, ReportePdfArchivo } from '@app/shared/models/reporte-medico';
import { ReporteMedicoService } from '@app/shared/services/reporte-medico.service';
import { catchError, finalize, forkJoin, map, of, Subscription, timer } from 'rxjs';

interface PacienteReporteChat { idPaciente: number; nombreCompleto: string; dni: string; fechaRegistro?: string | Date; cantidadConsultas: number; }
type VistaReporteChat = 'metodo' | 'busqueda' | 'pacientes' | 'alcance' | 'fecha' | 'rango' | 'resultado' | 'error';

@Component({
  selector: 'app-reporte-consultas-chat', standalone: true, imports: [CommonModule, FormsModule],
  templateUrl: './reporte-consultas-chat.component.html', styleUrl: './reporte-consultas-chat.component.scss'
})
export class ReporteConsultasChatComponent implements OnDestroy {
  @Input() active = false;
  @Output() pdfGenerado = new EventEmitter<ReportePdfArchivo>();
  @Output() pdfCargando = new EventEmitter<boolean>();
  @Output() pdfError = new EventEmitter<string>();
  @Output() mensajeConversacional = new EventEmitter<string>();
  @Output() volverConsultas = new EventEmitter<void>();
  @Output() avanzarFlujo = new EventEmitter<string[]>();
  @Output() reposicionar = new EventEmitter<void>();

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
    this.avanzarFlujo.emit([metodo === 'DNI' ? 'Ingresa el DNI del paciente.' : 'Ingresa el nombre del paciente.']);
  }

  buscar(): void {
    const criterio = this.criterio.trim();
    if (!criterio || (this.metodo === 'DNI' && !/^\d{8}$/.test(criterio))) {
      this.error = this.metodo === 'DNI' ? 'El DNI debe contener exactamente ocho dígitos.' : 'Ingresa el nombre del paciente.'; return;
    }
    this.cancelarSolicitud(); this.cargando = true; this.error = undefined; this.reposicionar.emit();
    const consulta = this.metodo === 'DNI'
      ? this.historiasService.buscarPacientesPorDni(criterio)
      : this.historiasService.buscarPacientesPorNombre(criterio);
    this.solicitud = forkJoin({ respuesta: consulta.pipe(map(resultados => ({ resultados })), catchError(() => of({ error: true }))), espera: timer(this.demoraAleatoria()) }).pipe(
      map(({ respuesta }) => respuesta), finalize(() => this.cargando = false)
    ).subscribe({
      next: respuesta => {
        if ('error' in respuesta) { this.mostrarError('No se pudo consultar al paciente en este momento.'); this.reposicionar.emit(); return; }
        const resultados = respuesta.resultados;
        const filtrados = this.metodo === 'DNI'
          ? resultados.filter(item => (item.numDocumento ?? item.dni ?? '').trim() === criterio) : resultados;
        const basicos = filtrados.filter(item => !!item.idPaciente).map(item => ({
          idPaciente: item.idPaciente!, nombreCompleto: [item.nombres, item.apellidos].filter(Boolean).join(' ').trim(),
          dni: (item.numDocumento ?? item.dni ?? '').trim(), fechaRegistro: item.fechaIngreso, cantidadConsultas: 0
        }));
        if (!basicos.length) { this.mostrarError('No se encontró un paciente con los datos ingresados.'); this.reposicionar.emit(); return; }
        if (basicos.length === 1) { this.pacientes = basicos; this.seleccionarPaciente(basicos[0]); return; }
        this.enriquecerPacientes(basicos);
      }
    });
  }

  seleccionarPaciente(paciente: PacienteReporteChat): void {
    this.paciente = paciente; this.seleccion = undefined; this.filtroConfirmado = undefined; this.vista = 'alcance';
    this.avanzarFlujo.emit([
      `Paciente seleccionado: ${paciente.nombreCompleto}${paciente.dni ? ` · DNI: ${paciente.dni}` : ''}`,
      '¿Qué consultas deseas incluir en el reporte?'
    ]);
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
    this.cancelarSolicitud(); this.cargando = true; this.error = undefined; this.seleccion = undefined; this.reposicionar.emit();
    const filtroExacto = { ...filtro };
    this.solicitud = forkJoin({ respuesta: this.reportesService.obtenerSeleccion(this.paciente.idPaciente, filtroExacto)
      .pipe(map(seleccion => ({ seleccion })), catchError(error => of({ error }))), espera: timer(this.demoraAleatoria()) })
      .pipe(map(({ respuesta }) => respuesta), finalize(() => this.cargando = false)).subscribe({
        next: respuesta => {
          if ('error' in respuesta) {
            this.reportesService.obtenerMensajeError(respuesta.error).then(mensaje => { this.mostrarError(mensaje); this.reposicionar.emit(); }); return;
          }
          const seleccion = respuesta.seleccion;
          this.seleccion = seleccion; this.filtroConfirmado = filtroExacto; this.vista = 'resultado'; this.avanzarFlujo.emit([seleccion.mensaje]);
        }
      });
  }

  private mostrarError(mensaje: string, cambiarVista = true): void { this.error = mensaje; if (cambiarVista) this.vista = 'error'; }
  formatearFecha(fecha?: string | Date): string {
    if (!fecha) return 'No registrada';
    if (fecha instanceof Date) {
      return `${String(fecha.getDate()).padStart(2, '0')}/${String(fecha.getMonth() + 1).padStart(2, '0')}/${fecha.getFullYear()}`;
    }
    const texto = String(fecha).slice(0, 10); const partes = texto.split('-');
    return partes.length === 3 ? `${partes[2]}/${partes[1]}/${partes[0]}` : texto;
  }
  private enriquecerPacientes(pacientes: PacienteReporteChat[]): void {
    this.cargando = true;
    this.solicitud = forkJoin(pacientes.map(paciente => this.historiasService.getByPaciente(paciente.idPaciente).pipe(
      map(historias => ({ ...paciente, cantidadConsultas: historias.reduce((total, historia) => total + (historia.cantidadConsultas ?? 0), 0) }))
    ))).pipe(finalize(() => this.cargando = false)).subscribe({
      next: enriquecidos => { this.pacientes = enriquecidos; this.vista = 'pacientes'; this.reposicionar.emit(); },
      error: () => { this.pacientes = pacientes; this.vista = 'pacientes'; this.reposicionar.emit(); }
    });
  }
  private demoraAleatoria(): number { return 3000 + Math.floor(Math.random() * 3001); }
  private limpiarSeleccion(): void { this.seleccion = undefined; this.filtroConfirmado = undefined; this.error = undefined; }
  private reiniciar(): void { this.vista = 'metodo'; this.metodo = undefined; this.criterio = ''; this.pacientes = []; this.paciente = undefined; this.alcance = undefined; this.fecha = ''; this.fechaDesde = ''; this.fechaHasta = ''; this.limpiarSeleccion(); }
  private cancelarSolicitud(): void { this.solicitud?.unsubscribe(); this.solicitud = undefined; this.cargando = false; }
}
