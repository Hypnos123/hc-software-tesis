import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { ClinicalHistoryFlowFeedback, ClinicalHistoryFlowFeedbackType } from '../models/clinical-history-flow-feedback';

@Injectable({ providedIn: 'root' })
export class ClinicalHistoryFlowFeedbackService {
  private readonly feedbackSubject = new Subject<ClinicalHistoryFlowFeedback>();
  readonly feedback$: Observable<ClinicalHistoryFlowFeedback> = this.feedbackSubject.asObservable();

  emit(type: ClinicalHistoryFlowFeedbackType): ClinicalHistoryFlowFeedback {
    const feedback: ClinicalHistoryFlowFeedback = {
      id: this.generateEventId(),
      type,
      createdAt: Date.now()
    };
    this.publish(feedback);
    return feedback;
  }

  publish(feedback: ClinicalHistoryFlowFeedback): void {
    this.feedbackSubject.next({ ...feedback });
  }

  private generateEventId(): string {
    const cryptoApi = globalThis.crypto;
    if (typeof cryptoApi?.randomUUID === 'function') return cryptoApi.randomUUID();
    if (typeof cryptoApi?.getRandomValues !== 'function') throw new Error('No hay un generador criptográfico seguro disponible.');
    const bytes = cryptoApi.getRandomValues(new Uint8Array(16));
    return Array.from(bytes, byte => byte.toString(16).padStart(2, '0')).join('');
  }
}
