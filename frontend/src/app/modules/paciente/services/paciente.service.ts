import { Injectable } from '@angular/core';
import { IConsultaDniResponse, IPaciente, IResponseModelGet } from '../models/paciente';
import { environment } from 'environments/environment';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { IResponse } from '@app/global/response';

@Injectable({
  providedIn: 'root'
})
export class PacienteService {
  URLServicio: string = environment.URLTienda;

  constructor(private httpClient: HttpClient) { }

  getAllActivos(): Observable<IPaciente[]> {
    return this.httpClient
      .get<IResponseModelGet<IPaciente> | IPaciente[]>(`${this.URLServicio}paciente/getAllActive`)
      .pipe(map((response) => this.getResponseData(response).map((paciente) => this.mapToView(paciente))));
  }

  getById(id: number): Observable<IPaciente | undefined> {
    return this.httpClient
      .get<IResponseModelGet<IPaciente> | IPaciente[]>(`${this.URLServicio}paciente/findById/${id}`)
      .pipe(map((response) => {
        const paciente = this.getResponseData(response)[0];
        return paciente ? this.mapToView(paciente) : undefined;
      }));
  }

  insert(header: IPaciente): Observable<IResponse> {
    return this.httpClient.post<IResponse>(`${this.URLServicio}paciente/insert/paciente`, header);
  }

  getFindById(id: number): Observable<IPaciente[]> {
    return this.httpClient
      .get<IResponseModelGet<IPaciente> | IPaciente[]>(`${this.URLServicio}paciente/findById/${id}`)
      .pipe(map((response) => this.getResponseData(response).map((paciente) => this.mapToView(paciente))));
  }

  update(id: number, header: IPaciente): Observable<IResponse> {
    return this.httpClient.put<IResponse>(`${this.URLServicio}paciente/update/${id}`, header);
  }

  consultarDni(dni: string): Observable<IConsultaDniResponse> {
    return this.httpClient.get<IConsultaDniResponse>(`${this.URLServicio}api/ia/consultar-dni/${dni}`);
  }

  private getResponseData(response: IResponseModelGet<IPaciente> | IPaciente[]): IPaciente[] {
    return Array.isArray(response) ? response : response.data ?? [];
  }

  private mapToView(paciente: IPaciente): IPaciente {
    const fechaNacimiento = this.toDate(paciente.fechaNacimiento);

    return {
      ...paciente,
      nombreApellidos: this.buildNombreApellidos(paciente),
      dni: paciente.numDocumento ?? paciente.dni,
      fechaRegistro: this.formatDate(paciente.fechaIngreso),
      edad: fechaNacimiento ? this.calculateAge(fechaNacimiento) : paciente.edad,
    };
  }

  private buildNombreApellidos(paciente: IPaciente): string {
    return [paciente.apellidos, paciente.nombres]
      .filter(Boolean)
      .join(' ')
      .trim();
  }

  private calculateAge(fechaNacimiento: Date): number {
    const hoy = new Date();
    let edad = hoy.getFullYear() - fechaNacimiento.getFullYear();
    const mes = hoy.getMonth() - fechaNacimiento.getMonth();

    if (mes < 0 || (mes === 0 && hoy.getDate() < fechaNacimiento.getDate())) {
      edad--;
    }

    return edad;
  }

  private formatDate(fecha: string | Date | undefined): string {
    const date = this.toDate(fecha);
    if (!date) return '';

    const dia = date.getDate().toString().padStart(2, '0');
    const mes = (date.getMonth() + 1).toString().padStart(2, '0');
    const anio = date.getFullYear();
    return `${dia}/${mes}/${anio}`;
  }

  private toDate(fecha: string | Date | undefined): Date | null {
    if (!fecha) return null;
    if (fecha instanceof Date) return fecha;

    if (fecha.includes('/')) {
      const [dia, mes, anio] = fecha.split('/').map(Number);
      return new Date(anio, mes - 1, dia);
    }

    const date = new Date(fecha);
    return isNaN(date.getTime()) ? null : date;
  }
}
