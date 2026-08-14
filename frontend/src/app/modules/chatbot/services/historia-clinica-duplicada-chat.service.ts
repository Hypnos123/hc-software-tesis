import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from 'environments/environment';
import {
  AnalisisHistoriasClinicasDuplicadas,
  DeteccionHistoriasClinicasDuplicadasResponse
} from '../models/historia-clinica-duplicada-chat';

@Injectable({ providedIn: 'root' })
export class HistoriaClinicaDuplicadaChatService {
  private readonly url = `${environment.URLTienda}api/historias-clinicas/duplicados`;

  constructor(private readonly http: HttpClient) {}

  detectar(): Observable<DeteccionHistoriasClinicasDuplicadasResponse> {
    return this.http.get<DeteccionHistoriasClinicasDuplicadasResponse>(this.url);
  }

  analizar(idsHistoriasClinicas: number[]): Observable<AnalisisHistoriasClinicasDuplicadas> {
    return this.http.post<AnalisisHistoriasClinicasDuplicadas>(`${this.url}/analizar`, { idsHistoriasClinicas });
  }
}
