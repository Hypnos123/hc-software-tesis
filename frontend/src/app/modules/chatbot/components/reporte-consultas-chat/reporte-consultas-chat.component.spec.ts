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

  it('busca por DNI y conserva el paciente seleccionado después de la espera mínima', fakeAsync(() => {
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

  it('emite el mensaje temporal de búsqueda por DNI hasta completar la espera', fakeAsync(() => {
    const cargas: Array<string | null> = []; component.cargaTemporal.subscribe(carga => cargas.push(carga));
    component.elegirMetodo('DNI'); component.criterio = '12345678'; component.buscar();
    expect(cargas).toEqual(['Buscando paciente…']);
    tick(2999); expect(cargas).toEqual(['Buscando paciente…']);
    tick(1); expect(cargas).toEqual(['Buscando paciente…', null]);
  }));

  it('emite el mensaje temporal de búsqueda por nombre hasta completar la espera', fakeAsync(() => {
    const cargas: Array<string | null> = []; component.cargaTemporal.subscribe(carga => cargas.push(carga));
    component.elegirMetodo('NOMBRE'); component.criterio = 'José'; component.buscar();
    expect(cargas).toEqual(['Buscando paciente…']); tick(3000);
    expect(cargas).toEqual(['Buscando paciente…', null]);
    expect(component.paciente?.idPaciente).toBe(8);
  }));

  it('renderiza un spinner durante la búsqueda y lo retira al finalizar', fakeAsync(() => {
    component.elegirMetodo('DNI'); component.criterio = '12345678'; component.buscar(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.report-loading .pi.pi-spin.pi-spinner')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.report-loading')?.textContent).toContain('Buscando paciente…');
    tick(3000); fixture.detectChanges(); expect(fixture.nativeElement.querySelector('.report-loading')).toBeNull();
  }));

  it('enriquece y permite elegir una coincidencia cuando el nombre devuelve varios pacientes', fakeAsync(() => {
    historias.buscarPacientesPorNombre.and.returnValue(of([paciente, { ...paciente, idPaciente: 9, dni: '87654321', numDocumento: '87654321' }]));
    component.elegirMetodo('NOMBRE'); component.criterio = 'José'; component.buscar(); tick(3000);
    expect(component.vista).toBe('pacientes'); component.seleccionarPaciente(component.pacientes[1]);
    expect(component.pacientes[0].cantidadConsultasAtendidas).toBe(4); expect(component.paciente?.idPaciente).toBe(9);
  }));

  it('presenta DNI, historias y consultas atendidas en pacientes repetidos', fakeAsync(() => {
    historias.buscarPacientesPorNombre.and.returnValue(of([paciente, { ...paciente, idPaciente: 9 }]));
    component.elegirMetodo('NOMBRE'); component.criterio = 'José'; component.buscar(); tick(3000); fixture.detectChanges();
    const texto = fixture.nativeElement.textContent;
    expect(texto).toContain('DNI: 12345678'); expect(texto).toContain('Historias clínicas: 1'); expect(texto).toContain('Consultas atendidas: 4');
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

  it('emite la preparación temporal del reporte para ULTIMA, TODAS, FECHA y RANGO_FECHAS', fakeAsync(() => {
    seleccionarPacientePorDni();
    const cargas: Array<string | null> = []; component.cargaTemporal.subscribe(carga => cargas.push(carga));
    const verificarCarga = (): void => {
      expect(cargas).toEqual(['Preparando información del reporte…']);
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('.report-loading .pi.pi-spin.pi-spinner')).not.toBeNull();
      expect(fixture.nativeElement.querySelector('.report-loading')?.textContent).toContain('Preparando información del reporte…');
      tick(2999); expect(cargas).toEqual(['Preparando información del reporte…']);
      tick(1); expect(cargas).toEqual(['Preparando información del reporte…', null]);
      cargas.length = 0;
      component.cambiarCriterio();
    };
    component.elegirAlcance('ULTIMA'); verificarCarga();
    component.elegirAlcance('TODAS'); verificarCarga();
    component.elegirAlcance('FECHA'); component.fecha = '2026-08-15'; component.consultarFecha(); verificarCarga();
    component.elegirAlcance('RANGO_FECHAS'); component.fechaDesde = '2026-08-01'; component.fechaHasta = '2026-08-31'; component.consultarRango(); verificarCarga();
  }));

  it('muestra directamente el formulario sin emitir mensajes redundantes', () => {
    const mensajes = jasmine.createSpy('mensajes'); component.avanzarFlujo.subscribe(mensajes);
    component.elegirMetodo('DNI'); fixture.detectChanges();
    expect(mensajes).not.toHaveBeenCalled(); expect(fixture.nativeElement.textContent).toContain('DNI del paciente');
    component.buscarOtroPaciente(); component.elegirMetodo('NOMBRE'); fixture.detectChanges();
    expect(mensajes).not.toHaveBeenCalled(); expect(fixture.nativeElement.textContent).toContain('Nombre del paciente');
    expect(fixture.nativeElement.textContent).not.toContain('DNI del paciente');
  });

  it('bloquea los alcances cuando el paciente no tiene consultas atendidas', fakeAsync(() => {
    reportes.obtenerSeleccion.and.returnValue(of({ ...seleccion, consultasAtendidasIncluidas: 0, puedeGenerar: false }));
    seleccionarPacientePorDni(); fixture.detectChanges();
    expect(component.vista).toBe('sin-consultas');
    expect(fixture.nativeElement.textContent).toContain('no cuenta con consultas atendidas disponibles');
    expect(fixture.nativeElement.textContent).not.toContain('Última consulta atendida');
    expect(fixture.nativeElement.textContent).toContain('Historias clínicas: 1');
  }));

  it('limpia la carga y cancela la solicitud al salir del flujo', fakeAsync(() => {
    const cargas: Array<string | null> = []; component.cargaTemporal.subscribe(carga => cargas.push(carga));
    component.elegirMetodo('DNI'); component.criterio = '12345678'; component.buscar(); component.cancelar(); tick(6000);
    expect(component.cargando).toBeFalse(); expect(cargas).toEqual(['Buscando paciente…', null]);
    expect(component.paciente).toBeUndefined();
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
    seleccionarPacientePorDni(); reportes.obtenerSeleccion.and.returnValue(throwError(() => new Error('fallo'))); component.elegirAlcance('TODAS'); tick(3000); tick();
    expect(component.vista).toBe('error');
    reportes.obtenerSeleccion.and.returnValue(of(seleccion)); component.vista = 'alcance'; component.elegirAlcance('TODAS'); tick(3000);
    reportes.obtenerReporteConsolidado.and.returnValue(throwError(() => new Error('fallo'))); component.generarPdf(); tick();
    expect(component.error).toBe('No se pudo generar el reporte médico.');
  }));

  it('cancela y permite volver al menú Consultas', () => {
    const volver = jasmine.createSpy('volver'); component.volverConsultas.subscribe(volver); component.cancelar(); expect(volver).toHaveBeenCalled();
  });
});
