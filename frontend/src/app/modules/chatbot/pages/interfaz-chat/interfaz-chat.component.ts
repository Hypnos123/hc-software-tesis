import { Component, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { AsistenteService } from '../../services/asistente.service';
interface ChatMessage { text: string; sender: 'user' | 'bot'; }
@Component({ selector: 'app-interfaz-chat', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './interfaz-chat.component.html', styleUrl: './interfaz-chat.component.scss' })
export class InterfazChatComponent {
  @ViewChild('chatBody') chatBody!: ElementRef;
  isOpen = false; userMessage = ''; isLoading = false;
  messages: ChatMessage[] = [{ sender: 'bot', text: 'Hola, soy tu asistente virtual. Puedes preguntarme sobre pacientes, historias clínicas, consultas, especialidades o tipos de enfermedad.' }];
  quickQuestions = ['Historias clínicas creadas hoy','Pacientes registrados este mes','Consultas incompletas','Consultas pendientes','Consultas atendidas hoy','Especialidad más requerida','Enfermedad más registrada'];
  constructor(private asistenteService: AsistenteService) {}
  toggleChat(): void { this.isOpen = !this.isOpen; if (this.isOpen) this.scrollToBottom(); }
  sendMessage(): void { const pregunta = this.userMessage.trim(); if (!pregunta || this.isLoading) return; this.messages.push({ sender:'user', text:pregunta }); this.userMessage=''; this.isLoading=true; this.messages.push({ sender:'bot', text:'Escribiendo...' }); this.scrollToBottom(); this.asistenteService.preguntar(pregunta).pipe(finalize(()=>{this.isLoading=false; this.scrollToBottom();})).subscribe({ next:(r)=>{this.messages.pop(); this.messages.push({ sender:'bot', text:r.respuesta || 'No pude identificar la consulta.' });}, error:()=>{this.messages.pop(); this.messages.push({ sender:'bot', text:'No pude obtener la información en este momento. Inténtalo nuevamente.' });} }); }
  onEnter(event: Event): void { const keyboardEvent = event as KeyboardEvent; if (keyboardEvent.shiftKey) return; keyboardEvent.preventDefault(); this.sendMessage(); }
  quickAsk(text: string): void { if (this.isLoading) return; this.userMessage = text; this.sendMessage(); }
  scrollToBottom(): void { setTimeout(() => { if (this.chatBody) this.chatBody.nativeElement.scrollTop = this.chatBody.nativeElement.scrollHeight; }, 50); }
}
