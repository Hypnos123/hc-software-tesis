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
  mensajes: PacienteImportacionChatMensaje[];
  operacion?: 'DESCARGANDO' | 'ANALIZANDO' | 'CONFIRMANDO';
  cancelarSolicitud?: () => void;
}

export interface PacienteImportacionChatMensaje {
  id: string;
  etapa: 'PLANTILLA' | 'ARCHIVO' | 'ANALISIS' | 'CONFIRMACION';
  remitente: 'user' | 'bot';
  texto: string;
  inicioGrupo?: boolean;
  vistasSiguientes?: PacienteImportView[];
  reemplazarVistaActiva?: boolean;
}

export type PacienteImportView = 'template' | 'file-selection' | 'file-ready' | 'analysis' | 'confirmation' | 'completed' | 'cancelled';

export function crearPacienteImportacionChatState(): PacienteImportacionChatState {
  return { estado: 'INICIAL', mensaje: '', plantillaDescargada: false, mensajes: [] };
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
  @Input({ required: true }) view!: PacienteImportView;
  @Input() active = false;
  @Output() volverPacientes = new EventEmitter<void>();
  @Output() registrarOtro = new EventEmitter<void>();
  @Output() mensajeConversacional = new EventEmitter<PacienteImportacionChatMensaje>();
  @ViewChild('archivoInput') archivoInput?: ElementRef<HTMLInputElement>;

  private solicitud?: Subscription;
  private expiracionTimer?: ReturnType<typeof setTimeout>;

  constructor(
    private readonly importacionService: PacienteImportacionService,
    private readonly refreshService: PacienteListRefreshService
  ) {}

  ngOnInit(): void {
    if (this.active && this.state.previsualizacion && this.state.estado === 'PREVISUALIZADA') {
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
    return !!this.state.operacion;
  }

  get cantidadValidos(): number {
    return this.state.previsualizacion?.filas.filter(fila => fila.estado === 'VALIDO').length ?? 0;
  }

  get plantillaFueDescargada(): boolean {
    return this.state.mensajes.some(mensaje => mensaje.id === 'plantilla-descargada');
  }

  get puedeConfirmar(): boolean {
    return this.state.estado === 'PREVISUALIZADA'
      && !!this.state.previsualizacion?.importacionId
      && this.cantidadValidos > 0
      && !this.estaExpirada();
  }

  descargarPlantilla(): void {
    if (!this.active || this.procesando || this.plantillaFueDescargada || ['CONFIRMADA', 'CANCELADA'].includes(this.state.estado)) return;
    this.agregarMensaje('descargar-plantilla', 'PLANTILLA', 'user', 'Descargar plantilla Excel', { inicioGrupo: true });
    this.state.operacion = 'DESCARGANDO';
    this.state.estado = 'VALIDANDO';
    this.state.mensaje = '';
    this.solicitud = this.importacionService.descargarPlantilla().pipe(
      finalize(() => { this.solicitud = undefined; this.state.operacion = undefined; })
    ).subscribe({
      next: response => {
        if (!response.body) return this.mostrarError('No se recibió la plantilla Excel.');
        saveAs(response.body, this.importacionService.obtenerNombreArchivo(response));
        this.state.plantillaDescargada = true;
        this.state.estado = 'PLANTILLA_DESCARGADA';
        this.agregarMensaje('plantilla-descargada', 'PLANTILLA', 'bot', 'La plantilla se descargó correctamente. Complétala sin modificar sus encabezados y luego adjúntala para analizarla.', { vistasSiguientes: ['file-selection'] });
      },
      error: error => this.manejarError(error)
    });
    this.registrarCancelacion();
  }

  yaTengoPlantilla(): void {
    if (!this.active || this.procesando || ['CONFIRMADA', 'CANCELADA'].includes(this.state.estado)) return;
    this.state.plantillaDescargada = true;
    this.state.estado = 'PLANTILLA_DESCARGADA';
    this.state.mensaje = '';
    this.agregarMensaje('ya-tengo-plantilla', 'PLANTILLA', 'user', 'Ya tengo la plantilla', { inicioGrupo: true });
    this.agregarMensaje('usar-plantilla-existente', 'PLANTILLA', 'bot', 'Perfecto. Selecciona la plantilla Excel que ya tienes para revisar su contenido. Recuerda que debe conservar los encabezados originales y tener formato .xlsx.', { vistasSiguientes: ['file-selection'] });
  }

  seleccionarArchivo(event: Event): void {
    if (!this.active || this.procesando || !this.state.plantillaDescargada) return;
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
    this.agregarMensaje(`archivo-${archivo!.name}-${archivo!.size}`, 'ARCHIVO', 'bot', `He recibido el archivo «${archivo!.name}». Puedes analizarlo para revisar sus datos antes de realizar cualquier registro.`, { inicioGrupo: true, vistasSiguientes: ['file-ready'] });
    this.limpiarTemporizador();
  }

  quitarArchivo(): void {
    if (!this.active || this.procesando) return;
    this.state.archivo = undefined;
    this.state.previsualizacion = undefined;
    this.state.confirmacion = undefined;
    this.state.estado = 'PLANTILLA_DESCARGADA';
    this.state.mensaje = '';
    if (this.archivoInput) this.archivoInput.nativeElement.value = '';
    this.limpiarTemporizador();
  }

  analizarArchivo(): void {
    if (!this.active || this.procesando) return;
    const error = validarArchivoImportacion(this.state.archivo);
    if (error) {
      this.state.mensaje = error;
      return;
    }
    this.state.estado = 'VALIDANDO';
    this.state.operacion = 'ANALIZANDO';
    this.state.mensaje = '';
    this.agregarMensaje(`analizar-${this.state.archivo!.name}`, 'ANALISIS', 'user', 'Analizar archivo', { inicioGrupo: true });
    this.solicitud = this.importacionService.validarArchivo(this.state.archivo!).pipe(
      finalize(() => { this.solicitud = undefined; this.state.operacion = undefined; })
    ).subscribe({
      next: previsualizacion => {
        this.state.previsualizacion = previsualizacion;
        this.state.estado = previsualizacion.estado === 'EXPIRADA' ? 'EXPIRADA' : 'PREVISUALIZADA';
        const tieneValidos = previsualizacion.filas.some(fila => fila.estado === 'VALIDO');
        const texto = tieneValidos
          ? 'Análisis completado. Estos son los resultados encontrados en el archivo.'
          : 'No se encontraron pacientes válidos para registrar. Revisa los errores indicados y vuelve a cargar el archivo corregido.';
        this.agregarMensaje(`analisis-completado-${previsualizacion.importacionId}`, 'ANALISIS', 'bot', texto,
          { vistasSiguientes: tieneValidos ? ['analysis', 'confirmation'] : ['analysis'] });
        this.programarExpiracion(previsualizacion.expiraEn);
      },
      error: errorHttp => {
        this.agregarMensaje(`analisis-error-${Date.now()}`, 'ANALISIS', 'bot', 'No pude analizar completamente el archivo. Revisa los errores indicados y vuelve a intentarlo.');
        this.manejarError(errorHttp);
      }
    });
    this.registrarCancelacion();
  }

  confirmar(): void {
    if (!this.active || !this.puedeConfirmar || this.procesando || !this.state.previsualizacion) return;
    this.agregarMensaje(`confirmar-${this.state.previsualizacion.importacionId}`, 'CONFIRMACION', 'user', `Confirmar registro de ${this.cantidadValidos} pacientes`, { inicioGrupo: true });
    this.state.estado = 'CONFIRMANDO';
    this.state.operacion = 'CONFIRMANDO';
    this.state.mensaje = '';
    this.solicitud = this.importacionService.confirmarImportacion(this.state.previsualizacion.importacionId).pipe(
      finalize(() => { this.solicitud = undefined; this.state.operacion = undefined; })
    ).subscribe({
      next: confirmacion => {
        this.state.confirmacion = confirmacion;
        this.state.estado = confirmacion.estado === 'CONFIRMADA' ? 'CONFIRMADA' : 'ERROR';
        if (this.state.estado === 'CONFIRMADA') {
          this.agregarMensaje(`confirmada-${confirmacion.importacionId}`, 'CONFIRMACION', 'bot', 'El registro masivo se completó correctamente. Los pacientes válidos fueron incorporados al sistema. Aquí tienes el resumen de la operación.', { vistasSiguientes: ['completed'] });
          this.refreshService.solicitarActualizacion();
        }
        this.limpiarTemporizador();
      },
      error: error => this.manejarError(error)
    });
    this.registrarCancelacion();
  }

  noRegistrarPacientes(): void {
    if (!this.active || !this.state.previsualizacion || this.state.estado !== 'PREVISUALIZADA' || this.procesando) return;
    this.state.estado = 'CANCELADA';
    this.state.mensaje = '';
    this.agregarMensaje(`cancelar-${this.state.previsualizacion.importacionId}`, 'CONFIRMACION', 'user', 'No registrar pacientes', { inicioGrupo: true });
    this.agregarMensaje(`cancelada-${this.state.previsualizacion.importacionId}`, 'CONFIRMACION', 'bot', 'La importación fue cancelada. Ningún paciente del archivo fue registrado en el sistema.', { vistasSiguientes: ['cancelled'] });
    this.limpiarTemporizador();
  }

  seleccionarOtroArchivo(): void {
    this.reiniciarIntento('Cargar otro archivo');
  }

  volverACargarExcel(): void {
    this.reiniciarIntento('Volver a cargar Excel');
  }

  private reiniciarIntento(textoAccion: string): void {
    if (!this.active || this.procesando) return;
    this.state.cancelarSolicitud = undefined;
    this.solicitud = undefined;
    this.state.archivo = undefined;
    this.state.previsualizacion = undefined;
    this.state.confirmacion = undefined;
    this.state.mensajes = this.state.mensajes.filter(mensaje => mensaje.etapa === 'PLANTILLA');
    this.state.estado = 'PLANTILLA_DESCARGADA';
    this.state.mensaje = '';
    if (this.archivoInput) this.archivoInput.nativeElement.value = '';
    this.limpiarTemporizador();
    this.agregarMensaje(`reiniciar-importacion-${Date.now()}`, 'ARCHIVO', 'user', textoAccion,
      { vistasSiguientes: ['file-selection'], reemplazarVistaActiva: true, inicioGrupo: true });
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

  private agregarMensaje(
    id: string,
    etapa: PacienteImportacionChatMensaje['etapa'],
    remitente: PacienteImportacionChatMensaje['remitente'],
    texto: string,
    transicion: Pick<PacienteImportacionChatMensaje, 'inicioGrupo' | 'vistasSiguientes' | 'reemplazarVistaActiva'> = {}
  ): void {
    if (this.state.mensajes.some(mensaje => mensaje.id === id)) return;
    const mensaje = { id, etapa, remitente, texto, ...transicion };
    this.state.mensajes.push(mensaje);
    this.mensajeConversacional.emit(mensaje);
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
