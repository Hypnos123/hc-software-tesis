import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { AdminGuard } from './admin.guard';
import { AuthService } from '../services/auth.service';
import { MensajesSwalService } from '@app/shared/services/mensajes-swal.service';

describe('AdminGuard', () => {
  let guard: AdminGuard;
  let auth: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    auth = jasmine.createSpyObj('AuthService', ['esAdministrador', 'getRutaInicialPermitida']);
    router = jasmine.createSpyObj('Router', ['createUrlTree']);
    auth.getRutaInicialPermitida.and.returnValue('/paciente');
    router.createUrlTree.and.returnValue({} as never);

    TestBed.configureTestingModule({
      providers: [
        AdminGuard,
        { provide: AuthService, useValue: auth },
        { provide: Router, useValue: router },
        { provide: MensajesSwalService, useValue: jasmine.createSpyObj('MensajesSwalService', ['mensajeAdvertencia']) },
      ],
    });
    guard = TestBed.inject(AdminGuard);
  });

  it('permite el acceso al Administrador', () => {
    auth.esAdministrador.and.returnValue(true);
    expect(guard.canActivate()).toBeTrue();
  });

  ['Enfermero', 'Doctor'].forEach((cargo) => {
    it(`impide el acceso directo al ${cargo}`, () => {
      auth.esAdministrador.and.returnValue(false);
      expect(guard.canActivate()).not.toBeTrue();
      expect(router.createUrlTree).toHaveBeenCalledWith(['/paciente']);
    });
  });
});
