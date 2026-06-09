import { Component, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

interface ChatMessage {
  text: string;
  sender: 'user' | 'bot';
}

@Component({
  selector: 'app-interfaz-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './interfaz-chat.component.html',
  styleUrl: './interfaz-chat.component.scss'
})
export class InterfazChatComponent {
  @ViewChild('chatBody') chatBody!: ElementRef;

  isOpen: boolean = false;
  userMessage: string = '';

  messages: ChatMessage[] = [
    {
      sender: 'bot',
      text: 'Hola, soy tu asistente virtual. Puedes preguntarme sobre historias clínicas, pacientes o consultas.'
    }
  ];

  toggleChat(): void {
    this.isOpen = !this.isOpen;

    if (this.isOpen) {
      this.scrollToBottom();
    }
  }

  sendMessage(): void {
    const pregunta = this.userMessage.trim();

    if (!pregunta) return;

    this.messages.push({
      sender: 'user',
      text: pregunta
    });

    this.userMessage = '';
    this.scrollToBottom();

    this.messages.push({
      sender: 'bot',
      text: 'Escribiendo...'
    });

    this.scrollToBottom();

    setTimeout(() => {
      this.messages.pop();

      this.messages.push({
        sender: 'bot',
        text: this.getMockResponse(pregunta)
      });

      this.scrollToBottom();
    }, 700);
  }

  quickAsk(text: string): void {
    this.userMessage = text;
    this.sendMessage();
  }

  scrollToBottom(): void {
    setTimeout(() => {
      if (this.chatBody) {
        this.chatBody.nativeElement.scrollTop =
          this.chatBody.nativeElement.scrollHeight;
      }
    }, 50);
  }

  getMockResponse(pregunta: string): string {
    const texto = pregunta.toLowerCase();

    if (texto.includes('historias') && texto.includes('hoy')) {
      return 'Hoy se registraron 8 historias clínicas.';
    }

    if (texto.includes('pacientes') && texto.includes('mes')) {
      return 'Este mes se atendieron 42 pacientes.';
    }

    if (texto.includes('antecedentes') || texto.includes('dni')) {
      return 'El paciente consultado registra antecedentes patológicos en el sistema.';
    }

    if (texto.includes('consultas') && texto.includes('paciente')) {
      return 'El paciente registra 3 consultas médicas en el sistema.';
    }

    if (texto.includes('incompletas')) {
      return 'Actualmente existen 5 historias clínicas incompletas.';
    }

    return 'Por ahora puedo ayudarte con consultas sobre historias clínicas registradas hoy, pacientes atendidos este mes, antecedentes por DNI, consultas de pacientes e historias incompletas.';
  }
}