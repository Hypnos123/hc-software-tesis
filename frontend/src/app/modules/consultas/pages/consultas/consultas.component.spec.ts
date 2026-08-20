import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';
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
    chatbotNavigation = jasmine.createSpyObj('ChatbotNavigationService', ['abrirResumenConsultas']);
    await TestBed.configureTestingModule({ imports: [ConsultasComponent], providers: [
      { provide: ConsultaService, useValue: consultaService }, { provide: Router, useValue: router },
      { provide: ChatbotNavigationService, useValue: chatbotNavigation },
      { provide: AuthService, useValue: { usuario: { idUsuario: 7, tipoUsuario: 'DOCTOR', cargo: 'DOCTOR' } } },
      { provide: MensajesSwalService, useValue: jasmine.createSpyObj('MensajesSwalService', ['mensajeError']) }
    ] }).compileComponents();
    fixture = TestBed.createComponent(ConsultasComponent); component = fixture.componentInstance; fixture.detectChanges();
  });

  const row = { id: 12, idPaciente: 6, paciente: { nombres: 'Ana', apellidos: 'Paz' }, consultasAtendidas: 0, estado: 'Por atender' } as any;

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
});
