import { ComponentFixture, fakeAsync, flushMicrotasks, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { AuthService } from '@app/auth/services/auth.service';
import { MensajesSwalService } from '@app/shared/services/mensajes-swal.service';
import { ChatbotNavigationService } from '@app/modules/chatbot/services/chatbot-navigation.service';
import { ConsultaService } from '../../services/consultas.service';
import { ConsultasComponent } from './consultas.component';
import { ReporteMedicoService } from '@app/shared/services/reporte-medico.service';
import { ReportePdfArchivo } from '@app/shared/models/reporte-medico';

describe('ConsultasComponent historial previo', () => {
  let component: ConsultasComponent;
  let fixture: ComponentFixture<ConsultasComponent>;
  let consultaService: jasmine.SpyObj<ConsultaService>;
  let router: jasmine.SpyObj<Router>;
  let chatbotNavigation: jasmine.SpyObj<ChatbotNavigationService>;
  let reporteMedicoService: jasmine.SpyObj<ReporteMedicoService>;

  beforeEach(async () => {
    consultaService = jasmine.createSpyObj('ConsultaService', ['getAllActivos', 'getCantidadAtendidasPaciente']);
    consultaService.getAllActivos.and.returnValue(of([]));
    router = jasmine.createSpyObj('Router', ['navigate']);
    chatbotNavigation = jasmine.createSpyObj('ChatbotNavigationService', ['abrirResumenConsultas']);
    reporteMedicoService = jasmine.createSpyObj('ReporteMedicoService', ['obtenerEvaluacionMedica', 'obtenerMensajeError']);
    await TestBed.configureTestingModule({ imports: [ConsultasComponent], providers: [
      { provide: ConsultaService, useValue: consultaService }, { provide: Router, useValue: router },
      { provide: ChatbotNavigationService, useValue: chatbotNavigation },
      { provide: ReporteMedicoService, useValue: reporteMedicoService },
      { provide: AuthService, useValue: { usuario: { idUsuario: 7, tipoUsuario: 'DOCTOR', cargo: 'DOCTOR' } } },
      { provide: MensajesSwalService, useValue: jasmine.createSpyObj('MensajesSwalService', ['mensajeError']) }
    ] }).compileComponents();
    fixture = TestBed.createComponent(ConsultasComponent); component = fixture.componentInstance; fixture.detectChanges();
  });

  const row = { id: 12, idPaciente: 6, paciente: { nombres: 'Ana', apellidos: 'Paz' }, consultasAtendidas: 0, estado: 'Por atender' } as any;
  const atendida = { ...row, id: 21, estado: 'Atendido' } as any;

  it('reutiliza el conteo precargado por idPaciente al abrir el único modal', () => {
    component.abrirConfirmacionAtencion({ ...row, consultasAtendidas: 2 });
    expect(component.mostrarConfirmacionAtencion).toBeTrue();
    expect(component.cantidadConsultasAtendidas).toBe(2);
    expect(consultaService.getCantidadAtendidasPaciente).not.toHaveBeenCalled();
  });

  it('muestra el modal simple cuando no existen consultas atendidas', () => {
    component.abrirConfirmacionAtencion(row); fixture.detectChanges();
    expect(component.mostrarConfirmacionAtencion).toBeTrue();
    expect(fixture.nativeElement.textContent).not.toContain('Ver resumen con el Asistente IA');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('muestra singular para una consulta y plural para varias', () => {
    component.abrirConfirmacionAtencion({ ...row, consultasAtendidas: 1 }); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('1 consulta atendida anteriormente');
    component.cancelarAtencion(); component.abrirConfirmacionAtencion({ ...row, consultasAtendidas: 4 }); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('4 consultas atendidas anteriormente');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('continúa, abre la orientación del chatbot o cancela sin cambiar datos', () => {
    component.consultaSeleccionada = row; component.cantidadConsultasAtendidas = 2; component.mostrarConfirmacionAtencion = true;
    component.verResumenAsistente();
    expect(chatbotNavigation.abrirResumenConsultas).toHaveBeenCalledOnceWith({
      idPaciente: 6, nombreCompleto: 'Ana Paz', dni: undefined, cantidadConsultasAtendidas: 2
    });
    expect(router.navigate).not.toHaveBeenCalled();
    component.mostrarConfirmacionAtencion = true; component.cancelarAtencion(); expect(component.mostrarConfirmacionAtencion).toBeFalse();
    component.consultaSeleccionada = row; component.mostrarConfirmacionAtencion = true; component.continuarAtencion();
    expect(router.navigate).toHaveBeenCalledOnceWith(['consultas/lista-consultas/detalle', 12], { queryParams: { modo: 'atender' } });
  });

  it('no bloquea la atención cuando el conteo precargado es cero', () => {
    component.abrirConfirmacionAtencion(row); fixture.detectChanges();
    expect(component.errorConsultaHistorial).toBeFalse();
    expect(component.mostrarConfirmacionAtencion).toBeTrue();
    expect(fixture.nativeElement.textContent).not.toContain('No fue posible consultar el historial previo');
    component.continuarAtencion(); expect(router.navigate).toHaveBeenCalled();
  });

  it('muestra Ver y Comenzar atención, pero no Imprimir, para una consulta pendiente', () => {
    component.rows = [row]; fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="ver-consulta"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="comenzar-atencion"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="imprimir-evaluacion"]')).toBeNull();
  });

  it('muestra Ver e Imprimir, pero no Comenzar atención, para una consulta atendida', () => {
    component.rows = [atendida]; fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="ver-consulta"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="comenzar-atencion"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="imprimir-evaluacion"]')).not.toBeNull();
  });

  it('abre la vista en carga y solicita el id de la consulta atendida', () => {
    const request = new Subject<ReportePdfArchivo>();
    reporteMedicoService.obtenerEvaluacionMedica.and.returnValue(request.asObservable());

    component.imprimirEvaluacion(atendida);

    expect(reporteMedicoService.obtenerEvaluacionMedica).toHaveBeenCalledOnceWith(21);
    expect(component.mostrarVistaPreviaReporte).toBeTrue();
    expect(component.cargandoReporte).toBeTrue();
    expect(component.reportePdf).toBeUndefined();
  });

  it('asigna Blob y nombre al recibir correctamente el PDF', () => {
    const archivo = { blob: new Blob(['%PDF'], { type: 'application/pdf' }), nombreArchivo: 'evaluacion-21.pdf' };
    reporteMedicoService.obtenerEvaluacionMedica.and.returnValue(of(archivo));

    component.imprimirEvaluacion(atendida);

    expect(component.reportePdf).toBe(archivo);
    expect(component.cargandoReporte).toBeFalse();
    expect(component.errorReporte).toBeUndefined();
    expect(component.mostrarVistaPreviaReporte).toBeTrue();
  });

  it('mantiene el visor abierto con mensaje seguro y sin PDF cuando falla', fakeAsync(() => {
    const error = { status: 422 };
    reporteMedicoService.obtenerEvaluacionMedica.and.returnValue(throwError(() => error));
    reporteMedicoService.obtenerMensajeError.and.returnValue(Promise.resolve('Solo se pueden generar reportes de consultas atendidas.'));

    component.imprimirEvaluacion(atendida); flushMicrotasks();

    expect(component.cargandoReporte).toBeFalse();
    expect(component.mostrarVistaPreviaReporte).toBeTrue();
    expect(component.reportePdf).toBeUndefined();
    expect(component.errorReporte).toBe('Solo se pueden generar reportes de consultas atendidas.');
  }));

  it('limpia PDF, error y carga al cerrar', () => {
    component.mostrarVistaPreviaReporte = true; component.cargandoReporte = true;
    component.errorReporte = 'Error'; component.reportePdf = { blob: new Blob(), nombreArchivo: 'anterior.pdf' };

    component.cerrarVistaPreviaReporte();

    expect(component.mostrarVistaPreviaReporte).toBeFalse();
    expect(component.cargandoReporte).toBeFalse();
    expect(component.errorReporte).toBeUndefined();
    expect(component.reportePdf).toBeUndefined();
  });

  it('reemplaza el reporte anterior al abrir una segunda consulta', () => {
    const primero = { blob: new Blob(['uno'], { type: 'application/pdf' }), nombreArchivo: 'uno.pdf' };
    const segundo = { blob: new Blob(['dos'], { type: 'application/pdf' }), nombreArchivo: 'dos.pdf' };
    reporteMedicoService.obtenerEvaluacionMedica.and.returnValues(of(primero), of(segundo));

    component.imprimirEvaluacion(atendida);
    component.imprimirEvaluacion({ ...atendida, id: 22 });

    expect(component.reportePdf).toBe(segundo);
    expect(reporteMedicoService.obtenerEvaluacionMedica).toHaveBeenCalledTimes(2);
    expect(reporteMedicoService.obtenerEvaluacionMedica.calls.argsFor(1)).toEqual([22]);
  });

  it('ignora doble clic mientras existe una solicitud en curso', () => {
    const request = new Subject<ReportePdfArchivo>();
    reporteMedicoService.obtenerEvaluacionMedica.and.returnValue(request.asObservable());

    component.imprimirEvaluacion(atendida);
    component.imprimirEvaluacion(atendida);

    expect(reporteMedicoService.obtenerEvaluacionMedica).toHaveBeenCalledTimes(1);
  });

  it('conserva las acciones anteriores de ver y comenzar atención', () => {
    component.ver(row);
    expect(router.navigate).toHaveBeenCalledWith(['consultas/lista-consultas/detalle', 12], { queryParams: { modo: 'ver' } });
    router.navigate.calls.reset();
    component.abrirConfirmacionAtencion(row); component.continuarAtencion();
    expect(router.navigate).toHaveBeenCalledWith(['consultas/lista-consultas/detalle', 12], { queryParams: { modo: 'atender' } });
  });
});
