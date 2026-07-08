import { Component, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { AsistenteService } from '../../services/asistente.service';
import { IAsistenteResponse } from '../../models/asistente';
interface ChatMessage { text: string; sender: 'user' | 'bot'; }
interface PacienteDuplicado { nombres?: string; apellidos?: string; numDocumento?: string; }
@Component({ selector: 'app-interfaz-chat', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './interfaz-chat.component.html', styleUrl: './interfaz-chat.component.scss' })
export class InterfazChatComponent {
  @ViewChild('chatBody') chatBody!: ElementRef;
  isOpen = false; userMessage = ''; isLoading = false;
  messages: ChatMessage[] = [{ sender: 'bot', text: 'Hola, soy tu asistente virtual. Puedes preguntarme sobre pacientes, historias clínicas, consultas, especialidades o tipos de enfermedad. También puedo ayudarte a verificar si un paciente ya está registrado por DNI o nombre completo antes de crear una historia clínica.' }];
  quickQuestions = ['Busca si existe el paciente con DNI 45678912','Verifica si Juan Carlos Pérez López ya está registrado','Historias clínicas creadas hoy','Pacientes registrados este mes','Consultas incompletas','Consultas pendientes','Consultas atendidas hoy','Especialidad más requerida','Enfermedad más registrada'];
  constructor(private asistenteService: AsistenteService) {}
  toggleChat(): void { this.isOpen = !this.isOpen; if (this.isOpen) this.scrollToBottom(); }
  sendMessage(): void { const pregunta = this.userMessage.trim(); if (!pregunta || this.isLoading) return; this.messages.push({ sender:'user', text:pregunta }); this.userMessage=''; this.isLoading=true; this.messages.push({ sender:'bot', text:'Escribiendo...' }); this.scrollToBottom(); this.asistenteService.preguntar(pregunta).pipe(finalize(()=>{this.isLoading=false; this.scrollToBottom();})).subscribe({ next:(r)=>{this.messages.pop(); this.messages.push({ sender:'bot', text:this.formatResponse(r) });}, error:()=>{this.messages.pop(); this.messages.push({ sender:'bot', text:'No pude obtener la información en este momento. Inténtalo nuevamente.' });} }); }
  onEnter(event: Event): void { const keyboardEvent = event as KeyboardEvent; if (keyboardEvent.shiftKey) return; keyboardEvent.preventDefault(); this.sendMessage(); }
  quickAsk(text: string): void { if (this.isLoading) return; this.userMessage = text; this.sendMessage(); }
  scrollToBottom(): void { setTimeout(() => { if (this.chatBody) this.chatBody.nativeElement.scrollTop = this.chatBody.nativeElement.scrollHeight; }, 50); }
  private formatResponse(response: IAsistenteResponse): string { const base = response.respuesta || 'No pude identificar la consulta.'; const resultados = response.datos?.['resultados']; if (!Array.isArray(resultados) || !response.intencion?.includes('BUSQUEDA_DUPLICADO_NOMBRE') || resultados.length <= 1) return base; const detalle = resultados.slice(1).map((paciente: PacienteDuplicado) => `• ${[paciente.nombres, paciente.apellidos].filter(Boolean).join(' ')}, DNI: ${paciente.numDocumento || 'Sin DNI'}`).join('\n'); return `${base}\n\nOtras coincidencias posibles:\n${detalle}`; }
}
