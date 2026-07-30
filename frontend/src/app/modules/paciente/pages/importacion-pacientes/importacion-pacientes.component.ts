import { CommonModule } from '@angular/common';
import { Component, ElementRef, OnDestroy, ViewChild } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { saveAs } from 'file-saver';
import { finalize } from 'rxjs/operators';
import {
  EstadoFlujoImportacion,
  EstadoFilaImportacion,
  IPacienteImportacionConfirmacion,
  IPacienteImportacionFila,
  IPacienteImportacionPrevisualizacion
} from '../../models/paciente-importacion';
import { PacienteImportacionService } from '../../services/paciente-importacion.service';

const MAX_TAMANO_ARCHIVO = 2 * 1024 * 1024;

@Component({
  selector: 'app-importacion-pacientes',
  standalone: true,
  imports: [CommonModule, ButtonModule, TableModule, TagModule, TooltipModule],
  templateUrl: './importacion-pacientes.component.html',
  styleUrl: './importacion-pacientes.component.scss'
})
export class ImportacionPacientesComponent implements OnDestroy {
  @ViewChild('archivoInput') archivoInput?: ElementRef<HTMLInputElement>;

  estado: EstadoFlujoImportacion = 'INICIAL';
  archivoSeleccionado?: File;
  previsualizacion?: IPacienteImportacionPrevisualizacion;
  confirmacion?: IPacienteImportacionConfirmacion;
  mensajeError = '';
  descargando = false;
  private expiracionTimer?: ReturnType<typeof setTimeout>;

  constructor(private importacionService: PacienteImportacionService) {}

  ngOnDestroy(): void {
    this.limpiarTemporizador();
  }

  get procesando(): boolean {
    return this.estado === 'VALIDANDO' || this.estado === 'CONFIRMANDO' || this.descargando;
  }

  get puedeSeleccionarArchivo(): boolean {
    return !this.procesando && this.estado !== 'INICIAL' && this.estado !== 'CONFIRMADA';
  }

  get cantidadValidos(): number {
    return this.previsualizacion?.filas.filter(fila => fila.estado === 'VALIDO').length ?? 0;
  }

  get puedeConfirmar(): boolean {
    return this.estado === 'PREVISUALIZADA'
      && !!this.previsualizacion?.importacionId
      && this.cantidadValidos > 0
      && !this.importacionExpirada();
  }

  descargarPlantilla(): void {
    if (this.procesando) return;
    this.descargando = true;
    this.mensajeError = '';
    this.importacionService.descargarPlantilla().pipe(
      finalize(() => this.descargando = false)
    ).subscribe({
      next: response => {
        if (!response.body) {
          this.mostrarError('No se recibió la plantilla Excel.');
          return;
        }
        saveAs(response.body, this.importacionService.obtenerNombreArchivo(response));
        this.estado = 'PLANTILLA_DESCARGADA';
      },
      error: error => this.manejarErrorHttp(error)
    });
  }

  seleccionarArchivo(event: Event): void {
    const input = event.target as HTMLInputElement;
    const archivo = input.files?.[0];
    if (!archivo) {
      this.mostrarErrorArchivo('Selecciona un archivo Excel.');
      return;
    }
    const error = this.validarArchivoLocal(archivo);
    if (error) {
      input.value = '';
      this.mostrarErrorArchivo(error);
      return;
    }
    this.archivoSeleccionado = archivo;
    this.previsualizacion = undefined;
    this.confirmacion = undefined;
    this.mensajeError = '';
    this.limpiarTemporizador();
    this.estado = 'ARCHIVO_SELECCIONADO';
  }

  quitarArchivo(): void {
    if (this.procesando) return;
    this.archivoSeleccionado = undefined;
    this.previsualizacion = undefined;
    this.confirmacion = undefined;
    this.mensajeError = '';
    this.limpiarTemporizador();
    if (this.archivoInput) this.archivoInput.nativeElement.value = '';
    this.estado = 'PLANTILLA_DESCARGADA';
  }

  analizarArchivo(): void {
    if (this.procesando) return;
    if (!this.archivoSeleccionado) {
      this.mostrarErrorArchivo('Selecciona un archivo Excel antes de analizarlo.');
      return;
    }
    const error = this.validarArchivoLocal(this.archivoSeleccionado);
    if (error) {
      this.mostrarErrorArchivo(error);
      return;
    }
    this.estado = 'VALIDANDO';
    this.mensajeError = '';
    this.importacionService.validarArchivo(this.archivoSeleccionado).subscribe({
      next: preview => {
        this.previsualizacion = preview;
        this.estado = preview.estado === 'EXPIRADA' ? 'EXPIRADA' : 'PREVISUALIZADA';
        this.programarExpiracion(preview.expiraEn);
      },
      error: errorHttp => this.manejarErrorHttp(errorHttp)
    });
  }

