import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { environment } from 'environments/environment';
import { IHistoriaClinica } from '../models/historiaClinica';
import { getHistoriasClinicas } from '@app/mocks/mocks';

@Injectable({
  providedIn: 'root'
})
export class HistoriaClinicaService {
  URLServicio: string = environment.URLTienda;

  constructor(private httpClient: HttpClient) {}

  getAll(): Observable<IHistoriaClinica[]> {
    return of(getHistoriasClinicas());
    // return this.httpClient.get<IHistoriaClinica[]>(`${this.URLServicio}historiaClinica/getAll`);
  }

  getById(id: number): Observable<IHistoriaClinica | undefined> {
    const historia = getHistoriasClinicas().find(x => x.idHistoriaClinica === id);
    return of(historia);
    // return this.httpClient.get<IHistoriaClinica>(`${this.URLServicio}historiaClinica/findById/${id}`);
  }

  insert(header: IHistoriaClinica): Observable<IHistoriaClinica> {
    return this.httpClient.post<IHistoriaClinica>(`${this.URLServicio}historiaClinica/insert`, header);
  }

  update(id: number, header: IHistoriaClinica): Observable<any> {
    return this.httpClient.put<any>(`${this.URLServicio}historiaClinica/update/${id}`, header);
  }

  setInactive(id: number): Observable<any> {
    return this.httpClient.put<any>(`${this.URLServicio}historiaClinica/setInactive/${id}`, id);
  }
}