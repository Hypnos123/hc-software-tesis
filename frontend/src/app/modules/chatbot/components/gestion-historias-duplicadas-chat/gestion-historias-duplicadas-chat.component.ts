import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import {
  GestionHistoriasDuplicadasEvento,
  GestionHistoriasDuplicadasState,
  GestionHistoriasDuplicadasVista,
  GrupoHistoriasClinicasDuplicadas,
  HistoriaClinicaAnalisisDetallado
} from '../../models/historia-clinica-duplicada-chat';
import { HistoriaClinicaDuplicadaChatService } from '../../services/historia-clinica-duplicada-chat.service';

@Component({
  selector: 'app-gestion-historias-duplicadas-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './gestion-historias-duplicadas-chat.component.html',
  styleUrl: './gestion-historias-duplicadas-chat.component.scss'
})
export class GestionHistoriasDuplicadasChatComponent implements OnInit, OnDestroy {
  @Input({ required: true }) state!: GestionHistoriasDuplicadasState;
  @Input({ required: true }) view!: GestionHistoriasDuplicadasVista;
  @Input() active = false;
  @Output() mensajeConversacional = new EventEmitter<GestionHistoriasDuplicadasEvento>();

  private solicitud?: Subscription;

  constructor(private readonly service: HistoriaClinicaDuplicadaChatService) {}

  ngOnInit(): void {
    if (this.active && this.state.estado === 'CONSULTANDO_DUPLICADOS') this.detectar();
  }

  ngOnDestroy(): void { this.detenerSolicitud(); }

  seleccionarGrupo(grupo: GrupoHistoriasClinicasDuplicadas): void {
    if (!this.active || this.state.estado !== 'MOSTRANDO_HISTORIAS') return;
    this.state.grupoSeleccionado = grupo;
    this.state.idsSeleccionados = grupo.historiasClinicas.map(historia => historia.idHistoriaClinica);
    this.state.estado = 'SELECCIONANDO_HISTORIAS';
    this.emitir('user', `Revisar grupo con ${grupo.cantidad} historias clínicas`, 'selection', false, true);
  }

  estaSeleccionada(idHistoria: number): boolean { return this.state.idsSeleccionados.includes(idHistoria); }

  cambiarSeleccion(idHistoria: number, event: Event): void {
    if (!this.active || this.state.estado !== 'SELECCIONANDO_HISTORIAS') return;
    const checked = (event.target as HTMLInputElement).checked;
    this.state.idsSeleccionados = checked
      ? Array.from(new Set([...this.state.idsSeleccionados, idHistoria]))
      : this.state.idsSeleccionados.filter(id => id !== idHistoria);
  }

  get todasSeleccionadas(): boolean {
    const total = this.state.grupoSeleccionado?.historiasClinicas.length ?? 0;
    return total > 0 && this.state.idsSeleccionados.length === total;
  }

  alternarTodas(): void {
    if (!this.active || this.state.estado !== 'SELECCIONANDO_HISTORIAS') return;
    this.state.idsSeleccionados = this.todasSeleccionadas ? []
      : (this.state.grupoSeleccionado?.historiasClinicas ?? []).map(historia => historia.idHistoriaClinica);
  }

  analizar(): void {
    if (!this.active || this.state.estado !== 'SELECCIONANDO_HISTORIAS' || this.state.idsSeleccionados.length < 2) return;
    const ids = [...this.state.idsSeleccionados];
    this.state.estado = 'ANALIZANDO_HISTORIAS';
    this.emitir('user', `Analizar historias clínicas ${ids.join(', ')}`, 'analyzing', false, true);
    const solicitud = this.service.analizar(ids).subscribe({
      next: analisis => {
        this.solicitud = undefined;
        this.state.cancelarSolicitud = undefined;
        this.state.analisis = analisis;
        this.state.estado = 'MOSTRANDO_COMPARACION';
        this.emitir('bot', this.presentacionInicial(analisis.historiasComparadas), undefined, true, true);
        this.emitir('bot', `Recomiendo conservar la historia clínica ${analisis.idHistoriaClinicaRecomendada}.`, undefined);
        this.emitir('bot', this.resumenFinal(), 'comparison');
      },
      error: () => this.manejarError('No fue posible analizar las historias clínicas seleccionadas.')
    });
    this.solicitud = solicitud.closed ? undefined : solicitud;
    this.state.cancelarSolicitud = solicitud.closed ? undefined : () => solicitud.unsubscribe();
  }

  finalizar(): void {
    if (!this.active || this.state.estado !== 'MOSTRANDO_COMPARACION') return;
    this.state.estado = 'COMPLETADO';
    this.state.idsSeleccionados = [];
    this.emitir('user', 'Finalizar análisis');
    this.emitir('bot', 'El análisis finalizó sin realizar cambios en las historias clínicas.', undefined, false, false, true);
  }

