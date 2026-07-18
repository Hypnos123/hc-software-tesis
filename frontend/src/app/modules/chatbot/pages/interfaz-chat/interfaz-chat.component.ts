import { Component, ElementRef, OnDestroy, QueryList, ViewChild, ViewChildren } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { AuthService } from '@app/auth/services/auth.service';
import { AsistenteService } from '../../services/asistente.service';
import { IAsistenteResponse } from '../../models/asistente';

interface ChatMessage { id: string; text: string; sender: 'user' | 'bot'; }
type MenuAction = 'menu' | 'prompt' | 'request';
interface MenuOption { label: string; description?: string; icon?: string; action: MenuAction; target?: string; text?: string; }
interface ChatMenu { question?: string; options: MenuOption[]; }

@Component({ selector: 'app-interfaz-chat', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './interfaz-chat.component.html', styleUrl: './interfaz-chat.component.scss' })
export class InterfazChatComponent implements OnDestroy {
  @ViewChild('chatBody') chatBody!: ElementRef;
  @ViewChildren('conversationBlock') conversationBlocks!: QueryList<ElementRef<HTMLElement>>;

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
      { label: '¿Cuál es la edad promedio de los pacientes?', action: 'request' }
    ] },
    historias: { question: 'Puedes realizar estas consultas sobre historias clínicas:', options: [
      { label: '¿Cuántas historias clínicas hay registradas?', action: 'request' },
      { label: '¿El paciente con DNI 72845292 tiene historia clínica?', action: 'request' },
      { label: 'Consulta si el paciente ID 4 tiene historia clínica', action: 'request' },
      { label: 'Busca la historia clínica de un paciente por nombre', action: 'prompt', text: 'Escribe el nombre completo del paciente.\nEjemplo: ¿Existe una historia clínica para Rafael Velásquez Morales?' },
      { label: 'Historias clínicas creadas hoy', action: 'request' }
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
      { label: 'Verificar si un paciente tiene historia clínica', action: 'prompt', text: 'Puedes consultar por DNI, ID o nombre completo.\n\nEjemplos:\n- ¿El paciente con DNI 72845292 tiene historia clínica?\n- Consulta si el paciente ID 4 tiene historia clínica.\n- ¿Existe una historia clínica para Rafael Velásquez Morales?' },
      { label: 'Verificar consultas médicas de un paciente', action: 'prompt', text: 'Puedes consultar por DNI, ID o nombre completo.\n\nEjemplos:\n- ¿El paciente con DNI 72845292 tiene consultas médicas?\n- Muéstrame las consultas médicas del paciente ID 4.\n- ¿Cuál fue la última consulta médica de Rafael Velásquez Morales?' },
      { label: 'Detectar posibles pacientes duplicados', action: 'prompt', text: 'Puedes usar estas preguntas:\n- ¿Existen pacientes duplicados?\n- Verifica si hay pacientes repetidos.\n- Analiza posibles duplicados.\n- Revisa la duplicidad de historias clínicas.' }
    ] },
    manejo: { question: 'Selecciona una pregunta sobre el manejo del sistema:', options: [
      { label: '¿Cómo registro un paciente?', action: 'request' }, { label: '¿Cómo edito un paciente?', action: 'request' },
      { label: '¿Cómo creo una historia clínica?', action: 'request' }, { label: '¿Cómo agrego una consulta médica?', action: 'request' },
      { label: '¿Cómo atiendo una consulta médica?', action: 'request' }, { label: '¿Cómo gestiono empleados?', action: 'request' },
      { label: '¿Cómo gestiono usuarios y permisos?', action: 'request' }
    ] },
    ayuda: { question: 'Selecciona una opción de soporte y ayuda:', options: [
      { label: '¿Qué preguntas puedo hacer?', action: 'request' }, { label: 'Mostrar consultas disponibles', action: 'request' },
      { label: 'Cómo usar el asistente', action: 'request' }
    ] }
  };

  private activeRequest?: Subscription;
  private logoutSubscription: Subscription;
  private readonly selectedMenuOptions = new Set<string>();
  private messageSequence = 0;
  private scrollPosition = 0;
  isOpen = false; userMessage = ''; isLoading = false; currentMenu = 'principal';
  messages: ChatMessage[] = this.getInitialMessages();
  quickQuestions = ['Menú principal', '¿Qué preguntas puedo hacer?', 'Buscar paciente por DNI', 'Verificar historia clínica', 'Consultas médicas de un paciente'];

  constructor(private asistenteService: AsistenteService, private authService: AuthService) { this.logoutSubscription = this.authService.logout$.subscribe(() => this.resetChat(true)); }
  getVisibleMenuOptions(): MenuOption[] { return this.menus[this.currentMenu].options.filter(option => option.action === 'menu' || !this.selectedMenuOptions.has(this.menuOptionKey(option))); }
  get allOptionsReviewed(): boolean {
    const options = this.menus[this.currentMenu].options.filter(option => option.action !== 'menu');
    return options.length > 0 && options.every(option => this.selectedMenuOptions.has(this.menuOptionKey(option)));
  }
  ngOnDestroy(): void { this.activeRequest?.unsubscribe(); this.logoutSubscription.unsubscribe(); }
  toggleChat(): void { this.isOpen ? this.minimizeChat() : this.openChat(); }
  openChat(): void { this.isOpen = true; this.restoreScrollPosition(); }
  minimizeChat(): void { this.saveScrollPosition(); this.isOpen = false; }
  closeChat(): void { this.resetChat(true); }
  sendMessage(): void { const pregunta = this.userMessage.trim(); if (!pregunta || this.isLoading) return; this.addUserMessage(pregunta); this.userMessage = ''; this.askBackend(pregunta, true); }
  onEnter(event: Event): void { const keyboardEvent = event as KeyboardEvent; if (keyboardEvent.shiftKey) return; keyboardEvent.preventDefault(); this.sendMessage(); }
  selectOption(option: MenuOption): void {
    if (this.isLoading) return;
    const selection = this.addUserMessage(option.label);
    if (option.action === 'request') { this.selectedMenuOptions.add(this.menuOptionKey(option)); this.askBackend(option.label, false); this.scrollToNewBlock(selection.id); return; }
    if (option.action === 'prompt') { this.selectedMenuOptions.add(this.menuOptionKey(option)); this.addBotMessage(option.text || ''); this.scrollToNewBlock(selection.id); return; }
    this.currentMenu = option.target || 'principal';
    if (this.currentMenu !== 'principal' && this.menus[this.currentMenu].question) this.addBotMessage(this.menus[this.currentMenu].question || '');
    this.scrollToNewBlock(selection.id);
  }
  quickAsk(text: string): void {
    if (text === 'Menú principal') { this.selectOption({ label: text, action: 'menu', target: 'principal' }); return; }
    const quickOption = text === 'Buscar paciente por DNI'
      ? this.menus['pacientes'].options[2]
      : text === 'Verificar historia clínica'
        ? this.menus['verificar'].options[1]
        : text === 'Consultas médicas de un paciente'
          ? this.menus['verificar'].options[2]
          : { label: text, action: 'request' as MenuAction };
    this.selectOption(quickOption);
  }
  scrollToBottom(): void { requestAnimationFrame(() => { if (this.chatBody) this.chatBody.nativeElement.scrollTop = this.chatBody.nativeElement.scrollHeight; }); }
  private scrollToNewBlock(blockId: string): void {
    requestAnimationFrame(() => this.conversationBlocks.find(block => block.nativeElement.dataset['blockId'] === blockId)?.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'start' }));
  }
  private saveScrollPosition(): void { if (this.chatBody) this.scrollPosition = this.chatBody.nativeElement.scrollTop; }
  private restoreScrollPosition(): void { requestAnimationFrame(() => { if (this.chatBody) this.chatBody.nativeElement.scrollTop = this.scrollPosition; }); }
  private addUserMessage(text: string): ChatMessage { const message = this.createMessage('user', text); this.messages.push(message); return message; }
  private addBotMessage(text: string): ChatMessage { const message = this.createMessage('bot', text); this.messages.push(message); return message; }
  private createMessage(sender: ChatMessage['sender'], text: string): ChatMessage { this.messageSequence += 1; return { id: `message-${this.messageSequence}`, sender, text }; }
  private askBackend(pregunta: string, scrollAfterResponse: boolean): void {
    this.isLoading = true; this.addBotMessage('Escribiendo...');
    this.activeRequest = this.asistenteService.preguntar(pregunta).pipe(finalize(() => { this.isLoading = false; this.activeRequest = undefined; })).subscribe({
      next: (response) => { this.removeTypingMessage(); this.addBotMessage(this.formatResponse(response)); if (scrollAfterResponse) this.scrollToBottom(); },
      error: () => { this.removeTypingMessage(); this.addBotMessage('No pude obtener la información en este momento. Inténtalo nuevamente.'); if (scrollAfterResponse) this.scrollToBottom(); }
    });
  }
  private resetChat(clearStorage: boolean): void { this.activeRequest?.unsubscribe(); this.activeRequest = undefined; this.isOpen = false; this.isLoading = false; this.userMessage = ''; this.currentMenu = 'principal'; this.selectedMenuOptions.clear(); this.scrollPosition = 0; this.messages = this.getInitialMessages(); if (clearStorage) this.clearStoredChat(); }
  private removeTypingMessage(): void { if (this.messages[this.messages.length - 1]?.text === 'Escribiendo...') this.messages.pop(); }
  private getInitialMessages(): ChatMessage[] { return [this.createMessage('bot', this.initialMessage)]; }
  private clearStoredChat(): void { localStorage.removeItem('asistenteChatState'); sessionStorage.removeItem('asistenteChatState'); }
  private menuOptionKey(option: MenuOption): string { return `${this.currentMenu}:${option.label}`; }
  private formatResponse(response: IAsistenteResponse): string { return response.respuesta || 'No pude identificar la consulta.'; }
}
