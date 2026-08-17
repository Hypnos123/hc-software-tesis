import { Component, ElementRef, OnDestroy, QueryList, ViewChild, ViewChildren } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin, map, Observable, of, Subscription, switchMap, throwError, timer } from 'rxjs';
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
import { HistoriasClinicasFaltantesChatComponent } from '../../components/historias-clinicas-faltantes-chat/historias-clinicas-faltantes-chat.component';
import { GestionHistoriasDuplicadasChatComponent } from '../../components/gestion-historias-duplicadas-chat/gestion-historias-duplicadas-chat.component';
import {
  crearGestionDuplicadosState,
  GestionDuplicadosChatState,
  GestionDuplicadosEvento,
  GestionDuplicadosVista
} from '../../models/paciente-duplicado-chat';
import {
  crearHistoriasClinicasFaltantesState,
  HistoriasClinicasFaltantesChatState,
  HistoriasClinicasFaltantesEvento,
  HistoriasClinicasFaltantesVista
} from '../../models/historias-clinicas-faltantes-chat';
import {
  crearGestionHistoriasDuplicadasState,
  GestionHistoriasDuplicadasEvento,
  GestionHistoriasDuplicadasState,
  GestionHistoriasDuplicadasVista
} from '../../models/historia-clinica-duplicada-chat';

type ChatPresentationState = 'pending' | 'presenting' | 'visible';
interface ChatMessage { id: string; sender: 'user' | 'bot'; type: 'text' | 'menu' | 'patient-import' | 'duplicate-management' | 'missing-clinical-histories' | 'clinical-history-duplicate-management'; presentationState: ChatPresentationState; visibleText?: string; animateText: boolean; preserveInteractionAnchor?: boolean; afterPresentation?: () => void; text?: string; menuId?: string; options?: MenuOption[]; importacion?: PacienteImportacionChatState; importView?: PacienteImportView; importActive?: boolean; duplicados?: GestionDuplicadosChatState; duplicateView?: GestionDuplicadosVista; duplicateActive?: boolean; historiasFaltantes?: HistoriasClinicasFaltantesChatState; missingHistoriesView?: HistoriasClinicasFaltantesVista; missingHistoriesActive?: boolean; historiasDuplicadas?: GestionHistoriasDuplicadasState; duplicateHistoriesView?: GestionHistoriasDuplicadasVista; duplicateHistoriesActive?: boolean; }
type MenuAction = 'menu' | 'prompt' | 'request' | 'clinical-history-flow' | 'patient-import-flow' | 'patient-duplicate-flow' | 'missing-clinical-histories-flow' | 'clinical-history-duplicate-flow';
interface MenuOption { id?: string; label: string; description?: string; icon?: string; action: MenuAction; target?: string; text?: string; }
interface ChatMenu { question?: string; options: MenuOption[]; }
interface ContextualActionRecommendation {
  message: string;
  option: MenuOption;
}

