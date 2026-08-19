import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
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
    component.consultaSeleccionada = { id: 12, idPaciente: 6, paciente: { nombres: 'Ana', apellidos: 'Paz' }, estado: 'Por atender' } as any;
  });

  it('continúa directamente cuando no existen consultas atendidas', () => {
    consultaService.getCantidadAtendidasPaciente.and.returnValue(of(0));
    component.confirmarAtencion();
    expect(component.mostrarHistorialPrevio).toBeFalse();
    expect(router.navigate).toHaveBeenCalledOnceWith(['consultas/lista-consultas/detalle', 12], { queryParams: { modo: 'atender' } });
  });

  it('muestra singular para una consulta y plural para varias', () => {
    consultaService.getCantidadAtendidasPaciente.and.returnValue(of(1)); component.confirmarAtencion(); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('1 consulta atendida anteriormente');
    component.cancelarHistorial(); consultaService.getCantidadAtendidasPaciente.and.returnValue(of(4)); component.confirmarAtencion(); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('4 consultas atendidas anteriormente');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('continúa, abre la orientación del chatbot o cancela sin cambiar datos', () => {
    component.cantidadConsultasAtendidas = 2; component.mostrarHistorialPrevio = true;
    component.verResumenAsistente();
    expect(chatbotNavigation.orientarAResumenConsultas).toHaveBeenCalledTimes(1);
    expect(router.navigate).not.toHaveBeenCalled();
    component.mostrarHistorialPrevio = true; component.cancelarHistorial(); expect(component.mostrarHistorialPrevio).toBeFalse();
    component.mostrarHistorialPrevio = true; component.continuarAtencion();
    expect(router.navigate).toHaveBeenCalledOnceWith(['consultas/lista-consultas/detalle', 12], { queryParams: { modo: 'atender' } });
  });

  it('no bloquea la atención cuando falla el conteo', () => {
    consultaService.getCantidadAtendidasPaciente.and.returnValue(throwError(() => new Error('red')));
    component.confirmarAtencion(); fixture.detectChanges();
    expect(component.errorConsultaHistorial).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('No fue posible consultar el historial previo');
    component.continuarAtencion(); expect(router.navigate).toHaveBeenCalled();
  });
});
