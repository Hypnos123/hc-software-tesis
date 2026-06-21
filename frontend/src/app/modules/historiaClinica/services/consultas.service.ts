import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from 'environments/environment';
import { IHistoriaClinica, IHistoriaClinicaRequest, IPacienteBusqueda, IResponseModelGet, IResponseModelSet } from '../models/historiaClinica';

@Injectable({ providedIn: 'root' })
export class HistoriaClinicaService {
  URLServicio: string = environment.URLTienda;
  constructor(private httpClient: HttpClient) {}

  getAll(): Observable<IHistoriaClinica[]> {
    return this.httpClient.get<IResponseModelGet<IHistoriaClinica>>(`${this.URLServicio}historiaClinica/getAll`).pipe(map(r => r.data ?? []));
  }
  getById(id: number): Observable<IHistoriaClinica | undefined> {
    return this.httpClient.get<IResponseModelGet<IHistoriaClinica>>(`${this.URLServicio}historiaClinica/findById/${id}`).pipe(map(r => (r.data ?? [])[0]));
  }
  getByPaciente(idPaciente: number): Observable<IHistoriaClinica | undefined> {
    return this.httpClient.get<IResponseModelGet<IHistoriaClinica>>(`${this.URLServicio}historiaClinica/findByPaciente/${idPaciente}`).pipe(map(r => (r.data ?? [])[0]));
  }
  insert(header: IHistoriaClinicaRequest): Observable<IResponseModelSet> {
    return this.httpClient.post<IResponseModelSet>(`${this.URLServicio}historiaClinica/insert`, header);
  }
  update(id: number, header: IHistoriaClinicaRequest): Observable<IResponseModelSet> {
    return this.httpClient.put<IResponseModelSet>(`${this.URLServicio}historiaClinica/update/${id}`, header);
  }
  buscarPacientesPorNombre(nombre: string): Observable<IPacienteBusqueda[]> {
    const params = new HttpParams().set('nombre', nombre).set('limit', 10);
    return this.httpClient.get<IResponseModelGet<IPacienteBusqueda>>(`${this.URLServicio}paciente/search`, { params }).pipe(map(r => r.data ?? []));
  }
  buscarPacientesPorDni(dni: string): Observable<IPacienteBusqueda[]> {
    const params = new HttpParams().set('dni', dni).set('limit', 10);
    return this.httpClient.get<IResponseModelGet<IPacienteBusqueda>>(`${this.URLServicio}paciente/search`, { params }).pipe(map(r => r.data ?? []));
  }
  getAntecedentesByPaciente(idPaciente: number): Observable<any | undefined> {
    return this.httpClient.get<any>(`${this.URLServicio}antecedentes/findByPaciente/${idPaciente}`).pipe(map(r => (r.data ?? [])[0]));
  }
}
