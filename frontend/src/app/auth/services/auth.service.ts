import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { IAuth, IAuthSuccess, IResponseModelGet } from '../models/auth';
import { StorageService } from '@app/shared/services/storage.service';
import { environment } from 'environments/environment';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private URLServicio: string = environment.URLTienda;
  private _auth: IAuthSuccess | undefined;

  constructor(private http: HttpClient, private storageService: StorageService) { }

  get auth(): IAuthSuccess | null {
    return this._auth
      ? { ...this._auth! }
      : this.storageService.getItem('token', true);
  }

  get detallePermisos() {
    return this.auth?.detallePermisos ?? [];
  }

  get usuario() {
    return this.auth?.usuario;
  }

  verificarAuth(): Observable<boolean> {
    return of(this.isValidAuth(this.auth));
  }

  login(header: IAuth): Observable<IAuthSuccess | null> {
    return this.http.post<IResponseModelGet<IAuthSuccess> | IAuthSuccess[]>(`${this.URLServicio}usuario/getLogin`, header)
      .pipe(
        map((response) => this.getAuthData(response)),
        tap((auth) => {
          if (auth?.usuario) {
            this._auth = auth;
            this.storageService.setItem('token', auth, true);
          }
        }),
      );
  }

  logout(): void {
    this._auth = undefined;
    this.storageService.removeItem('token');
  }

  private isValidAuth(auth: IAuthSuccess | null): boolean {
    return !!auth?.usuario?.idUsuario;
  }

  private getAuthData(response: IResponseModelGet<IAuthSuccess> | IAuthSuccess[] | null): IAuthSuccess | null {
    const data = Array.isArray(response) ? response : response?.data;
    const auth = data?.[0];

    if (!auth?.usuario) {
      return null;
    }

    return {
      ...auth,
      detallePermisos: auth.detallePermisos ?? [],
    };
  }
}
