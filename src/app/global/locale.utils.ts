import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class LocaleUtil {
  constructor() {
  }

  getItem(param: string) {
    return JSON.parse(localStorage.getItem(param) as string);
  }

  setItem(title: string, param: any) {
    if (typeof param === 'string') {
      return localStorage.setItem(title, param);
    }

    return localStorage.setItem(title, JSON.stringify(param));
  }

  removeItem(param: string) {
    return localStorage.removeItem(param);
  }

  isObjectOrArray(value: any): boolean {
    return (
      value !== null &&
      (typeof value === 'object' || Array.isArray(value))
    );
  }

  calcularEdad(fechaNacimiento: string): string {
    const nacimiento = new Date(fechaNacimiento);
    const hoy = new Date();

    let años = hoy.getFullYear() - nacimiento.getFullYear();
    let meses = hoy.getMonth() - nacimiento.getMonth();
    let días = hoy.getDate() - nacimiento.getDate();

    if (días < 0) {
      const ultimoDiaMesAnterior = new Date(hoy.getFullYear(), hoy.getMonth(), 0).getDate();
      días += ultimoDiaMesAnterior;
      meses--; // Ajustamos los meses después de compensar los días
    }

    if (meses < 0) {
      años--;
      meses += 12;
    }

    // Devolver solo la unidad relevante
    if (años > 0) {
      return `${años} ${años === 1 ? 'año' : 'años'}`;
    } else if (meses > 0) {
      return `${meses} ${meses === 1 ? 'mes' : 'meses'}`;
    } else {
      return `${días} ${días === 1 ? 'día' : 'días'}`;
    }
  }

}
