import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';

import { AuthService } from '@app/auth/services/auth.service';
import { AsistenteService } from '../../services/asistente.service';
import { InterfazChatComponent } from './interfaz-chat.component';

describe('InterfazChatComponent', () => {
  let component: InterfazChatComponent;
  let fixture: ComponentFixture<InterfazChatComponent>;
  let asistenteService: jasmine.SpyObj<AsistenteService>;
  let logoutSubject: Subject<void>;

  beforeEach(async () => {
    logoutSubject = new Subject<void>();
    asistenteService = jasmine.createSpyObj<AsistenteService>('AsistenteService', ['preguntar']);
    asistenteService.preguntar.and.returnValue(of({ intencion: 'ayuda', respuesta: 'Respuesta del asistente' }));

    await TestBed.configureTestingModule({
      imports: [InterfazChatComponent],
      providers: [
        { provide: AsistenteService, useValue: asistenteService },
        { provide: AuthService, useValue: { logout$: logoutSubject.asObservable() } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(InterfazChatComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => logoutSubject.complete());

  function abrirMenuHistorias(): any {
    const menuPrincipal = component.messages.find(mensaje => mensaje.menuId === 'principal')!;
    component.selectHistoricalMenuOption(menuPrincipal, menuPrincipal.options!.find(opcion => opcion.label === 'Consultar información')!);
    const menuConsultar = component.messages.find(mensaje => mensaje.menuId === 'consultar')!;
    component.selectHistoricalMenuOption(menuConsultar, menuConsultar.options!.find(opcion => opcion.label === 'Historias clínicas')!);
    return component.messages.find(mensaje => mensaje.menuId === 'historias')!;
  }

  function iniciarFlujoHistoriaClinica(): void {
    const menuHistorias = abrirMenuHistorias();
    const opcionCrear = menuHistorias.options.find((opcion: any) => opcion.label === 'Crear historia clínica');
    component.selectHistoricalMenuOption(menuHistorias, opcionCrear);
  }

  it('debe iniciar con el saludo y el menú principal', () => {
    expect(component.messages.length).toBe(2);
    expect(component.messages[0]).toEqual(jasmine.objectContaining({ sender: 'bot', type: 'text' }));
    expect(component.messages[0].text).toContain('Hola, soy el Asistente IA');
    expect(component.messages[1]).toEqual(jasmine.objectContaining({ sender: 'bot', type: 'menu', menuId: 'principal' }));
    expect(component.messages[1].options?.length).toBe(4);
  });

  it('debe abrir un submenú y conservar la selección en el historial', () => {
    const menuPrincipal = component.messages[1];
    const opcionManejo = menuPrincipal.options![0];

    component.selectHistoricalMenuOption(menuPrincipal, opcionManejo);

    expect(component.messages.some(mensaje => mensaje.sender === 'user' && mensaje.text === 'Manejo del sistema')).toBeTrue();
    expect(component.messages.some(mensaje => mensaje.type === 'menu' && mensaje.menuId === 'manejo')).toBeTrue();
    expect(menuPrincipal.options?.some(opcion => opcion.label === 'Manejo del sistema')).toBeFalse();
    expect(asistenteService.preguntar).not.toHaveBeenCalled();
  });

  it('debe mostrar localmente las instrucciones de una opción prompt', () => {
    component.quickAsk('Buscar paciente por DNI');

    expect(component.messages.at(-1)?.text).toContain('DNI de 8 dígitos');
    expect(asistenteService.preguntar).not.toHaveBeenCalled();
  });

  it('debe mostrar Crear historia clínica en el menú de historias', () => {
    const menuHistorias = abrirMenuHistorias();

    expect(menuHistorias.options.some((opcion: any) => opcion.label === 'Crear historia clínica')).toBeTrue();
  });

  it('debe iniciar localmente el flujo y solicitar el DNI sin consultar al asistente', () => {
    iniciarFlujoHistoriaClinica();

    expect(component.clinicalHistoryFlow).toEqual({ step: 'awaitingDni' });
    expect(component.messages.at(-1)?.text).toBe('Ingresa el DNI de ocho dígitos del paciente existente.');
    expect(asistenteService.preguntar).not.toHaveBeenCalled();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.clinical-history-flow-actions button')?.textContent).toContain('Cancelar');
  });

  it('debe capturar el siguiente mensaje como DNI string sin enviarlo al backend', () => {
    iniciarFlujoHistoriaClinica();
    component.userMessage = '  00123456  ';

    component.sendMessage();

    expect(component.clinicalHistoryFlow).toEqual({ step: 'dniCaptured', dni: '00123456' });
    expect(typeof (component.clinicalHistoryFlow as { step: 'dniCaptured'; dni: string }).dni).toBe('string');
    expect(component.messages.at(-1)?.text).toBe('DNI recibido. La búsqueda del paciente se realizará en el siguiente paso del flujo.');
    expect(asistenteService.preguntar).not.toHaveBeenCalled();
  });

  it('debe cancelar el flujo, limpiar el DNI y conservar el historial', () => {
    iniciarFlujoHistoriaClinica();
    component.userMessage = '00123456';
    component.sendMessage();
    const mensajesAntesDeCancelar = component.messages.length;

    component.cancelClinicalHistoryFlow();

    expect(component.clinicalHistoryFlow).toEqual({ step: 'idle' });
    expect(component.messages.length).toBe(mensajesAntesDeCancelar + 2);
    expect(component.messages.at(-1)?.text).toBe('La creación de la historia clínica fue cancelada.');
    expect(JSON.stringify(component.clinicalHistoryFlow)).not.toContain('00123456');
    expect(asistenteService.preguntar).not.toHaveBeenCalled();
  });

  it('debe limpiar el flujo al volver al Menú principal', () => {
    iniciarFlujoHistoriaClinica();
    component.userMessage = '00123456';
    component.sendMessage();

    component.quickAsk('Menú principal');

    expect(component.clinicalHistoryFlow).toEqual({ step: 'idle' });
    expect(component.messages.at(-1)).toEqual(jasmine.objectContaining({ type: 'menu', menuId: 'principal' }));
  });

  it('debe enviar las opciones request al backend y mostrar su respuesta', fakeAsync(() => {
    component.quickAsk('¿Qué preguntas puedo hacer?');
    tick();

    expect(asistenteService.preguntar).toHaveBeenCalledOnceWith('¿Qué preguntas puedo hacer?');
    expect(component.messages.at(-1)?.text).toBe('Respuesta del asistente');
    expect(component.isLoading).toBeFalse();
  }));

  it('debe enviar un mensaje escrito al backend', fakeAsync(() => {
    component.userMessage = '  ¿Cómo registro un paciente?  ';

    component.sendMessage();
    tick();

    expect(asistenteService.preguntar).toHaveBeenCalledOnceWith('¿Cómo registro un paciente?');
    expect(component.userMessage).toBe('');
    expect(component.messages.some(mensaje => mensaje.sender === 'user' && mensaje.text === '¿Cómo registro un paciente?')).toBeTrue();
  }));

  it('debe mostrar un mensaje recuperable cuando falla el backend', fakeAsync(() => {
    asistenteService.preguntar.and.returnValue(throwError(() => new Error('sin conexión')));

    component.quickAsk('¿Qué preguntas puedo hacer?');
    tick();

    expect(component.messages.at(-1)?.text).toContain('Inténtalo nuevamente');
    expect(component.messages.some(mensaje => mensaje.text === 'Escribiendo...')).toBeFalse();
    expect(component.isLoading).toBeFalse();
  }));

  it('debe minimizar sin borrar la conversación y volver a abrirla', () => {
    component.openChat();
    component.quickAsk('Buscar paciente por DNI');
    const cantidadMensajes = component.messages.length;

    component.minimizeChat();
    expect(component.isOpen).toBeFalse();
    expect(component.messages.length).toBe(cantidadMensajes);

    component.openChat();
    expect(component.isOpen).toBeTrue();
    expect(component.messages.length).toBe(cantidadMensajes);
  });

  it('debe conservar el estado del flujo al minimizar y reabrir', () => {
    component.openChat();
    iniciarFlujoHistoriaClinica();

    component.minimizeChat();
    component.openChat();

    expect(component.clinicalHistoryFlow).toEqual({ step: 'awaitingDni' });
    expect(asistenteService.preguntar).not.toHaveBeenCalled();
  });

  it('debe cerrar y reiniciar la conversación y limpiar el almacenamiento', () => {
    spyOn(localStorage, 'removeItem');
    spyOn(sessionStorage, 'removeItem');
    component.openChat();
    component.quickAsk('Buscar paciente por DNI');

    component.closeChat();

    expect(component.isOpen).toBeFalse();
    expect(component.messages.length).toBe(2);
    expect(localStorage.removeItem).toHaveBeenCalledWith('asistenteChatState');
    expect(sessionStorage.removeItem).toHaveBeenCalledWith('asistenteChatState');
  });

  it('debe limpiar el flujo especializado al cerrar el chat', () => {
    iniciarFlujoHistoriaClinica();

    component.closeChat();

    expect(component.clinicalHistoryFlow).toEqual({ step: 'idle' });
  });

  it('debe reiniciar la conversación al cerrar sesión', () => {
    component.openChat();
    iniciarFlujoHistoriaClinica();

    logoutSubject.next();

    expect(component.isOpen).toBeFalse();
    expect(component.messages.length).toBe(2);
    expect(component.messages[0].text).toContain('Hola, soy el Asistente IA');
    expect(component.clinicalHistoryFlow).toEqual({ step: 'idle' });
  });
});
