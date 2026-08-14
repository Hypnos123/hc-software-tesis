import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpHeaders } from '@angular/common/http';
import { AuthService } from '@app/auth/services/auth.service';
import { environment } from 'environments/environment';
import {
  AnalisisHistoriasClinicasDuplicadas,
  DeteccionHistoriasClinicasDuplicadasResponse,
  FusionarHistoriasClinicasRequest,
  FusionarHistoriasClinicasResponse
} from '../models/historia-clinica-duplicada-chat';

@Injectable({ providedIn: 'root' })
export class HistoriaClinicaDuplicadaChatService {
  private readonly url = `${environment.URLTienda}api/historias-clinicas/duplicados`;

  constructor(private readonly http: HttpClient, private readonly authService: AuthService) {}

  detectar(): Observable<DeteccionHistoriasClinicasDuplicadasResponse> {
    return this.http.get<DeteccionHistoriasClinicasDuplicadasResponse>(this.url);
  }

  analizar(idsHistoriasClinicas: number[]): Observable<AnalisisHistoriasClinicasDuplicadas> {
    return this.http.post<AnalisisHistoriasClinicasDuplicadas>(`${this.url}/analizar`, { idsHistoriasClinicas });
  }

  fusionar(idHistoriaSecundaria: number, request: FusionarHistoriasClinicasRequest): Observable<FusionarHistoriasClinicasResponse> {
    const idUsuario = this.authService.usuario?.idUsuario;
    const headers = new HttpHeaders({ 'X-Usuario-Id': String(idUsuario ?? '') });
    return this.http.post<FusionarHistoriasClinicasResponse>(`${environment.URLTienda}api/historias-clinicas/${idHistoriaSecundaria}/fusionar`, request, { headers });
  }
}
