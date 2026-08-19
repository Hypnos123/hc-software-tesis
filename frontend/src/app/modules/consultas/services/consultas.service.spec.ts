import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from '@app/auth/services/auth.service';
import { environment } from 'environments/environment';
import { ConsultaService } from './consultas.service';

describe('ConsultaService conteo de historial', () => {
  let service: ConsultaService; let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule], providers: [
      { provide: AuthService, useValue: { usuario: { idUsuario: 7 } } }
    ] });
    service = TestBed.inject(ConsultaService); http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('reutiliza la búsqueda administrativa y toma atendidas solo del idPaciente solicitado', () => {
    let cantidad = -1; service.getCantidadAtendidasPaciente(6).subscribe(value => cantidad = value);
    const request = http.expectOne(req => req.url === `${environment.URLTienda}api/consultas-medicas/buscar` && req.params.get('criterio') === '6');
    request.flush({ pacientes: [
      { idPaciente: 6, dni: '12345678', consultasAtendidas: 2, consultasPendientes: 5 },
      { idPaciente: 9, dni: '12345678', consultasAtendidas: 8, consultasPendientes: 0 }
    ] });
    expect(cantidad).toBe(2);
  });
});
