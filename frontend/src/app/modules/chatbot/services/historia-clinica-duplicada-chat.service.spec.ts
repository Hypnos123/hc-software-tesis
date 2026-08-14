import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from 'environments/environment';
import { HistoriaClinicaDuplicadaChatService } from './historia-clinica-duplicada-chat.service';
import { AuthService } from '@app/auth/services/auth.service';

describe('HistoriaClinicaDuplicadaChatService', () => {
  let service: HistoriaClinicaDuplicadaChatService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule], providers: [{ provide: AuthService, useValue: { usuario: { idUsuario: 7 } } }] });
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

  it('envía usuario y snapshot al endpoint destructivo', () => {
    const body: any = { idHistoriaPrincipal: 7, contrasena: 'clave', confirmacion: true };
    service.fusionar(8, body).subscribe();
    const request = http.expectOne(`${environment.URLTienda}api/historias-clinicas/8/fusionar`);
    expect(request.request.method).toBe('POST');
    expect(request.request.headers.get('X-Usuario-Id')).toBe('7');
    expect(request.request.body).toBe(body);
    request.flush({ fusionada: true, resultado: 'HISTORIAS_FUSIONADAS', mensaje: 'OK' });
  });
});
