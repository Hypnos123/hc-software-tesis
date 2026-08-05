import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from '@app/auth/services/auth.service';
import { PacienteDuplicadoChatService } from './paciente-duplicado-chat.service';
import { environment } from 'environments/environment';


describe('PacienteDuplicadoChatService', () => {
  let service: PacienteDuplicadoChatService;
  let http: HttpTestingController;
  const auth = { usuario: { idUsuario: 7, cargo: 'ADMINISTRADOR' } };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [{ provide: AuthService, useValue: auth }]
    });
    service = TestBed.inject(PacienteDuplicadoChatService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('consulta duplicados por DNI sin enviar datos sensibles', () => {
    service.analizar('12345678').subscribe();
    const request = http.expectOne(`${environment.URLTienda}api/pacientes/duplicados?dni=12345678`);
    expect(request.request.method).toBe('GET');
    expect(request.request.body).toBeNull();
    request.flush({ pacientes: [], razonesRecomendacion: [] });
  });

  it('archiva con X-Usuario-Id y sin agregar usuario ni cargo al body', () => {
    const body = {
      idPacientePrincipal: 2, motivo: 'PACIENTE_DUPLICADO' as const, detalleMotivo: 'Desde chatbot',
      contrasena: 'temporal', confirmarRevisionClinica: false, origen: 'CHATBOT' as const
    };
    service.archivar(1, body).subscribe();
    const request = http.expectOne(`${environment.URLTienda}api/pacientes/1/archivar-duplicado`);
    expect(request.request.method).toBe('POST');
    expect(request.request.headers.get('X-Usuario-Id')).toBe('7');
    expect(request.request.body.idUsuario).toBeUndefined();
    expect(request.request.body.cargo).toBeUndefined();
    expect(request.request.body.origen).toBe('CHATBOT');
    request.flush({ archivado: true, resultado: 'PACIENTE_ARCHIVADO', mensaje: 'OK' });
  });

  it('no llama al backend si no puede identificar al usuario conectado', () => {
    auth.usuario = undefined as any;
    let resultado = '';
    service.archivar(1, {} as any).subscribe({ error: error => resultado = error.error.resultado });
    expect(resultado).toBe('USUARIO_REQUERIDO');
    http.expectNone(`${environment.URLTienda}api/pacientes/1/archivar-duplicado`);
    auth.usuario = { idUsuario: 7, cargo: 'ADMINISTRADOR' };
  });
});
