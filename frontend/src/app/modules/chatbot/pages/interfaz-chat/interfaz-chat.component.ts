import { Component, ElementRef, OnDestroy, QueryList, ViewChild, ViewChildren } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin, map, Observable, of, Subscription, switchMap, throwError } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { AuthService } from '@app/auth/services/auth.service';
import { AsistenteService } from '../../services/asistente.service';
import { IAsistenteResponse } from '../../models/asistente';
import { HistoriaClinicaService } from '@app/modules/historiaClinica/services/consultas.service';
import { IPacienteBusqueda } from '@app/modules/historiaClinica/models/historiaClinica';
import { AntecedentesService } from '@app/modules/paciente/services/antecedentes.service';
import { IPaciente } from '@app/modules/paciente/models/paciente';
import { Router } from '@angular/router';
import { ClinicalHistoryTransferService } from '@app/shared/services/clinical-history-transfer.service';
import { ClinicalHistoryTransferCandidate } from '@app/shared/models/clinical-history-transfer';
import { ClinicalHistoryFlowFeedbackService } from '@app/shared/services/clinical-history-flow-feedback.service';
import { ClinicalHistoryFlowFeedback } from '@app/shared/models/clinical-history-flow-feedback';
import {
  crearPacienteImportacionChatState,
  ImportacionPacientesChatComponent,
  PacienteImportacionChatMensaje,
  PacienteImportacionChatState,
  PacienteImportView
} from '@app/modules/paciente/components/importacion-pacientes-chat/importacion-pacientes-chat.component';
import { GestionDuplicadosChatComponent } from '../../components/gestion-duplicados-chat/gestion-duplicados-chat.component';
import {
  crearGestionDuplicadosState,
  GestionDuplicadosChatState,
  GestionDuplicadosEvento,
  GestionDuplicadosVista
} from '../../models/paciente-duplicado-chat';

interface ChatMessage { id: string; sender: 'user' | 'bot'; type: 'text' | 'menu' | 'patient-import' | 'duplicate-management'; text?: string; menuId?: string; options?: MenuOption[]; importacion?: PacienteImportacionChatState; importView?: PacienteImportView; importActive?: boolean; duplicados?: GestionDuplicadosChatState; duplicateView?: GestionDuplicadosVista; duplicateActive?: boolean; }
type MenuAction = 'menu' | 'prompt' | 'request' | 'clinical-history-flow' | 'patient-import-flow' | 'patient-duplicate-flow';
interface MenuOption { id?: string; label: string; description?: string; icon?: string; action: MenuAction; target?: string; text?: string; }
interface ChatMenu { question?: string; options: MenuOption[]; }

const VERIFY_CLINICAL_HISTORY_OPTION: MenuOption = {
  label: 'Verificar si un paciente tiene historia clínica',
  action: 'prompt',
  text: 'Puedes consultar por DNI, ID o nombre completo.\n\nEjemplos:\n- ¿El paciente con DNI 72845292 tiene historia clínica?\n- Consulta si el paciente ID 4 tiene historia clínica.\n- ¿Existe una historia clínica para Rafael Velásquez Morales?'
};
const VERIFY_PATIENT_CONSULTATIONS_OPTION: MenuOption = {
  label: 'Verificar consultas médicas de un paciente',
  action: 'prompt',
  text: 'Puedes consultar por DNI, ID o nombre completo.\n\nEjemplos:\n- ¿El paciente con DNI 72845292 tiene consultas médicas?\n- Muéstrame las consultas médicas del paciente ID 4.\n- ¿Cuál fue la última consulta médica de Rafael Velásquez Morales?'
};
export interface PatientClinicalHistorySummary {
  idPaciente: number;
  nombreCompleto: string;
  dni: string;
  fechaNacimiento: string | Date;
  estadoCivil: string;
  existingClinicalHistoryCount: number;
}
export interface ClinicalHistoryCandidateData {
  idPaciente: number;
  dni: string;
  nombres: string;
  apellidos: string;
  fechaIngreso: string | Date;
  fechaNacimiento: string | Date;
  estadoCivil: string;
  enfermedadesPrevias: string | null;
  cirugiasPrevias: string | null;
  alergiaMedicamentos: string | null;
}
export type ClinicalHistoryChatFlow =
  | { step: 'idle' }
  | { step: 'awaitingDni' }
  | { step: 'searchingPatient'; dni: string }
  | { step: 'awaitingConfirmation'; dni: string; patient: PatientClinicalHistorySummary; prefill: ClinicalHistoryCandidateData }
  | { step: 'navigating'; dni: string; patient: PatientClinicalHistorySummary; prefill: ClinicalHistoryCandidateData; transferId: string };

type PatientResolution =
  | { kind: 'none' }
  | { kind: 'multiple' }
  | { kind: 'unique'; patient: IPacienteBusqueda; antecedentes: IPaciente | undefined; existingClinicalHistoryCount: number };

@Component({ selector: 'app-interfaz-chat', standalone: true, imports: [CommonModule, FormsModule, ImportacionPacientesChatComponent, GestionDuplicadosChatComponent], templateUrl: './interfaz-chat.component.html', styleUrl: './interfaz-chat.component.scss' })
export class InterfazChatComponent implements OnDestroy {
  @ViewChild('chatBody') chatBody!: ElementRef;
  @ViewChildren('conversationBlock') conversationBlocks!: QueryList<ElementRef<HTMLElement>>;
  @ViewChildren(ImportacionPacientesChatComponent) importacionComponents!: QueryList<ImportacionPacientesChatComponent>;
  @ViewChildren(GestionDuplicadosChatComponent) gestionDuplicadosComponents!: QueryList<GestionDuplicadosChatComponent>;

