import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { LoaderComponent } from './shared/components';
import { ToastModule } from 'primeng/toast';
import { InterfazChatComponent } from './modules/chatbot/pages/interfaz-chat/interfaz-chat.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, LoaderComponent, ToastModule, InterfazChatComponent, ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent {
  title = 'hc-app';
}
