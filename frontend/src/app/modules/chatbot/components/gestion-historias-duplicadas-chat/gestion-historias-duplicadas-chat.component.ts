import { CommonModule } from '@angular/common';
import { Component, ElementRef, EventEmitter, Input, OnDestroy, OnInit, Output, ViewChild } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs/operators';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import {
  GestionHistoriasDuplicadasEvento,
  GestionHistoriasDuplicadasState,
  GestionHistoriasDuplicadasVista,
  GrupoHistoriasClinicasDuplicadas,
  HistoriaClinicaAnalisisDetallado
  , FusionarHistoriasClinicasRequest
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
  @ViewChild('passwordInput') passwordInput?: ElementRef<HTMLInputElement>;
  password = '';
  mostrarPassword = false;

  private solicitud?: Subscription;

  constructor(private readonly service: HistoriaClinicaDuplicadaChatService) {}

  ngOnInit(): void {
    if (this.active && this.state.estado === 'CONSULTANDO_DUPLICADOS') this.detectar();
  }

  ngOnDestroy(): void { this.detenerSolicitud(); this.limpiarPassword(); }

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

  continuarConFusion(): void {
    const analisis = this.state.analisis;
    if (!this.active || this.state.estado !== 'MOSTRANDO_COMPARACION' || !analisis?.futuraFusionPermitida) return;
    this.state.idHistoriaPrincipal = analisis.idHistoriaClinicaRecomendada;
    this.state.idHistoriaSecundaria = analisis.historiasComparadas.find(h => h.idHistoriaClinica !== analisis.idHistoriaClinicaRecomendada)?.idHistoriaClinica;
    this.state.estado = 'SELECCIONANDO_PRINCIPAL';
    this.emitir('user', this.textoBotonContinuar, 'principal-selection', false, true);
  }

  cambiarPrincipal(id: number): void {
    if (!this.active || this.state.estado !== 'SELECCIONANDO_PRINCIPAL') return;
    this.state.idHistoriaPrincipal = id;
    if (this.state.idHistoriaSecundaria === id) this.state.idHistoriaSecundaria = this.secundariasDisponibles[0]?.idHistoriaClinica;
  }
  get secundariasDisponibles(): HistoriaClinicaAnalisisDetallado[] {
    return (this.state.analisis?.historiasComparadas ?? []).filter(h => h.idHistoriaClinica !== this.state.idHistoriaPrincipal);
  }
  mostrarVistaPrevia(): void {
    if (!this.active || this.state.estado !== 'SELECCIONANDO_PRINCIPAL' || !this.principal || !this.secundaria) return;
    this.state.estado = 'MOSTRANDO_VISTA_PREVIA';
    this.emitir('user', `Conservar HC ${this.principal.idHistoriaClinica} y fusionar HC ${this.secundaria.idHistoriaClinica}`, 'preview', false, true);
  }
  volverSeleccionPrincipal(): void {
    if (!this.active || this.state.estado !== 'MOSTRANDO_VISTA_PREVIA') return;
    this.state.estado = 'SELECCIONANDO_PRINCIPAL';
    this.emitir('user', 'Cambiar historia principal', 'principal-selection', false, true);
  }
  get principal(): HistoriaClinicaAnalisisDetallado | undefined { return this.historia(this.state.idHistoriaPrincipal); }
  get secundaria(): HistoriaClinicaAnalisisDetallado | undefined { return this.historia(this.state.idHistoriaSecundaria); }
  get cantidadConsultasATransferir(): number {
    return this.historiaSecundariaSeleccionada?.cantidadConsultasExclusivas ?? 0;
  }
  get hayConsultasParaTransferir(): boolean { return this.cantidadConsultasATransferir > 0; }
  get textoBotonContinuar(): string {
    return this.hayConsultasParaTransferir ? 'Continuar con la fusión' : 'Continuar y eliminar duplicada';
  }
  get textoConfirmacion(): string {
    if (!this.principal || !this.secundaria) return '';
    const principal = this.principal.idHistoriaClinica;
    const secundaria = this.secundaria.idHistoriaClinica;
    const cantidad = this.cantidadConsultasATransferir;
    if (cantidad === 0) {
      return `Confirmas que deseas conservar la HC ${principal} y eliminar la HC ${secundaria}. No existen consultas para transferir.`;
    }
    const consultas = cantidad === 1 ? '1 consulta' : `${cantidad} consultas`;
    return `Confirmas que deseas conservar la HC ${principal}, transferir ${consultas} desde la HC ${secundaria} y posteriormente eliminar la HC ${secundaria}.`;
  }
  get mensajeResultadoFusion(): string {
    const respuesta = this.state.respuestaFusion;
    if (respuesta?.idHistoriaPrincipal == null || respuesta.idHistoriaEliminada == null) return respuesta?.mensaje ?? '';
    const cantidad = respuesta.cantidadConsultasTransferidas ?? 0;
    if (cantidad === 0) {
      return `Se conservó la historia clínica ${respuesta.idHistoriaPrincipal} y se eliminó la historia clínica ${respuesta.idHistoriaEliminada}. No fue necesario transferir consultas.`;
    }
    const transferencia = cantidad === 1 ? 'se transfirió 1 consulta' : `se transfirieron ${cantidad} consultas`;
    return `Se conservó la historia clínica ${respuesta.idHistoriaPrincipal}, ${transferencia} desde la HC ${respuesta.idHistoriaEliminada} y posteriormente se eliminó la HC ${respuesta.idHistoriaEliminada}.`;
  }
  confirmarVistaPrevia(): void {
    if (!this.active || this.state.estado !== 'MOSTRANDO_VISTA_PREVIA') return;
    this.state.estado = 'SOLICITANDO_CONTRASENA';
    this.emitir('user', 'Confirmar fusión', 'password', false, true);
    setTimeout(() => this.passwordInput?.nativeElement.focus());
  }
  fusionar(): void {
    if (!this.active || this.state.estado !== 'SOLICITANDO_CONTRASENA' || !this.password || !this.principal || !this.secundaria) return;
    const contrasena = this.password; this.limpiarPassword();
    const request: FusionarHistoriasClinicasRequest = {
      idHistoriaPrincipal: this.principal.idHistoriaClinica, contrasena, confirmacion: true,
      motivo: 'HISTORIA_CLINICA_DUPLICADA', detalle: 'Fusión confirmada desde el Asistente IA.', origen: 'CHATBOT',
      cantidadEsperadaPrincipal: this.principal.cantidadConsultas, cantidadEsperadaSecundaria: this.secundaria.cantidadConsultas,
      idsConsultasEsperadasPrincipal: this.principal.consultasExclusivas.map(c => c.idConsulta),
      idsConsultasEsperadasSecundaria: this.secundaria.consultasExclusivas.map(c => c.idConsulta),
      tokenAnalisis: this.state.analisis!.tokenAnalisis
    };
    this.state.estado = 'FUSIONANDO';
    this.emitir('bot', 'Verificando identidad y fusionando las historias clínicas...', 'fusing', true, true);
    this.solicitud = this.service.fusionar(this.secundaria.idHistoriaClinica, request).pipe(finalize(() => {
      request.contrasena = ''; this.solicitud = undefined;
      if (this.state.estado === 'FUSIONANDO') {
        this.state.estado = 'ERROR';
        this.state.mensajeError = 'No se pudo completar la fusión. No se realizaron cambios.';
        this.emitir('bot', this.state.mensajeError, 'error', true, true);
      }
    })).subscribe({ next: respuesta => {
      this.state.respuestaFusion = respuesta; this.state.estado = 'COMPLETADO'; this.state.intentosRestantes = 3;
      this.emitir('bot', this.mensajeResultadoFusion, 'success', true, true);
    }, error: error => this.procesarErrorFusion(error) });
  }
  limpiarPassword(): void { this.password = ''; this.mostrarPassword = false; }
  alternarPassword(): void { this.mostrarPassword = !this.mostrarPassword; }

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
    this.state.idHistoriaPrincipal = undefined; this.state.idHistoriaSecundaria = undefined;
    this.state.intentosRestantes = 3; this.state.respuestaFusion = undefined; this.limpiarPassword();
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
  private historia(id?: number): HistoriaClinicaAnalisisDetallado | undefined {
    return this.state.analisis?.historiasComparadas.find(h => h.idHistoriaClinica === id);
  }
  private get historiaSecundariaSeleccionada(): HistoriaClinicaAnalisisDetallado | undefined {
    if (this.secundaria) return this.secundaria;
    const analisis = this.state.analisis;
    return analisis?.historiasComparadas.find(h => h.idHistoriaClinica !== analisis.idHistoriaClinicaRecomendada);
  }
  private procesarErrorFusion(error: HttpErrorResponse): void {
    this.limpiarPassword();
    const codigo = error.error?.resultado;
    const mensajeBackend = typeof error.error?.mensaje === 'string' ? error.error.mensaje.trim() : '';
    if (error.status === 401 && codigo === 'CONTRASENA_INCORRECTA') {
      this.state.intentosRestantes--;
      if (this.state.intentosRestantes <= 0) { this.limpiarFlujo(); this.emitir('bot', 'Se alcanzó el máximo de intentos. La fusión fue cancelada.', 'cancelled', true, true); return; }
      this.state.estado = 'SOLICITANDO_CONTRASENA';
      const mensaje = mensajeBackend || 'La contraseña ingresada no es correcta.';
      this.emitir('bot', `${mensaje} Inténtalo nuevamente. Te quedan ${this.state.intentosRestantes} intentos.`, 'password', true, true); return;
    }
    if (codigo === 'ANALISIS_DESACTUALIZADO') {
      this.state.estado = 'CANCELADO'; this.emitir('bot', 'La información cambió. Debes volver a analizar antes de fusionar.', 'cancelled', true, true); return;
    }
    const mensajes: Record<string,string> = { CARGO_NO_AUTORIZADO: 'Tu cargo no permite fusionar historias clínicas.', HISTORIAS_DE_PACIENTES_DIFERENTES: 'Las historias pertenecen a pacientes diferentes.', CONSULTA_INCONSISTENTE: 'Se detectó una consulta inconsistente.', PACIENTE_INACTIVO: 'El paciente ya no está activo.' };
    this.state.estado = 'ERROR'; this.state.mensajeError = mensajeBackend || mensajes[codigo] || 'No se pudo completar la fusión. No se realizaron cambios.';
    this.emitir('bot', this.state.mensajeError, 'error', true, true);
  }

  private emitir(remitente: 'user' | 'bot', texto: string, vistaSiguiente?: GestionHistoriasDuplicadasVista,
      reemplazarVistaActiva = false, inicioGrupo = false, volverHistorias = false): void {
    this.mensajeConversacional.emit({ remitente, texto, vistaSiguiente, reemplazarVistaActiva, inicioGrupo, volverHistorias });
  }
}
