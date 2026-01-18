import { Observable } from "rxjs";
import { IConsulta } from "../models/consultas";
import { environment } from "environments/environment";
import { Injectable } from "@angular/core";
import { IResponse } from "@app/global/response";
import { HttpClient } from "@angular/common/http";

@Injectable({
  providedIn: 'root'
})
export class ConsultaService {
  URLServicio: string = environment.URLTienda;

  constructor(private httpClient: HttpClient) { }

  getAllActivos(): Observable<IConsulta[]> {
    return this.httpClient.get<IConsulta[]>(`${this.URLServicio}paciente/getAllActive`)
  }

  insert(header: IConsulta): Observable<IConsulta> {
    return this.httpClient.post<IConsulta>(`${this.URLServicio}paciente/insert/paciente`, header);
  }

  getFindById(id: number): Observable<IConsulta[]> {
    return this.httpClient.get<IConsulta[]>(`${this.URLServicio}paciente/findById/${id}`)
  }

  update(id: number, header: IConsulta): Observable<IResponse> {
    return this.httpClient.put<IResponse>(`${this.URLServicio}paciente/update/${id}`, header);
  }

  setInactive(id: number): Observable<IResponse> {
    return this.httpClient.put<IResponse>(`${this.URLServicio}paciente/setInactive/${id}`, id);
  }
}
