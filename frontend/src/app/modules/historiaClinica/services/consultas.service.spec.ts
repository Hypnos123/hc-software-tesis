import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { AuthService } from '@app/auth/services/auth.service';
import { HistoriaClinicaService } from './consultas.service';

describe('HistoriaClinicaService - historias faltantes', () => {
  let service: HistoriaClinicaService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [{ provide: AuthService, useValue: { usuario: { idUsuario: 7 } } }]
    });
    service = TestBed.inject(HistoriaClinicaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('consulta una sola vez el preview sin solicitar datos personales adicionales', () => {
    const preview = {
      cantidad: 2,
      pacientes: [
        { idPaciente: 1, nombreCompleto: 'Ana Pérez', dniEnmascarado: '******42' },
        { idPaciente: 2, nombreCompleto: 'Luis Soto', dniEnmascarado: '******18' }
      ]
    };

    service.getHistoriasClinicasFaltantes().subscribe(response => expect(response).toEqual(preview));

    const request = http.expectOne(request => request.url.endsWith('historiaClinica/faltantes'));
    expect(request.request.method).toBe('GET');
    expect(request.request.body).toBeNull();
    request.flush(preview);
  });
});
