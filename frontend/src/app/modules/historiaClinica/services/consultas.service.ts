import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from 'environments/environment';
import { IDetalleConsulta, IEmpleadoDoctor, IHistoriaClinica, IHistoriaClinicaRequest, INuevaConsultaRequest, IPacienteBusqueda, IResponseModelGet, IResponseModelSet } from '../models/historiaClinica';

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
  getConsultasByHistoria(idHistoriaClinica: number): Observable<IDetalleConsulta[]> {
    return this.httpClient.get<IResponseModelGet<IDetalleConsulta>>(`${this.URLServicio}consulta/findByHistoriaClinica/${idHistoriaClinica}`).pipe(map(r => r.data ?? []));
  }
  getConsultaById(idConsulta: number): Observable<IDetalleConsulta | undefined> {
    return this.httpClient.get<IResponseModelGet<IDetalleConsulta>>(`${this.URLServicio}consulta/findById/${idConsulta}`).pipe(map(r => (r.data ?? [])[0]));
  }
  insertConsulta(request: INuevaConsultaRequest): Observable<IResponseModelSet> {
    return this.httpClient.post<IResponseModelSet>(`${this.URLServicio}consulta/insert/consulta`, request);
  }
  updateConsulta(idConsulta: number, request: INuevaConsultaRequest): Observable<IResponseModelSet> {
    return this.httpClient.put<IResponseModelSet>(`${this.URLServicio}consulta/update/${idConsulta}`, request);
  }
  getDoctoresActivos(): Observable<IEmpleadoDoctor[]> {
    return this.httpClient.get<IResponseModelGet<IEmpleadoDoctor>>(`${this.URLServicio}empleado/doctores-activos`).pipe(
      map(r => (r.data ?? []).map(d => ({ ...d, nombreCompleto: [d.apellidos, d.nombres].filter(Boolean).join(' ') })))
    );
  }
}