  analizarOtroGrupo(): void {
    if (!this.active || this.state.estado !== 'MOSTRANDO_COMPARACION') return;
    this.state.analisis = undefined;
    this.state.grupoSeleccionado = undefined;
    this.state.idsSeleccionados = [];
    this.state.estado = 'MOSTRANDO_HISTORIAS';
    this.emitir('user', 'Analizar otro grupo', 'groups', false, true);
  }

  cancelar(): void {
    if (!this.active || ['CANCELADO', 'COMPLETADO'].includes(this.state.estado)) return;
    this.limpiarFlujo();
    this.emitir('user', 'Cancelar análisis');
    this.emitir('bot', 'El análisis de historias clínicas duplicadas fue cancelado sin realizar cambios.', 'cancelled', false, false, true);
  }

  limpiarFlujo(): void {
    this.detenerSolicitud();
    this.state.estado = 'CANCELADO';
    this.state.deteccion = undefined;
    this.state.grupoSeleccionado = undefined;
    this.state.idsSeleccionados = [];
    this.state.analisis = undefined;
    this.state.mensajeError = undefined;
  }

  formatDate(value?: string): string {
    if (!value) return 'No registrada';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? 'No registrada' : new Intl.DateTimeFormat('es-PE', { dateStyle: 'short', timeStyle: 'short' }).format(date);
  }

  esRecomendada(historia: HistoriaClinicaAnalisisDetallado): boolean {
    return historia.idHistoriaClinica === this.state.analisis?.idHistoriaClinicaRecomendada;
  }

  trackHistoria(_: number, historia: { idHistoriaClinica: number }): number { return historia.idHistoriaClinica; }

  private detectar(): void {
    const solicitud = this.service.detectar().subscribe({
      next: deteccion => {
        this.solicitud = undefined;
        this.state.cancelarSolicitud = undefined;
        this.state.deteccion = deteccion;
        if (!deteccion.hayDuplicados || deteccion.duplicados.length === 0) {
          this.state.estado = 'COMPLETADO';
          this.emitir('bot', deteccion.mensaje || 'No se encontraron historias clínicas duplicadas.', undefined, true, true, true);
          return;
        }
        this.state.estado = 'MOSTRANDO_HISTORIAS';
        this.emitir('bot', `${deteccion.mensaje} Selecciona el grupo que deseas analizar.`, 'groups', true);
      },
      error: () => this.manejarError('No fue posible consultar las historias clínicas duplicadas.')
    });
    this.solicitud = solicitud.closed ? undefined : solicitud;
    this.state.cancelarSolicitud = solicitud.closed ? undefined : () => solicitud.unsubscribe();
  }

  private presentacionInicial(historias: HistoriaClinicaAnalisisDetallado[]): string {
    const paciente = historias[0]?.nombreCompleto?.trim();
    return paciente ? `He analizado las historias clínicas seleccionadas para ${paciente}.`
      : 'He analizado las historias clínicas seleccionadas.';
  }

  private resumenFinal(): string {
    const analisis = this.state.analisis!;
    if (analisis.advertenciasIntegridad.length) {
      return 'Se detectaron advertencias de integridad. Por seguridad, estas historias no se consideran aptas para una futura fusión.';
    }
    if (!analisis.futuraFusionPermitida) {
      return analisis.motivoBloqueo || 'No es seguro fusionar automáticamente estas historias clínicas.';
    }
    const secundarias = analisis.historiasComparadas.filter(h => h.idHistoriaClinica !== analisis.idHistoriaClinicaRecomendada);
    const exclusivas = secundarias.reduce((total, historia) => total + historia.cantidadConsultasExclusivas, 0);
    return exclusivas === 0 ? 'No existen consultas para transferir en este caso.'
      : `Las historias secundarias contienen ${exclusivas} consultas exclusivas que podrían transferirse en una futura fusión.`;
  }

  private manejarError(mensaje: string): void {
    this.solicitud = undefined;
    this.state.cancelarSolicitud = undefined;
    this.state.estado = 'ERROR';
    this.state.mensajeError = mensaje;
    this.emitir('bot', mensaje, 'error', true);
  }

  private detenerSolicitud(): void {
    this.state.cancelarSolicitud?.();
    this.state.cancelarSolicitud = undefined;
    this.solicitud?.unsubscribe();
    this.solicitud = undefined;
  }

  private emitir(remitente: 'user' | 'bot', texto: string, vistaSiguiente?: GestionHistoriasDuplicadasVista,
      reemplazarVistaActiva = false, inicioGrupo = false, volverHistorias = false): void {
    this.mensajeConversacional.emit({ remitente, texto, vistaSiguiente, reemplazarVistaActiva, inicioGrupo, volverHistorias });
  }
}
