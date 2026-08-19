import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

export interface ResumenConsultasContexto {
  idPaciente: number;
  nombreCompleto?: string;
  dni?: string;
  cantidadConsultasAtendidas?: number;
}

@Injectable({ providedIn: 'root' })
export class ChatbotNavigationService {
  private readonly resumenConsultasSubject = new Subject<ResumenConsultasContexto>();
  readonly resumenConsultas$ = this.resumenConsultasSubject.asObservable();

  abrirResumenConsultas(contexto: ResumenConsultasContexto): void {
    this.resumenConsultasSubject.next(contexto);
  }
}
