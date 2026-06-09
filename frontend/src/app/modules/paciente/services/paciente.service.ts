import { Injectable } from '@angular/core';
import { IPaciente } from '../models/paciente';
import { environment } from 'environments/environment';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { IResponse } from '@app/global/response';
import { getPacientes } from '@app/mocks/mocks';


@Injectable({
  providedIn: 'root'
})
export class PacienteService {
  URLServicio: string = environment.URLTienda;

  constructor(private httpClient: HttpClient) { }

  getAllActivos(): Observable<IPaciente[]> {
    return of(getPacientes()) 
    return this.httpClient.get<IPaciente[]>(`${this.URLServicio}paciente/getAllActive`)
  }

  getById(id: number): Observable<IPaciente | undefined> {
    const paciente = getPacientes().find(x => x.idPaciente === id);
    return of(paciente);
    // return this.httpClient.get<IPaciente>(`${this.URLServicio}paciente/findById/${id}`);
  }

  insert(header: IPaciente): Observable<IPaciente> {
    return this.httpClient.post<IPaciente>(`${this.URLServicio}paciente/insert/paciente`, header);
  }

  getFindById(id: number): Observable<IPaciente[]> {
    return this.httpClient.get<IPaciente[]>(`${this.URLServicio}paciente/findById/${id}`)
  }

  update(id: number, header: IPaciente): Observable<IResponse> {
    return this.httpClient.put<IResponse>(`${this.URLServicio}paciente/update/${id}`, header);
  }

  setInactive(id: number): Observable<IResponse> {
    return this.httpClient.put<IResponse>(`${this.URLServicio}paciente/setInactive/${id}`, id);
  }

  
}
