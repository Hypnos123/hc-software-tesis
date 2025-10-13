import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { tap } from 'rxjs/operators';
import { IAuth, IAuthSuccess } from '../models/auth';
import { StorageService } from '@app/shared/services/storage.service';
import { environment } from 'environments/environment';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private URLServicio: string = environment.URLTienda;
  private _auth: IAuthSuccess | undefined;

  constructor(private http: HttpClient, private storageService: StorageService) { }

  get auth(): IAuthSuccess {
    return this._auth
      ? { ...this._auth! }
      : this.storageService.getItem('token', true);
  }

  get detallePermisos() {
    return this.auth.detallePermisos;
  }

  get usuario() {
    return this.auth.usuario;
  }

  verificarAuth(): Observable<boolean> {
    if (!this.storageService.getItem('token', true)) {
      return of(false);
    }

    return of(true);
  }

  login(header: IAuth): Observable<IAuthSuccess[]> {
    return this.http
      .post<IAuthSuccess[]>(`${this.URLServicio}usuario/getLogin`, header)
      .pipe(
        tap((auth) => {
          this._auth = auth[0];
        }),
        tap((auth: any) => {
          this.storageService.setItem('token', auth[0], true);
        })
      );
  }

  logout(): void {
    this._auth = undefined;
    this.storageService.removeItem('token');
  }
}