  private readonly initialMessage = 'Hola, soy el Asistente IA del sistema de historias clínicas.\n\nPuedo ayudarte a usar el sistema, consultar información registrada, verificar datos y revisar las opciones disponibles.\n\nSelecciona una categoría para continuar o escribe tu pregunta.';
  private readonly menus: Record<string, ChatMenu> = {
    principal: {
      options: [
        { label: 'Manejo del sistema', description: 'Aprende a utilizar las funciones principales.', icon: 'pi pi-cog', action: 'menu', target: 'manejo' },
        { label: 'Consultar información', description: 'Consulta datos registrados en el sistema.', icon: 'pi pi-search', action: 'menu', target: 'consultar' },
        { label: 'Verificar datos', description: 'Confirma si la información ya existe antes de registrar.', icon: 'pi pi-check-circle', action: 'menu', target: 'verificar' },
        { label: 'Soporte y ayuda', description: 'Revisa preguntas disponibles y ayuda del asistente.', icon: 'pi pi-question-circle', action: 'menu', target: 'ayuda' }
      ]
    },
    consultar: { question: '¿Sobre qué sección deseas consultar información?', options: [
      { label: 'Pacientes', icon: 'pi pi-users', action: 'menu', target: 'pacientes' },
      { label: 'Historias clínicas', icon: 'pi pi-folder-open', action: 'menu', target: 'historias' },
      { label: 'Consultas médicas', icon: 'pi pi-calendar', action: 'menu', target: 'consultas' }
    ] },
    pacientes: { question: 'Puedes realizar estas consultas sobre pacientes:', options: [
      { label: '¿Cuántos pacientes hay registrados?', action: 'request' },
      { label: 'Muéstrame los últimos pacientes registrados', action: 'request' },
      { label: 'Buscar paciente por DNI', action: 'prompt', text: 'Escribe el DNI de 8 dígitos del paciente.\nEjemplo: Buscar paciente por DNI 72845292' },
      { label: 'Buscar paciente por nombre', action: 'prompt', text: 'Escribe los nombres y apellidos del paciente.\nEjemplo: Buscar paciente por nombre Rafael Velásquez Morales' },
      { label: 'Consulta el paciente por ID', action: 'prompt', text: 'Escribe el ID del paciente.\nEjemplo: Consulta el paciente ID 4' },
      { label: '¿Cuál es la edad promedio de los pacientes?', action: 'request' },
      { label: 'Registrar pacientes de forma masiva', description: 'Importa pacientes mediante la plantilla oficial Excel.', icon: 'pi pi-file-excel', action: 'patient-import-flow' },
      { label: 'Gestionar paciente duplicado', description: 'Compara y archiva de forma segura un registro repetido.', icon: 'pi pi-clone', action: 'patient-duplicate-flow' }
    ] },
    historias: { question: 'Puedes realizar estas consultas sobre historias clínicas:', options: [
      { label: 'Crear historia clínica', description: 'Completa una nueva historia usando los datos de un paciente existente.', action: 'clinical-history-flow' },
      { label: '¿Cuántas historias clínicas hay registradas?', action: 'request' },
      { label: '¿El paciente con DNI 72845292 tiene historia clínica?', action: 'request' },
      { label: 'Consulta si el paciente ID 4 tiene historia clínica', action: 'request' },
      { label: 'Busca la historia clínica de un paciente por nombre', action: 'prompt', text: 'Escribe el nombre completo del paciente.\nEjemplo: ¿Existe una historia clínica para Rafael Velásquez Morales?' },
      { label: 'Historias clínicas creadas hoy', action: 'request' },
      { label: 'Detectar historias clínicas duplicadas', description: 'Lista historias activas repetidas y recomienda cuál conservar.', action: 'request' }
    ] },
    consultas: { question: 'Puedes realizar estas consultas médicas:', options: [
      { label: '¿Cuántas consultas médicas hay registradas?', action: 'request' },
      { label: '¿El paciente con DNI 72845292 tiene consultas médicas?', action: 'request' },
      { label: 'Muéstrame las consultas médicas del paciente ID 4', action: 'request' },
      { label: '¿Cuál fue la última consulta médica de un paciente?', action: 'prompt', text: 'Indica el DNI, ID o nombre completo del paciente.\nEjemplo: ¿Cuál fue la última consulta médica de Rafael Velásquez Morales?' },
      { label: '¿Tiene consultas médicas pendientes?', action: 'prompt', text: 'Indica el DNI, ID o nombre completo del paciente.\nEjemplo: ¿El paciente con DNI 72845292 tiene consultas médicas pendientes?' },
      { label: 'Consultas médicas atendidas hoy', action: 'request' }
    ] },
    verificar: { question: 'Selecciona qué información deseas verificar:', options: [
      { label: 'Verificar si un paciente existe', action: 'prompt', text: 'Puedes verificarlo por DNI, ID o nombre completo.\n\nEjemplos:\n- ¿Existe un paciente con DNI 72845292?\n- Consulta el paciente ID 4\n- Verifica si Rafael Velásquez Morales está registrado.' },
      VERIFY_CLINICAL_HISTORY_OPTION,
      VERIFY_PATIENT_CONSULTATIONS_OPTION,
      { label: 'Detectar posibles pacientes duplicados', action: 'prompt', text: 'Puedes usar estas preguntas:\n- ¿Existen pacientes duplicados?\n- Verifica si hay pacientes repetidos.\n- Analiza posibles duplicados.\n- Busca pacientes duplicados.' },
      { label: 'Detectar historias clínicas duplicadas', action: 'prompt', text: 'Puedes buscar en general o por DNI.\n\nEjemplos:\n- ¿Existen historias clínicas duplicadas?\n- Revisa la duplicidad de historias clínicas.\n- ¿El DNI 01234567 tiene historias clínicas duplicadas?' }
    ] },
    manejo: { question: '¿Sobre qué proceso del sistema necesitas ayuda? Selecciona una opción o escribe tu pregunta.', options: [
      { label: 'Pacientes', icon: 'pi pi-users', action: 'menu', target: 'manejo-pacientes' },
      { label: 'Historias clínicas', icon: 'pi pi-folder-open', action: 'menu', target: 'manejo-historias' },
      { label: 'Consultas médicas', icon: 'pi pi-calendar', action: 'menu', target: 'manejo-consultas' }
    ] },
    'manejo-pacientes': { question: 'Selecciona una opción o escribe tu pregunta sobre la gestión de pacientes.', options: [
      { label: '¿Cómo registro un paciente?', action: 'request' },
      { label: '¿Cómo edito los datos de un paciente?', action: 'request' },
      { label: '¿Cómo visualizo los datos de un paciente?', action: 'request' }
    ] },
    'manejo-historias': { question: 'Selecciona una opción o escribe tu pregunta sobre la gestión de historias clínicas.', options: [
      { label: '¿Cómo creo una historia clínica?', action: 'request' },
      { label: '¿Cómo edito una historia clínica?', action: 'request' },
      { label: '¿Cómo visualizo una historia clínica?', action: 'request' }
    ] },
    'manejo-consultas': { question: 'Selecciona una opción o escribe tu pregunta sobre la gestión de consultas médicas.', options: [
      { label: '¿Cómo agrego una consulta médica?', action: 'request' },
      { label: '¿Cómo comienzo la atención de una consulta médica?', action: 'request' },
      { label: '¿Cómo visualizo una consulta médica antes de atenderla?', action: 'request' }
    ] },
    ayuda: { question: 'Selecciona una opción de soporte y ayuda:', options: [
      { label: '¿Qué preguntas puedo hacer?', action: 'request' }, { label: 'Mostrar consultas disponibles', action: 'request' },
      { label: 'Cómo usar el asistente', action: 'request' }
    ] },
    'duplicados-final': { question: '¿Qué deseas hacer ahora?', options: [
      { label: 'Consultar otro DNI', action: 'patient-duplicate-flow' },
      { label: 'Volver al menú de pacientes', action: 'menu', target: 'pacientes' },
      { label: 'Menú principal', action: 'menu', target: 'principal' }
    ] }
  };

