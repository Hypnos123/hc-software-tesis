import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { AuthService } from '@app/auth/services/auth.service';
import {
  ArchivadoPacienteDuplicadoRequest,
  ArchivadoPacienteDuplicadoResponse,
  PacienteDuplicadoAnalisisResponse
} from '../models/paciente-duplicado-chat';
import { environment } from 'environments/environment';
import { Observable, throwError } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class PacienteDuplicadoChatService {
  private readonly urlBase = environment.URLTienda;

  constructor(private http: HttpClient, private authService: AuthService) {}

  analizar(dni: string): Observable<PacienteDuplicadoAnalisisResponse> {
    return this.http.get<PacienteDuplicadoAnalisisResponse>(`${this.urlBase}api/pacientes/duplicados`, {
      params: { dni }
    });
  }

  archivar(idPacienteArchivado: number, request: ArchivadoPacienteDuplicadoRequest): Observable<ArchivadoPacienteDuplicadoResponse> {
    const idUsuario = this.authService.usuario?.idUsuario;
    if (!idUsuario) {
      return throwError(() => ({
        status: 401,
        error: { resultado: 'USUARIO_REQUERIDO', mensaje: 'No se pudo identificar al usuario conectado.' }
      }));
    }
    const headers = new HttpHeaders({ 'X-Usuario-Id': String(idUsuario) });
    return this.http.post<ArchivadoPacienteDuplicadoResponse>(
      `${this.urlBase}api/pacientes/${idPacienteArchivado}/archivar-duplicado`,
      request,
      { headers }
    );
  }
}
