import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { PacienteImportacionService } from './paciente-importacion.service';


describe('PacienteImportacionService', () => {
  let service: PacienteImportacionService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(PacienteImportacionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('descarga la plantilla como Blob y recupera el nombre del header', () => {
    const blob = new Blob(['excel'], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
    service.descargarPlantilla().subscribe(response => {
      expect(response.body).toBe(blob);
      expect(service.obtenerNombreArchivo(response)).toBe('plantilla-pacientes-v1.0.xlsx');
    });

    const request = http.expectOne(req => req.url.endsWith('/paciente/importacion/plantilla'));
    expect(request.request.responseType).toBe('blob');
    request.flush(blob, { headers: { 'Content-Disposition': 'attachment; filename="plantilla-pacientes-v1.0.xlsx"' } });
  });

  it('usa el nombre predeterminado cuando Content-Disposition no está disponible', () => {
    const response = new HttpResponse({ body: new Blob(), headers: new HttpHeaders() });
    expect(service.obtenerNombreArchivo(response)).toBe('plantilla-importacion-pacientes-v1.0.xlsx');
  });

  it('envía el archivo en FormData con la clave exacta archivo', () => {
    const archivo = new File(['contenido'], 'pacientes.xlsx');
    service.validarArchivo(archivo).subscribe();

    const request = http.expectOne(req => req.url.endsWith('/paciente/importacion/validar'));
    expect(request.request.method).toBe('POST');
    expect(request.request.body instanceof FormData).toBeTrue();
    expect(request.request.body.get('archivo')).toBe(archivo);
    request.flush({ importacionId: 'id', estado: 'PREVISUALIZADA', expiraEn: '', resumen: {}, filas: [] });
  });

  it('consulta y confirma usando el identificador sin enviar datos de pacientes', () => {
    service.obtenerPrevisualizacion('abc').subscribe();
    http.expectOne(req => req.url.endsWith('/paciente/importacion/abc') && req.method === 'GET').flush({});

    service.confirmarImportacion('abc').subscribe();
    const confirmar = http.expectOne(req => req.url.endsWith('/paciente/importacion/abc/confirmar'));
    expect(confirmar.request.method).toBe('POST');
    expect(confirmar.request.body).toBeNull();
    confirmar.flush({});
  });
});
