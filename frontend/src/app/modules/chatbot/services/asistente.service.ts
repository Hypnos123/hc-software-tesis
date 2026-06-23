import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { AuthService } from '@app/auth/services/auth.service';
import { environment } from 'environments/environment';
import { Observable } from 'rxjs';
import { IAsistenteRequest, IAsistenteResponse } from '../models/asistente';

@Injectable({ providedIn: 'root' })
export class AsistenteService {
  private URLServicio = environment.URLTienda;
  constructor(private http: HttpClient, private authService: AuthService) {}
  preguntar(pregunta: string): Observable<IAsistenteResponse> {
    const idUsuario = this.authService.usuario?.idUsuario;
    const headers = idUsuario ? new HttpHeaders({ 'X-Usuario-Id': String(idUsuario) }) : new HttpHeaders();
    return this.http.post<IAsistenteResponse>(`${this.URLServicio}asistente/preguntar`, { pregunta } as IAsistenteRequest, { headers });
  }
}
