import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { StorageService } from '@app/shared/services/storage.service';

import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule], providers: [
      { provide: StorageService, useValue: { getItem: () => null, setItem: () => undefined, removeItem: () => undefined } }
    ] });
    service = TestBed.inject(AuthService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('permite crear consultas solo a administrador y enfermería', () => {
    (service as any)._auth = { usuario: { cargo: 'ENFERMERA' }, detallePermisos: [] };
    expect(service.puedeCrearConsultas()).toBeTrue();
    (service as any)._auth = { usuario: { cargo: 'DOCTOR' }, detallePermisos: [] };
    expect(service.puedeCrearConsultas()).toBeFalse();
    (service as any)._auth = { usuario: { tipoUsuario: 'ADMINISTRADOR' }, detallePermisos: [] };
    expect(service.puedeCrearConsultas()).toBeTrue();
  });
});
