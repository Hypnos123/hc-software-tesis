import { Component, ElementRef, OnDestroy, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { AuthService } from '@app/auth/services/auth.service';
import { AsistenteService } from '../../services/asistente.service';
import { IAsistenteResponse } from '../../models/asistente';
interface ChatMessage { text: string; sender: 'user' | 'bot'; }
@Component({ selector: 'app-interfaz-chat', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './interfaz-chat.component.html', styleUrl: './interfaz-chat.component.scss' })
export class InterfazChatComponent implements OnDestroy {
  @ViewChild('chatBody') chatBody!: ElementRef;
  private readonly initialMessage = 'Hola, soy el Asistente IA del sistema de historias clínicas.\n\nPuedo ayudarte a consultar datos existentes y revisar estadísticas.\n\nEscribe ‘¿Qué preguntas puedo hacer?’ para ver todas las consultas disponibles o elige una opción rápida.';
  private activeRequest?: Subscription;
  private logoutSubscription: Subscription;
  isOpen = false; userMessage = ''; isLoading = false;
  messages: ChatMessage[] = this.getInitialMessages();
  quickQuestions = ['¿Qué preguntas puedo hacer?','Buscar paciente por DNI','Buscar paciente por nombre','Verificar historia clínica','Buscar posibles duplicados','Últimos pacientes registrados','Total de pacientes','Estadísticas por edad'];
  constructor(private asistenteService: AsistenteService, private authService: AuthService) { this.logoutSubscription = this.authService.logout$.subscribe(() => this.resetChat(true)); }
  ngOnDestroy(): void { this.activeRequest?.unsubscribe(); this.logoutSubscription.unsubscribe(); }
  toggleChat(): void { this.isOpen ? this.minimizeChat() : this.openChat(); }
  openChat(): void { this.isOpen = true; this.scrollToBottom(); }
  minimizeChat(): void { this.isOpen = false; }
  closeChat(): void { this.resetChat(true); }
  sendMessage(): void { const pregunta = this.userMessage.trim(); if (!pregunta || this.isLoading) return; this.messages.push({ sender:'user', text:pregunta }); this.userMessage=''; this.isLoading=true; this.messages.push({ sender:'bot', text:'Escribiendo...' }); this.scrollToBottom(); this.activeRequest = this.asistenteService.preguntar(pregunta).pipe(finalize(()=>{this.isLoading=false; this.activeRequest=undefined; this.scrollToBottom();})).subscribe({ next:(r)=>{this.removeTypingMessage(); this.messages.push({ sender:'bot', text:this.formatResponse(r) });}, error:()=>{this.removeTypingMessage(); this.messages.push({ sender:'bot', text:'No pude obtener la información en este momento. Inténtalo nuevamente.' });} }); }
  onEnter(event: Event): void { const keyboardEvent = event as KeyboardEvent; if (keyboardEvent.shiftKey) return; keyboardEvent.preventDefault(); this.sendMessage(); }
  quickAsk(text: string): void { if (this.isLoading) return; const guidance = this.quickQuestionGuidance(text); if (guidance) { this.messages.push({ sender: 'bot', text: guidance }); this.scrollToBottom(); return; } this.userMessage = this.quickQuestionQuery(text); this.sendMessage(); }
  scrollToBottom(): void { setTimeout(() => { if (this.chatBody) this.chatBody.nativeElement.scrollTop = this.chatBody.nativeElement.scrollHeight; }, 50); }
  private resetChat(clearStorage: boolean): void { this.activeRequest?.unsubscribe(); this.activeRequest = undefined; this.isOpen = false; this.isLoading = false; this.userMessage = ''; this.messages = this.getInitialMessages(); if (clearStorage) this.clearStoredChat(); }
  private removeTypingMessage(): void { if (this.messages[this.messages.length - 1]?.text === 'Escribiendo...') this.messages.pop(); }
  private getInitialMessages(): ChatMessage[] { return [{ sender: 'bot', text: this.initialMessage }]; }
  private clearStoredChat(): void { localStorage.removeItem('asistenteChatState'); sessionStorage.removeItem('asistenteChatState'); }
  private quickQuestionGuidance(text: string): string | null { if (text === 'Buscar paciente por DNI') return 'Escribe el DNI de 8 dígitos del paciente.\nEjemplo: Buscar paciente por DNI '; if (text === 'Buscar paciente por nombre') return 'Escribe los nombres y apellidos del paciente.\nEjemplo: Buscar paciente por nombre '; if (text === 'Verificar historia clínica') return 'Puedes consultar por DNI, ID o nombre completo.\n\nEjemplos:\n- ¿El paciente con DNI  tiene historia clínica?\n- Consulta si el paciente ID tiene historia clínica.\n- ¿Existe una historia clínica para ?'; if (text === 'Buscar posibles duplicados') return 'Puedes buscar posibles duplicados con preguntas como:\n- ¿Existen pacientes duplicados?\n- Busca pacientes duplicados.\n- Verifica si hay pacientes repetidos.\n- Analiza posibles duplicados.'; return null; }
  private quickQuestionQuery(text: string): string { if (text === 'Estadísticas por edad') return 'Muéstrame los pacientes por rango de edad.'; return text; }
  private formatResponse(response: IAsistenteResponse): string { return response.respuesta || 'No pude identificar la consulta.'; }
}
