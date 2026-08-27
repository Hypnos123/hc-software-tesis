import { ComponentFixture, fakeAsync, flushMicrotasks, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, Subject, throwError } from 'rxjs';
import { ReporteConsultaSeleccion, ReportePdfArchivo } from '@app/shared/models/reporte-medico';
import { ReporteMedicoService } from '@app/shared/services/reporte-medico.service';
import { ReporteConsultasDialogComponent } from './reporte-consultas-dialog.component';

describe('ReporteConsultasDialogComponent', () => {
  let fixture: ComponentFixture<ReporteConsultasDialogComponent>;
  let component: ReporteConsultasDialogComponent;
  let service: jasmine.SpyObj<ReporteMedicoService>;

  const seleccion: ReporteConsultaSeleccion = {
    idPaciente: 10, paciente: 'Ana Pérez Gómez', alcance: 'TODAS',
    totalConsultasEncontradas: 6, consultasAtendidasIncluidas: 4,
    consultasNoAtendidasExcluidas: 2, idsHistoriasClinicasIncluidas: [11, 22],
    puedeGenerar: true, mensaje: 'Se encontraron 6 consultas. El reporte incluirá 4 consultas atendidas.'
  };

  beforeEach(async () => {
    service = jasmine.createSpyObj('ReporteMedicoService', [
      'obtenerSeleccion', 'obtenerReporteConsolidado', 'obtenerMensajeError'
    ]);
    await TestBed.configureTestingModule({ imports: [ReporteConsultasDialogComponent, NoopAnimationsModule], providers: [
      { provide: ReporteMedicoService, useValue: service }
    ] }).compileComponents();
    fixture = TestBed.createComponent(ReporteConsultasDialogComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('pacientes', [
      { idPaciente: 10, nombreCompleto: 'Ana Pérez Gómez', dni: '12345678', etiqueta: 'Ana Pérez Gómez · DNI 12345678' }
    ]);
    fixture.componentRef.setInput('visible', true);
    fixture.detectChanges();
    component.idPaciente = 10;
  });

  it('envía el alcance ULTIMA sin fechas', () => {
    service.obtenerSeleccion.and.returnValue(of({ ...seleccion, alcance: 'ULTIMA' }));
    component.alcance = 'ULTIMA';
    component.consultarSeleccion();
    expect(service.obtenerSeleccion).toHaveBeenCalledOnceWith(10, {
      alcance: 'ULTIMA', fecha: undefined, fechaDesde: undefined, fechaHasta: undefined
    });
  });

  it('envía el alcance TODAS sin fechas', () => {
    service.obtenerSeleccion.and.returnValue(of(seleccion));
    component.alcance = 'TODAS';
    component.consultarSeleccion();
    expect(service.obtenerSeleccion).toHaveBeenCalledOnceWith(10, {
      alcance: 'TODAS', fecha: undefined, fechaDesde: undefined, fechaHasta: undefined
    });
  });

  it('FECHA envía el día seleccionado en formato ISO local', () => {
    service.obtenerSeleccion.and.returnValue(of({ ...seleccion, alcance: 'FECHA', fecha: '2026-08-15' }));
    component.alcance = 'FECHA'; component.fecha = new Date(2026, 7, 15);
    component.consultarSeleccion();
    expect(service.obtenerSeleccion).toHaveBeenCalledWith(10, {
      alcance: 'FECHA', fecha: '2026-08-15', fechaDesde: undefined, fechaHasta: undefined
    });
  });

  it('RANGO_FECHAS envía ambos extremos inclusivos', () => {
    service.obtenerSeleccion.and.returnValue(of({ ...seleccion, alcance: 'RANGO_FECHAS' }));
    component.alcance = 'RANGO_FECHAS';
    component.fechaDesde = new Date(2026, 7, 1); component.fechaHasta = new Date(2026, 7, 31);
    component.consultarSeleccion();
    expect(service.obtenerSeleccion).toHaveBeenCalledWith(10, {
      alcance: 'RANGO_FECHAS', fecha: undefined, fechaDesde: '2026-08-01', fechaHasta: '2026-08-31'
    });
  });

  it('rechaza en frontend un rango invertido sin llamar al backend', () => {
    component.alcance = 'RANGO_FECHAS';
    component.fechaDesde = new Date(2026, 7, 31); component.fechaHasta = new Date(2026, 7, 1);
    component.consultarSeleccion();
    expect(service.obtenerSeleccion).not.toHaveBeenCalled();
    expect(component.errorSeleccion).toContain('no puede ser posterior');
  });

  it('consulta primero la selección y muestra conteos antes de generar', () => {
    service.obtenerSeleccion.and.returnValue(of(seleccion));
    component.consultarSeleccion(); fixture.detectChanges();
    expect(service.obtenerSeleccion).toHaveBeenCalled();
    expect(service.obtenerReporteConsolidado).not.toHaveBeenCalled();
    expect(component.seleccion).toBe(seleccion);
    expect(fixture.nativeElement.textContent).toContain('6');
    expect(fixture.nativeElement.textContent).toContain('4');
    expect(fixture.nativeElement.textContent).toContain('2');
  });

  it('puedeGenerar=false bloquea la generación del PDF', () => {
    service.obtenerSeleccion.and.returnValue(of({ ...seleccion, puedeGenerar: false, consultasAtendidasIncluidas: 0 }));
    component.consultarSeleccion();
    component.generarVistaPrevia();
    expect(component.puedeGenerar).toBeFalse();
    expect(service.obtenerReporteConsolidado).not.toHaveBeenCalled();
  });

  it('puedeGenerar=true habilita la vista previa', () => {
    service.obtenerSeleccion.and.returnValue(of(seleccion));
    component.consultarSeleccion();
    expect(component.puedeGenerar).toBeTrue();
  });

  it('genera con exactamente el mismo filtro previamente seleccionado y entrega el PDF al visor', () => {
    const archivo = { blob: new Blob(['%PDF'], { type: 'application/pdf' }), nombreArchivo: 'consolidado.pdf' };
    service.obtenerSeleccion.and.returnValue(of({ ...seleccion, alcance: 'FECHA', fecha: '2026-08-15' }));
    service.obtenerReporteConsolidado.and.returnValue(of(archivo));
    component.alcance = 'FECHA'; component.fecha = new Date(2026, 7, 15);
    component.consultarSeleccion();
    component.generarVistaPrevia();
    expect(service.obtenerReporteConsolidado).toHaveBeenCalledOnceWith(10, {
      alcance: 'FECHA', fecha: '2026-08-15', fechaDesde: undefined, fechaHasta: undefined
    });
    expect(component.reportePdf).toBe(archivo);
    expect(component.mostrarPdf).toBeTrue();
  });

  it('mantiene carga mientras se genera el consolidado', () => {
    const request = new Subject<ReportePdfArchivo>();
    service.obtenerSeleccion.and.returnValue(of(seleccion));
    service.obtenerReporteConsolidado.and.returnValue(request.asObservable());
    component.consultarSeleccion(); component.generarVistaPrevia();
    expect(component.cargandoPdf).toBeTrue();
    expect(component.mostrarPdf).toBeTrue();
  });

  it('muestra un error seguro cuando falla la selección', fakeAsync(() => {
    service.obtenerSeleccion.and.returnValue(throwError(() => ({ status: 400 })));
    service.obtenerMensajeError.and.returnValue(Promise.resolve('Los criterios indicados no son válidos.'));
    component.consultarSeleccion(); flushMicrotasks();
    expect(component.errorSeleccion).toBe('Los criterios indicados no son válidos.');
    expect(component.seleccion).toBeUndefined();
  }));

  it('muestra un error seguro y no conserva PDF cuando falla la generación', fakeAsync(() => {
    service.obtenerSeleccion.and.returnValue(of(seleccion));
    service.obtenerReporteConsolidado.and.returnValue(throwError(() => ({ status: 500 })));
    service.obtenerMensajeError.and.returnValue(Promise.resolve('No se pudo generar el reporte médico.'));
    component.consultarSeleccion(); component.generarVistaPrevia(); flushMicrotasks();
    expect(component.errorPdf).toBe('No se pudo generar el reporte médico.');
    expect(component.reportePdf).toBeUndefined();
    expect(component.mostrarPdf).toBeTrue();
  }));

  it('cerrar limpia selección, filtro y PDF', () => {
    service.obtenerSeleccion.and.returnValue(of(seleccion));
    service.obtenerReporteConsolidado.and.returnValue(of({
      blob: new Blob(['%PDF'], { type: 'application/pdf' }), nombreArchivo: 'reporte.pdf'
    }));
    component.consultarSeleccion(); component.generarVistaPrevia();
    component.cerrar();
    expect(component.dialogVisible).toBeFalse();
    expect(component.seleccion).toBeUndefined();
    expect(component.reportePdf).toBeUndefined();
    expect(component.idPaciente).toBeUndefined();
  });

  it('cambiar el criterio limpia la selección anterior', () => {
    service.obtenerSeleccion.and.returnValue(of(seleccion));
    component.consultarSeleccion();
    component.alcance = 'ULTIMA'; component.criterioCambiado();
    expect(component.seleccion).toBeUndefined();
    expect(component.puedeGenerar).toBeFalse();
  });
});
