import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { HistoriaClinicaService } from '@app/modules/historiaClinica/services/consultas.service';
import { ReporteMedicoService } from '@app/shared/services/reporte-medico.service';
import { of, throwError } from 'rxjs';
import { ReporteConsultasChatComponent } from './reporte-consultas-chat.component';

describe('ReporteConsultasChatComponent', () => {
  let component: ReporteConsultasChatComponent;
  let fixture: ComponentFixture<ReporteConsultasChatComponent>;
  let historias: jasmine.SpyObj<HistoriaClinicaService>;
  let reportes: jasmine.SpyObj<ReporteMedicoService>;
  const paciente = { idPaciente: 8, nombres: 'José', apellidos: 'Muñoz Peña', dni: '12345678', numDocumento: '12345678' };
  const seleccion = { idPaciente: 8, paciente: 'José Muñoz Peña', alcance: 'TODAS' as const,
    totalConsultasEncontradas: 6, consultasAtendidasIncluidas: 4, consultasNoAtendidasExcluidas: 2,
    idsHistoriasClinicasIncluidas: [12, 18], puedeGenerar: true, mensaje: 'Se encontraron 6 consultas.' };

  beforeEach(async () => {
    historias = jasmine.createSpyObj('HistoriaClinicaService', ['buscarPacientesPorDni', 'buscarPacientesPorNombre']);
    reportes = jasmine.createSpyObj('ReporteMedicoService', ['obtenerSeleccion', 'obtenerReporteConsolidado', 'obtenerMensajeError']);
    historias.buscarPacientesPorDni.and.returnValue(of([paciente]));
    historias.buscarPacientesPorNombre.and.returnValue(of([paciente]));
    reportes.obtenerSeleccion.and.returnValue(of(seleccion));
    reportes.obtenerReporteConsolidado.and.returnValue(of({ blob: new Blob(['%PDF'], { type: 'application/pdf' }), nombreArchivo: 'reporte.pdf' }));
    reportes.obtenerMensajeError.and.resolveTo('No se pudo generar el reporte médico.');
    await TestBed.configureTestingModule({ imports: [ReporteConsultasChatComponent], providers: [
      { provide: HistoriaClinicaService, useValue: historias }, { provide: ReporteMedicoService, useValue: reportes }
    ] }).compileComponents();
    fixture = TestBed.createComponent(ReporteConsultasChatComponent); component = fixture.componentInstance; fixture.detectChanges();
  });

  function seleccionarPacientePorDni(): void {
    component.elegirMetodo('DNI'); component.criterio = '12345678'; component.buscar();
  }

  it('busca por DNI y conserva el paciente seleccionado', () => {
    seleccionarPacientePorDni();
    expect(historias.buscarPacientesPorDni).toHaveBeenCalledWith('12345678');
    expect(component.paciente?.idPaciente).toBe(8); expect(component.vista).toBe('alcance');
  });

  it('permite elegir una coincidencia cuando el nombre devuelve varios pacientes', () => {
    historias.buscarPacientesPorNombre.and.returnValue(of([paciente, { ...paciente, idPaciente: 9, dni: '87654321', numDocumento: '87654321' }]));
    component.elegirMetodo('NOMBRE'); component.criterio = 'José'; component.buscar();
    expect(component.vista).toBe('pacientes'); component.seleccionarPaciente(component.pacientes[1]);
    expect(component.paciente?.idPaciente).toBe(9);
  });

  it('muestra un error recuperable cuando el DNI no existe', () => {
    historias.buscarPacientesPorDni.and.returnValue(of([])); seleccionarPacientePorDni();
    expect(component.vista).toBe('error'); expect(component.error).toContain('No se encontró');
  });

  it('consulta ULTIMA y TODAS antes de permitir generar', () => {
    seleccionarPacientePorDni(); component.elegirAlcance('ULTIMA');
    expect(reportes.obtenerSeleccion).toHaveBeenCalledWith(8, { alcance: 'ULTIMA' });
    component.cambiarCriterio(); component.elegirAlcance('TODAS');
    expect(reportes.obtenerSeleccion).toHaveBeenCalledWith(8, { alcance: 'TODAS' });
    expect(component.seleccion?.consultasAtendidasIncluidas).toBe(4);
  });

  it('envía FECHA y RANGO_FECHAS en formato ISO', () => {
    seleccionarPacientePorDni(); component.elegirAlcance('FECHA'); component.fecha = '2026-08-15'; component.consultarFecha();
    expect(reportes.obtenerSeleccion).toHaveBeenCalledWith(8, { alcance: 'FECHA', fecha: '2026-08-15' });
    component.cambiarCriterio(); component.elegirAlcance('RANGO_FECHAS'); component.fechaDesde = '2026-08-01'; component.fechaHasta = '2026-08-31'; component.consultarRango();
    expect(reportes.obtenerSeleccion).toHaveBeenCalledWith(8, { alcance: 'RANGO_FECHAS', fechaDesde: '2026-08-01', fechaHasta: '2026-08-31' });
  });

  it('rechaza un rango invertido sin llamar al backend', () => {
    seleccionarPacientePorDni(); reportes.obtenerSeleccion.calls.reset(); component.elegirAlcance('RANGO_FECHAS'); component.fechaDesde = '2026-09-01'; component.fechaHasta = '2026-08-01'; component.consultarRango();
    expect(component.error).toContain('posterior'); expect(reportes.obtenerSeleccion).not.toHaveBeenCalled();
  });

  it('no genera si puedeGenerar es false', () => {
    reportes.obtenerSeleccion.and.returnValue(of({ ...seleccion, puedeGenerar: false })); seleccionarPacientePorDni(); component.elegirAlcance('TODAS'); component.generarPdf();
    expect(reportes.obtenerReporteConsolidado).not.toHaveBeenCalled();
  });

  it('genera el PDF con exactamente el filtro confirmado y lo emite', () => {
    const emitido = jasmine.createSpy('pdf'); component.pdfGenerado.subscribe(emitido);
    seleccionarPacientePorDni(); component.elegirAlcance('TODAS'); component.generarPdf();
    expect(reportes.obtenerReporteConsolidado).toHaveBeenCalledWith(8, { alcance: 'TODAS' });
    expect(emitido).toHaveBeenCalledWith(jasmine.objectContaining({ nombreArchivo: 'reporte.pdf' }));
  });

  it('maneja errores de selección y generación sin bloquear el flujo', fakeAsync(() => {
    reportes.obtenerSeleccion.and.returnValue(throwError(() => new Error('fallo'))); seleccionarPacientePorDni(); component.elegirAlcance('TODAS'); tick();
    expect(component.vista).toBe('error');
    reportes.obtenerSeleccion.and.returnValue(of(seleccion)); component.vista = 'alcance'; component.elegirAlcance('TODAS');
    reportes.obtenerReporteConsolidado.and.returnValue(throwError(() => new Error('fallo'))); component.generarPdf(); tick();
    expect(component.error).toBe('No se pudo generar el reporte médico.');
  }));

  it('cancela y permite volver al menú Consultas', () => {
    const volver = jasmine.createSpy('volver'); component.volverConsultas.subscribe(volver); component.cancelar(); expect(volver).toHaveBeenCalled();
  });
});
