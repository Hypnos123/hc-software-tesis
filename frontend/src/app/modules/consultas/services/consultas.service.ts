import { Observable } from "rxjs";
import { map } from "rxjs/operators";
import { environment } from "environments/environment";
import { Injectable } from "@angular/core";
import { HttpClient, HttpHeaders } from "@angular/common/http";
import { IDetalleConsulta, INuevaConsultaRequest, IResponseModelGet, IResponseModelSet } from "@app/modules/historiaClinica/models/historiaClinica";
import { AuthService } from "@app/auth/services/auth.service";

@Injectable({ providedIn: 'root' })
export class ConsultaService {
  URLServicio: string = environment.URLTienda;

  constructor(private httpClient: HttpClient, private authService: AuthService) { }

  getAllActivos(): Observable<any[]> {
    return this.httpClient.get<IResponseModelGet<IDetalleConsulta>>(`${this.URLServicio}consulta/getAllActive`, { headers: this.authHeaders() })
      .pipe(map(r => r.data ?? []));
  }

  getFindById(id: number): Observable<IDetalleConsulta | undefined> {
    return this.httpClient.get<IResponseModelGet<IDetalleConsulta>>(`${this.URLServicio}consulta/findById/${id}`, { headers: this.authHeaders() })
      .pipe(map(r => (r.data ?? [])[0]));
  }

  finalizarAtencion(id: number, header: INuevaConsultaRequest): Observable<IResponseModelSet> {
    return this.httpClient.put<IResponseModelSet>(`${this.URLServicio}consulta/finalizar-atencion/${id}`, header, { headers: this.authHeaders() });
  }

  private authHeaders(): HttpHeaders {
    const idUsuario = this.authService.usuario?.idUsuario;
    return idUsuario ? new HttpHeaders({ 'X-Usuario-Id': String(idUsuario) }) : new HttpHeaders();
  }
}
