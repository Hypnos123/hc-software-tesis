import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environment } from 'environments/environment';
import { Observable } from 'rxjs';
import {
  IOllamaEjecucionRequest,
  IOllamaEjecucionResponse
} from '../models/ollama-ejecucion';

@Injectable({ providedIn: 'root' })
export class OllamaEjecucionService {
  private readonly url = `${environment.URLTienda}ollama/ejecutar`;

  constructor(private http: HttpClient) {}

  ejecutar(mensaje: string): Observable<IOllamaEjecucionResponse> {
    return this.http.post<IOllamaEjecucionResponse>(this.url, {
      mensaje
    } as IOllamaEjecucionRequest);
  }
}
