import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { Subscription } from 'rxjs';

import { HistoriaClinicaService } from '@app/modules/historiaClinica/services/consultas.service';
import {
  HistoriasClinicasFaltantesChatState,
  HistoriasClinicasFaltantesEvento,
  HistoriasClinicasFaltantesVista
} from '../../models/historias-clinicas-faltantes-chat';

@Component({
  selector: 'app-historias-clinicas-faltantes-chat',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './historias-clinicas-faltantes-chat.component.html',
  styleUrl: './historias-clinicas-faltantes-chat.component.scss'
})
export class HistoriasClinicasFaltantesChatComponent implements OnInit, OnDestroy {
  @Input({ required: true }) state!: HistoriasClinicasFaltantesChatState;
  @Input({ required: true }) view!: HistoriasClinicasFaltantesVista;
  @Input() active = false;
  @Output() mensajeConversacional = new EventEmitter<HistoriasClinicasFaltantesEvento>();

  private solicitud?: Subscription;

  constructor(private readonly historiaClinicaService: HistoriaClinicaService) {}

  ngOnInit(): void {
    if (this.active && this.state.estado === 'CARGANDO' && !this.state.preview) {
      this.cargarPreview();
    }
  }

  ngOnDestroy(): void {
    this.solicitud?.unsubscribe();
  }

  get cantidadSeleccionados(): number {
    return this.state.idsSeleccionados.length;
  }

  get todosSeleccionados(): boolean {
    const pacientes = this.state.preview?.pacientes ?? [];
    return pacientes.length > 0 && this.cantidadSeleccionados === pacientes.length;
  }

  get seleccionParcial(): boolean {
    return this.cantidadSeleccionados > 0 && !this.todosSeleccionados;
  }

  estaSeleccionado(idPaciente: number): boolean {
    return this.state.idsSeleccionados.includes(idPaciente);
  }

  cambiarSeleccion(idPaciente: number, event: Event): void {
    if (!this.puedeSeleccionar() || !this.esCandidato(idPaciente)) return;
    const seleccionado = (event.target as HTMLInputElement).checked;
    this.state.idsSeleccionados = seleccionado
      ? Array.from(new Set([...this.state.idsSeleccionados, idPaciente]))
      : this.state.idsSeleccionados.filter(id => id !== idPaciente);
  }

  alternarTodos(): void {
    if (!this.puedeSeleccionar()) return;
    this.state.idsSeleccionados = this.todosSeleccionados
      ? []
      : (this.state.preview?.pacientes ?? []).map(paciente => paciente.idPaciente);
  }

  continuar(): void {
    if (!this.puedeSeleccionar() || this.cantidadSeleccionados === 0) return;
    this.state.idsConfirmados = [...this.state.idsSeleccionados];
    this.state.estado = 'CONFIRMANDO';
    this.emitir('user', `Continuar con ${this.cantidadSeleccionados} pacientes`, 'confirmation', false, true);
  }

  volverASeleccionar(): void {
    if (!this.active || this.state.estado !== 'CONFIRMANDO') return;
    this.state.estado = 'SELECCIONANDO';
    this.emitir('user', 'Volver a seleccionar', 'selection', false, true);
  }

  cancelar(): void {
    if (!this.active || this.state.estado === 'CANCELADO') return;
    this.state.idsSeleccionados = [];
    this.state.idsConfirmados = [];
    this.state.estado = 'CANCELADO';
    this.state.cancelarSolicitud?.();
    this.state.cancelarSolicitud = undefined;
    this.emitir('user', 'Cancelar');
    this.emitir('bot', 'La creación de historias clínicas faltantes fue cancelada.', undefined, false, false, true);
  }

  limpiarFlujo(): void {
    this.state.cancelarSolicitud?.();
    this.state.cancelarSolicitud = undefined;
    this.solicitud = undefined;
    this.state.idsSeleccionados = [];
    this.state.idsConfirmados = [];
    this.state.estado = 'CANCELADO';
  }

  trackByPaciente(_: number, paciente: { idPaciente: number }): number {
    return paciente.idPaciente;
  }

  private cargarPreview(): void {
    this.solicitud = this.historiaClinicaService.getHistoriasClinicasFaltantes().subscribe({
      next: preview => {
        this.solicitud = undefined;
        this.state.cancelarSolicitud = undefined;
        this.state.preview = { ...preview, pacientes: [...(preview.pacientes ?? [])] };
        this.state.idsSeleccionados = [];
        this.state.idsConfirmados = [];
        if (this.state.preview.pacientes.length === 0) {
          this.state.estado = 'SIN_CANDIDATOS';
          this.emitir('bot', 'No se encontraron pacientes activos sin historia clínica.', 'empty', true);
          return;
        }
        this.state.estado = 'SELECCIONANDO';
        this.emitir('bot', `Se encontraron ${this.state.preview.cantidad} pacientes activos sin historia clínica. Selecciona los pacientes que deseas incluir.`, 'selection', true);
      },
      error: () => {
        this.solicitud = undefined;
        this.state.cancelarSolicitud = undefined;
        this.state.estado = 'ERROR';
        this.state.mensajeError = 'No fue posible consultar los pacientes sin historia clínica.';
        this.emitir('bot', this.state.mensajeError, 'error', true);
      }
    });
    const solicitud = this.solicitud;
    this.state.cancelarSolicitud = () => solicitud?.unsubscribe();
  }

  private puedeSeleccionar(): boolean {
    return this.active && this.state.estado === 'SELECCIONANDO';
  }

  private esCandidato(idPaciente: number): boolean {
    return (this.state.preview?.pacientes ?? []).some(paciente => paciente.idPaciente === idPaciente);
  }

  private emitir(
    remitente: HistoriasClinicasFaltantesEvento['remitente'],
    texto: string,
    vistaSiguiente?: HistoriasClinicasFaltantesVista,
    reemplazarVistaActiva = false,
    inicioGrupo = false,
    volverHistorias = false
  ): void {
    this.mensajeConversacional.emit({
      remitente,
      texto,
      vistaSiguiente,
      reemplazarVistaActiva,
      inicioGrupo,
      volverHistorias
    });
  }
}
