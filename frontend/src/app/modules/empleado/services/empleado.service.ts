import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { IEmpleado, IResponseModelGet } from '../models/empleado';
import { IResponse } from '@app/global/response';
import { environment } from 'environments/environment';

@Injectable({
  providedIn: 'root'
})
export class EmpleadoService {
  URLServicio: string = environment.URLTienda;

  constructor(private httpClient: HttpClient) { }

  getAllActivos(): Observable<IEmpleado[]> {
    return this.httpClient
      .get<IResponseModelGet<IEmpleado> | IEmpleado[]>(`${this.URLServicio}empleado/getAllActive`)
      .pipe(map((response) => this.getResponseData(response)));
  }

  getAll(): Observable<IEmpleado[]> {
    return this.httpClient
      .get<IResponseModelGet<IEmpleado> | IEmpleado[]>(`${this.URLServicio}empleado/getAll`)
      .pipe(map((response) => this.getResponseData(response)));
  }

  insert(header: IEmpleado): Observable<IResponse> {
    return this.httpClient.post<IResponse>(`${this.URLServicio}empleado/insert/empleado`, header);
  }

  getFindById(id: number): Observable<IEmpleado[]> {
    return this.httpClient
      .get<IResponseModelGet<IEmpleado> | IEmpleado[]>(`${this.URLServicio}empleado/findById/${id}`)
      .pipe(map((response) => this.getResponseData(response)));
  }

  update(id: number, header: IEmpleado): Observable<IResponse> {
    return this.httpClient.put<IResponse>(`${this.URLServicio}empleado/update/${id}`, header);
  }

  changeStatus(id: number): Observable<IResponse> {
    return this.httpClient.put<IResponse>(`${this.URLServicio}empleado/changeStatus/${id}`, null);
  }

  private getResponseData(response: IResponseModelGet<IEmpleado> | IEmpleado[]): IEmpleado[] {
    return Array.isArray(response) ? response : response.data ?? [];
  }
}
