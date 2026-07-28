import { TestBed } from '@angular/core/testing';
import { ClinicalHistoryTransferCandidate } from '../models/clinical-history-transfer';
import { ClinicalHistoryTransferService } from './clinical-history-transfer.service';

describe('ClinicalHistoryTransferService', () => {
  let service: ClinicalHistoryTransferService;
  const candidate: ClinicalHistoryTransferCandidate = {
    idPaciente: 987654321,
    dni: '01234567',
    nombres: 'Andrea Lucía',
    apellidos: 'Quispe Ramírez',
    fechaIngreso: '2020-03-10',
    fechaNacimiento: '1992-01-01',
    estadoCivil: 'SOLTERO',
    enfermedadesPrevias: null,
    cirugiasPrevias: 'Apendicectomía',
    alergiaMedicamentos: null
  };

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ClinicalHistoryTransferService);
    service.clearAll();
  });

  it('debe crear un identificador opaco y mantener el payload disponible en memoria', () => {
    spyOn(localStorage, 'setItem');
    spyOn(sessionStorage, 'setItem');

    const transferId = service.createTransfer(candidate);
    const transfer = service.peekTransfer(transferId);

    expect(transferId).toBeTruthy();
    expect(transferId).not.toContain(candidate.dni);
    expect(transferId).not.toContain(candidate.nombres);
    expect(transferId).not.toContain(String(candidate.idPaciente));
    expect(transfer?.source).toBe('chatbot');
    expect(transfer?.candidate).toEqual(candidate);
    expect(localStorage.setItem).not.toHaveBeenCalled();
    expect(sessionStorage.setItem).not.toHaveBeenCalled();
  });

  it('debe consumir una transferencia una sola vez', () => {
    const transferId = service.createTransfer(candidate);

    expect(service.consumeTransfer(transferId)?.candidate).toEqual(candidate);
    expect(service.consumeTransfer(transferId)).toBeNull();
  });

  it('debe revocar una transferencia específica', () => {
    const transferId = service.createTransfer(candidate);

    service.revokeTransfer(transferId);

    expect(service.peekTransfer(transferId)).toBeNull();
  });

  it('debe eliminar todas las transferencias', () => {
    const first = service.createTransfer(candidate);
    const second = service.createTransfer({ ...candidate, dni: '87654321' });

    service.clearAll();

    expect(service.peekTransfer(first)).toBeNull();
    expect(service.peekTransfer(second)).toBeNull();
  });

  it('debe eliminar y rechazar una transferencia vencida', () => {
    const now = 1_700_000_000_000;
    const nowSpy = spyOn(Date, 'now').and.returnValue(now);
    const transferId = service.createTransfer(candidate);
    nowSpy.and.returnValue(now + ClinicalHistoryTransferService.TTL_MS + 1);

    expect(service.peekTransfer(transferId)).toBeNull();
    expect(service.consumeTransfer(transferId)).toBeNull();
  });

  it('debe conservar las fechas como YYYY-MM-DD', () => {
    const transfer = service.peekTransfer(service.createTransfer(candidate));

    expect(transfer?.candidate.fechaIngreso).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    expect(transfer?.candidate.fechaNacimiento).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    expect(transfer?.candidate.fechaIngreso).not.toContain('T');
    expect(transfer?.candidate.fechaNacimiento).not.toContain('Z');
  });
});
