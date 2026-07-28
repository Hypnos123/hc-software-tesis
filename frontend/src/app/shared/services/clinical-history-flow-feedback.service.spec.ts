import { TestBed } from '@angular/core/testing';
import { ClinicalHistoryFlowFeedback } from '../models/clinical-history-flow-feedback';
import { ClinicalHistoryFlowFeedbackService } from './clinical-history-flow-feedback.service';

describe('ClinicalHistoryFlowFeedbackService', () => {
  let service: ClinicalHistoryFlowFeedbackService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ClinicalHistoryFlowFeedbackService);
  });

  it('debe emitir un evento tipado sin datos personales ni almacenamiento persistente', () => {
    const received: ClinicalHistoryFlowFeedback[] = [];
    spyOn(localStorage, 'setItem');
    spyOn(sessionStorage, 'setItem');
    const subscription = service.feedback$.subscribe(feedback => received.push(feedback));

    const emitted = service.emit('prefill-success');

    expect(received).toEqual([emitted]);
    expect(Object.keys(emitted).sort()).toEqual(['createdAt', 'id', 'type']);
    expect(emitted.id).toBeTruthy();
    expect(emitted.type).toBe('prefill-success');
    expect(localStorage.setItem).not.toHaveBeenCalled();
    expect(sessionStorage.setItem).not.toHaveBeenCalled();
    subscription.unsubscribe();
  });

  it('no debe reproducir eventos anteriores a nuevos suscriptores', () => {
    service.emit('prefill-failure');
    const received: ClinicalHistoryFlowFeedback[] = [];

    const subscription = service.feedback$.subscribe(feedback => received.push(feedback));

    expect(received).toEqual([]);
    subscription.unsubscribe();
  });
});
