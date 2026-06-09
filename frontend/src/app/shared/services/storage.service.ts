import { Injectable } from '@angular/core';
import { LocaleUtil } from '@app/global/locale.utils';


@Injectable({
  providedIn: 'root'
})
export class StorageService {

  constructor(private util: LocaleUtil) { }

  setItem(key: string, value?: any, toEncrypt: boolean = false): void {
    try {
      var fixedValue = value;
      if(this.util.isObjectOrArray(value))  fixedValue = JSON.stringify(value)
      const encodedString = toEncrypt? btoa(fixedValue) : fixedValue;
      localStorage.setItem(key, encodedString);
    } catch (error) {
      console.error('Error al guardar en localStorage:', error);
    }
  }

  getItem(key: string, isEncrypted: boolean = false) {
    try {
      const encodedString = localStorage.getItem(key);
      if (!encodedString) return null;
      const fixedValue = isEncrypted ? atob(encodedString) : encodedString;

      try {
        return JSON.parse(fixedValue);
      } catch {
        return fixedValue;
      }
    } catch (error) {
      console.error('Error al obtener desde localStorage:', error);
      return null;
    }
  }

   // Método para eliminar un objeto del localStorage
  removeItem(key: string): void {
    try {
      localStorage.removeItem(key);
    } catch (error) {
      console.error('Error al eliminar del localStorage:', error);
    }
  }

  // Método para limpiar todo el localStorage
  clear(): void {
    try {
      localStorage.clear();
    } catch (error) {
      console.error('Error al limpiar localStorage:', error);
    }
  }
}
