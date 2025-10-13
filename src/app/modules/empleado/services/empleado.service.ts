import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { IEmpleado } from '../models/empleado';
import { IResponse } from '@app/global/response';
import { environment } from 'environments/environment.prod';

@Injectable({
  providedIn: 'root'
})
export class EmpleadoService {
  URLServicio: string = environment.URLTienda;

  constructor(private httpClient: HttpClient) { }

  getAllActivos(): Observable<IEmpleado[]> {
    return this.httpClient.get<IEmpleado[]>(`${this.URLServicio}empleado/getAllActive`)
  }

  insert(header: IEmpleado): Observable<IEmpleado> {
    return this.httpClient.post<IEmpleado>(`${this.URLServicio}empleado/insert/empleado`, header);
  }

  getFindById(id: number): Observable<IEmpleado[]> {
    return this.httpClient.get<IEmpleado[]>(`${this.URLServicio}empleado/findById/${id}`)
  }

  update(id: number, header: IEmpleado): Observable<IResponse> {
    return this.httpClient.put<IResponse>(`${this.URLServicio}empleado/update/${id}`, header);
  }

  setInactive(id: number): Observable<IResponse> {
    return this.httpClient.put<IResponse>(`${this.URLServicio}empleado/setInactive/${id}`, id);
  }
}
