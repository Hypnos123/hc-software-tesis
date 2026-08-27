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
  const paciente = { idPaciente: 8, nombres: 'José', apellidos: 'Muñoz Peña', dni: '12345678', numDocumento: '12345678', fechaIngreso: '2026-08-14' };
  const seleccion = { idPaciente: 8, paciente: 'José Muñoz Peña', alcance: 'TODAS' as const,
    totalConsultasEncontradas: 6, consultasAtendidasIncluidas: 4, consultasNoAtendidasExcluidas: 2,
    idsHistoriasClinicasIncluidas: [12, 18], puedeGenerar: true, mensaje: 'Se encontraron 6 consultas.' };

  beforeEach(async () => {
    historias = jasmine.createSpyObj('HistoriaClinicaService', ['buscarPacientesPorDni', 'buscarPacientesPorNombre', 'getByPaciente']);
    reportes = jasmine.createSpyObj('ReporteMedicoService', ['obtenerSeleccion', 'obtenerReporteConsolidado', 'obtenerMensajeError']);
    historias.buscarPacientesPorDni.and.returnValue(of([paciente]));
    historias.buscarPacientesPorNombre.and.returnValue(of([paciente]));
    historias.getByPaciente.and.returnValue(of([{ idHistoriaClinica: 12, cantidadConsultas: 4 }]));
    reportes.obtenerSeleccion.and.returnValue(of(seleccion));
    reportes.obtenerReporteConsolidado.and.returnValue(of({ blob: new Blob(['%PDF'], { type: 'application/pdf' }), nombreArchivo: 'reporte.pdf' }));
    reportes.obtenerMensajeError.and.resolveTo('No se pudo generar el reporte médico.');
    await TestBed.configureTestingModule({ imports: [ReporteConsultasChatComponent], providers: [
      { provide: HistoriaClinicaService, useValue: historias }, { provide: ReporteMedicoService, useValue: reportes }
    ] }).compileComponents();
    fixture = TestBed.createComponent(ReporteConsultasChatComponent); component = fixture.componentInstance; fixture.detectChanges();
    spyOn<any>(component, 'demoraAleatoria').and.returnValue(3000);
  });

  function seleccionarPacientePorDni(): void {
    component.elegirMetodo('DNI'); component.criterio = '12345678'; component.buscar(); tick(3000);
  }

  it('busca por DNI y conserva el paciente seleccionado después del spinner mínimo', fakeAsync(() => {
    component.elegirMetodo('DNI'); component.criterio = '12345678'; component.buscar();
    expect(component.cargando).toBeTrue(); tick(2999); expect(component.paciente).toBeUndefined(); tick(1);
    expect(historias.buscarPacientesPorDni).toHaveBeenCalledWith('12345678');
    expect(component.paciente?.idPaciente).toBe(8); expect(component.vista).toBe('alcance');
  }));

  it('muestra solo las opciones de búsqueda sin repetir la pregunta del chatbot', () => {
    fixture.detectChanges();
    const texto = fixture.nativeElement.textContent;
    expect(texto).not.toContain('¿Cómo deseas buscar al paciente?');
    expect(texto).toContain('Buscar por DNI'); expect(texto).toContain('Buscar por nombre');
  });

  it('muestra el spinner y el mensaje de búsqueda por DNI hasta completar la espera', fakeAsync(() => {
    component.elegirMetodo('DNI'); component.criterio = '12345678'; component.buscar(); fixture.detectChanges();
    const carga = fixture.nativeElement.querySelector('.loading');
    expect(carga.textContent).toContain('Buscando paciente...');
    expect(carga.querySelector('.pi.pi-spin.pi-spinner')).not.toBeNull();
    tick(2999); fixture.detectChanges(); expect(fixture.nativeElement.querySelector('.loading')).not.toBeNull();
    tick(1); fixture.detectChanges(); expect(fixture.nativeElement.querySelector('.loading')).toBeNull();
  }));

  it('muestra el spinner y el mensaje de búsqueda por nombre hasta completar la espera', fakeAsync(() => {
    component.elegirMetodo('NOMBRE'); component.criterio = 'José'; component.buscar(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.loading').textContent).toContain('Buscando paciente...');
    tick(3000); fixture.detectChanges(); expect(fixture.nativeElement.querySelector('.loading')).toBeNull();
    expect(component.paciente?.idPaciente).toBe(8);
  }));

  it('enriquece y permite elegir una coincidencia cuando el nombre devuelve varios pacientes', fakeAsync(() => {
    historias.buscarPacientesPorNombre.and.returnValue(of([paciente, { ...paciente, idPaciente: 9, dni: '87654321', numDocumento: '87654321' }]));
    component.elegirMetodo('NOMBRE'); component.criterio = 'José'; component.buscar(); tick(3000);
    expect(component.vista).toBe('pacientes'); component.seleccionarPaciente(component.pacientes[1]);
    expect(component.pacientes[0].cantidadConsultas).toBe(4); expect(component.paciente?.idPaciente).toBe(9);
  }));

  it('presenta ID, fecha de registro y cantidad de consultas en pacientes repetidos', fakeAsync(() => {
    historias.buscarPacientesPorNombre.and.returnValue(of([paciente, { ...paciente, idPaciente: 9 }]));
    component.elegirMetodo('NOMBRE'); component.criterio = 'José'; component.buscar(); tick(3000); fixture.detectChanges();
    const texto = fixture.nativeElement.textContent;
    expect(texto).toContain('Paciente ID 8'); expect(texto).toContain('Registrado: 14/08/2026'); expect(texto).toContain('Consultas: 4');
  }));

  it('muestra un error recuperable cuando el DNI no existe', fakeAsync(() => {
    historias.buscarPacientesPorDni.and.returnValue(of([])); seleccionarPacientePorDni();
    expect(component.vista).toBe('error'); expect(component.error).toContain('No se encontró');
  }));

  it('consulta ULTIMA y TODAS después de la espera y antes de permitir generar', fakeAsync(() => {
    seleccionarPacientePorDni(); component.elegirAlcance('ULTIMA');
    expect(component.cargando).toBeTrue(); tick(3000);
    expect(reportes.obtenerSeleccion).toHaveBeenCalledWith(8, { alcance: 'ULTIMA' });
    component.cambiarCriterio(); component.elegirAlcance('TODAS'); tick(3000);
    expect(reportes.obtenerSeleccion).toHaveBeenCalledWith(8, { alcance: 'TODAS' });
    expect(component.seleccion?.consultasAtendidasIncluidas).toBe(4);
  }));

  it('muestra la preparación del reporte para ULTIMA, TODAS, FECHA y RANGO_FECHAS', fakeAsync(() => {
    seleccionarPacientePorDni();
    const verificarCarga = (): void => {
      fixture.detectChanges();
      const carga = fixture.nativeElement.querySelector('.loading');
      expect(carga.textContent).toContain('Preparando información del reporte...');
      expect(carga.querySelector('.pi.pi-spin.pi-spinner')).not.toBeNull();
      tick(3000); fixture.detectChanges(); expect(fixture.nativeElement.querySelector('.loading')).toBeNull();
      component.cambiarCriterio();
    };
    component.elegirAlcance('ULTIMA'); verificarCarga();
    component.elegirAlcance('TODAS'); verificarCarga();
    component.elegirAlcance('FECHA'); component.fecha = '2026-08-15'; component.consultarFecha(); verificarCarga();
    component.elegirAlcance('RANGO_FECHAS'); component.fechaDesde = '2026-08-01'; component.fechaHasta = '2026-08-31'; component.consultarRango(); verificarCarga();
  }));

  it('calcula una demora aleatoria inclusiva entre tres y seis segundos', () => {
    (component as any).demoraAleatoria.and.callThrough();
    for (let intento = 0; intento < 100; intento++) {
      const demora = (component as any).demoraAleatoria();
      expect(demora).toBeGreaterThanOrEqual(3000); expect(demora).toBeLessThanOrEqual(6000);
    }
  });

  it('envía FECHA y RANGO_FECHAS en formato ISO', fakeAsync(() => {
    seleccionarPacientePorDni(); component.elegirAlcance('FECHA'); component.fecha = '2026-08-15'; component.consultarFecha(); tick(3000);
    expect(reportes.obtenerSeleccion).toHaveBeenCalledWith(8, { alcance: 'FECHA', fecha: '2026-08-15' });
    component.cambiarCriterio(); component.elegirAlcance('RANGO_FECHAS'); component.fechaDesde = '2026-08-01'; component.fechaHasta = '2026-08-31'; component.consultarRango(); tick(3000);
    expect(reportes.obtenerSeleccion).toHaveBeenCalledWith(8, { alcance: 'RANGO_FECHAS', fechaDesde: '2026-08-01', fechaHasta: '2026-08-31' });
  }));

  it('rechaza un rango invertido sin llamar al backend', fakeAsync(() => {
    seleccionarPacientePorDni(); reportes.obtenerSeleccion.calls.reset(); component.elegirAlcance('RANGO_FECHAS'); component.fechaDesde = '2026-09-01'; component.fechaHasta = '2026-08-01'; component.consultarRango();
    expect(component.error).toContain('posterior'); expect(reportes.obtenerSeleccion).not.toHaveBeenCalled();
  }));

  it('no genera si puedeGenerar es false', fakeAsync(() => {
    reportes.obtenerSeleccion.and.returnValue(of({ ...seleccion, puedeGenerar: false })); seleccionarPacientePorDni(); component.elegirAlcance('TODAS'); tick(3000); component.generarPdf();
    expect(reportes.obtenerReporteConsolidado).not.toHaveBeenCalled();
  }));

  it('genera el PDF con exactamente el filtro confirmado y lo emite', fakeAsync(() => {
    const emitido = jasmine.createSpy('pdf'); component.pdfGenerado.subscribe(emitido);
    seleccionarPacientePorDni(); component.elegirAlcance('TODAS'); tick(3000); component.generarPdf();
    expect(reportes.obtenerReporteConsolidado).toHaveBeenCalledWith(8, { alcance: 'TODAS' });
    expect(emitido).toHaveBeenCalledWith(jasmine.objectContaining({ nombreArchivo: 'reporte.pdf' }));
  }));

  it('maneja errores de selección y generación sin bloquear el flujo', fakeAsync(() => {
    reportes.obtenerSeleccion.and.returnValue(throwError(() => new Error('fallo'))); seleccionarPacientePorDni(); component.elegirAlcance('TODAS'); tick(3000); tick();
    expect(component.vista).toBe('error');
    reportes.obtenerSeleccion.and.returnValue(of(seleccion)); component.vista = 'alcance'; component.elegirAlcance('TODAS'); tick(3000);
    reportes.obtenerReporteConsolidado.and.returnValue(throwError(() => new Error('fallo'))); component.generarPdf(); tick();
    expect(component.error).toBe('No se pudo generar el reporte médico.');
  }));

  it('cancela y permite volver al menú Consultas', () => {
    const volver = jasmine.createSpy('volver'); component.volverConsultas.subscribe(volver); component.cancelar(); expect(volver).toHaveBeenCalled();
  });
});
