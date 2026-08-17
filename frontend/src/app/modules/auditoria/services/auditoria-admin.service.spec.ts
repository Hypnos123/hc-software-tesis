import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from '@app/auth/services/auth.service';
import { environment } from 'environments/environment';
import { AuditoriaAdminService } from './auditoria-admin.service';

describe('AuditoriaAdminService', () => {
  let service: AuditoriaAdminService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule],
      providers: [{ provide: AuthService, useValue: { usuario: { idUsuario: 7, cargo: 'ADMINISTRADOR' } } }] });
    service = TestBed.inject(AuditoriaAdminService); http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('consulta el listado paginado con filtros y X-Usuario-Id', () => {
    service.listarPacientesArchivados({ page: 1, size: 25, search: 'Ana', dni: '12345678' }).subscribe();
    const request = http.expectOne((req) => req.url === `${environment.URLTienda}api/admin/pacientes-archivados`);
    expect(request.request.method).toBe('GET'); expect(request.request.headers.get('X-Usuario-Id')).toBe('7');
    expect(request.request.params.get('page')).toBe('1'); expect(request.request.params.get('search')).toBe('Ana');
    request.flush({ content: [], page: 1, size: 25, totalElements: 0, totalPages: 0 });
  });

  it('consulta el detalle tipado', () => {
    service.obtenerPacienteArchivado(13).subscribe();
    const request = http.expectOne(`${environment.URLTienda}api/admin/pacientes-archivados/13`);
    expect(request.request.headers.get('X-Usuario-Id')).toBe('7'); request.flush({ idPaciente: 13 });
  });
});