  confirmar(): void {
    if (!this.puedeConfirmar || this.procesando || !this.previsualizacion) return;
    this.estado = 'CONFIRMANDO';
    this.mensajeError = '';
    this.importacionService.confirmarImportacion(this.previsualizacion.importacionId).subscribe({
      next: resultado => {
        this.confirmacion = resultado;
        this.estado = resultado.estado === 'CONFIRMADA' ? 'CONFIRMADA' : 'ERROR';
        this.limpiarTemporizador();
      },
      error: error => this.manejarErrorHttp(error)
    });
  }

  registrarOtroArchivo(): void {
    if (this.procesando) return;
    this.limpiarTemporizador();
    this.archivoSeleccionado = undefined;
    this.previsualizacion = undefined;
    this.confirmacion = undefined;
    this.mensajeError = '';
    if (this.archivoInput) this.archivoInput.nativeElement.value = '';
    this.estado = 'INICIAL';
  }

  formatoTamano(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    return `${(bytes / 1024 / 1024).toFixed(2)} MB`;
  }

  claseEstadoFila(fila: IPacienteImportacionFila): string {
    if (fila.estado === 'VALIDO' && fila.advertencias.length) return 'advertencia';
    return fila.estado.toLowerCase().replaceAll('_', '-');
  }

  etiquetaEstadoFila(fila: IPacienteImportacionFila): string {
    const etiquetas: Record<EstadoFilaImportacion, string> = {
      VALIDO: fila.advertencias.length ? 'ADVERTENCIA' : 'VÁLIDO',
      ERROR_DATOS: 'ERROR',
      DNI_EXISTENTE: 'DNI EXISTENTE',
      DNI_DUPLICADO_ARCHIVO: 'DNI DUPLICADO',
      REGISTRADO: 'REGISTRADO',
      NO_REGISTRADO: 'NO REGISTRADO'
    };
    return etiquetas[fila.estado];
  }

  private validarArchivoLocal(archivo: File): string {
    if (archivo.size === 0) return 'El archivo seleccionado está vacío.';
    if (!archivo.name.toLowerCase().endsWith('.xlsx')) return 'Solo se permiten archivos con extensión .xlsx.';
    if (archivo.size > MAX_TAMANO_ARCHIVO) return 'El archivo supera el tamaño permitido de 2 MB.';
    return '';
  }

  private programarExpiracion(expiraEn: string): void {
    this.limpiarTemporizador();
    const restante = new Date(expiraEn).getTime() - Date.now();
    if (!Number.isFinite(restante) || restante <= 0) {
      this.estado = 'EXPIRADA';
      return;
    }
    this.expiracionTimer = setTimeout(() => {
      if (this.estado === 'PREVISUALIZADA') this.estado = 'EXPIRADA';
    }, restante);
  }

  private importacionExpirada(): boolean {
    if (!this.previsualizacion) return true;
    return new Date(this.previsualizacion.expiraEn).getTime() <= Date.now();
  }

  private limpiarTemporizador(): void {
    if (this.expiracionTimer) clearTimeout(this.expiracionTimer);
    this.expiracionTimer = undefined;
  }

  private mostrarErrorArchivo(mensaje: string): void {
    this.mensajeError = mensaje;
    this.estado = this.archivoSeleccionado ? 'ARCHIVO_SELECCIONADO' : 'PLANTILLA_DESCARGADA';
  }

  private mostrarError(mensaje: string): void {
    this.mensajeError = mensaje;
    this.estado = 'ERROR';
  }

  private manejarErrorHttp(error: HttpErrorResponse): void {
    const funcional = typeof error.error?.mensaje === 'string' ? error.error.mensaje : '';
    const mensajes: Record<number, string> = {
      400: 'El archivo o su estructura no son válidos.',
      404: 'La importación no fue encontrada.',
      409: 'La importación está siendo procesada o ya fue confirmada.',
      410: 'La previsualización expiró. Vuelve a analizar el archivo.',
      413: 'El archivo supera el tamaño permitido de 2 MB.',
      500: 'Ocurrió un error inesperado al procesar la importación.'
    };
    this.mensajeError = funcional || mensajes[error.status] || 'No se pudo completar la operación.';
    this.estado = error.status === 410 ? 'EXPIRADA' : 'ERROR';
  }
}
