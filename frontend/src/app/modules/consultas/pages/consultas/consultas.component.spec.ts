import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { AuthService } from '@app/auth/services/auth.service';
import { MensajesSwalService } from '@app/shared/services/mensajes-swal.service';
import { ChatbotNavigationService } from '@app/modules/chatbot/services/chatbot-navigation.service';
import { ConsultaService } from '../../services/consultas.service';
import { ConsultasComponent } from './consultas.component';

describe('ConsultasComponent historial previo', () => {
  let component: ConsultasComponent;
  let fixture: ComponentFixture<ConsultasComponent>;
  let consultaService: jasmine.SpyObj<ConsultaService>;
  let router: jasmine.SpyObj<Router>;
  let chatbotNavigation: jasmine.SpyObj<ChatbotNavigationService>;

  beforeEach(async () => {
    consultaService = jasmine.createSpyObj('ConsultaService', ['getAllActivos', 'getCantidadAtendidasPaciente']);
    consultaService.getAllActivos.and.returnValue(of([]));
    router = jasmine.createSpyObj('Router', ['navigate']);
    chatbotNavigation = jasmine.createSpyObj('ChatbotNavigationService', ['orientarAResumenConsultas']);
    await TestBed.configureTestingModule({ imports: [ConsultasComponent], providers: [
      { provide: ConsultaService, useValue: consultaService }, { provide: Router, useValue: router },
      { provide: ChatbotNavigationService, useValue: chatbotNavigation },
      { provide: AuthService, useValue: { usuario: { idUsuario: 7, tipoUsuario: 'DOCTOR', cargo: 'DOCTOR' } } },
      { provide: MensajesSwalService, useValue: jasmine.createSpyObj('MensajesSwalService', ['mensajeError']) }
    ] }).compileComponents();
    fixture = TestBed.createComponent(ConsultasComponent); component = fixture.componentInstance; fixture.detectChanges();
  });

  const row = { id: 12, idPaciente: 6, paciente: { nombres: 'Ana', apellidos: 'Paz' }, estado: 'Por atender' } as any;

  it('consulta el historial antes de abrir el único modal', () => {
    const resultado = new Subject<number>();
    consultaService.getCantidadAtendidasPaciente.and.returnValue(resultado);
    component.abrirConfirmacionAtencion(row);
    expect(component.mostrarConfirmacionAtencion).toBeFalse();
    resultado.next(2);
    expect(component.mostrarConfirmacionAtencion).toBeTrue();
    expect(consultaService.getCantidadAtendidasPaciente).toHaveBeenCalledOnceWith(6);
  });

  it('muestra el modal simple cuando no existen consultas atendidas', () => {
    consultaService.getCantidadAtendidasPaciente.and.returnValue(of(0));
    component.abrirConfirmacionAtencion(row); fixture.detectChanges();
    expect(component.mostrarConfirmacionAtencion).toBeTrue();
    expect(fixture.nativeElement.textContent).not.toContain('Ver resumen con el Asistente IA');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('muestra singular para una consulta y plural para varias', () => {
    consultaService.getCantidadAtendidasPaciente.and.returnValue(of(1)); component.abrirConfirmacionAtencion(row); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('1 consulta atendida anteriormente');
    component.cancelarAtencion(); consultaService.getCantidadAtendidasPaciente.and.returnValue(of(4)); component.abrirConfirmacionAtencion(row); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('4 consultas atendidas anteriormente');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('continúa, abre la orientación del chatbot o cancela sin cambiar datos', () => {
    component.consultaSeleccionada = row; component.cantidadConsultasAtendidas = 2; component.mostrarConfirmacionAtencion = true;
    component.verResumenAsistente();
    expect(chatbotNavigation.orientarAResumenConsultas).toHaveBeenCalledTimes(1);
    expect(router.navigate).not.toHaveBeenCalled();
    component.mostrarConfirmacionAtencion = true; component.cancelarAtencion(); expect(component.mostrarConfirmacionAtencion).toBeFalse();
    component.consultaSeleccionada = row; component.mostrarConfirmacionAtencion = true; component.continuarAtencion();
    expect(router.navigate).toHaveBeenCalledOnceWith(['consultas/lista-consultas/detalle', 12], { queryParams: { modo: 'atender' } });
  });

  it('no bloquea la atención cuando falla el conteo', () => {
    consultaService.getCantidadAtendidasPaciente.and.returnValue(throwError(() => new Error('red')));
    component.abrirConfirmacionAtencion(row); fixture.detectChanges();
    expect(component.errorConsultaHistorial).toBeTrue();
    expect(component.mostrarConfirmacionAtencion).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('No fue posible consultar el historial previo');
    component.continuarAtencion(); expect(router.navigate).toHaveBeenCalled();
  });
});
