import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { AuthService } from '@app/auth/services/auth.service';
import { Observable, throwError } from 'rxjs';
import { environment } from 'environments/environment';
import { ResumenConsultasPaciente } from '../models/resumen-consultas-paciente';

@Injectable({ providedIn: 'root' })
export class ResumenConsultasPacienteService {
  private readonly url = environment.URLTienda;
  constructor(private http: HttpClient, private authService: AuthService) {}

  obtener(idPaciente: number): Observable<ResumenConsultasPaciente> {
    const idUsuario = this.authService.usuario?.idUsuario;
    if (!idUsuario) return throwError(() => new Error('Usuario autenticado requerido'));
    const headers = new HttpHeaders({ 'X-Usuario-Id': String(idUsuario) });
    return this.http.get<ResumenConsultasPaciente>(
      `${this.url}api/consultas-medicas/pacientes/${idPaciente}/resumen`, { headers });
  }
}
