import { CommonModule } from '@angular/common';
import { Component, ElementRef, EventEmitter, Input, OnDestroy, Output, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { catchError, forkJoin, map, of, Subscription, timer } from 'rxjs';
import { finalize } from 'rxjs/operators';
import {
  ArchivadoPacienteDuplicadoRequest,
  GestionDuplicadosChatState,
  GestionDuplicadosEvento,
  GestionDuplicadosVista,
  PacienteDuplicadoDetalle
} from '../../models/paciente-duplicado-chat';
import { PacienteDuplicadoChatService } from '../../services/paciente-duplicado-chat.service';
import { PacienteListRefreshService } from '@app/modules/paciente/services/paciente-list-refresh.service';

@Component({
  selector: 'app-gestion-duplicados-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './gestion-duplicados-chat.component.html',
  styleUrl: './gestion-duplicados-chat.component.scss'
})
export class GestionDuplicadosChatComponent implements OnDestroy {
  @Input({ required: true }) state!: GestionDuplicadosChatState;
  @Input({ required: true }) view!: GestionDuplicadosVista;
  @Input() active = false;
  @Output() mensajeConversacional = new EventEmitter<GestionDuplicadosEvento>();
  @ViewChild('passwordInput') passwordInput?: ElementRef<HTMLInputElement>;

  dniInput = '';
  password = '';
  mostrarPassword = false;
  private request?: Subscription;

  constructor(
    private duplicadosService: PacienteDuplicadoChatService,
    private refreshService: PacienteListRefreshService
  ) {}

  ngOnDestroy(): void {
    this.limpiarPassword();
    this.request?.unsubscribe();
  }

  consultarDni(): void {
    if (!this.active || this.state.estado !== 'SOLICITANDO_DNI') return;
    const dni = this.dniInput.trim();
    if (!/^\d{8}$/.test(dni)) {
      this.state.mensajeError = 'El DNI debe contener exactamente ocho números.';
      return;
    }
    this.state.dni = dni;
    this.state.estado = 'CONSULTANDO_DUPLICADOS';
    this.state.mensajeError = undefined;
    this.emitir('user', dni, 'loading', true, true);
    const resultado$ = this.duplicadosService.analizar(dni).pipe(
      map(analisis => ({ analisis })),
      catchError(() => of({ error: true as const }))
    );
    this.request = forkJoin([resultado$, timer(this.duracionAnalisis())]).pipe(finalize(() => {
      this.request = undefined;
    })).subscribe({
      next: ([resultado]) => 'analisis' in resultado
        ? this.procesarAnalisis(resultado.analisis)
        : this.mostrarErrorConsulta()
    });
    this.state.cancelarSolicitud = () => this.request?.unsubscribe();
  }

  seleccionarParaArchivar(paciente: PacienteDuplicadoDetalle): void {
    if (!this.active || this.state.estado !== 'MOSTRANDO_RESULTADOS' || !this.state.analisis) return;
    const candidatosPrincipal = this.state.analisis.pacientes.filter(item => item.idPaciente !== paciente.idPaciente);
    const principal = candidatosPrincipal.find(item => item.idPaciente === this.state.analisis?.idPacienteRecomendado)
      ?? candidatosPrincipal[0];
    if (!principal || principal.idPaciente === paciente.idPaciente) return;
    this.state.pacienteArchivado = paciente;
    this.state.pacientePrincipal = principal;
    this.state.estado = 'CONFIRMANDO_PRINCIPAL';
    this.emitir('user', `Archivar paciente ID ${paciente.idPaciente}`, 'confirmation', false, true);
  }

  confirmarSeleccion(): void {
    if (!this.active || this.state.estado !== 'CONFIRMANDO_PRINCIPAL') return;
    if (this.state.pacienteArchivado?.idPaciente === this.state.pacientePrincipal?.idPaciente) return;
    if (this.state.analisis?.requiereRevision) {
      this.state.estado = 'REQUIERE_REVISION_CLINICA';
      this.state.revisionClinicaConfirmada = false;
      this.emitir('user', 'Sí, continuar', 'warning', false, true);
      return;
    }
    this.state.revisionClinicaConfirmada = false;
    this.mostrarSolicitudPassword('Sí, continuar');
  }

  cambiarSeleccion(): void {
    if (!this.active) return;
    this.state.pacienteArchivado = undefined;
    this.state.pacientePrincipal = undefined;
    this.state.estado = 'MOSTRANDO_RESULTADOS';
    this.emitir('user', 'Cambiar selección', 'results', false, true);
  }

  confirmarRevision(): void {
    if (!this.active || this.state.estado !== 'REQUIERE_REVISION_CLINICA') return;
    this.state.revisionClinicaConfirmada = true;
    this.mostrarSolicitudPassword('He revisado la información y deseo continuar');
  }

  confirmarArchivado(): void {
    if (!this.active || this.state.estado !== 'SOLICITANDO_CONTRASENA' || !this.password || (this.request && !this.request.closed)) return;
    const contrasenaTemporal = this.password;
    this.limpiarPassword();
    const archivado = this.state.pacienteArchivado;
    const principal = this.state.pacientePrincipal;
    if (!archivado || !principal || archivado.idPaciente === principal.idPaciente) return;

    const request: ArchivadoPacienteDuplicadoRequest = {
      idPacientePrincipal: principal.idPaciente,
      motivo: 'PACIENTE_DUPLICADO',
      detalleMotivo: 'Paciente duplicado archivado desde el Asistente IA después de revisar coincidencia de DNI.',
      contrasena: contrasenaTemporal,
      confirmarRevisionClinica: this.state.revisionClinicaConfirmada,
      origen: 'CHATBOT'
    };
    this.state.estado = 'ARCHIVANDO';
    this.emitir('bot', 'Verificando identidad y archivando al paciente...', 'archiving', true, true);
    this.request = this.duplicadosService.archivar(archivado.idPaciente, request).pipe(finalize(() => {
      request.contrasena = '';
      this.request = undefined;
    })).subscribe({
      next: response => {
        this.state.respuestaArchivado = response;
        this.state.estado = 'COMPLETADO';
        this.state.intentosRestantes = 3;
        this.refreshService.solicitarActualizacion();
        this.emitir('bot', 'Paciente archivado correctamente. La operación fue registrada en auditoría.', 'success', true, true);
      },
      error: error => this.procesarErrorArchivado(error)
    });
    this.state.cancelarSolicitud = () => this.request?.unsubscribe();
  }

  onPasswordEnter(event: Event): void {
    event.preventDefault();
    this.confirmarArchivado();
  }

  alternarPassword(): void {
    this.mostrarPassword = !this.mostrarPassword;
    setTimeout(() => this.passwordInput?.nativeElement.focus());
  }

  cancelar(): void {
    if (!this.active) return;
    this.limpiarFlujo();
    this.emitir('user', 'Cancelar', 'cancelled', true, true, true);
  }

  limpiarPassword(): void {
    this.password = '';
    this.mostrarPassword = false;
  }

  limpiarFlujo(): void {
    this.request?.unsubscribe();
    this.request = undefined;
    this.limpiarPassword();
    this.state.cancelarSolicitud = undefined;
    this.state.estado = 'CANCELADO';
    this.state.dni = '';
    this.state.intentosRestantes = 3;
    this.state.analisis = undefined;
    this.state.pacienteArchivado = undefined;
    this.state.pacientePrincipal = undefined;
    this.state.revisionClinicaConfirmada = false;
    this.state.mensajeError = undefined;
  }

  private duracionAnalisis(): number {
    return 3000 + Math.floor(Math.random() * 3001);
  }

  formatDate(value?: string): string {
    if (!value) return 'No registrada';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return 'No registrada';
    return new Intl.DateTimeFormat('es-PE', { dateStyle: 'short', timeStyle: 'short' }).format(date);
  }

  esRecomendadoParaArchivar(paciente: PacienteDuplicadoDetalle): boolean {
    const analisis = this.state.analisis;
    return !!analisis?.idPacienteRecomendado
      && analisis.pacientes.length === 2
      && analisis.pacientes.some(item => item.idPaciente === analisis.idPacienteRecomendado)
      && paciente.idPaciente !== analisis.idPacienteRecomendado;
  }

  private procesarAnalisis(analisis: GestionDuplicadosChatState['analisis']): void {
    if (!analisis) return;
    this.state.analisis = analisis;
    if (!analisis.esDuplicado) {
      this.state.estado = 'SOLICITANDO_DNI';
      const texto = analisis.cantidadPacientesActivos === 0
        ? 'No se encontraron pacientes activos con ese DNI.'
        : 'Solo existe un paciente activo con ese DNI. No hay duplicados para gestionar.';
      this.emitir('bot', texto, 'dni', true, true);
      return;
    }
    this.state.estado = 'MOSTRANDO_RESULTADOS';
    this.emitir('bot', analisis.mensaje, 'results', true, true);
  }

  private mostrarSolicitudPassword(textoUsuario: string): void {
    this.state.estado = 'SOLICITANDO_CONTRASENA';
    this.emitir('user', textoUsuario, 'password', false, true);
    setTimeout(() => this.passwordInput?.nativeElement.focus());
  }

  private mostrarErrorConsulta(): void {
    this.state.estado = 'SOLICITANDO_DNI';
    this.emitir('bot', 'No se pudo consultar la información en este momento. Inténtalo nuevamente.', 'dni', true, true);
  }

  private procesarErrorArchivado(error: HttpErrorResponse | { status?: number; error?: { resultado?: string; mensaje?: string } }): void {
    this.limpiarPassword();
    const codigo = error?.error?.resultado ?? '';
    if (error?.status === 401 && codigo === 'CONTRASENA_INCORRECTA') {
      this.state.intentosRestantes -= 1;
      if (this.state.intentosRestantes <= 0) {
        this.limpiarFlujo();
        this.emitir('bot', 'Se alcanzó el máximo de intentos. La operación fue cancelada.', 'cancelled', true, true, true);
        return;
      }
      this.state.estado = 'SOLICITANDO_CONTRASENA';
      const intento = this.state.intentosRestantes === 1 ? 'Te queda 1 intento.' : `Te quedan ${this.state.intentosRestantes} intentos.`;
      this.emitir('bot', `La contraseña no es correcta. ${intento}`, 'password', true, true);
      return;
    }

    const mensajes: Record<string, string> = {
      CARGO_NO_AUTORIZADO: 'Tu cargo no tiene permiso para archivar pacientes.',
      USUARIO_INACTIVO: 'Tu usuario no se encuentra habilitado para realizar esta operación.',
      EMPLEADO_INACTIVO: 'Tu usuario no se encuentra habilitado para realizar esta operación.',
      USUARIO_REQUERIDO: 'No se pudo identificar al usuario conectado. Cierra sesión e ingresa nuevamente.',
      PACIENTE_YA_ARCHIVADO: 'El paciente seleccionado ya fue archivado previamente.',
      PACIENTE_PRINCIPAL_ARCHIVADO: 'El paciente principal seleccionado ya no se encuentra activo.',
      PACIENTES_NO_SON_DUPLICADOS: 'Los pacientes seleccionados no comparten el mismo DNI.',
      CONFIRMACION_REVISION_REQUERIDA: 'Debes confirmar que revisaste la información clínica antes de continuar.',
      CONFLICTO_VERSION: 'La información del paciente cambió durante el proceso. Vuelve a consultar los duplicados.'
    };
    const mensaje = mensajes[codigo] ?? 'No se pudo completar el archivado. No se realizaron cambios.';
    const reintentar = codigo === 'CONFLICTO_VERSION';
    if (reintentar) {
      this.state.estado = 'SOLICITANDO_DNI';
      this.state.analisis = undefined;
      this.state.pacienteArchivado = undefined;
      this.state.pacientePrincipal = undefined;
      this.emitir('bot', mensaje, 'dni', true, true);
      return;
    }
    this.limpiarFlujo();
    this.emitir('bot', mensaje, 'cancelled', true, true, true);
  }

  private emitir(
    remitente: 'user' | 'bot',
    texto: string,
    vistaSiguiente?: GestionDuplicadosVista,
    reemplazarVistaActiva = false,
    inicioGrupo = false,
    volverPacientes = false
  ): void {
    this.mensajeConversacional.emit({ remitente, texto, vistaSiguiente, reemplazarVistaActiva, inicioGrupo, volverPacientes });
  }
}
