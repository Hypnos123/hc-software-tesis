import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ChatbotNavigationService {
  private readonly resumenConsultasSubject = new Subject<void>();
  readonly resumenConsultas$ = this.resumenConsultasSubject.asObservable();

  orientarAResumenConsultas(): void {
    this.resumenConsultasSubject.next();
  }
}
