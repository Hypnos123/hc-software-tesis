import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from '@app/auth/services/auth.service';
import { ReporteConsultaFiltro } from '@app/shared/models/reporte-medico';
import { environment } from 'environments/environment';
import { ReporteMedicoService } from './reporte-medico.service';

describe('ReporteMedicoService', () => {
  let service: ReporteMedicoService;
  let http: HttpTestingController;
  const base = `${environment.URLTienda}api/reportes-medicos`;
  const filtro: ReporteConsultaFiltro = { alcance: 'RANGO_FECHAS', fechaDesde: '2026-08-01', fechaHasta: '2026-08-31' };

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule], providers: [
      { provide: AuthService, useValue: { usuario: { idUsuario: 7 } } }
    ] });
    service = TestBed.inject(ReporteMedicoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('solicita la evaluación individual como Blob, envía usuario y recupera el nombre', () => {
    const pdf = new Blob(['%PDF'], { type: 'application/pdf' });
    service.obtenerEvaluacionMedica(31).subscribe(archivo => {
      expect(archivo.blob).toBe(pdf);
      expect(archivo.nombreArchivo).toBe('evaluacion-medica-consulta-31.pdf');
    });
    const request = http.expectOne(`${base}/consultas/31/pdf`);
    expect(request.request.method).toBe('GET');
    expect(request.request.responseType).toBe('blob');
    expect(request.request.headers.get('X-Usuario-Id')).toBe('7');
    request.flush(pdf, { headers: { 'Content-Disposition': 'inline; filename="evaluacion-medica-consulta-31.pdf"' } });
  });

  it('solicita el consolidado como Blob en la URL correcta', () => {
    const pdf = new Blob(['%PDF'], { type: 'application/pdf' });
    service.obtenerReporteConsolidado(10, filtro).subscribe(archivo => expect(archivo.blob).toBe(pdf));
    const request = http.expectOne(`${base}/pacientes/10/consultas/pdf`);
    expect(request.request.method).toBe('POST');
    expect(request.request.responseType).toBe('blob');
    expect(request.request.body).toEqual(filtro);
    expect(request.request.headers.get('X-Usuario-Id')).toBe('7');
    request.flush(pdf, { headers: { 'Content-Disposition': "inline; filename*=UTF-8''reporte-consultas-12345678.pdf" } });
  });

  it('solicita la selección consolidada como JSON', () => {
    service.obtenerSeleccion(10, filtro).subscribe(response => expect(response.totalConsultasEncontradas).toBe(6));
    const request = http.expectOne(`${base}/pacientes/10/consultas/seleccion`);
    expect(request.request.method).toBe('POST');
    expect(request.request.responseType).toBe('json');
    expect(request.request.body).toEqual(filtro);
    request.flush({ idPaciente: 10, alcance: 'RANGO_FECHAS', totalConsultasEncontradas: 6,
      consultasAtendidasIncluidas: 4, consultasNoAtendidasExcluidas: 2,
      idsHistoriasClinicasIncluidas: [11], puedeGenerar: true, mensaje: 'Reporte disponible.' });
  });

  it('usa fallback cuando Content-Disposition no contiene un PDF válido', () => {
    const pdf = new Blob(['%PDF'], { type: 'application/pdf' });
    service.obtenerEvaluacionMedica(31).subscribe(archivo => expect(archivo.nombreArchivo).toBe('reporte-medico.pdf'));
    http.expectOne(`${base}/consultas/31/pdf`).flush(pdf);
  });

  it('rechaza respuestas que no son PDF', () => {
    let error: Error | undefined;
    service.obtenerEvaluacionMedica(31).subscribe({ error: value => error = value });
    http.expectOne(`${base}/consultas/31/pdf`).flush(new Blob(['html'], { type: 'text/html' }));
    expect(error?.message).toContain('no es un PDF válido');
  });
});
