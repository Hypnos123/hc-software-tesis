import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { AuthService } from '@app/auth/services/auth.service';
import { environment } from 'environments/environment';
import { Observable } from 'rxjs';
import { PaginaResponse, PacienteArchivadoDetalle, PacienteArchivadoResumen, PacientesArchivadosFiltros } from '../models/paciente-archivado-admin';
import { FusionHistoriaAuditoriaDetalle, FusionHistoriaAuditoriaResumen, FusionesHistoriasFiltros } from '../models/fusion-historia-auditoria';

@Injectable({ providedIn: 'root' })
export class AuditoriaAdminService {
  private readonly url = `${environment.URLTienda}api/admin/pacientes-archivados`;
  private readonly fusionesUrl = `${environment.URLTienda}api/admin/auditoria/fusiones-historias-clinicas`;

  constructor(private readonly http: HttpClient, private readonly auth: AuthService) {}

  listarPacientesArchivados(filtros: PacientesArchivadosFiltros): Observable<PaginaResponse<PacienteArchivadoResumen>> {
    let params = new HttpParams().set('page', filtros.page).set('size', filtros.size);
    (['sort', 'search', 'dni', 'idPaciente', 'desde', 'hasta'] as const).forEach((campo) => {
      const valor = filtros[campo];
      if (valor !== undefined && valor !== null && valor !== '') params = params.set(campo, String(valor));
    });
    return this.http.get<PaginaResponse<PacienteArchivadoResumen>>(this.url, { params, headers: this.headers() });
  }

  obtenerPacienteArchivado(idPaciente: number): Observable<PacienteArchivadoDetalle> {
    return this.http.get<PacienteArchivadoDetalle>(`${this.url}/${idPaciente}`, { headers: this.headers() });
  }

  listarFusionesHistorias(filtros: FusionesHistoriasFiltros): Observable<PaginaResponse<FusionHistoriaAuditoriaResumen>> {
    let params = new HttpParams().set('page', filtros.page).set('size', filtros.size);
    (['sort', 'search', 'dni', 'idPaciente', 'idHistoriaPrincipal', 'idHistoriaEliminada', 'idUsuario', 'resultado', 'desde', 'hasta'] as const)
      .forEach((campo) => {
        const valor = filtros[campo];
        if (valor !== undefined && valor !== null && valor !== '') params = params.set(campo, String(valor));
      });
    return this.http.get<PaginaResponse<FusionHistoriaAuditoriaResumen>>(this.fusionesUrl, { params, headers: this.headers() });
  }

  obtenerFusionHistoria(idAuditoria: number): Observable<FusionHistoriaAuditoriaDetalle> {
    return this.http.get<FusionHistoriaAuditoriaDetalle>(`${this.fusionesUrl}/${idAuditoria}`, { headers: this.headers() });
  }

  private headers(): HttpHeaders {
    return new HttpHeaders({ 'X-Usuario-Id': String(this.auth.usuario?.idUsuario ?? '') });
  }
}
