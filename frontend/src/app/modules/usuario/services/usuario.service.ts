import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { IDetallePermiso, IMenu, IResponseModelGet, IUsuario } from '../models/usuario';
import { IResponse } from '@app/global/response';
import { environment } from 'environments/environment';

@Injectable({
  providedIn: 'root'
})
export class UsuarioService {

  URLServicio: string = environment.URLTienda;

  constructor(private httpClient: HttpClient) { }

  getAll(): Observable<IUsuario[]> {
    return this.httpClient
      .get<IResponseModelGet<IUsuario> | IUsuario[]>(`${this.URLServicio}usuario/getAllActive`)
      .pipe(map((response) => this.getResponseData(response)));
  }

  insert(header: IUsuario): Observable<IResponse> {
    return this.httpClient.post<IResponse>(`${this.URLServicio}usuario/insert/usuario`, header);
  }

  getFindById(id: number): Observable<IUsuario[]> {
    return this.httpClient
      .get<IResponseModelGet<IUsuario> | IUsuario[]>(`${this.URLServicio}usuario/findById/${id}`)
      .pipe(map((response) => this.getResponseData(response)));
  }

  update(id: number, header: IUsuario): Observable<IResponse> {
    return this.httpClient.put<IResponse>(`${this.URLServicio}usuario/update/${id}`, header);
  }

  setInactive(id: number): Observable<IResponse> {
    return this.httpClient.put<IResponse>(`${this.URLServicio}usuario/setInactive/${id}`, id);
  }

  getMenuAllActive(): Observable<IMenu[]> {
    return this.httpClient
      .get<IResponseModelGet<IMenu> | IMenu[]>(`${this.URLServicio}menu/getAllActive`)
      .pipe(map((response) => this.getResponseData(response)));
  }

  insertDetallePermiso(header: IDetallePermiso[]): Observable<IResponse> {
    return this.httpClient.post<IResponse>(`${this.URLServicio}detallepermiso/insert/detallepermisos`, header);
  }

  getFindByIdDetallePermiso(id: number): Observable<IDetallePermiso[]> {
    return this.httpClient
      .get<IResponseModelGet<IDetallePermiso> | IDetallePermiso[]>(`${this.URLServicio}detallepermiso/findById/${id}`)
      .pipe(map((response) => this.getResponseData(response)));
  }

  updateDetallePermiso(id: number, header: IDetallePermiso[]): Observable<IResponse> {
    return this.httpClient.put<IResponse>(`${this.URLServicio}detallepermiso/update/${id}`, header);
  }

  private getResponseData<T>(response: IResponseModelGet<T> | T[]): T[] {
    return Array.isArray(response) ? response : response.data ?? [];
  }
}
