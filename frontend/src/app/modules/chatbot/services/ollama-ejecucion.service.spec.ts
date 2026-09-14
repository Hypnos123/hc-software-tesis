import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from 'environments/environment';
import { OllamaEjecucionService } from './ollama-ejecucion.service';

describe('OllamaEjecucionService', () => {
  let service: OllamaEjecucionService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(OllamaEjecucionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('envía el contrato esperado a /ollama/ejecutar', () => {
    const respuesta = {
      categoria: 'PACIENTES',
      intencion: 'VERIFICAR_EXISTENCIA',
      encontrado: true,
      dni: '12345678',
      mensaje: 'El paciente se encuentra registrado.'
    };

    service.ejecutar('¿Existe el paciente 12345678?').subscribe(valor => {
      expect(valor).toEqual(respuesta);
    });

    const request = http.expectOne(`${environment.URLTienda}ollama/ejecutar`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ mensaje: '¿Existe el paciente 12345678?' });
    request.flush(respuesta);
  });
});
