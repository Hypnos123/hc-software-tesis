import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from 'environments/environment';
import { HistoriaClinicaDuplicadaChatService } from './historia-clinica-duplicada-chat.service';

describe('HistoriaClinicaDuplicadaChatService', () => {
  let service: HistoriaClinicaDuplicadaChatService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(HistoriaClinicaDuplicadaChatService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('consulta la detección existente', () => {
    service.detectar().subscribe();
    const request = http.expectOne(`${environment.URLTienda}api/historias-clinicas/duplicados`);
    expect(request.request.method).toBe('GET');
    request.flush({ hayDuplicados: false, totalGrupos: 0, duplicados: [], mensaje: 'Sin duplicados' });
  });

  it('envía IDs explícitos al análisis enriquecido', () => {
    service.analizar([7, 8]).subscribe();
    const request = http.expectOne(`${environment.URLTienda}api/historias-clinicas/duplicados/analizar`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ idsHistoriasClinicas: [7, 8] });
    request.flush({});
  });
});
