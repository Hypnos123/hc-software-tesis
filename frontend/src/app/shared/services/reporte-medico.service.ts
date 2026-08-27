import { HttpClient, HttpErrorResponse, HttpHeaders, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { AuthService } from '@app/auth/services/auth.service';
import {
  ApiReporteError,
  ReporteConsultaFiltro,
  ReporteConsultaSeleccion,
  ReportePdfArchivo
} from '@app/shared/models/reporte-medico';
import { environment } from 'environments/environment';
import { map, Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ReporteMedicoService {
  private readonly url = `${environment.URLTienda}api/reportes-medicos`;
  private readonly nombrePredeterminado = 'reporte-medico.pdf';

  constructor(private http: HttpClient, private authService: AuthService) {}

  obtenerEvaluacionMedica(idConsulta: number): Observable<ReportePdfArchivo> {
    return this.http.get(`${this.url}/consultas/${idConsulta}/pdf`, {
      headers: this.authHeaders(), observe: 'response', responseType: 'blob'
    }).pipe(map(response => this.mapearPdf(response)));
  }

  obtenerSeleccion(idPaciente: number, filtro: ReporteConsultaFiltro): Observable<ReporteConsultaSeleccion> {
    return this.http.post<ReporteConsultaSeleccion>(
      `${this.url}/pacientes/${idPaciente}/consultas/seleccion`, filtro, { headers: this.authHeaders() }
    );
  }

  obtenerReporteConsolidado(idPaciente: number, filtro: ReporteConsultaFiltro): Observable<ReportePdfArchivo> {
    return this.http.post(`${this.url}/pacientes/${idPaciente}/consultas/pdf`, filtro, {
      headers: this.authHeaders(), observe: 'response', responseType: 'blob'
    }).pipe(map(response => this.mapearPdf(response)));
  }

  obtenerNombreArchivo(response: HttpResponse<Blob>): string {
    const disposition = response.headers.get('Content-Disposition') ?? '';
    const utf8 = disposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1];
    const simple = disposition.match(/filename="?([^";]+)"?/i)?.[1];
    const encontrado = utf8 ?? simple;
    if (!encontrado) return this.nombrePredeterminado;
    let decodificado = encontrado.trim();
    try { decodificado = decodeURIComponent(decodificado); } catch { /* conserva el valor recibido */ }
    const nombre = decodificado.split(/[\\/]/).pop()?.trim();
    return nombre?.toLowerCase().endsWith('.pdf') ? nombre : this.nombrePredeterminado;
  }

  async obtenerMensajeError(error: unknown): Promise<string> {
    if (!(error instanceof HttpErrorResponse)) return 'No se pudo generar el reporte médico.';
    const respuesta = await this.leerErrorBackend(error.error);
    if (respuesta?.mensaje?.trim()) return respuesta.mensaje.trim();
    if (error.status === 404) return 'No se encontró la consulta o el paciente seleccionado.';
    if (error.status === 422) return 'No existen consultas atendidas para generar este reporte.';
    if (error.status === 400) return 'Los criterios indicados para el reporte no son válidos.';
    return 'No se pudo generar el reporte médico.';
  }

  private mapearPdf(response: HttpResponse<Blob>): ReportePdfArchivo {
    const blob = response.body;
    const tipo = blob?.type?.split(';')[0].trim().toLowerCase();
    if (!blob || tipo !== 'application/pdf') throw new Error('La respuesta recibida no es un PDF válido.');
    return { blob, nombreArchivo: this.obtenerNombreArchivo(response) };
  }

  private authHeaders(): HttpHeaders {
    const idUsuario = this.authService.usuario?.idUsuario;
    return idUsuario ? new HttpHeaders({ 'X-Usuario-Id': String(idUsuario) }) : new HttpHeaders();
  }

  private async leerErrorBackend(error: unknown): Promise<ApiReporteError | undefined> {
    if (error instanceof Blob) {
      try { return JSON.parse(await error.text()) as ApiReporteError; } catch { return undefined; }
    }
    return typeof error === 'object' && error !== null ? error as ApiReporteError : undefined;
  }
}
