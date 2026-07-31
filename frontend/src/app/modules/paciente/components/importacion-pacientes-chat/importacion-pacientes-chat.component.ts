import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, ElementRef, EventEmitter, Input, OnDestroy, OnInit, Output, ViewChild } from '@angular/core';
import { saveAs } from 'file-saver';
import { finalize, Subscription } from 'rxjs';
import {
  EstadoFlujoImportacion,
  IPacienteImportacionConfirmacion,
  IPacienteImportacionFila,
  IPacienteImportacionPrevisualizacion
} from '../../models/paciente-importacion';
import { validarArchivoImportacion } from '../../services/paciente-importacion-archivo.util';
import { PacienteImportacionService } from '../../services/paciente-importacion.service';
import { PacienteListRefreshService } from '../../services/paciente-list-refresh.service';

export interface PacienteImportacionChatState {
  estado: EstadoFlujoImportacion;
  archivo?: File;
  previsualizacion?: IPacienteImportacionPrevisualizacion;
  confirmacion?: IPacienteImportacionConfirmacion;
  mensaje: string;
  plantillaDescargada: boolean;
  cancelarSolicitud?: () => void;
}

export function crearPacienteImportacionChatState(): PacienteImportacionChatState {
  return { estado: 'INICIAL', mensaje: '', plantillaDescargada: false };
}

@Component({
  selector: 'app-importacion-pacientes-chat',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './importacion-pacientes-chat.component.html',
  styleUrl: './importacion-pacientes-chat.component.scss'
})
export class ImportacionPacientesChatComponent implements OnInit, OnDestroy {
  @Input({ required: true }) state!: PacienteImportacionChatState;
  @Output() volverPacientes = new EventEmitter<void>();
  @Output() registrarOtro = new EventEmitter<void>();
  @ViewChild('archivoInput') archivoInput?: ElementRef<HTMLInputElement>;

  private solicitud?: Subscription;
  private expiracionTimer?: ReturnType<typeof setTimeout>;

  constructor(
    private readonly importacionService: PacienteImportacionService,
    private readonly refreshService: PacienteListRefreshService
  ) {}

  ngOnInit(): void {
    if (this.state.previsualizacion && this.state.estado === 'PREVISUALIZADA') {
      this.programarExpiracion(this.state.previsualizacion.expiraEn);
    }
  }

  ngOnDestroy(): void {
    this.limpiarTemporizador();
  }

  limpiarFlujo(): void {
    this.state.cancelarSolicitud?.();
    this.state.cancelarSolicitud = undefined;
    this.solicitud = undefined;
    this.limpiarTemporizador();
  }

  get procesando(): boolean {
    return ['VALIDANDO', 'CONFIRMANDO'].includes(this.state.estado);
  }

  get cantidadValidos(): number {
    return this.state.previsualizacion?.filas.filter(fila => fila.estado === 'VALIDO').length ?? 0;
  }

  get puedeConfirmar(): boolean {
    return this.state.estado === 'PREVISUALIZADA'
      && !!this.state.previsualizacion?.importacionId
      && this.cantidadValidos > 0
      && !this.estaExpirada();
  }

  descargarPlantilla(): void {
    if (this.procesando || this.state.estado === 'PLANTILLA_DESCARGADA') return;
    this.state.estado = 'VALIDANDO';
    this.state.mensaje = '';
    this.solicitud = this.importacionService.descargarPlantilla().pipe(
      finalize(() => this.solicitud = undefined)
    ).subscribe({
      next: response => {
        if (!response.body) return this.mostrarError('No se recibió la plantilla Excel.');
        saveAs(response.body, this.importacionService.obtenerNombreArchivo(response));
        this.state.plantillaDescargada = true;
        this.state.estado = 'PLANTILLA_DESCARGADA';
        this.state.mensaje = 'La plantilla se descargó correctamente. Complétala y adjúntala para analizarla. Solo se permiten archivos .xlsx de hasta 2 MB.';
      },
      error: error => this.manejarError(error)
    });
    this.registrarCancelacion();
  }

  seleccionarArchivo(event: Event): void {
    if (this.procesando || !this.state.plantillaDescargada) return;
    const input = event.target as HTMLInputElement;
    const archivo = input.files?.[0];
    const error = validarArchivoImportacion(archivo);
    if (error) {
      input.value = '';
      this.state.mensaje = error;
      return;
    }
    this.state.archivo = archivo;
    this.state.previsualizacion = undefined;
    this.state.confirmacion = undefined;
    this.state.estado = 'ARCHIVO_SELECCIONADO';
    this.state.mensaje = '';
    this.limpiarTemporizador();
  }

  quitarArchivo(): void {
    if (this.procesando) return;
    this.state.archivo = undefined;
    this.state.previsualizacion = undefined;
    this.state.confirmacion = undefined;
    this.state.estado = 'PLANTILLA_DESCARGADA';
    this.state.mensaje = '';
    if (this.archivoInput) this.archivoInput.nativeElement.value = '';
    this.limpiarTemporizador();
  }

