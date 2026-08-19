import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from '@app/auth/services/auth.service';
import { ResumenConsultasPacienteService } from './resumen-consultas-paciente.service';
import { environment } from 'environments/environment';

describe('ResumenConsultasPacienteService', () => {
  let service: ResumenConsultasPacienteService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule], providers: [
      { provide: AuthService, useValue: { usuario: { idUsuario: 7, tipoUsuario: 'DOCTOR' } } }
    ] });
    service = TestBed.inject(ResumenConsultasPacienteService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('uses the existing summary endpoint once with X-Usuario-Id', () => {
    service.obtener(6).subscribe();
    const request = http.expectOne(`${environment.URLTienda}api/consultas-medicas/pacientes/6/resumen`);
    expect(request.request.method).toBe('GET');
    expect(request.request.headers.get('X-Usuario-Id')).toBe('7');
    request.flush({});
  });
});