  private activeRequest?: Subscription;
  private clinicalHistoryRequest?: Subscription;
  private logoutSubscription: Subscription;
  private sessionChangedSubscription: Subscription;
  private feedbackSubscription: Subscription;
  private readonly processedFeedbackIds = new Set<string>();
  private messageSequence = 0;
  private scrollPosition = 0;
  private floatingMessageTimer?: ReturnType<typeof setTimeout>;
  private floatingMessageIndex = 0;
  private readonly floatingDismissedUntilKey = 'asistenteFloatingDismissedUntil';
  private readonly floatingMessages = [
    'Estoy aquí para ayudarte.',
    'Puedes hacerme varias preguntas.',
    'Puedo ayudarte a optimizar procesos.',
    'Consulta pacientes, historias y duplicados.',
    'Te guío paso a paso en el sistema.',
    'Puedo ayudarte a verificar información.'
  ];
  mensajeFlotanteVisible = false;
  mensajeFlotante = '';
  isOpen = false; userMessage = ''; isLoading = false;
  messages: ChatMessage[] = this.getInitialMessages();
  clinicalHistoryFlow: ClinicalHistoryChatFlow = { step: 'idle' };
  quickQuestions = ['Menú principal', '¿Qué preguntas puedo hacer?', 'Buscar paciente por DNI', 'Verificar historia clínica', 'Consultas médicas de un paciente'];
  private readonly quickQuestionOptions: Record<string, MenuOption> = {
    'Verificar historia clínica': VERIFY_CLINICAL_HISTORY_OPTION,
    'Consultas médicas de un paciente': VERIFY_PATIENT_CONSULTATIONS_OPTION
  };
  get gestionDuplicadosActiva(): boolean { return this.hayGestionDuplicadosActiva(); }
  get autenticado(): boolean { return !!this.authService.usuario?.idUsuario; }

