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

  it('envía únicamente una copia de los ids confirmados al coordinador masivo', () => {
    const ids = [1, 3];
    const response = { totalSolicitados: 2, totalProcesados: 2, creadas: 2, omitidas: 0,
      noEncontrados: 0, inactivos: 0, errores: 0, resultados: [] };

    service.crearHistoriasClinicasFaltantes(ids).subscribe(resultado => expect(resultado).toEqual(response));
    ids.push(99);

    const request = http.expectOne(request => request.url.endsWith('historiaClinica/faltantes/crear'));
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ idsPacientes: [1, 3] });
    expect(Object.keys(request.request.body)).toEqual(['idsPacientes']);
    request.flush(response);
  });

  it('envía X-Usuario-Id al crear una historia clínica', () => {
    service.insert({} as any).subscribe();

    const request = http.expectOne(request => request.url.endsWith('historiaClinica/insert'));
    expect(request.request.method).toBe('POST');
    expect(request.request.headers.get('X-Usuario-Id')).toBe('7');
    request.flush({ idGenerado: 1 });
  });

  it('envía X-Usuario-Id al visualizar el detalle de una consulta', () => {
    service.getConsultaById(12).subscribe(response => expect(response?.idConsulta).toBe(12));

    const request = http.expectOne(request => request.url.endsWith('consulta/findById/12'));
    expect(request.request.method).toBe('GET');
    expect(request.request.headers.get('X-Usuario-Id')).toBe('7');
    request.flush({ data: [{ idConsulta: 12 }] });
  });
});
