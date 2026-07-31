import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class PacienteListRefreshService {
  private readonly refreshSubject = new Subject<void>();
  readonly refresh$: Observable<void> = this.refreshSubject.asObservable();

  solicitarActualizacion(): void {
    this.refreshSubject.next();
  }
}
