import { Injectable } from '@angular/core';
import { ClinicalHistoryPrefillTransfer, ClinicalHistoryTransferCandidate } from '../models/clinical-history-transfer';

@Injectable({ providedIn: 'root' })
export class ClinicalHistoryTransferService {
  static readonly TTL_MS = 5 * 60 * 1000;
  private readonly transfers = new Map<string, ClinicalHistoryPrefillTransfer>();

  createTransfer(candidate: ClinicalHistoryTransferCandidate): string {
    this.clearExpiredTransfers();
    const createdAt = Date.now();
    const transferId = this.generateTransferId();
    this.transfers.set(transferId, {
      source: 'chatbot',
      createdAt,
      expiresAt: createdAt + ClinicalHistoryTransferService.TTL_MS,
      candidate: { ...candidate }
    });
    return transferId;
  }

  peekTransfer(transferId: string): ClinicalHistoryPrefillTransfer | null {
    this.clearExpiredTransfers();
    const transfer = this.transfers.get(transferId);
    return transfer ? this.cloneTransfer(transfer) : null;
  }

  consumeTransfer(transferId: string): ClinicalHistoryPrefillTransfer | null {
    this.clearExpiredTransfers();
    const transfer = this.transfers.get(transferId);
    if (!transfer) return null;
    this.transfers.delete(transferId);
    return this.cloneTransfer(transfer);
  }

  revokeTransfer(transferId: string): void {
    this.transfers.delete(transferId);
  }

  clearExpiredTransfers(): void {
    const now = Date.now();
    this.transfers.forEach((transfer, transferId) => {
      if (transfer.expiresAt <= now) this.transfers.delete(transferId);
    });
  }

  clearAll(): void {
    this.transfers.clear();
  }

  private cloneTransfer(transfer: ClinicalHistoryPrefillTransfer): ClinicalHistoryPrefillTransfer {
    return { ...transfer, candidate: { ...transfer.candidate } };
  }

  private generateTransferId(): string {
    const cryptoApi = globalThis.crypto;
    if (typeof cryptoApi?.randomUUID === 'function') return cryptoApi.randomUUID();
    if (typeof cryptoApi?.getRandomValues !== 'function') throw new Error('No hay un generador criptográfico seguro disponible.');
    const bytes = cryptoApi.getRandomValues(new Uint8Array(16));
    bytes[6] = (bytes[6] & 0x0f) | 0x40;
    bytes[8] = (bytes[8] & 0x3f) | 0x80;
    const hex = Array.from(bytes, byte => byte.toString(16).padStart(2, '0'));
    return `${hex.slice(0, 4).join('')}-${hex.slice(4, 6).join('')}-${hex.slice(6, 8).join('')}-${hex.slice(8, 10).join('')}-${hex.slice(10).join('')}`;
  }
}