  constructor(
    private asistenteService: AsistenteService,
    private authService: AuthService,
    private historiaClinicaService: HistoriaClinicaService,
    private antecedentesService: AntecedentesService,
    private router: Router,
    private clinicalHistoryTransferService: ClinicalHistoryTransferService,
    private feedbackService: ClinicalHistoryFlowFeedbackService
  ) {
    this.logoutSubscription = this.authService.logout$.subscribe(() => this.resetChat(true));
    this.sessionChangedSubscription = this.authService.sessionChanged$.subscribe(autenticado => this.handleSessionChange(autenticado));
    this.feedbackSubscription = this.feedbackService.feedback$.subscribe(feedback => this.handleClinicalHistoryFeedback(feedback));
    if (this.autenticado) this.scheduleFloatingMessage(10_000);
  }
  ngOnDestroy(): void { this.clearFloatingMessageTimer(); this.gestionDuplicadosComponents?.forEach(component => component.limpiarFlujo()); this.activeRequest?.unsubscribe(); this.clinicalHistoryRequest?.unsubscribe(); this.logoutSubscription.unsubscribe(); this.sessionChangedSubscription.unsubscribe(); this.feedbackSubscription.unsubscribe(); }
  toggleChat(): void { this.isOpen ? this.minimizeChat() : this.openChat(); }
  openChat(): void { this.clearFloatingMessageTimer(); this.hideFloatingMessage(); this.isOpen = true; this.restoreScrollPosition(); }
  minimizeChat(): void { this.gestionDuplicadosComponents?.forEach(component => component.limpiarPassword()); this.saveScrollPosition(); this.isOpen = false; this.scheduleFloatingMessage(90_000); }
  closeChat(): void {
    this.importacionComponents?.forEach(component => component.limpiarFlujo());
    this.gestionDuplicadosComponents?.forEach(component => component.limpiarFlujo());
    this.resetChat(true);
    this.scheduleFloatingMessage(90_000);
  }
  sendMessage(): void {
    const pregunta = this.userMessage.trim();
    if (!this.autenticado || this.isLoading) return;
    if (this.clinicalHistoryFlow.step === 'awaitingDni') {
      const dniMessage = pregunta ? this.addUserMessage(pregunta) : undefined;
      this.userMessage = '';
      this.captureAndSearchDni(pregunta, dniMessage?.id);
      return;
    }
    if (this.clinicalHistoryFlow.step !== 'idle' || this.hayGestionDuplicadosActiva() || !pregunta) return;
    this.addUserMessage(pregunta);
    this.userMessage = '';
    if (this.esIntencionGestionDuplicados(pregunta)) {
      this.iniciarGestionDuplicadosDesdeTexto();
      return;
    }
    this.askBackend(pregunta, true);
  }
  onEnter(event: Event): void { const keyboardEvent = event as KeyboardEvent; if (keyboardEvent.shiftKey) return; keyboardEvent.preventDefault(); this.sendMessage(); }
  selectHistoricalMenuOption(menuMessage: ChatMessage, option: MenuOption): void {
    if (!this.autenticado || this.isLoading) return;
    const selection = this.addUserMessage(option.label);
    this.removeMenuOption(menuMessage, option);
    this.executeMenuOption(option, selection.id);
  }
  quickAsk(text: string): void {
    if (!this.autenticado || this.isLoading) return;
    if (text === 'Menú principal') { this.cancelarGestionDuplicadosSilenciosamente(); this.stopClinicalHistoryRequest(); this.resetClinicalHistoryFlow(); const selection = this.addUserMessage(text); this.addMenuBlock('principal'); this.scrollToNewBlock(selection.id); return; }
    const quickOption = text === 'Buscar paciente por DNI'
      ? this.menus['pacientes'].options[2]
      : this.quickQuestionOptions[text] ?? { label: text, action: 'request' as MenuAction };
    const selection = this.addUserMessage(quickOption.label);
    this.executeMenuOption(quickOption, selection.id);
  }
  mostrarOpcionesPacientes(): void {
    const selection = this.addUserMessage('Volver a opciones de Pacientes');
    this.addMenuBlock('pacientes');
    this.scrollToNewBlock(selection.id);
  }
  registrarOtroArchivo(): void {
    const selection = this.addUserMessage('Registrar otro archivo');
    this.addBotMessage('Adjunta otra plantilla Excel para analizarla antes de confirmar el registro.');
    const state = crearPacienteImportacionChatState();
    state.plantillaDescargada = true;
    state.estado = 'PLANTILLA_DESCARGADA';
    this.addImportBlock(state, 'file-selection', true);
    this.scrollToNewBlock(selection.id);
  }
  manejarMensajeImportacion(
    state: PacienteImportacionChatState,
    evento: PacienteImportacionChatMensaje
  ): void {
    const tarjetaActiva = this.messages.find(message => message.type === 'patient-import' && message.importacion === state && message.importActive);
    if (evento.reemplazarVistaActiva && tarjetaActiva) {
      this.messages = this.messages.filter(message => message !== tarjetaActiva);
    } else if (evento.vistasSiguientes?.length && tarjetaActiva) {
      tarjetaActiva.importActive = false;
    }
    const mensaje = evento.remitente === 'user'
      ? this.addUserMessage(evento.texto)
      : this.addBotMessage(evento.texto);
    evento.vistasSiguientes?.forEach((view, index, views) => this.addImportBlock(state, view, index === views.length - 1));
    if (evento.inicioGrupo) this.scrollToNewBlock(mensaje.id);
  }
  manejarMensajeGestionDuplicados(state: GestionDuplicadosChatState, evento: GestionDuplicadosEvento): void {
    const tarjetaActiva = this.messages.find(message => message.type === 'duplicate-management' && message.duplicados === state && message.duplicateActive);
    let vistaActualizada = false;
    if (evento.reemplazarVistaActiva && tarjetaActiva) {
      const posicionTarjeta = this.messages.indexOf(tarjetaActiva);
      if (posicionTarjeta >= 0) this.messages.splice(posicionTarjeta, 1);
      tarjetaActiva.duplicateView = evento.vistaSiguiente ?? tarjetaActiva.duplicateView;
      tarjetaActiva.duplicateActive = !['COMPLETADO', 'CANCELADO'].includes(state.estado);
      vistaActualizada = true;
    } else if (tarjetaActiva) {
      tarjetaActiva.duplicateActive = false;
    }
    const mensaje = evento.remitente === 'user' ? this.addUserMessage(evento.texto) : this.addBotMessage(evento.texto);
    if (vistaActualizada && tarjetaActiva) this.messages.push(tarjetaActiva);
    if (evento.vistaSiguiente && !vistaActualizada) this.addDuplicateBlock(state, evento.vistaSiguiente, !['COMPLETADO', 'CANCELADO'].includes(state.estado));
    if (state.estado === 'COMPLETADO') this.addMenuBlock('duplicados-final');
    if (evento.volverPacientes) this.addMenuBlock('pacientes');
    if (evento.inicioGrupo) this.scrollToNewBlock(mensaje.id);
  }
  cancelarGestionDuplicados(): void {
    const activa = this.gestionDuplicadosComponents?.find(component => component.active);
    activa?.cancelar();
  }
  cancelClinicalHistoryFlow(): void {
    if (this.clinicalHistoryFlow.step === 'idle') return;
    this.stopClinicalHistoryRequest();
    this.resetClinicalHistoryFlow();
    this.addUserMessage('Cancelar');
    this.addBotMessage('La creación de la historia clínica fue cancelada.');
    this.scrollToBottom();
  }
  continueClinicalHistoryFlow(): void {
    if (this.clinicalHistoryFlow.step !== 'awaitingConfirmation') return;
    const { dni, patient, prefill } = this.clinicalHistoryFlow;
    const transferId = this.clinicalHistoryTransferService.createTransfer(this.toTransferCandidate(prefill));
    this.clinicalHistoryFlow = { step: 'navigating', dni, patient, prefill, transferId };
    this.isLoading = true;
    const selection = this.addUserMessage('Continuar');
    this.scrollToNewBlock(selection.id);
    void this.router.navigate(
      ['/historiaClinica', 'mantenimiento-historias-clinicas', 'nuevo'],
      { state: { source: 'chatbot', transferId } }
    ).then(navigated => {
      if (!navigated) {
        this.handleClinicalHistoryNavigationError(dni, patient, prefill, transferId);
        return;
      }
      if (this.clinicalHistoryFlow.step === 'navigating' && this.clinicalHistoryFlow.transferId === transferId) {
        this.resetClinicalHistoryFlow();
      }
    }).catch(() => this.handleClinicalHistoryNavigationError(dni, patient, prefill, transferId))
      .finally(() => { this.isLoading = false; });
  }
  scrollToBottom(): void { requestAnimationFrame(() => { if (this.chatBody) this.chatBody.nativeElement.scrollTop = this.chatBody.nativeElement.scrollHeight; }); }
  private scrollToNewBlock(blockId: string): void {
    requestAnimationFrame(() => this.conversationBlocks.find(block => block.nativeElement.dataset['blockId'] === blockId)?.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'start' }));
  }
  cerrarMensajeFlotante(): void {
    this.clearFloatingMessageTimer();
    this.hideFloatingMessage();
    const bloqueadoHasta = Date.now() + 300_000;
    sessionStorage.setItem(this.floatingDismissedUntilKey, String(bloqueadoHasta));
    this.scheduleFloatingMessage(300_000);
  }
  private handleSessionChange(autenticado: boolean): void {
    this.clearFloatingMessageTimer();
    this.hideFloatingMessage();
    if (!autenticado) return;
    this.isOpen = false;
    this.messages = this.getInitialMessages();
    this.scheduleFloatingMessage(10_000);
  }
  private scheduleFloatingMessage(delay: number): void {
    this.clearFloatingMessageTimer();
    if (!this.autenticado || this.isOpen) return;
    const bloqueoRestante = this.getFloatingDismissalRemaining();
    this.floatingMessageTimer = setTimeout(() => this.tryShowFloatingMessage(), Math.max(delay, bloqueoRestante));
  }
  private tryShowFloatingMessage(): void {
    this.floatingMessageTimer = undefined;
    if (!this.canShowFloatingMessage()) {
      if (this.autenticado && !this.isOpen) this.scheduleFloatingMessage(Math.max(1_000, this.getFloatingDismissalRemaining()));
      return;
    }
    this.mensajeFlotante = this.floatingMessages[this.floatingMessageIndex];
    this.floatingMessageIndex = (this.floatingMessageIndex + 1) % this.floatingMessages.length;
    this.mensajeFlotanteVisible = true;
    this.floatingMessageTimer = setTimeout(() => {
      this.floatingMessageTimer = undefined;
      this.hideFloatingMessage();
      this.scheduleFloatingMessage(90_000);
    }, 5_000);
  }
  private canShowFloatingMessage(): boolean {
    return this.autenticado && !this.isOpen && !this.isLoading && !this.userMessage.trim()
        && this.clinicalHistoryFlow.step === 'idle' && !this.hayGestionDuplicadosActiva()
        && this.getFloatingDismissalRemaining() === 0;
  }
  private getFloatingDismissalRemaining(): number {
    const bloqueadoHasta = Number(sessionStorage.getItem(this.floatingDismissedUntilKey) ?? 0);
    return Math.max(0, bloqueadoHasta - Date.now());
  }
  private hideFloatingMessage(): void { this.mensajeFlotanteVisible = false; }
  private clearFloatingMessageTimer(): void {
    if (this.floatingMessageTimer !== undefined) clearTimeout(this.floatingMessageTimer);
    this.floatingMessageTimer = undefined;
  }
  private saveScrollPosition(): void { if (this.chatBody) this.scrollPosition = this.chatBody.nativeElement.scrollTop; }
  private restoreScrollPosition(): void { requestAnimationFrame(() => { if (this.chatBody) this.chatBody.nativeElement.scrollTop = this.scrollPosition; }); }
  private addUserMessage(text: string): ChatMessage { const message = this.createTextMessage('user', text); this.messages.push(message); return message; }
  private addBotMessage(text: string): ChatMessage { const message = this.createTextMessage('bot', text); this.messages.push(message); return message; }
  private addMenuBlock(menuId: string): void {
    const menu = this.menus[menuId];
    if (menuId !== 'principal' && menu.question) this.addBotMessage(menu.question);
    this.messages.push({ id: this.nextMessageId(), sender: 'bot', type: 'menu', menuId, options: this.createMenuOptions(menuId) });
  }
  private addImportBlock(state: PacienteImportacionChatState, view: PacienteImportView, active: boolean): void {
    this.messages.push({ id: this.nextMessageId(), sender: 'bot', type: 'patient-import', importacion: state, importView: view, importActive: active });
  }
  private addDuplicateBlock(state: GestionDuplicadosChatState, view: GestionDuplicadosVista, active: boolean): void {
    this.messages.push({ id: this.nextMessageId(), sender: 'bot', type: 'duplicate-management', duplicados: { ...state }, duplicateView: view, duplicateActive: active });
  }
  private createTextMessage(sender: ChatMessage['sender'], text: string): ChatMessage { return { id: this.nextMessageId(), sender, type: 'text', text }; }
  private nextMessageId(): string { this.messageSequence += 1; return `message-${this.messageSequence}`; }
  private askBackend(pregunta: string, scrollAfterResponse: boolean): void {
    this.isLoading = true; this.addBotMessage('Escribiendo...');
    this.activeRequest = this.asistenteService.preguntar(pregunta).pipe(finalize(() => { this.isLoading = false; this.activeRequest = undefined; })).subscribe({
      next: (response) => {
        this.removeTypingMessage();
        const resultado = this.addBotMessage(this.formatResponse(response));
        if (this.esResultadoDuplicadoExtenso(response)) {
          this.scrollToNewBlock(resultado.id);
        } else if (scrollAfterResponse) {
          this.scrollToBottom();
        }
      },
      error: () => { this.removeTypingMessage(); this.addBotMessage('No pude obtener la información en este momento. Inténtalo nuevamente.'); if (scrollAfterResponse) this.scrollToBottom(); }
    });
  }
  private esResultadoDuplicadoExtenso(response: IAsistenteResponse): boolean {
    if (response.intencion === 'ANALISIS_DUPLICADOS_PACIENTES' || response.intencion === 'BUSQUEDA_DUPLICADO_DNI_MULTIPLE') return true;
    if (response.intencion !== 'HISTORIAS_CLINICAS_DUPLICADAS') return false;
    return response.datos?.['hayDuplicados'] === true;
  }
  private resetChat(clearStorage: boolean): void { this.clearFloatingMessageTimer(); this.hideFloatingMessage(); this.messages.forEach(message => { message.importacion?.cancelarSolicitud?.(); message.duplicados?.cancelarSolicitud?.(); }); this.cancelarGestionDuplicadosSilenciosamente(); this.activeRequest?.unsubscribe(); this.activeRequest = undefined; this.stopClinicalHistoryRequest(); this.isOpen = false; this.isLoading = false; this.userMessage = ''; this.scrollPosition = 0; this.resetClinicalHistoryFlow(); this.messages = this.getInitialMessages(); if (clearStorage) this.clearStoredChat(); }
  private removeTypingMessage(): void { if (this.messages[this.messages.length - 1]?.text === 'Escribiendo...') this.messages.pop(); }
  private getInitialMessages(): ChatMessage[] {
    if (!this.autenticado) {
      return [this.createTextMessage('bot', 'Hola, soy el Asistente IA del sistema.\nPara realizar consultas, verificar datos o ayudarte con los procesos, primero debes iniciar sesión.')];
    }
    const welcome = this.createTextMessage('bot', this.initialMessage);
    return [welcome, { id: this.nextMessageId(), sender: 'bot', type: 'menu', menuId: 'principal', options: this.createMenuOptions('principal') }];
  }
  private clearStoredChat(): void { localStorage.removeItem('asistenteChatState'); sessionStorage.removeItem('asistenteChatState'); }
  private createMenuOptions(menuId: string): MenuOption[] {
    return this.menus[menuId].options
      .filter(option => option.action !== 'patient-duplicate-flow' || this.puedeGestionarDuplicados())
      .map((option, index) => ({ ...option, id: `${menuId}-${index}` }));
  }
  private removeMenuOption(menuMessage: ChatMessage, option: MenuOption): void { menuMessage.options = (menuMessage.options || []).filter(item => item.id !== option.id); }
  private executeMenuOption(option: MenuOption, selectionId: string): void {
    if (option.action === 'request') { this.askBackend(option.label, false); this.scrollToNewBlock(selectionId); return; }
    if (option.action === 'prompt') { this.addBotMessage(option.text || ''); this.scrollToNewBlock(selectionId); return; }
    if (option.action === 'clinical-history-flow') {
      this.clinicalHistoryFlow = { step: 'awaitingDni' };
      this.addBotMessage('Ingresa el DNI de ocho dígitos del paciente existente.');
      this.scrollToNewBlock(selectionId);
      return;
    }
    if (option.action === 'patient-import-flow') {
      const activa = this.messages.find(message => message.type === 'patient-import' && message.importacion?.estado !== 'CONFIRMADA');
      if (activa) {
        this.addBotMessage('Ya existe una importación activa. Complétala o ciérrala antes de iniciar otra.');
        this.scrollToNewBlock(selectionId);
        return;
      }
      this.addBotMessage('Puedes registrar varios pacientes al mismo tiempo utilizando la plantilla oficial Excel. Descarga la plantilla, completa la información sin modificar los encabezados y luego adjunta el archivo para revisarlo antes de confirmar el registro.');
      this.addImportBlock(crearPacienteImportacionChatState(), 'template', true);
      this.scrollToNewBlock(selectionId);
      return;
    }
    if (option.action === 'patient-duplicate-flow') {
      this.iniciarGestionDuplicados(selectionId);
      return;
    }
    this.addMenuBlock(option.target || 'principal');
    this.scrollToNewBlock(selectionId);
  }
  private resetClinicalHistoryFlow(): void { this.clinicalHistoryFlow = { step: 'idle' }; }
  private iniciarGestionDuplicados(selectionId?: string): void {
    if (!this.puedeGestionarDuplicados()) {
      this.addBotMessage('Tu cargo no tiene permiso para archivar pacientes.');
      if (selectionId) this.scrollToNewBlock(selectionId);
      return;
    }
    if (!this.authService.usuario?.idUsuario) {
      this.addBotMessage('No se pudo identificar al usuario conectado. Cierra sesión e ingresa nuevamente.');
      return;
    }
    if (this.clinicalHistoryFlow.step !== 'idle' || this.hayGestionDuplicadosActiva()) {
      this.addBotMessage('Ya existe una operación guiada activa. Complétala o cancélala antes de iniciar otra.');
      return;
    }
    const state = crearGestionDuplicadosState();
    this.addBotMessage('Ingresa el DNI de ocho dígitos del paciente duplicado que deseas revisar.');
    this.addDuplicateBlock(state, 'dni', true);
    if (selectionId) this.scrollToNewBlock(selectionId);
  }
  private iniciarGestionDuplicadosDesdeTexto(): void {
    this.iniciarGestionDuplicados();
    this.scrollToBottom();
  }
  private hayGestionDuplicadosActiva(): boolean {
    return this.messages.some(message => message.type === 'duplicate-management' && message.duplicateActive);
  }
  private cancelarGestionDuplicadosSilenciosamente(): void {
    this.gestionDuplicadosComponents?.forEach(component => {
      if (component.active) component.limpiarFlujo();
    });
    this.messages.forEach(message => { if (message.type === 'duplicate-management') message.duplicateActive = false; });
  }
  private esIntencionGestionDuplicados(texto: string): boolean {
    const normalizado = texto.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
    if (/historias? clinicas?/.test(normalizado)) return false;
    const mencionaObjetoGestionable = /(duplicad|repetid|paciente|registro)/.test(normalizado);
    const accionGestion = /\b(gestionar|eliminar|archivar|decidir|conservar)\b/.test(normalizado);
    return mencionaObjetoGestionable && accionGestion;
  }
  private puedeGestionarDuplicados(): boolean {
    const cargo = this.normalizarCargo(this.authService.usuario?.cargo);
    return cargo === 'ADMINISTRADOR' || cargo === 'ENFERMERO';
  }
  private normalizarCargo(cargo?: string): string {
    const normalizado = (cargo ?? '').trim().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/\s+/g, ' ').toUpperCase();
    return ['ENFERMERA', 'ENFERMERA(O)', 'ENFERMERO(A)', 'ENFERMERIA'].includes(normalizado) ? 'ENFERMERO' : normalizado;
  }
  private captureAndSearchDni(dni: string, messageId?: string): void {
    if (messageId) this.scrollToNewBlock(messageId);
    if (!/^\d{8}$/.test(dni)) {
      this.addBotMessage('El DNI debe contener exactamente ocho dígitos. Inténtalo nuevamente o cancela la operación.');
      return;
    }

    this.clinicalHistoryFlow = { step: 'searchingPatient', dni };
    this.isLoading = true;
    this.addBotMessage('Consultando paciente...');
    this.clinicalHistoryRequest = this.resolvePatient(dni).pipe(finalize(() => {
      this.isLoading = false;
      this.clinicalHistoryRequest = undefined;
    })).subscribe({
      next: resolution => this.handlePatientResolution(dni, resolution),
      error: () => this.handleClinicalHistoryError()
    });
  }
  private resolvePatient(dni: string): Observable<PatientResolution> {
    return this.historiaClinicaService.buscarPacientesPorDni(dni).pipe(
      map(pacientes => pacientes.filter(paciente => (paciente.numDocumento ?? paciente.dni)?.trim() === dni)),
      switchMap(coincidencias => {
        if (coincidencias.length === 0) return of({ kind: 'none' } as PatientResolution);
        if (coincidencias.length > 1) return of({ kind: 'multiple' } as PatientResolution);
        const patient = coincidencias[0];
        if (!patient.idPaciente) return throwError(() => new Error('El paciente no tiene identificador.'));
        return forkJoin({
          antecedentes: this.antecedentesService.getByPacienteId(patient.idPaciente),
          historias: this.historiaClinicaService.getByPaciente(patient.idPaciente)
        }).pipe(map(({ antecedentes, historias }) => ({
          kind: 'unique' as const,
          patient,
          antecedentes,
          existingClinicalHistoryCount: historias.length
        })));
      })
    );
  }
  private handlePatientResolution(dni: string, resolution: PatientResolution): void {
    this.removeClinicalHistoryLoadingMessage();
    if (resolution.kind === 'none') {
      this.clinicalHistoryFlow = { step: 'awaitingDni' };
      this.addBotMessage('No existe un paciente registrado con el DNI indicado.');
      return;
    }
    if (resolution.kind === 'multiple') {
      this.clinicalHistoryFlow = { step: 'awaitingDni' };
      this.addBotMessage('Se encontraron varios pacientes con el mismo DNI. Por seguridad, no se puede seleccionar automáticamente uno de ellos. Ingresa otro DNI o cancela la operación.');
      return;
    }
    const { patient, antecedentes, existingClinicalHistoryCount } = resolution;
    const prefill = this.createCandidateData(dni, patient, antecedentes);
    const summary: PatientClinicalHistorySummary = {
      idPaciente: prefill.idPaciente,
      nombreCompleto: [prefill.nombres, prefill.apellidos].filter(Boolean).join(' ').trim(),
      dni,
      fechaNacimiento: prefill.fechaNacimiento,
      estadoCivil: prefill.estadoCivil,
      existingClinicalHistoryCount
    };
    this.clinicalHistoryFlow = { step: 'awaitingConfirmation', dni, patient: summary, prefill };
    this.addBotMessage(this.formatPatientSummary(summary));
  }
  private createCandidateData(dni: string, patient: IPacienteBusqueda, antecedentes?: IPaciente): ClinicalHistoryCandidateData {
    return {
      idPaciente: patient.idPaciente!, dni,
      nombres: patient.nombres?.trim() ?? '', apellidos: patient.apellidos?.trim() ?? '',
      fechaIngreso: patient.fechaIngreso ?? '', fechaNacimiento: patient.fechaNacimiento ?? '',
      estadoCivil: patient.estadoCivil?.trim() ?? '',
      enfermedadesPrevias: antecedentes?.enfermedadesPrevias?.trim() || null,
      cirugiasPrevias: antecedentes?.cirugiasPrevias?.trim() || null,
      alergiaMedicamentos: antecedentes?.alergiaMedicamentos?.trim() || null
    };
  }
  private formatPatientSummary(patient: PatientClinicalHistorySummary): string {
    return `Paciente encontrado:\n\nNombre: ${patient.nombreCompleto}\nDNI: ${patient.dni}\nFecha de nacimiento: ${this.formatDate(patient.fechaNacimiento)}\nEstado civil: ${this.formatCivilStatus(patient.estadoCivil)}\nHistorias clínicas existentes: ${patient.existingClinicalHistoryCount}\n\n¿Deseas continuar a la creación de una nueva historia clínica?`;
  }
  private formatDate(value: string | Date): string {
    if (!value) return 'No registrada';
    if (typeof value === 'string') {
      const match = value.match(/^(\d{4})-(\d{2})-(\d{2})/);
      if (match) return `${match[3]}/${match[2]}/${match[1]}`;
    }
    const date = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(date.getTime())) return 'No registrada';
    return `${String(date.getDate()).padStart(2, '0')}/${String(date.getMonth() + 1).padStart(2, '0')}/${date.getFullYear()}`;
  }
  private formatCivilStatus(value: string): string {
    const statuses: Record<string, string> = { SOLTERO: 'Soltero(a)', CASADO: 'Casado(a)', DIVORCIADO: 'Divorciado(a)', VIUDO: 'Viudo(a)' };
    return statuses[value.trim().toUpperCase()] ?? (value || 'No registrado');
  }
  private toTransferCandidate(prefill: ClinicalHistoryCandidateData): ClinicalHistoryTransferCandidate {
    return {
      idPaciente: prefill.idPaciente,
      dni: prefill.dni.trim(),
      nombres: prefill.nombres.trim(),
      apellidos: prefill.apellidos.trim(),
      fechaIngreso: this.toDateOnly(prefill.fechaIngreso),
      fechaNacimiento: this.toDateOnly(prefill.fechaNacimiento),
      estadoCivil: prefill.estadoCivil.trim(),
      enfermedadesPrevias: prefill.enfermedadesPrevias?.trim() || null,
      cirugiasPrevias: prefill.cirugiasPrevias?.trim() || null,
      alergiaMedicamentos: prefill.alergiaMedicamentos?.trim() || null
    };
  }
  private toDateOnly(value: string | Date): string {
    if (typeof value === 'string') {
      const match = value.match(/^(\d{4}-\d{2}-\d{2})/);
      return match?.[1] ?? '';
    }
    if (!(value instanceof Date) || Number.isNaN(value.getTime())) return '';
    return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}-${String(value.getDate()).padStart(2, '0')}`;
  }
  private handleClinicalHistoryNavigationError(
    dni: string,
    patient: PatientClinicalHistorySummary,
    prefill: ClinicalHistoryCandidateData,
    transferId: string
  ): void {
    this.clinicalHistoryTransferService.revokeTransfer(transferId);
    if (this.clinicalHistoryFlow.step !== 'navigating' || this.clinicalHistoryFlow.transferId !== transferId) return;
    this.clinicalHistoryFlow = { step: 'awaitingConfirmation', dni, patient, prefill };
    this.addBotMessage('No se pudo abrir el formulario de Nueva Historia Clínica. Inténtalo nuevamente.');
  }
  private handleClinicalHistoryFeedback(feedback: ClinicalHistoryFlowFeedback): void {
    if (this.processedFeedbackIds.has(feedback.id)) return;
    this.processedFeedbackIds.add(feedback.id);
    this.stopClinicalHistoryRequest();
    this.resetClinicalHistoryFlow();
    this.addBotMessage(feedback.type === 'prefill-success'
      ? 'Los datos del paciente se autocompletaron correctamente en Nueva Historia Clínica. Revísalos y pulsa Guardar para registrar la historia.'
      : 'No fue posible autocompletar los datos. Puedes completar el formulario manualmente.');
    this.addBotMessage('¿Necesitas ayuda con algo más?');
    this.addMenuBlock('principal');
  }
  private handleClinicalHistoryError(): void {
    this.removeClinicalHistoryLoadingMessage();
    this.clinicalHistoryFlow = { step: 'awaitingDni' };
    this.addBotMessage('No se pudo consultar la información del paciente en este momento. Inténtalo nuevamente.');
  }
  private removeClinicalHistoryLoadingMessage(): void { if (this.messages.at(-1)?.text === 'Consultando paciente...') this.messages.pop(); }
  private stopClinicalHistoryRequest(): void { this.clinicalHistoryRequest?.unsubscribe(); this.clinicalHistoryRequest = undefined; this.isLoading = false; this.removeClinicalHistoryLoadingMessage(); }
  private formatResponse(response: IAsistenteResponse): string { return response.respuesta || 'No pude identificar la consulta.'; }
}
