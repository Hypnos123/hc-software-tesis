import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from 'environments/environment';
import {
  IPacienteImportacionConfirmacion,
  IPacienteImportacionPrevisualizacion
} from '../models/paciente-importacion';

@Injectable({ providedIn: 'root' })
export class PacienteImportacionService {
  private readonly URLServicio = `${environment.URLTienda}paciente/importacion`;
  readonly nombrePlantillaPredeterminado = 'plantilla-importacion-pacientes-v1.0.xlsx';

  constructor(private http: HttpClient) {}

  descargarPlantilla(): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.URLServicio}/plantilla`, {
      observe: 'response',
      responseType: 'blob'
    });
  }

  validarArchivo(archivo: File): Observable<IPacienteImportacionPrevisualizacion> {
    const formData = new FormData();
    formData.append('archivo', archivo);
    return this.http.post<IPacienteImportacionPrevisualizacion>(`${this.URLServicio}/validar`, formData);
  }

  obtenerPrevisualizacion(importacionId: string): Observable<IPacienteImportacionPrevisualizacion> {
    return this.http.get<IPacienteImportacionPrevisualizacion>(`${this.URLServicio}/${importacionId}`);
  }

  confirmarImportacion(importacionId: string): Observable<IPacienteImportacionConfirmacion> {
    return this.http.post<IPacienteImportacionConfirmacion>(
      `${this.URLServicio}/${importacionId}/confirmar`,
      null
    );
  }

  obtenerNombreArchivo(response: HttpResponse<Blob>): string {
    const disposition = response.headers.get('Content-Disposition') ?? '';
    const utf8 = disposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1];
    const simple = disposition.match(/filename="?([^";]+)"?/i)?.[1];
    const encontrado = utf8 ?? simple;
    if (!encontrado) return this.nombrePlantillaPredeterminado;
    try {
      return decodeURIComponent(encontrado.trim());
    } catch {
      return encontrado.trim();
    }
  }
}