const VERIFY_CLINICAL_HISTORY_OPTION: MenuOption = {
  label: 'Verificar si un paciente tiene historia clínica',
  action: 'prompt',
  text: 'Escribe el DNI o el nombre y los dos apellidos del paciente.\n\nEjemplos:\n- El paciente con DNI (PONER DNI) tiene historia clínica\n- El paciente (AGREGAR NOMBRE Y DOS APELLIDOS) tiene historia clínica'
};
const VERIFY_PATIENT_CONSULTATIONS_OPTION: MenuOption = {
  label: 'Verificar consultas médicas de un paciente',
  action: 'prompt',
  text: 'Escribe el DNI o el nombre y los dos apellidos del paciente.\n\nEjemplos:\n- El paciente con DNI (PONER DNI) tiene consultas médicas\n- El paciente (AGREGAR NOMBRE Y DOS APELLIDOS) tiene consultas médicas'
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

@Component({ selector: 'app-interfaz-chat', standalone: true, imports: [CommonModule, FormsModule, ImportacionPacientesChatComponent, GestionDuplicadosChatComponent, HistoriasClinicasFaltantesChatComponent, GestionHistoriasDuplicadasChatComponent], templateUrl: './interfaz-chat.component.html', styleUrl: './interfaz-chat.component.scss' })
export class InterfazChatComponent implements OnDestroy {
  @ViewChild('chatBody') chatBody!: ElementRef;
  @ViewChildren('conversationBlock') conversationBlocks!: QueryList<ElementRef<HTMLElement>>;
  @ViewChildren(ImportacionPacientesChatComponent) importacionComponents!: QueryList<ImportacionPacientesChatComponent>;
  @ViewChildren(GestionDuplicadosChatComponent) gestionDuplicadosComponents!: QueryList<GestionDuplicadosChatComponent>;
  @ViewChildren(HistoriasClinicasFaltantesChatComponent) historiasFaltantesComponents!: QueryList<HistoriasClinicasFaltantesChatComponent>;
  @ViewChildren(GestionHistoriasDuplicadasChatComponent) historiasDuplicadasComponents!: QueryList<GestionHistoriasDuplicadasChatComponent>;

  private readonly initialMessage = 'Hola, soy el Asistente IA del sistema de historias clínicas.\n\nPuedo ayudarte a usar el sistema, consultar información registrada, verificar datos y revisar las opciones disponibles.\n\nSelecciona una categoría para continuar o escribe tu pregunta.';
  private readonly menus: Record<string, ChatMenu> = {
    principal: {
      options: [
        { label: 'Manejo del sistema', description: 'Aprende a utilizar las funciones principales.', icon: 'pi pi-cog', action: 'menu', target: 'manejo' },
        { label: 'Consultar información', description: 'Consulta datos registrados en el sistema.', icon: 'pi pi-search', action: 'menu', target: 'consultar' },
        { label: 'Asistencia guiada', description: 'Realiza procesos del sistema con la guía paso a paso del asistente.', icon: 'pi pi-compass', action: 'menu', target: 'asistencia' }
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
      { label: 'Buscar paciente por DNI', action: 'prompt', text: 'Escribe el DNI del paciente.\nEjemplo: Buscar paciente por DNI (PONER DNI)' },
      { label: 'Buscar paciente por nombre', action: 'prompt', text: 'Escribe el nombre y los dos apellidos del paciente.\nEjemplo: Buscar paciente por nombre (AGREGAR NOMBRE Y DOS APELLIDOS)' },
      { label: '¿Cuál es la edad promedio de los pacientes?', action: 'request' },
      { label: 'Detectar posibles pacientes duplicados', action: 'request' }
    ] },
    historias: { question: 'Puedes realizar estas consultas sobre historias clínicas:', options: [
      { label: '¿Cuántas historias clínicas hay registradas?', action: 'request' },
      { label: 'Buscar si un paciente tiene historia clínica', action: 'prompt', text: 'Escribe el DNI o el nombre y los dos apellidos del paciente.\n\nEjemplos:\n- El paciente con DNI (PONER DNI) tiene historia clínica\n- El paciente (AGREGAR NOMBRE Y DOS APELLIDOS) tiene historia clínica' },
      { label: 'Historias clínicas creadas hoy', action: 'request' },
      { label: 'Detectar historias clínicas duplicadas', action: 'request' }
    ] },
    consultas: { question: 'Puedes realizar estas consultas médicas:', options: [
      { label: '¿Cuántas consultas médicas hay registradas?', action: 'request' },
      { label: 'Buscar si un paciente tiene consultas médicas', action: 'prompt', text: 'Escribe el DNI o el nombre y los dos apellidos del paciente.\n\nEjemplos:\n- El paciente con DNI (PONER DNI) tiene consultas médicas\n- El paciente (AGREGAR NOMBRE Y DOS APELLIDOS) tiene consultas médicas' },
      { label: '¿Cuál fue la última consulta médica de un paciente?', action: 'prompt', text: 'Escribe el DNI o el nombre y los dos apellidos del paciente.\n\nEjemplos:\n- ¿Cuál fue la última consulta médica del paciente con DNI (PONER DNI)?\n- ¿Cuál fue la última consulta médica del paciente (AGREGAR NOMBRE Y DOS APELLIDOS)?' },
      { label: '¿Tiene consultas médicas pendientes?', action: 'prompt', text: 'Escribe el DNI o el nombre y los dos apellidos del paciente.\n\nEjemplos:\n- ¿El paciente con DNI (PONER DNI) tiene consultas médicas pendientes?\n- ¿El paciente (AGREGAR NOMBRE Y DOS APELLIDOS) tiene consultas médicas pendientes?' },
      { label: 'Consultas médicas atendidas hoy', action: 'request' }
    ] },
    asistencia: { question: '¿Qué proceso deseas realizar con ayuda del asistente? Selecciona una opción o escribe tu solicitud.', options: [
      { label: 'Pacientes', icon: 'pi pi-users', action: 'menu', target: 'asistencia-pacientes' },
      { label: 'Historias clínicas', icon: 'pi pi-folder-open', action: 'menu', target: 'asistencia-historias' }
    ] },
    'asistencia-pacientes': { question: 'Selecciona una opción o escribe tu solicitud sobre la gestión de pacientes.', options: [
      { label: 'Registrar pacientes desde Excel', description: 'Importa pacientes mediante la plantilla oficial Excel.', icon: 'pi pi-file-excel', action: 'patient-import-flow' },
      { label: 'Gestionar pacientes duplicados', description: 'Compara los registros y archiva de forma segura el duplicado.', icon: 'pi pi-clone', action: 'patient-duplicate-flow' }
    ] },
    'asistencia-historias': { question: 'Selecciona una opción o escribe tu solicitud sobre historias clínicas.', options: [
      { label: 'Crear una historia clínica con el asistente', description: 'Completa una nueva historia usando los datos de un paciente existente.', icon: 'pi pi-file-plus', action: 'clinical-history-flow' },
      { label: 'Crear historias clínicas faltantes', description: 'Selecciona pacientes activos que todavía no tienen historia clínica.', icon: 'pi pi-list-check', action: 'missing-clinical-histories-flow' },
      { label: 'Analizar historias clínicas duplicadas', description: 'Compara historias repetidas y revisa cuál convendría conservar, sin modificar datos.', icon: 'pi pi-clone', action: 'clinical-history-duplicate-flow' }
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
    'duplicados-final': { question: '¿Qué deseas hacer ahora?', options: [
      { label: 'Consultar otro DNI', action: 'patient-duplicate-flow' },
      { label: 'Volver al menú de pacientes', action: 'menu', target: 'asistencia-pacientes' },
      { label: 'Menú principal', action: 'menu', target: 'principal' }
    ] }
  };

  private activeRequest?: Subscription;
  private clinicalHistoryRequest?: Subscription;
  private missingHistoriesRequest?: Subscription;
  private missingHistoriesSearchTimer?: ReturnType<typeof setTimeout>;
  private missingHistoriesSearchReady = false;
  private missingHistoriesSearchState?: HistoriasClinicasFaltantesChatState;
  private pendingMissingHistoriesEvent?: { state: HistoriasClinicasFaltantesChatState; event: HistoriasClinicasFaltantesEvento };
  private logoutSubscription: Subscription;
  private sessionChangedSubscription: Subscription;
  private feedbackSubscription: Subscription;
  private readonly processedFeedbackIds = new Set<string>();
  private messageSequence = 0;
  private readonly presentationQueue: ChatMessage[] = [];
  private activePresentationId?: string;
  private presentationTimer?: ReturnType<typeof setTimeout>;
  private readonly characterPresentationDelay = 20;
  private presentationScrollFrame?: number;
  private autoFollowPresentation = true;
  private readonly autoFollowThreshold = 48;
  private presentationSequenceHadText = false;
  private interactionScrollAnchorId?: string;
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
  clinicalHistoryConfirmationActionsVisible = false;
  messages: ChatMessage[] = this.initializeMessages(this.getInitialMessages());
  clinicalHistoryFlow: ClinicalHistoryChatFlow = { step: 'idle' };
  quickQuestions = ['Menú principal', '¿Qué preguntas puedo hacer?', 'Buscar paciente por DNI', 'Verificar historia clínica', 'Consultas médicas de un paciente'];
  private readonly quickQuestionOptions: Record<string, MenuOption> = {
    'Verificar historia clínica': VERIFY_CLINICAL_HISTORY_OPTION,
    'Consultas médicas de un paciente': VERIFY_PATIENT_CONSULTATIONS_OPTION
  };
  get gestionDuplicadosActiva(): boolean { return this.hayGestionDuplicadosActiva(); }
  get gestionHistoriasDuplicadasActiva(): boolean { return this.hayGestionHistoriasDuplicadasActiva(); }
  get autenticado(): boolean { return !!this.authService.usuario?.idUsuario; }
  get asistenteEscribiendo(): boolean {
    if (this.activePresentationId) {
      const active = this.messages.find(message => message.id === this.activePresentationId);
      if (active && this.isAnimatedBotText(active)) return true;
    }
    for (const message of this.presentationQueue) {
      if (!this.isAnimatedBotText(message)) return false;
      return true;
    }
    return false;
  }

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
  ngOnDestroy(): void { this.clearFloatingMessageTimer(); this.clearMissingHistoriesSearchPresentation(); this.resetPresentationCoordinator(); this.gestionDuplicadosComponents?.forEach(component => component.limpiarFlujo()); this.historiasFaltantesComponents?.forEach(component => component.limpiarFlujo()); this.historiasDuplicadasComponents?.forEach(component => component.limpiarFlujo()); this.activeRequest?.unsubscribe(); this.clinicalHistoryRequest?.unsubscribe(); this.missingHistoriesRequest?.unsubscribe(); this.logoutSubscription.unsubscribe(); this.sessionChangedSubscription.unsubscribe(); this.feedbackSubscription.unsubscribe(); }
  toggleChat(): void { this.isOpen ? this.minimizeChat() : this.openChat(); }
  openChat(): void { this.clearFloatingMessageTimer(); this.hideFloatingMessage(); this.isOpen = true; this.restoreScrollPosition(); }
  minimizeChat(): void { this.gestionDuplicadosComponents?.forEach(component => component.limpiarPassword()); this.cancelarHistoriasDuplicadasSilenciosamente(); this.saveScrollPosition(); this.autoFollowPresentation = false; this.isOpen = false; this.scheduleFloatingMessage(90_000); }
  closeChat(): void {
    this.importacionComponents?.forEach(component => component.limpiarFlujo());
    this.gestionDuplicadosComponents?.forEach(component => component.limpiarFlujo());
    this.historiasFaltantesComponents?.forEach(component => component.limpiarFlujo());
    this.historiasDuplicadasComponents?.forEach(component => component.limpiarFlujo());
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
    if (this.clinicalHistoryFlow.step !== 'idle' || this.hayGestionDuplicadosActiva() || this.hayGestionHistoriasDuplicadasActiva() || !pregunta) return;
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
    if (text === 'Menú principal') { this.cancelarGestionDuplicadosSilenciosamente(); this.cancelarHistoriasDuplicadasSilenciosamente(); this.stopClinicalHistoryRequest(); this.resetClinicalHistoryFlow(); const selection = this.addUserMessage(text); this.addMenuBlock('principal'); this.scrollToNewBlock(selection.id); return; }
    const quickOption = text === 'Buscar paciente por DNI'
      ? this.menus['pacientes'].options[2]
      : this.quickQuestionOptions[text] ?? { label: text, action: 'request' as MenuAction };
    const selection = this.addUserMessage(quickOption.label);
    this.executeMenuOption(quickOption, selection.id);
  }
  mostrarOpcionesPacientes(): void {
    const selection = this.addUserMessage('Volver a opciones de Pacientes');
    this.addMenuBlock('asistencia-pacientes');
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
    const esResultadoConTarjetas = evento.remitente === 'bot' && evento.vistaSiguiente === 'results';
    const mostrarVistaSiguiente = (): void => {
      if (vistaActualizada && tarjetaActiva) this.messages.push(tarjetaActiva);
      if (evento.vistaSiguiente && !vistaActualizada) {
        this.addDuplicateBlock(state, evento.vistaSiguiente, !['COMPLETADO', 'CANCELADO'].includes(state.estado));
      }
    };
    if (esResultadoConTarjetas) {
      const orientacion = this.addBotMessage('Revisa los pacientes encontrados. El registro recomendado para conservar aparecerá destacado. Si deseas continuar, selecciona “Archivar paciente” en el registro duplicado que deseas consolidar. No se realizará ningún cambio hasta que completes las confirmaciones posteriores.');
      this.runAfterPresentation(orientacion, mostrarVistaSiguiente);
    } else {
      mostrarVistaSiguiente();
    }
    if (state.estado === 'COMPLETADO') this.addMenuBlock('duplicados-final');
    if (evento.volverPacientes) this.addMenuBlock('asistencia-pacientes');
    if (evento.inicioGrupo && evento.remitente === 'user') this.pinInteractionStart(mensaje.id);
  }
  manejarMensajeHistoriasFaltantes(state: HistoriasClinicasFaltantesChatState, evento: HistoriasClinicasFaltantesEvento): void {
    if (this.shouldWaitForMissingHistoriesSearch(state, evento)) {
      this.pendingMissingHistoriesEvent = { state, event: evento };
      return;
    }
    if (this.missingHistoriesSearchState === state && this.missingHistoriesSearchReady
        && evento.remitente === 'bot' && evento.vistaSiguiente) {
      this.clearMissingHistoriesSearchPresentation();
    }
    const tarjetaActiva = this.messages.find(message => message.type === 'missing-clinical-histories'
      && message.historiasFaltantes === state && message.missingHistoriesActive);
    let vistaActualizada = false;
    if (evento.reemplazarVistaActiva && evento.vistaSiguiente && tarjetaActiva) {
      const posicionTarjeta = this.messages.indexOf(tarjetaActiva);
      if (posicionTarjeta >= 0) this.messages.splice(posicionTarjeta, 1);
      tarjetaActiva.missingHistoriesView = evento.vistaSiguiente;
      vistaActualizada = true;
    } else if (tarjetaActiva && (evento.vistaSiguiente || evento.accionPosterior || state.estado === 'CANCELADO')) {
      tarjetaActiva.missingHistoriesActive = false;
    }
    const mensaje = evento.remitente === 'user' ? this.addUserMessage(evento.texto) : this.addBotMessage(evento.texto);
    if (evento.accionPosterior) {
      this.procesarAccionPosteriorHistoriasFaltantes(evento.accionPosterior);
      if (evento.inicioGrupo) this.scrollToNewBlock(mensaje.id);
      return;
    }
    const showNextView = (): void => {
      if (vistaActualizada && tarjetaActiva) this.messages.push(tarjetaActiva);
      if (evento.vistaSiguiente && !vistaActualizada) this.addMissingHistoriesBlock(state, evento.vistaSiguiente, state.estado !== 'CANCELADO');
    };
    if (evento.remitente === 'bot' && evento.vistaSiguiente) this.runAfterPresentation(mensaje, showNextView);
    else showNextView();
    if (evento.volverHistorias) this.addMenuBlock('asistencia-historias');
    if (evento.inicioGrupo && evento.remitente === 'user') this.pinInteractionStart(mensaje.id);
    if (evento.ejecutarCreacion) this.ejecutarCreacionHistoriasFaltantes(state);
  }

  private shouldWaitForMissingHistoriesSearch(state: HistoriasClinicasFaltantesChatState,
      event: HistoriasClinicasFaltantesEvento): boolean {
    return this.missingHistoriesSearchState === state && !this.missingHistoriesSearchReady
      && event.remitente === 'bot' && !!event.vistaSiguiente;
  }

  private startMissingHistoriesSearchPresentation(state: HistoriasClinicasFaltantesChatState): void {
    this.clearMissingHistoriesSearchPresentation();
    this.missingHistoriesSearchState = state;
    this.missingHistoriesSearchReady = false;
    this.missingHistoriesSearchTimer = setTimeout(() => {
      this.missingHistoriesSearchTimer = undefined;
      this.missingHistoriesSearchReady = true;
      const pending = this.pendingMissingHistoriesEvent;
      this.pendingMissingHistoriesEvent = undefined;
      if (pending) this.manejarMensajeHistoriasFaltantes(pending.state, pending.event);
    }, this.randomPresentationDuration());
  }

  private clearMissingHistoriesSearchPresentation(): void {
    if (this.missingHistoriesSearchTimer !== undefined) clearTimeout(this.missingHistoriesSearchTimer);
    this.missingHistoriesSearchTimer = undefined;
    this.missingHistoriesSearchReady = false;
    this.missingHistoriesSearchState = undefined;
    this.pendingMissingHistoriesEvent = undefined;
  }

  private randomPresentationDuration(): number { return 3_000 + Math.floor(Math.random() * 3_001); }

  manejarMensajeHistoriasDuplicadas(state: GestionHistoriasDuplicadasState, evento: GestionHistoriasDuplicadasEvento): void {
    const tarjetaActiva = this.messages.find(message => message.type === 'clinical-history-duplicate-management'
      && message.historiasDuplicadas === state && message.duplicateHistoriesActive);
    if (evento.reemplazarVistaActiva && tarjetaActiva) {
      this.removeMessageFromPresentation(tarjetaActiva);
      this.messages = this.messages.filter(message => message !== tarjetaActiva);
    } else if (tarjetaActiva && (evento.vistaSiguiente || ['COMPLETADO', 'CANCELADO', 'ERROR'].includes(state.estado))) {
      tarjetaActiva.duplicateHistoriesActive = false;
    }
    const mensaje = evento.remitente === 'user' ? this.addUserMessage(evento.texto) : this.addBotMessage(evento.texto);
    if (evento.vistaSiguiente) this.addDuplicateHistoriesBlock(state, evento.vistaSiguiente,
      !['COMPLETADO', 'CANCELADO', 'ERROR'].includes(state.estado));
    if (evento.volverHistorias) this.addMenuBlock('historias');
    if (evento.inicioGrupo) this.pinInteractionStart(mensaje.id);
  }

  private ejecutarCreacionHistoriasFaltantes(state: HistoriasClinicasFaltantesChatState): void {
    if (this.missingHistoriesRequest || state.estado !== 'CREANDO' || state.idsConfirmados.length === 0) return;
    const idsConfirmados = [...state.idsConfirmados];
    const solicitud = forkJoin({
      resultado: this.historiaClinicaService.crearHistoriasClinicasFaltantes(idsConfirmados),
      presentacionMinima: timer(this.randomPresentationDuration())
    })
      .pipe(finalize(() => { this.missingHistoriesRequest = undefined; }))
      .subscribe({
        next: ({ resultado }) => {
          state.resultado = resultado;
          state.estado = 'COMPLETADO';
          this.mostrarFinHistoriasFaltantes(state, 'result');
        },
        error: () => {
          state.estado = 'ERROR_CREACION';
          state.mensajeError = 'No se pudo completar la creación de historias clínicas.';
          this.mostrarFinHistoriasFaltantes(state, 'creation-error');
        }
      });
    this.missingHistoriesRequest = solicitud.closed ? undefined : solicitud;
  }

  private mostrarFinHistoriasFaltantes(state: HistoriasClinicasFaltantesChatState,
      vista: HistoriasClinicasFaltantesVista): void {
    const tarjetaProcesando = this.messages.find(message => message.type === 'missing-clinical-histories'
      && message.historiasFaltantes === state && message.missingHistoriesActive);
    if (tarjetaProcesando) {
      this.removeMessageFromPresentation(tarjetaProcesando);
      this.messages = this.messages.filter(message => message !== tarjetaProcesando);
    }
    const mensaje = this.addBotMessage(vista === 'result' ? '✓ Procesamiento completado' : state.mensajeError ?? 'No se pudo completar el proceso.');
    this.runAfterPresentation(mensaje, () => this.addMissingHistoriesBlock(state, vista, true));
  }

  private procesarAccionPosteriorHistoriasFaltantes(accion: 'REVISAR' | 'HISTORIAS' | 'PRINCIPAL'): void {
    if (accion === 'REVISAR') {
      const state = crearHistoriasClinicasFaltantesState();
      const message = this.addBotMessage('Consultaré nuevamente los pacientes activos que todavía no tienen historia clínica.');
      this.runAfterPresentation(message, () => {
        this.startMissingHistoriesSearchPresentation(state);
        this.addMissingHistoriesBlock(state, 'loading', true);
      });
      return;
    }
    this.addMenuBlock(accion === 'HISTORIAS' ? 'asistencia-historias' : 'principal');
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
    if (this.clinicalHistoryFlow.step !== 'awaitingConfirmation' || !this.clinicalHistoryConfirmationActionsVisible) return;
    const { dni, patient, prefill } = this.clinicalHistoryFlow;
    const transferId = this.clinicalHistoryTransferService.createTransfer(this.toTransferCandidate(prefill));
    this.clinicalHistoryFlow = { step: 'navigating', dni, patient, prefill, transferId };
    this.clinicalHistoryConfirmationActionsVisible = false;
    this.isLoading = true;
    const selection = this.addUserMessage('Continuar');
    this.pinInteractionStart(selection.id);
    const transitionMessage = this.addBotMessage('Abriré Nueva Historia Clínica con los datos del paciente seleccionado.');
    this.runAfterPresentation(transitionMessage, () => this.navigateToClinicalHistory(dni, patient, prefill, transferId));
  }
  private navigateToClinicalHistory(dni: string, patient: PatientClinicalHistorySummary,
      prefill: ClinicalHistoryCandidateData, transferId: string): void {
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
  onChatBodyScroll(): void {
    if (!this.chatBody) return;
    const body = this.chatBody.nativeElement;
    this.autoFollowPresentation = body.scrollHeight - body.scrollTop - body.clientHeight <= this.autoFollowThreshold;
  }
  scrollToBottom(): void { this.autoFollowPresentation = true; requestAnimationFrame(() => { if (this.chatBody) this.chatBody.nativeElement.scrollTop = this.chatBody.nativeElement.scrollHeight; }); }
  private scrollToNewBlock(blockId: string): void {
    this.autoFollowPresentation = true;
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
    this.resetPresentationCoordinator();
    this.messages = this.initializeMessages(this.getInitialMessages());
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
  private addUserMessage(text: string): ChatMessage { return this.addMessage(this.createTextMessage('user', text)); }
  private addBotMessage(text: string): ChatMessage { return this.addMessage(this.createTextMessage('bot', text)); }
  private addMenuBlock(menuId: string, waitForQuestion = false): void {
    const menu = this.menus[menuId];
    const addOptions = (): void => {
      this.addMessage(this.createBlockMessage('menu', { menuId, options: this.createMenuOptions(menuId) }));
    };
    if (menuId !== 'principal' && menu.question) {
      const question = this.addBotMessage(menu.question);
      if (waitForQuestion) {
        this.runAfterPresentation(question, addOptions);
        return;
      }
    }
    addOptions();
  }
  private addContextualAction(recommendation: ContextualActionRecommendation): void {
    const helpMessage = this.addBotMessage(recommendation.message);
    this.runAfterPresentation(helpMessage, () => {
      this.addMessage(this.createBlockMessage('menu', {
        menuId: 'contextual-action',
        preserveInteractionAnchor: true,
        options: [{ ...recommendation.option, id: `contextual-${this.messageSequence + 1}` }]
      }));
    });
  }
  private addImportBlock(state: PacienteImportacionChatState, view: PacienteImportView, active: boolean): void {
    this.addMessage(this.createBlockMessage('patient-import', { importacion: state, importView: view, importActive: active }));
  }
  private addDuplicateBlock(state: GestionDuplicadosChatState, view: GestionDuplicadosVista, active: boolean): void {
    this.addMessage(this.createBlockMessage('duplicate-management', { duplicados: { ...state }, duplicateView: view, duplicateActive: active }));
  }
  private addMissingHistoriesBlock(state: HistoriasClinicasFaltantesChatState, view: HistoriasClinicasFaltantesVista, active: boolean): void {
    this.addMessage(this.createBlockMessage('missing-clinical-histories', { historiasFaltantes: state, missingHistoriesView: view, missingHistoriesActive: active }));
  }
  private addDuplicateHistoriesBlock(state: GestionHistoriasDuplicadasState, view: GestionHistoriasDuplicadasVista, active: boolean): void {
    this.addMessage(this.createBlockMessage('clinical-history-duplicate-management', {
      historiasDuplicadas: state,
      duplicateHistoriesView: view,
      duplicateHistoriesActive: active,
      preserveInteractionAnchor: view === 'loading' || view === 'analyzing'
    }));
  }
  private addMessage(message: ChatMessage): ChatMessage {
    this.messages.push(message);
    this.enqueueForPresentation(message);
    return message;
  }
  private initializeMessages(messages: ChatMessage[]): ChatMessage[] {
    messages.forEach(message => this.revealMessageImmediately(message));
    return messages;
  }
  private enqueueForPresentation(message: ChatMessage): void {
    if (message.sender === 'user') {
      this.revealMessageImmediately(message);
      return;
    }
    this.presentationQueue.push(message);
    this.processPresentationQueue();
  }
  private processPresentationQueue(): void {
    if (this.activePresentationId) return;
    const message = this.presentationQueue.shift();
    if (!message) return;
    if (!this.messages.includes(message)) {
      this.processPresentationQueue();
      return;
    }
    if (!this.isAnimatedBotText(message)) {
      this.revealMessageImmediately(message);
      if (this.interactionScrollAnchorId) {
        if (!message.preserveInteractionAnchor) {
          this.interactionScrollAnchorId = undefined;
          this.autoFollowPresentation = false;
        }
      } else if (this.presentationSequenceHadText) {
        this.focusPresentedBlock(message.id);
      }
      this.presentationSequenceHadText = false;
      this.processPresentationQueue();
      return;
    }
    this.activePresentationId = message.id;
    this.presentationSequenceHadText = true;
    message.presentationState = 'presenting';
    message.visibleText = '';
    this.followActivePresentation();
    this.revealNextCharacter(message, Array.from(message.text ?? ''), 0);
  }
  private revealNextCharacter(message: ChatMessage, characters: string[], index: number): void {
    if (this.activePresentationId !== message.id) return;
    if (index >= characters.length) {
      this.completeMessagePresentation(message);
      return;
    }
    this.presentationTimer = setTimeout(() => {
      this.presentationTimer = undefined;
      if (this.activePresentationId !== message.id) return;
      message.visibleText = `${message.visibleText ?? ''}${characters[index]}`;
      this.followActivePresentation();
      this.revealNextCharacter(message, characters, index + 1);
    }, this.characterPresentationDelay);
  }
  private completeMessagePresentation(message: ChatMessage): void {
    message.visibleText = message.text ?? '';
    message.presentationState = 'visible';
    this.activePresentationId = undefined;
    const afterPresentation = message.afterPresentation;
    message.afterPresentation = undefined;
    afterPresentation?.();
    this.processPresentationQueue();
  }
  private runAfterPresentation(message: ChatMessage, callback: () => void): void {
    if (message.presentationState === 'visible') {
      callback();
      return;
    }
    message.afterPresentation = callback;
  }
  private revealMessageImmediately(message: ChatMessage): void {
    if (message.type === 'text') message.visibleText = message.text ?? '';
    message.presentationState = 'visible';
  }
  private followActivePresentation(): void {
    if (!this.isOpen || this.interactionScrollAnchorId || !this.autoFollowPresentation || this.presentationScrollFrame !== undefined) return;
    this.presentationScrollFrame = requestAnimationFrame(() => {
      this.presentationScrollFrame = undefined;
      if (!this.chatBody || !this.isOpen || !this.autoFollowPresentation) return;
      const body = this.chatBody.nativeElement;
      body.scrollTop = Math.max(body.scrollTop, body.scrollHeight - body.clientHeight);
    });
  }
  private focusPresentedBlock(blockId: string): void {
    if (!this.isOpen) return;
    this.scrollToNewBlock(blockId);
  }
  private pinInteractionStart(blockId: string): void {
    this.interactionScrollAnchorId = blockId;
    this.scrollToNewBlock(blockId);
    this.autoFollowPresentation = false;
  }
  private resetPresentationCoordinator(): void {
    if (this.presentationTimer !== undefined) clearTimeout(this.presentationTimer);
    this.presentationTimer = undefined;
    if (this.presentationScrollFrame !== undefined) cancelAnimationFrame(this.presentationScrollFrame);
    this.presentationScrollFrame = undefined;
    this.presentationQueue.length = 0;
    this.activePresentationId = undefined;
    this.presentationSequenceHadText = false;
    this.interactionScrollAnchorId = undefined;
    this.autoFollowPresentation = true;
  }
  private removeMessageFromPresentation(message: ChatMessage): void {
    const queuedIndex = this.presentationQueue.indexOf(message);
    if (queuedIndex >= 0) this.presentationQueue.splice(queuedIndex, 1);
    if (this.activePresentationId !== message.id) return;
    if (this.presentationTimer !== undefined) clearTimeout(this.presentationTimer);
    this.presentationTimer = undefined;
    this.activePresentationId = undefined;
    this.processPresentationQueue();
  }
  private createTextMessage(sender: ChatMessage['sender'], text: string): ChatMessage {
    return { id: this.nextMessageId(), sender, type: 'text', text, visibleText: '', presentationState: 'pending', animateText: this.shouldAnimateText(sender, 'text') };
  }
  private createBlockMessage(type: Exclude<ChatMessage['type'], 'text'>, data: Partial<ChatMessage>): ChatMessage {
    return { ...data, id: this.nextMessageId(), sender: 'bot', type, presentationState: 'pending', animateText: false };
  }
  private shouldAnimateText(sender: ChatMessage['sender'], type: ChatMessage['type']): boolean {
    return sender === 'bot' && type === 'text';
  }
  private isAnimatedBotText(message: ChatMessage): boolean {
    return message.sender === 'bot' && message.type === 'text' && message.animateText;
  }
  private nextMessageId(): string { this.messageSequence += 1; return `message-${this.messageSequence}`; }
  private askBackend(pregunta: string, scrollAfterResponse: boolean): void {
    this.isLoading = true; this.addBotMessage('Escribiendo...');
    this.activeRequest = this.asistenteService.preguntar(pregunta).pipe(finalize(() => { this.isLoading = false; this.activeRequest = undefined; })).subscribe({
      next: (response) => {
        this.removeTypingMessage();
        const resultado = this.addBotMessage(this.formatResponse(response));
        const recommendation = this.getContextualAction(response);
        if (recommendation) this.addContextualAction(recommendation);
        if (this.esResultadoDuplicadoExtenso(response) || recommendation) {
          this.pinInteractionStart(resultado.id);
        } else if (scrollAfterResponse) {
          this.scrollToBottom();
        }
      },
      error: () => { this.removeTypingMessage(); this.addBotMessage('No pude obtener la información en este momento. Inténtalo nuevamente.'); if (scrollAfterResponse) this.scrollToBottom(); }
    });
  }
  private getContextualAction(response: IAsistenteResponse): ContextualActionRecommendation | undefined {
    if (response.intencion === 'ANALISIS_DUPLICADOS_PACIENTES'
        || response.intencion === 'BUSQUEDA_DUPLICADO_DNI_MULTIPLE') {
      const resultados = response.datos?.['resultados'];
      const cantidad = Number(response.datos?.['cantidad'] ?? (Array.isArray(resultados) ? resultados.length : 0));
      if (cantidad < 2 && (!Array.isArray(resultados) || resultados.length < 2)) return undefined;
      return {
        message: 'Si deseas, puedo ayudarte a revisar estos registros y determinar cuál conviene conservar y archivar.',
        option: {
          label: 'Gestionar pacientes duplicados',
          description: 'Compara los registros y archiva de forma segura el duplicado.',
          icon: 'pi pi-clone',
          action: 'patient-duplicate-flow'
        }
      };
    }
    if (response.intencion === 'HISTORIAS_CLINICAS_DUPLICADAS' && response.datos?.['hayDuplicados'] === true) {
      return {
        message: 'Puedo analizar estas historias para determinar cuál contiene mayor información clínica y recomendar cuál conservar.',
        option: {
          label: 'Analizar historias clínicas duplicadas',
          description: 'Compara las historias repetidas y revisa cuál conviene conservar.',
          icon: 'pi pi-clone',
          action: 'clinical-history-duplicate-flow'
        }
      };
    }
    return undefined;
  }
  private esResultadoDuplicadoExtenso(response: IAsistenteResponse): boolean {
    if (response.intencion === 'ANALISIS_DUPLICADOS_PACIENTES' || response.intencion === 'BUSQUEDA_DUPLICADO_DNI_MULTIPLE') return true;
    if (response.intencion !== 'HISTORIAS_CLINICAS_DUPLICADAS') return false;
    return response.datos?.['hayDuplicados'] === true;
  }
  private resetChat(clearStorage: boolean): void { this.clearFloatingMessageTimer(); this.clearMissingHistoriesSearchPresentation(); this.hideFloatingMessage(); this.messages.forEach(message => { message.importacion?.cancelarSolicitud?.(); message.duplicados?.cancelarSolicitud?.(); message.historiasFaltantes?.cancelarSolicitud?.(); message.historiasDuplicadas?.cancelarSolicitud?.(); if (message.historiasFaltantes) { message.historiasFaltantes.idsSeleccionados = []; message.historiasFaltantes.idsConfirmados = []; } if (message.historiasDuplicadas) message.historiasDuplicadas.idsSeleccionados = []; }); this.cancelarGestionDuplicadosSilenciosamente(); this.cancelarHistoriasDuplicadasSilenciosamente(); this.activeRequest?.unsubscribe(); this.activeRequest = undefined; this.missingHistoriesRequest?.unsubscribe(); this.missingHistoriesRequest = undefined; this.stopClinicalHistoryRequest(); this.isOpen = false; this.isLoading = false; this.userMessage = ''; this.scrollPosition = 0; this.resetClinicalHistoryFlow(); this.resetPresentationCoordinator(); this.messages = this.initializeMessages(this.getInitialMessages()); if (clearStorage) this.clearStoredChat(); }
  private removeTypingMessage(): void {
    const message = this.messages[this.messages.length - 1];
    if (message?.text !== 'Escribiendo...') return;
    this.messages.pop();
    this.removeMessageFromPresentation(message);
  }
  private getInitialMessages(): ChatMessage[] {
    if (!this.autenticado) {
      return [this.createTextMessage('bot', 'Hola, soy el Asistente IA del sistema.\nPara realizar consultas, verificar datos o ayudarte con los procesos, primero debes iniciar sesión.')];
    }
    const welcome = this.createTextMessage('bot', this.initialMessage);
    return [welcome, this.createBlockMessage('menu', { menuId: 'principal', options: this.createMenuOptions('principal') })];
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
      this.clinicalHistoryConfirmationActionsVisible = false;
      this.addBotMessage('Ingresa el DNI de ocho dígitos del paciente que deseas utilizar para crear la historia clínica. Puedes cancelar la asistencia en cualquier momento pulsando “Cancelar”.');
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
    if (option.action === 'missing-clinical-histories-flow') {
      if (this.messages.some(message => message.type === 'missing-clinical-histories' && message.missingHistoriesActive)) {
        this.addBotMessage('Ya existe una selección de historias clínicas faltantes activa. Complétala o cancélala antes de iniciar otra.');
        this.scrollToNewBlock(selectionId);
        return;
      }
      const state = crearHistoriasClinicasFaltantesState();
      const message = this.addBotMessage('Consultaré los pacientes activos que todavía no tienen historia clínica. Nada se creará sin una confirmación explícita.');
      this.runAfterPresentation(message, () => {
        this.startMissingHistoriesSearchPresentation(state);
        this.addMissingHistoriesBlock(state, 'loading', true);
      });
      this.pinInteractionStart(selectionId);
      return;
    }
    if (option.action === 'clinical-history-duplicate-flow') {
      if (!this.puedeGestionarDuplicados()) {
        this.addBotMessage('La gestión de registros duplicados está disponible únicamente para personal autorizado.');
        this.scrollToNewBlock(selectionId);
        return;
      }
      if (this.hayGestionHistoriasDuplicadasActiva() || this.hayGestionDuplicadosActiva() || this.clinicalHistoryFlow.step !== 'idle') {
        this.addBotMessage('Ya existe una operación guiada activa. Complétala o cancélala antes de iniciar otra.');
        this.scrollToNewBlock(selectionId);
        return;
      }
      const inicio = this.addBotMessage('Consultaré las historias clínicas duplicadas disponibles. El análisis será únicamente informativo y no modificará ningún registro.');
      this.addDuplicateHistoriesBlock(crearGestionHistoriasDuplicadasState(), 'loading', true);
      this.pinInteractionStart(inicio.id);
      return;
    }
    const target = option.target || 'principal';
    const waitForQuestion = target === 'asistencia-historias';
    this.addMenuBlock(target, waitForQuestion);
    if (waitForQuestion) this.pinInteractionStart(selectionId);
    else this.scrollToNewBlock(selectionId);
  }
  private resetClinicalHistoryFlow(): void { this.clinicalHistoryFlow = { step: 'idle' }; this.clinicalHistoryConfirmationActionsVisible = false; }
  private iniciarGestionDuplicados(selectionId?: string): void {
    if (!this.puedeGestionarDuplicados()) {
      this.addBotMessage('La gestión de registros duplicados está disponible únicamente para personal autorizado.');
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
    this.addBotMessage('Comencemos revisando un grupo específico de pacientes duplicados.');
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
  private hayGestionHistoriasDuplicadasActiva(): boolean {
    return this.messages.some(message => message.type === 'clinical-history-duplicate-management' && message.duplicateHistoriesActive);
  }
  private cancelarHistoriasDuplicadasSilenciosamente(): void {
    this.historiasDuplicadasComponents?.forEach(component => { if (component.active) component.limpiarFlujo(); });
    this.messages.forEach(message => {
      if (message.type === 'clinical-history-duplicate-management') message.duplicateHistoriesActive = false;
    });
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
    if (messageId) this.pinInteractionStart(messageId);
    this.clinicalHistoryConfirmationActionsVisible = false;
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
      this.addBotMessage('No se encontró un paciente registrado con el DNI indicado. Verifica el número e inténtalo nuevamente con el DNI de un paciente existente.');
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
    this.clinicalHistoryConfirmationActionsVisible = false;
    this.addBotMessage(this.formatPatientSummary(summary));
    const orientation = this.addBotMessage('Revisa los datos del paciente encontrado. Si corresponde al paciente que deseas utilizar, pulsa “Continuar”. Si deseas salir de este proceso, pulsa “Cancelar”.');
    this.runAfterPresentation(orientation, () => {
      if (this.clinicalHistoryFlow.step === 'awaitingConfirmation' && this.clinicalHistoryFlow.dni === dni) {
        this.clinicalHistoryConfirmationActionsVisible = true;
      }
    });
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
    return `Paciente encontrado:\n\nNombre: ${patient.nombreCompleto}\nDNI: ${patient.dni}\nFecha de nacimiento: ${this.formatDate(patient.fechaNacimiento)}\nEstado civil: ${this.formatCivilStatus(patient.estadoCivil)}\nHistorias clínicas existentes: ${patient.existingClinicalHistoryCount}`;
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
    const errorMessage = this.addBotMessage('No se pudo abrir el formulario de Nueva Historia Clínica. Inténtalo nuevamente.');
    this.runAfterPresentation(errorMessage, () => {
      if (this.clinicalHistoryFlow.step === 'awaitingConfirmation' && this.clinicalHistoryFlow.dni === dni) {
        this.clinicalHistoryConfirmationActionsVisible = true;
      }
    });
  }
  private handleClinicalHistoryFeedback(feedback: ClinicalHistoryFlowFeedback): void {
    if (this.processedFeedbackIds.has(feedback.id)) return;
    this.processedFeedbackIds.add(feedback.id);
    this.stopClinicalHistoryRequest();
    this.resetClinicalHistoryFlow();
    const resultMessage = this.addBotMessage(feedback.type === 'prefill-success'
      ? 'Los datos del paciente se autocompletaron correctamente en Nueva Historia Clínica. Revísalos y pulsa Guardar para registrar la historia.'
      : 'No fue posible autocompletar los datos. Puedes completar el formulario manualmente.');
    this.runAfterPresentation(resultMessage, () => {
      const nextHelp = this.addBotMessage('¿Necesitas ayuda con algo más?');
      this.runAfterPresentation(nextHelp, () => this.addMenuBlock('principal'));
    });
  }
  private handleClinicalHistoryError(): void {
    this.removeClinicalHistoryLoadingMessage();
    this.clinicalHistoryFlow = { step: 'awaitingDni' };
    this.addBotMessage('No se pudo consultar la información del paciente en este momento. Inténtalo nuevamente.');
  }
  private removeClinicalHistoryLoadingMessage(): void {
    const message = this.messages.at(-1);
    if (message?.text !== 'Consultando paciente...') return;
    this.messages.pop();
    this.removeMessageFromPresentation(message);
  }
  private stopClinicalHistoryRequest(): void { this.clinicalHistoryRequest?.unsubscribe(); this.clinicalHistoryRequest = undefined; this.isLoading = false; this.removeClinicalHistoryLoadingMessage(); }
  private formatResponse(response: IAsistenteResponse): string { return response.respuesta || 'No pude identificar la consulta.'; }
}
