import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { MensajesSwalService } from '@app/shared/services/mensajes-swal.service';

@Injectable({ providedIn: 'root' })
export class AdminGuard implements CanActivate {
  constructor(
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly mensajes: MensajesSwalService,
  ) {}

  canActivate(): boolean | UrlTree {
    if (this.authService.esAdministrador()) {
      return true;
    }

    this.mensajes.mensajeAdvertencia('Esta sección es exclusiva para Administradores');
    return this.router.createUrlTree([this.authService.getRutaInicialPermitida()]);
  }
}
