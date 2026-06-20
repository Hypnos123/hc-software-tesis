import { Injectable } from '@angular/core';
import {
  ActivatedRouteSnapshot,
  CanActivate,
  Router,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { AuthService } from '../services/auth.service';
import { MensajesSwalService } from '@app/shared/services/mensajes-swal.service';

@Injectable({
  providedIn: 'root',
})
export class PermissionGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private router: Router,
    private readonly servicioMensajesSwal: MensajesSwalService,
  ) { }

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): boolean | UrlTree {
    const permisoRuta = this.normalizarRuta(route.data['permisoRuta'] as string);

    if (!permisoRuta) {
      return true;
    }

    const rutasPermitidas = (this.authService.detallePermisos ?? [])
      .map((permiso) => this.normalizarRuta(permiso?.ruta))
      .filter((ruta) => !!ruta);

    const tienePermiso = rutasPermitidas.includes(permisoRuta);

    if (tienePermiso) {
      return true;
    }

    if (this.normalizarRuta(state.url) !== 'dashboard') {
      this.servicioMensajesSwal.mensajeAdvertencia('No tiene permisos para acceder a esta sección');
    }

    return this.router.createUrlTree(['/dashboard']);
  }

  private normalizarRuta(ruta?: string): string {
    if (!ruta) {
      return '';
    }

    return ruta
      .toString()
      .trim()
      .split('?')[0]
      .split('#')[0]
      .replace(/^\/+|\/+$/g, '')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[\s_-]+/g, '')
      .toLowerCase();
  }
}
