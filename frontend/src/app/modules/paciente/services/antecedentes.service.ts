import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { IResponse } from '@app/global/response';
import { IPaciente, IResponseModelGet } from '../models/paciente';
import { environment } from 'environments/environment';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class AntecedentesService {
  URLServicio: string = environment.URLTienda;

  constructor(private httpClient: HttpClient) { }

  getByPacienteId(idPaciente: number): Observable<IPaciente | undefined> {
    return this.httpClient
      .get<IResponseModelGet<IPaciente> | IPaciente[]>(`${this.URLServicio}antecedentes/findByPaciente/${idPaciente}`)
      .pipe(map((response) => this.getResponseData(response)[0]));
  }

  insert(header: IPaciente): Observable<IResponse> {
    return this.httpClient.post<IResponse>(`${this.URLServicio}antecedentes/insert/antecedente`, header);
  }

  update(idAntecedente: number, header: IPaciente): Observable<IResponse> {
    return this.httpClient.put<IResponse>(`${this.URLServicio}antecedentes/update/${idAntecedente}`, header);
  }

  private getResponseData(response: IResponseModelGet<IPaciente> | IPaciente[]): IPaciente[] {
    return Array.isArray(response) ? response : response.data ?? [];
  }
}