  analizarArchivo(): void {
    if (this.procesando) return;
    const error = validarArchivoImportacion(this.state.archivo);
    if (error) {
      this.state.mensaje = error;
      return;
    }
    this.state.estado = 'VALIDANDO';
    this.state.mensaje = '';
    this.solicitud = this.importacionService.validarArchivo(this.state.archivo!).pipe(
      finalize(() => this.solicitud = undefined)
    ).subscribe({
      next: previsualizacion => {
        this.state.previsualizacion = previsualizacion;
        this.state.estado = previsualizacion.estado === 'EXPIRADA' ? 'EXPIRADA' : 'PREVISUALIZADA';
        this.programarExpiracion(previsualizacion.expiraEn);
      },
      error: errorHttp => this.manejarError(errorHttp)
    });
    this.registrarCancelacion();
  }

  confirmar(): void {
    if (!this.puedeConfirmar || this.procesando || !this.state.previsualizacion) return;
    this.state.estado = 'CONFIRMANDO';
    this.state.mensaje = '';
    this.solicitud = this.importacionService.confirmarImportacion(this.state.previsualizacion.importacionId).pipe(
      finalize(() => this.solicitud = undefined)
    ).subscribe({
      next: confirmacion => {
        this.state.confirmacion = confirmacion;
        this.state.estado = confirmacion.estado === 'CONFIRMADA' ? 'CONFIRMADA' : 'ERROR';
        if (this.state.estado === 'CONFIRMADA') this.refreshService.solicitarActualizacion();
        this.limpiarTemporizador();
      },
      error: error => this.manejarError(error)
    });
    this.registrarCancelacion();
  }

  seleccionarOtroArchivo(): void {
    if (this.procesando) return;
    this.state.archivo = undefined;
    this.state.previsualizacion = undefined;
    this.state.confirmacion = undefined;
    this.state.estado = 'PLANTILLA_DESCARGADA';
    this.state.mensaje = '';
    if (this.archivoInput) this.archivoInput.nativeElement.value = '';
    this.limpiarTemporizador();
  }

  mensajeFila(fila: IPacienteImportacionFila): string {
    return [...fila.errores, ...fila.advertencias].map(item => item.mensaje).join(' · ') || 'Sin observaciones';
  }

  etiquetaFila(fila: IPacienteImportacionFila): string {
    if (fila.estado === 'VALIDO' && fila.advertencias.length) return 'ADVERTENCIA';
    return ({ VALIDO: 'VÁLIDO', DNI_EXISTENTE: 'DNI EXISTENTE', DNI_DUPLICADO_ARCHIVO: 'DNI DUPLICADO', ERROR_DATOS: 'ERROR' } as Record<string, string>)[fila.estado] ?? fila.estado;
  }

  formatoTamano(bytes: number): string {
    return bytes < 1024 ? `${bytes} B` : `${(bytes / 1024 / 1024).toFixed(2)} MB`;
  }

  private estaExpirada(): boolean {
    const expiraEn = this.state.previsualizacion?.expiraEn;
    return !expiraEn || new Date(expiraEn).getTime() <= Date.now();
  }

  private registrarCancelacion(): void {
    const solicitud = this.solicitud;
    this.state.cancelarSolicitud = () => solicitud?.unsubscribe();
  }

  private programarExpiracion(expiraEn: string): void {
    this.limpiarTemporizador();
    const restante = new Date(expiraEn).getTime() - Date.now();
    if (!Number.isFinite(restante) || restante <= 0) return this.marcarExpirada();
    this.expiracionTimer = setTimeout(() => {
      if (this.state.estado === 'PREVISUALIZADA') this.marcarExpirada();
    }, restante);
  }

  private marcarExpirada(): void {
    this.state.estado = 'EXPIRADA';
    this.state.mensaje = 'La previsualización expiró.';
  }

  private limpiarTemporizador(): void {
    if (this.expiracionTimer) clearTimeout(this.expiracionTimer);
    this.expiracionTimer = undefined;
  }

  private mostrarError(mensaje: string): void {
    this.state.estado = 'ERROR';
    this.state.mensaje = mensaje;
  }

  private manejarError(error: HttpErrorResponse): void {
    const funcional = typeof error.error?.mensaje === 'string' ? error.error.mensaje : '';
    const mensajes: Record<number, string> = {
      400: 'El archivo o su estructura no son válidos.', 404: 'La importación no fue encontrada.',
      409: 'La importación está siendo procesada o ya fue confirmada.', 410: 'La previsualización expiró.',
      413: 'El archivo supera los 2 MB.', 500: 'Ocurrió un error inesperado.'
    };
    this.state.mensaje = funcional || mensajes[error.status] || 'No se pudo completar la operación.';
    this.state.estado = error.status === 410 ? 'EXPIRADA' : 'ERROR';
  }
}
