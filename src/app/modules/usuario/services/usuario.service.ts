import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { IDetallePermiso, IMenu, IUsuario } from '../models/usuario';
import { IResponse } from '@app/global/response';
import { environment } from 'environments/environment';
import { getAllUsuarios } from '@app/mocks/mocks';

@Injectable({
  providedIn: 'root'
})
export class UsuarioService {

  URLServicio: string = environment.URLTienda;

  constructor( private httpClient:HttpClient) { }

  getAll():Observable<IUsuario[]> {
    return of(getAllUsuarios())
    return this.httpClient.get<IUsuario[]>(`${this.URLServicio}usuario/getAllActive`);
  }

  insert(header: IUsuario):Observable<IUsuario> {
    return this.httpClient.post<IUsuario>(`${this.URLServicio}usuario/insert/usuario`, header);
  }

  getFindById(id: number):Observable<IUsuario[]> {
    return of([getAllUsuarios()[0]])
    return this.httpClient.get<IUsuario[]>(`${this.URLServicio}usuario/findById/${id}`);
  }

  update(id: number, header: IUsuario):Observable<IResponse> {
    return this.httpClient.put<IResponse>(`${this.URLServicio}usuario/update/${id}`, header);
  }

  setInactive(id: number):Observable<any> {
    return this.httpClient.put<any>(`${this.URLServicio}usuario/setInactive/${id}`, id);
  }

  getMenuAllActive():Observable<IMenu[]> {
    return this.httpClient.get<IMenu[]>(`${this.URLServicio}menu/getAllActive`);
  }

  insertDetallePermiso(header: IDetallePermiso[]):Observable<IDetallePermiso[]> {
    return this.httpClient.post<IDetallePermiso[]>(`${this.URLServicio}detallepermiso/insert/detallepermisos`, header);
  }

  getFindByIdDetallePermiso(id: number):Observable<IDetallePermiso[]> {
    return this.httpClient.get<IDetallePermiso[]>(`${this.URLServicio}detallepermiso/findById/${id}`);
  }

  updateDetallePermiso(id: number, header: IDetallePermiso[]):Observable<IDetallePermiso> {
    return this.httpClient.put<IDetallePermiso>(`${this.URLServicio}detallepermiso/update/${id}`, header);
  }

}

