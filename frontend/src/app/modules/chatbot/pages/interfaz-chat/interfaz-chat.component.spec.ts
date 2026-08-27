import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { Subject, of, throwError } from 'rxjs';

import { AuthService } from '@app/auth/services/auth.service';
import { HistoriaClinicaService } from '@app/modules/historiaClinica/services/consultas.service';
import { AntecedentesService } from '@app/modules/paciente/services/antecedentes.service';
import { Router } from '@angular/router';
import { ClinicalHistoryTransferService } from '@app/shared/services/clinical-history-transfer.service';
import { ClinicalHistoryFlowFeedbackService } from '@app/shared/services/clinical-history-flow-feedback.service';
import { ClinicalHistoryFlowFeedback } from '@app/shared/models/clinical-history-flow-feedback';
import { AsistenteService } from '../../services/asistente.service';
import { InterfazChatComponent } from './interfaz-chat.component';
import { PacienteImportacionService } from '@app/modules/paciente/services/paciente-importacion.service';
import { PacienteListRefreshService } from '@app/modules/paciente/services/paciente-list-refresh.service';
import { ImportacionPacientesChatComponent } from '@app/modules/paciente/components/importacion-pacientes-chat/importacion-pacientes-chat.component';
import { PacienteDuplicadoChatService } from '../../services/paciente-duplicado-chat.service';
import { HistoriasClinicasFaltantesChatComponent } from '../../components/historias-clinicas-faltantes-chat/historias-clinicas-faltantes-chat.component';
import { HistoriaClinicaDuplicadaChatService } from '../../services/historia-clinica-duplicada-chat.service';
import { crearGestionHistoriasDuplicadasState } from '../../models/historia-clinica-duplicada-chat';
import { GestionHistoriasDuplicadasChatComponent } from '../../components/gestion-historias-duplicadas-chat/gestion-historias-duplicadas-chat.component';
import { ResumenConsultasPacienteService } from '../../services/resumen-consultas-paciente.service';
import { ChatbotNavigationService } from '../../services/chatbot-navigation.service';
import { ReporteMedicoService } from '@app/shared/services/reporte-medico.service';

describe('InterfazChatComponent', () => {
  const DNI_PRUEBA = '0'.repeat(8);
  const OTRO_DNI_PRUEBA = '1'.repeat(8);
  const DNI_INEXISTENTE = '9'.repeat(8);
  let component: InterfazChatComponent;
  let fixture: ComponentFixture<InterfazChatComponent>;
  let asistenteService: jasmine.SpyObj<AsistenteService>;
  let historiaClinicaService: jasmine.SpyObj<HistoriaClinicaService>;
  let antecedentesService: jasmine.SpyObj<AntecedentesService>;
  let router: jasmine.SpyObj<Router>;
  let transferService: ClinicalHistoryTransferService;
  let feedbackService: ClinicalHistoryFlowFeedbackService;
  let logoutSubject: Subject<void>;
  let sessionChangedSubject: Subject<boolean>;
  let importacionService: jasmine.SpyObj<PacienteImportacionService>;
  let duplicadosService: jasmine.SpyObj<PacienteDuplicadoChatService>;
  let historiasDuplicadasService: jasmine.SpyObj<HistoriaClinicaDuplicadaChatService>;
  let authServiceMock: any;
  let resumenConsultasService: jasmine.SpyObj<ResumenConsultasPacienteService>;
  let reporteMedicoService: jasmine.SpyObj<ReporteMedicoService>;
  const paciente = {
    idPaciente: 8, dni: DNI_PRUEBA, numDocumento: DNI_PRUEBA, nombres: 'NOMBRE PRUEBA',
    apellidos: 'APELLIDO UNO APELLIDO DOS', fechaIngreso: '2020-03-10', fechaNacimiento: '1992-01-01', estadoCivil: 'SOLTERO'
  };

  beforeEach(async () => {
    logoutSubject = new Subject<void>();
    sessionChangedSubject = new Subject<boolean>();
    asistenteService = jasmine.createSpyObj<AsistenteService>('AsistenteService', ['preguntar']);
    asistenteService.preguntar.and.returnValue(of({ intencion: 'ayuda', respuesta: 'Respuesta del asistente' }));
    historiaClinicaService = jasmine.createSpyObj<HistoriaClinicaService>('HistoriaClinicaService', ['buscarPacientesPorDni', 'buscarPacientesPorNombre', 'getByPaciente', 'insert', 'update', 'getHistoriasClinicasFaltantes', 'crearHistoriasClinicasFaltantes']);
    historiaClinicaService.buscarPacientesPorDni.and.returnValue(of([paciente]));
    historiaClinicaService.buscarPacientesPorNombre.and.returnValue(of([paciente]));
    historiaClinicaService.getByPaciente.and.returnValue(of([]));
    historiaClinicaService.getHistoriasClinicasFaltantes.and.returnValue(of({
      cantidad: 2,
      pacientes: [
        { idPaciente: 8, nombreCompleto: 'NOMBRE PRUEBA APELLIDO UNO APELLIDO DOS', dniEnmascarado: '******00' },
        { idPaciente: 9, nombreCompleto: 'OTRO PACIENTE', dniEnmascarado: '******11' }
      ]
    }));
    historiaClinicaService.crearHistoriasClinicasFaltantes.and.returnValue(of({
      totalSolicitados: 1, totalProcesados: 1, creadas: 1, omitidas: 0,
      noEncontrados: 0, inactivos: 0, errores: 0,
      resultados: [{ idPaciente: 8, estado: 'CREADA', idHistoriaClinica: 101 }]
    }));
    antecedentesService = jasmine.createSpyObj<AntecedentesService>('AntecedentesService', ['getByPacienteId']);
    antecedentesService.getByPacienteId.and.returnValue(of(undefined));
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    router.navigate.and.returnValue(Promise.resolve(true));
    importacionService = jasmine.createSpyObj('PacienteImportacionService', ['descargarPlantilla', 'obtenerNombreArchivo', 'validarArchivo', 'confirmarImportacion']);
    duplicadosService = jasmine.createSpyObj('PacienteDuplicadoChatService', ['analizar', 'archivar']);
    historiasDuplicadasService = jasmine.createSpyObj('HistoriaClinicaDuplicadaChatService', ['detectar', 'analizar', 'fusionar']);
    historiasDuplicadasService.detectar.and.returnValue(of({ hayDuplicados: false, totalGrupos: 0, duplicados: [], mensaje: 'Sin duplicados' }));
    authServiceMock = {
      logout$: logoutSubject.asObservable(),
      sessionChanged$: sessionChangedSubject.asObservable(),
      usuario: { idUsuario: 7, cargo: 'ADMINISTRADOR' }
    };
    resumenConsultasService = jasmine.createSpyObj<ResumenConsultasPacienteService>('ResumenConsultasPacienteService', ['obtener']);
    resumenConsultasService.obtener.and.returnValue(of({
      paciente: { idPaciente: 8, nombreCompleto: 'NOMBRE PRUEBA APELLIDO UNO APELLIDO DOS', dni: DNI_PRUEBA, edad: 34, estado: 'ACTIVO', cantidadHistoriasClinicas: 1, idsHistoriasClinicas: [2] },
      antecedentes: {}, resumenAtencion: { totalConsultasAtendidas: 1, proximasCitas: [] }, tiposEnfermedad: [], especialidades: [], funcionesVitales: {}, evaluacionesRecientes: [], consultasRecientes: [],
      calidadDatos: { consultasSinFecha: 0, consultasSinTipoEnfermedad: 0, consultasSinEspecialidad: 0, valoresVitalesDescartados: 0, consultasConRelacionInconsistente: 0 }
    }));
    reporteMedicoService = jasmine.createSpyObj<ReporteMedicoService>('ReporteMedicoService', ['obtenerSeleccion', 'obtenerReporteConsolidado', 'obtenerMensajeError']);
    reporteMedicoService.obtenerMensajeError.and.resolveTo('No se pudo generar el reporte médico.');

    await TestBed.configureTestingModule({
      imports: [InterfazChatComponent],
      providers: [
        { provide: AsistenteService, useValue: asistenteService },
        { provide: HistoriaClinicaService, useValue: historiaClinicaService },
        { provide: AntecedentesService, useValue: antecedentesService },
        { provide: Router, useValue: router },
        { provide: PacienteImportacionService, useValue: importacionService },
        { provide: PacienteDuplicadoChatService, useValue: duplicadosService },
        { provide: HistoriaClinicaDuplicadaChatService, useValue: historiasDuplicadasService },
        { provide: PacienteListRefreshService, useValue: jasmine.createSpyObj('PacienteListRefreshService', ['solicitarActualizacion']) },
        { provide: AuthService, useValue: authServiceMock }
        ,{ provide: ResumenConsultasPacienteService, useValue: resumenConsultasService }
        ,{ provide: ReporteMedicoService, useValue: reporteMedicoService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(InterfazChatComponent);
    component = fixture.componentInstance;
    transferService = TestBed.inject(ClinicalHistoryTransferService);
    feedbackService = TestBed.inject(ClinicalHistoryFlowFeedbackService);
    transferService.clearAll();
    fixture.detectChanges();
  });

  afterEach(() => { logoutSubject.complete(); sessionChangedSubject.complete(); sessionStorage.removeItem('asistenteFloatingDismissedUntil'); });

  function abrirMenuHistorias(): any {
    const menuPrincipal = component.messages.find(mensaje => mensaje.menuId === 'principal')!;
    component.selectHistoricalMenuOption(menuPrincipal, menuPrincipal.options!.find(opcion => opcion.label === 'Consultar información')!);
    const menuConsultar = component.messages.find(mensaje => mensaje.menuId === 'consultar')!;
    component.selectHistoricalMenuOption(menuConsultar, menuConsultar.options!.find(opcion => opcion.label === 'Historias clínicas')!);
    return component.messages.find(mensaje => mensaje.menuId === 'historias')!;
  }

  function iniciarFlujoHistoriaClinica(): void {
    const opcionCrear = (component as any).menus['asistencia-historias'].options
      .find((opcion: any) => opcion.label === 'Crear una historia clínica con el asistente');
    (component as any).executeMenuOption(opcionCrear, 'test-clinical-history-selection');
  }

  function iniciarFlujoHistoriasFaltantes(): any {
    const opcion = (component as any).menus['asistencia-historias'].options
      .find((item: any) => item.label === 'Crear historias clínicas faltantes');
    const selection = (component as any).addUserMessage(opcion.label);
    (component as any).executeMenuOption(opcion, selection.id);
    tick(20_000);
    fixture.detectChanges();
    return component.messages.find(mensaje => mensaje.type === 'missing-clinical-histories' && mensaje.missingHistoriesActive)!;
  }

  function abrirMenuPacientes(): any {
    const principal = component.messages.find(mensaje => mensaje.menuId === 'principal')!;
    component.selectHistoricalMenuOption(principal, principal.options!.find(opcion => opcion.label === 'Consultar información')!);
    const consultar = component.messages.find(mensaje => mensaje.menuId === 'consultar')!;
    component.selectHistoricalMenuOption(consultar, consultar.options!.find(opcion => opcion.label === 'Pacientes')!);
    return component.messages.find(mensaje => mensaje.menuId === 'pacientes')!;
  }

  function iniciarImportacion(): any {
    const pacientes = abrirMenuAsistenciaPacientes();
    const opcion = pacientes.options.find((item: any) => item.label === 'Registrar pacientes desde Excel');
    component.selectHistoricalMenuOption(pacientes, opcion);
    fixture.detectChanges();
    return component.messages.find(mensaje => mensaje.type === 'patient-import')!;
  }

  function abrirMenuAsistenciaPacientes(): any {
    const principal = component.messages.find(mensaje => mensaje.menuId === 'principal')!;
    component.selectHistoricalMenuOption(principal, principal.options!.find(opcion => opcion.label === 'Asistencia guiada')!);
    const asistencia = component.messages.find(mensaje => mensaje.menuId === 'asistencia')!;
    component.selectHistoricalMenuOption(asistencia, asistencia.options!.find(opcion => opcion.label === 'Pacientes')!);
    return component.messages.find(mensaje => mensaje.menuId === 'asistencia-pacientes')!;
  }

  function abrirMenuAsistenciaHistorias(): any {
    return {
      options: (component as any).createMenuOptions('asistencia-historias')
    };
  }

  function enviarDni(dni: string): void {
    component.userMessage = dni;
    component.sendMessage();
  }

  it('debe iniciar con el saludo y el menú principal', () => {
    expect(component.messages.length).toBe(2);
    expect(component.messages[0]).toEqual(jasmine.objectContaining({ sender: 'bot', type: 'text' }));
    expect(component.messages[0].text).toContain('Hola, soy el Asistente IA');
    expect(component.messages[1]).toEqual(jasmine.objectContaining({ sender: 'bot', type: 'menu', menuId: 'principal' }));
    expect(component.messages[1].options?.length).toBe(3);
  });

  it('incluye Generar reporte de consultas como flujo independiente en Asistencia Guiada', fakeAsync(() => {
    const opciones = (component as any).createMenuOptions('asistencia-consultas');
    const reporte = opciones.find((opcion: any) => opcion.label === 'Generar reporte de consultas');
    expect(reporte?.action).toBe('patient-consultation-report-flow');
    (component as any).executeMenuOption(reporte, 'seleccion-reporte');
    tick(20_000); fixture.detectChanges();
    expect(component.reporteConsultasActivo).toBeTrue();
    expect(component.messages.some((mensaje: any) => mensaje.type === 'patient-consultation-report')).toBeTrue();
  }));

  it('debe revelar inmediatamente los mensajes iniciales mediante el estado de presentación', () => {
    expect(component.messages.map(mensaje => mensaje.presentationState)).toEqual(['visible', 'visible']);
    expect(component.messages[0].visibleText).toBe(component.messages[0].text);
    expect(component.messages[0].animateText).toBeTrue();
    expect(component.messages[1].animateText).toBeFalse();
    expect((component as any).presentationQueue).toEqual([]);
    expect((component as any).activePresentationId).toBeUndefined();
  });

  it('debe escribir progresivamente un mensaje bot y conservar intacto el texto final', fakeAsync(() => {
    const mensaje = (component as any).addBotMessage('Hola');

    expect(mensaje.presentationState).toBe('presenting');
    expect(mensaje.visibleText).toBe('');
    expect(mensaje.text).toBe('Hola');
    tick(20);
    expect(mensaje.visibleText).toBe('H');
    tick(60);

    expect(mensaje.visibleText).toBe('Hola');
    expect(mensaje.text).toBe('Hola');
    expect(mensaje.presentationState).toBe('visible');
  }));

  it('debe terminar un mensaje bot antes de comenzar el siguiente', fakeAsync(() => {
    const primero = (component as any).addBotMessage('AB');
    const segundo = (component as any).addBotMessage('CD');

    expect(primero.presentationState).toBe('presenting');
    expect(segundo.presentationState).toBe('pending');
    expect(component.messages.filter(mensaje => mensaje.presentationState === 'presenting').length).toBe(1);
    tick(40);
    expect(primero).toEqual(jasmine.objectContaining({ visibleText: 'AB', presentationState: 'visible' }));
    expect(segundo).toEqual(jasmine.objectContaining({ visibleText: '', presentationState: 'presenting' }));
    tick(40);
    expect(segundo).toEqual(jasmine.objectContaining({ visibleText: 'CD', presentationState: 'visible' }));
  }));

  it('debe mostrar inmediatamente el mensaje de usuario sin ocupar el timer', () => {
    const mensaje = (component as any).addUserMessage('Mensaje inmediato');

    expect(mensaje).toEqual(jasmine.objectContaining({ visibleText: 'Mensaje inmediato', presentationState: 'visible', animateText: false }));
    expect((component as any).presentationTimer).toBeUndefined();
  });

  it('debe preservar tildes, Unicode y saltos de línea durante la presentación', fakeAsync(() => {
    const texto = 'Información 🩺\nLínea número dos';
    const mensaje = (component as any).addBotMessage(texto);

    tick(Array.from(texto).length * 20);

    expect(mensaje.visibleText).toBe(texto);
    expect(mensaje.text).toBe(texto);
    expect(mensaje.visibleText.split('\n')).toEqual(['Información 🩺', 'Línea número dos']);
  }));

  it('debe mantener un único timer y un único mensaje presenting para toda la cola', fakeAsync(() => {
    const primero = (component as any).addBotMessage('ABC');
    const timerInicial = (component as any).presentationTimer;
    const segundo = (component as any).addBotMessage('DEF');

    expect(timerInicial).toBeDefined();
    expect((component as any).presentationTimer).toBe(timerInicial);
    expect(component.messages.filter(mensaje => mensaje.presentationState === 'presenting')).toEqual([primero]);
    expect((component as any).presentationQueue).toEqual([segundo]);
    tick(120);
    expect((component as any).presentationTimer).toBeUndefined();
    expect((component as any).activePresentationId).toBeUndefined();
    expect(component.messages.filter(mensaje => mensaje.presentationState === 'presenting')).toEqual([]);
  }));

  it('no debe renderizar la burbuja de un texto pendiente', fakeAsync(() => {
    component.openChat();
    const primero = (component as any).addBotMessage('AB');
    const pendiente = (component as any).addBotMessage('CD');
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector(`[data-block-id="${primero.id}"]`)).not.toBeNull();
    expect(fixture.nativeElement.querySelector(`[data-block-id="${pendiente.id}"]`)).toBeNull();
    tick(40);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector(`[data-block-id="${pendiente.id}"]`)).not.toBeNull();
  }));

  it('debe mostrar un único indicador fuera del historial mientras escribe y retirarlo al terminar', fakeAsync(() => {
    component.openChat();
    const cantidadInicial = component.messages.length;
    (component as any).addBotMessage('AB');
    (component as any).addBotMessage('CD');
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('.assistant-typing-indicator').length).toBe(1);
    expect(fixture.nativeElement.querySelector('.assistant-typing-indicator').textContent).toContain('Asistente escribiendo');
    expect(component.messages.length).toBe(cantidadInicial + 2);
    expect(component.messages.some(mensaje => mensaje.text?.includes('Asistente escribiendo'))).toBeFalse();
    tick(80);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.assistant-typing-indicator')).toBeNull();
  }));

  it('debe ocultar las opciones y bloquear la entrada hasta terminar la pregunta', fakeAsync(() => {
    component.openChat();
    (component as any).addMenuBlock('manejo');
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector('.chatbot-footer input') as HTMLInputElement;
    expect(component.messages.some(message => message.menuId === 'manejo')).toBeFalse();
    expect(fixture.nativeElement.querySelectorAll('[data-block-id][aria-label="Opciones del asistente"]').length).toBe(1);
    expect(input.disabled).toBeTrue();
    expect(fixture.nativeElement.querySelector('[aria-label="Enviar consulta"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('[aria-label="Detener escritura"]')).not.toBeNull();

    component.userMessage = 'no debe enviarse';
    const enter = new KeyboardEvent('keydown', { key: 'Enter', cancelable: true });
    spyOn(component, 'sendMessage').and.callThrough();
    component.onEnter(enter);
    expect(enter.defaultPrevented).toBeTrue();
    expect(component.sendMessage).not.toHaveBeenCalled();

    tick(30_000);
    fixture.detectChanges();
    expect(component.messages.some(message => message.menuId === 'manejo')).toBeTrue();
    expect(input.disabled).toBeFalse();
    expect(fixture.nativeElement.querySelector('[aria-label="Enviar consulta"]')).not.toBeNull();
  }));

  it('debe detener solo el texto activo sin ejecutar su continuación ni mover el ancla', fakeAsync(() => {
    component.openChat();
    fixture.detectChanges();
    const callback = jasmine.createSpy('afterPresentation');
    const message = (component as any).addBotMessage('Un mensaje suficientemente largo');
    (component as any).runAfterPresentation(message, callback);
    (component as any).interactionScrollAnchorId = 'anchor-actual';
    const scrollSpy = spyOn<any>(component, 'scrollToNewBlock');
    tick(60);
    const partialText = message.visibleText;

    component.stopPresentation();
    fixture.detectChanges();

    expect(message.visibleText).toBe(partialText);
    expect(message.presentationState).toBe('visible');
    expect(callback).not.toHaveBeenCalled();
    expect(component.asistenteEscribiendo).toBeFalse();
    expect((component as any).interactionScrollAnchorId).toBe('anchor-actual');
    expect(scrollSpy).not.toHaveBeenCalled();
    expect((fixture.nativeElement.querySelector('.chatbot-footer input') as HTMLInputElement).disabled).toBeFalse();
  }));

  it('debe ejecutar todas las continuaciones registradas al finalizar normalmente', fakeAsync(() => {
    component.openChat();
    const firstCallback = jasmine.createSpy('firstCallback');
    const secondCallback = jasmine.createSpy('secondCallback');
    const message = (component as any).addBotMessage('AB');
    (component as any).runAfterPresentation(message, firstCallback);
    (component as any).runAfterPresentation(message, secondCallback);

    expect(firstCallback).not.toHaveBeenCalled();
    expect(secondCallback).not.toHaveBeenCalled();
    tick(40);

    expect(message.presentationState).toBe('visible');
    expect(firstCallback).toHaveBeenCalledTimes(1);
    expect(secondCallback).toHaveBeenCalledTimes(1);
    expect((component as any).presentationContinuations.has(message.id)).toBeFalse();
  }));

  it('debe cancelar todas las continuaciones de la presentación detenida', fakeAsync(() => {
    component.openChat();
    const firstCallback = jasmine.createSpy('firstCallback');
    const secondCallback = jasmine.createSpy('secondCallback');
    const message = (component as any).addBotMessage('Mensaje interrumpido');
    (component as any).runAfterPresentation(message, firstCallback);
    (component as any).runAfterPresentation(message, secondCallback);
    tick(40);

    component.stopPresentation();
    tick(30_000);

    expect(firstCallback).not.toHaveBeenCalled();
    expect(secondCallback).not.toHaveBeenCalled();
    expect((component as any).presentationContinuations.has(message.id)).toBeFalse();
  }));

  it('debe detener la presentación al pulsar el botón Detener', fakeAsync(() => {
    component.openChat();
    const callback = jasmine.createSpy('afterPresentation');
    const message = (component as any).addBotMessage('Texto que todavía se está presentando');
    (component as any).runAfterPresentation(message, callback);
    tick(60);
    fixture.detectChanges();
    const partialText = message.visibleText;

    const stopButton = fixture.nativeElement.querySelector('[aria-label="Detener escritura"]') as HTMLButtonElement;
    expect(stopButton).not.toBeNull();
    stopButton.click();
    fixture.detectChanges();

    expect(message.visibleText).toBe(partialText);
    expect(callback).not.toHaveBeenCalled();
    expect(component.asistenteEscribiendo).toBeFalse();
    expect((fixture.nativeElement.querySelector('.chatbot-footer input') as HTMLInputElement).disabled).toBeFalse();
    expect(fixture.nativeElement.querySelector('[aria-label="Enviar consulta"]')).not.toBeNull();
  }));

  it('no debe agregar el menú programado cuando se detiene su pregunta', fakeAsync(() => {
    component.openChat();
    (component as any).addMenuBlock('consultar');
    tick(60);

    component.stopPresentation();
    fixture.detectChanges();

    expect(component.messages.some(message => message.menuId === 'consultar')).toBeFalse();
    expect(fixture.nativeElement.querySelectorAll('[data-block-id][aria-label="Opciones del asistente"]').length).toBe(1);
  }));

  it('debe conservar el ancla al agregar la respuesta y sus opciones posteriores', fakeAsync(() => {
    component.openChat();
    fixture.detectChanges();
    const principal = component.messages.find(message => message.menuId === 'principal')!;
    spyOn(component, 'scrollToBottom').and.callThrough();
    const genericScrollSpy = spyOn<any>(component, 'scrollToNewBlock').and.callThrough();
    const anchorScrollSpy = spyOn<any>(component, 'positionInteractionAnchor').and.callThrough();

    component.selectHistoricalMenuOption(principal, principal.options![0]);
    const selection = component.messages.find(message => message.sender === 'user' && message.text === 'Manejo del sistema')!;
    expect((component as any).interactionScrollAnchorId).toBe(selection.id);
    expect(anchorScrollSpy).toHaveBeenCalledOnceWith(selection.id);

    tick(30_000);
    fixture.detectChanges();
    expect(component.messages.some(message => message.menuId === 'manejo')).toBeTrue();
    expect(component.scrollToBottom).not.toHaveBeenCalled();
    expect(genericScrollSpy).not.toHaveBeenCalled();
    expect((component as any).interactionScrollAnchorId).toBe(selection.id);
  }));

  it('debe conservar el seguimiento automático cuando no existe un ancla', fakeAsync(() => {
    component.openChat();
    fixture.detectChanges();
    const body = fixture.nativeElement.querySelector('.chatbot-body') as HTMLElement;
    Object.defineProperty(body, 'clientHeight', { configurable: true, value: 100 });
    Object.defineProperty(body, 'scrollHeight', { configurable: true, value: 240 });
    body.scrollTop = 0;

    (component as any).addBotMessage('A');
    tick(20);
    tick(20);

    expect((component as any).interactionScrollAnchorId).toBeUndefined();
    expect(body.scrollTop).toBe(140);
  }));

  it('debe crear una nueva ancla y permitir otra interacción después de detener', fakeAsync(() => {
    component.openChat();
    const interrupted = (component as any).addBotMessage('Texto interrumpido');
    (component as any).runAfterPresentation(interrupted, () => (component as any).addMenuBlock('manejo'));
    tick(20);
    component.stopPresentation();

    component.quickAsk('Verificar historia clínica');

    const selection = component.messages.find(message => message.sender === 'user' && message.text === 'Verificar historia clínica')!;
    expect(selection).toBeDefined();
    expect((component as any).interactionScrollAnchorId).toBe(selection.id);
    expect(component.messages.some(message => message.menuId === 'manejo')).toBeFalse();
    expect(component.asistenteEscribiendo).toBeTrue();
    tick(30_000);
  }));

  it('debe respetar texto, texto y componente sin mostrar el componente antes de su turno', fakeAsync(() => {
    component.openChat();
    const primero = (component as any).addBotMessage('AB');
    const segundo = (component as any).addBotMessage('CD');
    const componente = (component as any).createBlockMessage('menu', { menuId: 'principal', options: [] });
    (component as any).addMessage(componente);
    fixture.detectChanges();

    const menuElement = (): HTMLElement | null => fixture.nativeElement.querySelector(`[data-block-id="${componente.id}"]`);
    expect([primero, segundo, componente].map(mensaje => mensaje.presentationState)).toEqual(['presenting', 'pending', 'pending']);
    expect(menuElement()?.hidden).toBeTrue();
    tick(40);
    fixture.detectChanges();
    expect(segundo.presentationState).toBe('presenting');
    expect(componente.presentationState).toBe('pending');
    expect(menuElement()?.hidden).toBeTrue();
    tick(40);
    fixture.detectChanges();
    expect(componente.presentationState).toBe('visible');
    expect(menuElement()?.hidden).toBeFalse();
    expect(menuElement()?.querySelectorAll('button').length).toBe(0);
  }));

  it('debe seguir suavemente el crecimiento del texto con un solo frame pendiente', fakeAsync(() => {
    component.openChat();
    fixture.detectChanges();
    const body = fixture.nativeElement.querySelector('.chatbot-body') as HTMLElement;
    Object.defineProperty(body, 'clientHeight', { configurable: true, value: 100 });
    Object.defineProperty(body, 'scrollHeight', { configurable: true, value: 240 });
    body.scrollTop = 140;

    (component as any).addBotMessage('AB');
    tick(20);
    expect((component as any).presentationScrollFrame).toBeDefined();
    tick(20);

    expect(body.scrollTop).toBe(140);
    expect((component as any).presentationScrollFrame).toBeUndefined();
  }));

  it('debe mantener el seguimiento al cambiar de un texto al siguiente', fakeAsync(() => {
    component.openChat();
    fixture.detectChanges();
    spyOn<any>(component, 'followActivePresentation').and.callThrough();
    (component as any).addBotMessage('A');
    (component as any).addBotMessage('B');

    tick(40);

    expect((component as any).followActivePresentation).toHaveBeenCalledTimes(4);
    expect(component.messages.slice(-2).map(mensaje => mensaje.presentationState)).toEqual(['visible', 'visible']);
  }));

  it('debe enfocar el inicio del componente que aparece después de textos', fakeAsync(() => {
    component.openChat();
    fixture.detectChanges();
    const scrollSpy = spyOn<any>(component, 'scrollToNewBlock').and.callThrough();
    (component as any).addBotMessage('A');
    const tarjeta = (component as any).createBlockMessage('menu', { menuId: 'principal', options: [] });
    (component as any).addMessage(tarjeta);
    fixture.detectChanges();
    const tarjetaElement = fixture.nativeElement.querySelector(`[data-block-id="${tarjeta.id}"]`) as HTMLElement;
    tarjetaElement.scrollIntoView = jasmine.createSpy('scrollIntoView');

    tick(20);

    expect(scrollSpy).toHaveBeenCalledWith(tarjeta.id);
    expect(tarjetaElement.scrollIntoView).toHaveBeenCalledWith({ behavior: 'smooth', block: 'start' });
  }));

  it('debe suspender el seguimiento cuando el usuario se aleja del final', fakeAsync(() => {
    component.openChat();
    fixture.detectChanges();
    const body = fixture.nativeElement.querySelector('.chatbot-body') as HTMLElement;
    Object.defineProperty(body, 'clientHeight', { configurable: true, value: 100 });
    Object.defineProperty(body, 'scrollHeight', { configurable: true, value: 500 });
    body.scrollTop = 100;
    component.onChatBodyScroll();
    (component as any).addBotMessage('AB');

    tick(40);

    expect(body.scrollTop).toBe(100);
    expect((component as any).autoFollowPresentation).toBeFalse();
  }));

  it('no debe modificar el scroll interno de una tarjeta al enfocar su inicio', fakeAsync(() => {
    component.openChat();
    (component as any).addBotMessage('A');
    const tarjeta = (component as any).createBlockMessage('menu', { menuId: 'principal', options: [] });
    (component as any).addMessage(tarjeta);
    fixture.detectChanges();
    const tarjetaElement = fixture.nativeElement.querySelector(`[data-block-id="${tarjeta.id}"]`) as HTMLElement;
    tarjetaElement.scrollTop = 37;
    tarjetaElement.scrollIntoView = jasmine.createSpy('scrollIntoView');

    tick(20);

    expect(tarjetaElement.scrollTop).toBe(37);
    expect(tarjetaElement.scrollIntoView).toHaveBeenCalledWith({ behavior: 'smooth', block: 'start' });
  }));

  it('debe conservar la posición al minimizar y reabrir durante la presentación', fakeAsync(() => {
    component.openChat();
    fixture.detectChanges();
    const body = fixture.nativeElement.querySelector('.chatbot-body') as HTMLElement;
    body.scrollTop = 73;
    const mensaje = (component as any).addBotMessage('ABCD');
    component.minimizeChat();
    tick(40);
    fixture.detectChanges();
    component.openChat();
    fixture.detectChanges();
    tick(20);

    const restored = fixture.nativeElement.querySelector('.chatbot-body') as HTMLElement;
    expect(restored.scrollTop).toBe(73);
    expect(mensaje.visibleText.length).toBeGreaterThan(0);
    tick(40);
  }));

  it('debe conservar los cinco botones inferiores con el mismo texto y orden', () => {
    component.openChat();
    fixture.detectChanges();

    const botones = Array.from<HTMLButtonElement>(fixture.nativeElement.querySelectorAll('.quick-questions button'));
    expect(botones.map(boton => boton.textContent?.trim())).toEqual([
      'Menú principal',
      '¿Qué preguntas puedo hacer?',
      'Buscar paciente por DNI',
      'Verificar historia clínica',
      'Consultas médicas de un paciente'
    ]);
  });

  it('debe mostrar exactamente las tres categorías de la nueva arquitectura', () => {
    expect(component.messages[1].options?.map(opcion => opcion.label)).toEqual([
      'Manejo del sistema', 'Consultar información', 'Asistencia guiada'
    ]);
    expect((component as any).menus['verificar']).toBeUndefined();
    expect((component as any).menus['ayuda']).toBeUndefined();
  });

  it('debe ejecutar las verificaciones rápidas sin depender del menú verificar', () => {
    const cantidadInicial = component.messages.length;
    const scrollSpy = spyOn(component as any, 'scrollToNewBlock');
    delete (component as any).menus['verificar'];

    component.quickAsk('Verificar historia clínica');
    component.quickAsk('Consultas médicas de un paciente');

    const mensajesNuevos = component.messages.slice(cantidadInicial);
    expect(mensajesNuevos.map(mensaje => mensaje.sender)).toEqual(['user', 'bot', 'user', 'bot']);
    expect(mensajesNuevos[0].text).toBe('Verificar si un paciente tiene historia clínica');
    expect(mensajesNuevos[1].text).toContain('Escribe el DNI o el nombre y los dos apellidos del paciente.');
    expect(mensajesNuevos[2].text).toBe('Verificar consultas médicas de un paciente');
    expect(mensajesNuevos[3].text).toContain('Escribe el DNI o el nombre y los dos apellidos del paciente.');
    expect(component.messages.filter(mensaje => mensaje.text === mensajesNuevos[0].text).length).toBe(1);
    expect(component.messages.filter(mensaje => mensaje.text === mensajesNuevos[2].text).length).toBe(1);
    expect(scrollSpy.calls.allArgs()).toEqual([[mensajesNuevos[0].id], [mensajesNuevos[2].id]]);
    expect(asistenteService.preguntar).not.toHaveBeenCalled();
  });

  it('debe mostrar solo el aviso informativo y bloquear consultas cuando no existe sesión', () => {
    fixture.destroy();
    authServiceMock.usuario = undefined;
    fixture = TestBed.createComponent(InterfazChatComponent);
    component = fixture.componentInstance;
    component.openChat();
    fixture.detectChanges();

    expect(component.messages.length).toBe(1);
    expect(component.messages[0].text).toBe('Hola, soy el Asistente IA del sistema.\nPara realizar consultas, verificar datos o ayudarte con los procesos, primero debes iniciar sesión.');
    expect(fixture.nativeElement.querySelector('.navigation-options')).toBeNull();
    expect(fixture.nativeElement.querySelector('.chatbot-footer')).toBeNull();
    expect(fixture.nativeElement.querySelector('.quick-questions')).toBeNull();

    component.userMessage = '¿Existen pacientes duplicados?';
    component.sendMessage();
    expect(asistenteService.preguntar).not.toHaveBeenCalled();
  });

  it('debe mostrar y ocultar el primer mensaje flotante según la temporización inicial', fakeAsync(() => {
    (component as any).handleSessionChange(true);
    tick(9_999);
    expect(component.mensajeFlotanteVisible).toBeFalse();

    tick(1);
    expect(component.mensajeFlotanteVisible).toBeTrue();
    expect(component.mensajeFlotante).toBe('Estoy aquí para ayudarte.');

    tick(5_000);
    expect(component.mensajeFlotanteVisible).toBeFalse();
    (component as any).clearFloatingMessageTimer();
  }));

  it('no debe mostrar mensajes flotantes con el chatbot abierto', fakeAsync(() => {
    (component as any).handleSessionChange(true);
    component.openChat();
    tick(100_000);

    expect(component.mensajeFlotanteVisible).toBeFalse();
    expect((component as any).floatingMessageTimer).toBeUndefined();
    (component as any).clearFloatingMessageTimer();
  }));

  it('debe esperar 90 segundos después de minimizar el chatbot', fakeAsync(() => {
    component.openChat();
    component.minimizeChat();
    tick(89_999);
    expect(component.mensajeFlotanteVisible).toBeFalse();

    tick(1);
    expect(component.mensajeFlotanteVisible).toBeTrue();
    (component as any).clearFloatingMessageTimer();
  }));

  it('debe suspender mensajes durante cinco minutos después de cerrar la burbuja', fakeAsync(() => {
    (component as any).handleSessionChange(true);
    tick(10_000);
    expect(component.mensajeFlotanteVisible).toBeTrue();

    component.cerrarMensajeFlotante();
    expect(component.mensajeFlotanteVisible).toBeFalse();
    tick(299_999);
    expect(component.mensajeFlotanteVisible).toBeFalse();

    tick(1);
    expect(component.mensajeFlotanteVisible).toBeTrue();
    (component as any).clearFloatingMessageTimer();
  }));

  it('debe ocultar mensajes y limpiar el único temporizador al cerrar sesión', fakeAsync(() => {
    (component as any).handleSessionChange(true);
    tick(10_000);
    expect(component.mensajeFlotanteVisible).toBeTrue();

    authServiceMock.usuario = undefined;
    sessionChangedSubject.next(false);

    expect(component.mensajeFlotanteVisible).toBeFalse();
    expect((component as any).floatingMessageTimer).toBeUndefined();
    tick(300_000);
    expect(component.mensajeFlotanteVisible).toBeFalse();
    (component as any).clearFloatingMessageTimer();
  }));

  it('no debe crear temporizadores duplicados ante notificaciones repetidas de sesión', fakeAsync(() => {
    (component as any).handleSessionChange(true);
    const primerTemporizador = (component as any).floatingMessageTimer;
    (component as any).handleSessionChange(true);
    const segundoTemporizador = (component as any).floatingMessageTimer;

    expect(segundoTemporizador).not.toBe(primerTemporizador);
    tick(10_000);
    expect(component.mensajeFlotanteVisible).toBeTrue();
    expect((component as any).floatingMessageIndex).toBe(1);
    (component as any).clearFloatingMessageTimer();
  }));

  it('debe procesar una sola vez el feedback de precarga exitosa y volver al menú principal', fakeAsync(() => {
    iniciarFlujoHistoriaClinica();
    enviarDni(DNI_PRUEBA);
    tick(30_000);
    fixture.detectChanges();
    const scrollBottomSpy = spyOn(component, 'scrollToBottom');
    const scrollNewBlockSpy = spyOn(component as any, 'scrollToNewBlock');
    const feedback: ClinicalHistoryFlowFeedback = { id: 'feedback-success-1', type: 'prefill-success', createdAt: Date.now() };

    feedbackService.publish(feedback);
    feedbackService.publish(feedback);
    fixture.detectChanges();

    const successMessage = 'Los datos del paciente se autocompletaron correctamente en Nueva Historia Clínica. Revísalos y pulsa Guardar para registrar la historia.';
    expect(component.messages.filter(message => message.text === successMessage).length).toBe(1);
    expect(component.messages.some(message => message.text === '¿Necesitas ayuda con algo más?')).toBeFalse();
    expect(component.messages.some(message => message.menuId === 'principal' && component.messages.indexOf(message) > 1)).toBeFalse();
    tick(30_000);
    fixture.detectChanges();
    expect(component.messages.filter(message => message.text === '¿Necesitas ayuda con algo más?').length).toBe(1);
    expect(component.messages.some(message => message.text === 'Historia clínica guardada correctamente')).toBeFalse();
    expect(component.clinicalHistoryFlow).toEqual({ step: 'idle' });
    const principal = component.messages.at(-1)!;
    expect(principal.menuId).toBe('principal');
    expect(principal.options?.map(option => option.label)).toEqual([
      'Manejo del sistema', 'Consultar información', 'Asistencia guiada'
    ]);
    expect(fixture.nativeElement.querySelector('.continue-action')).toBeNull();
    expect(fixture.nativeElement.querySelector('.cancel-action')).toBeNull();
    expect(asistenteService.preguntar).not.toHaveBeenCalled();
    expect(historiaClinicaService.insert).not.toHaveBeenCalled();
    expect(historiaClinicaService.update).not.toHaveBeenCalled();
    expect(scrollBottomSpy).not.toHaveBeenCalled();
    expect(scrollNewBlockSpy).not.toHaveBeenCalled();
  }));

  it('debe mostrar ayuda manual y el menú principal ante un fallo de precarga', fakeAsync(() => {
    const scrollBottomSpy = spyOn(component, 'scrollToBottom');
    const scrollNewBlockSpy = spyOn(component as any, 'scrollToNewBlock');
    const feedback: ClinicalHistoryFlowFeedback = { id: 'feedback-failure-1', type: 'prefill-failure', createdAt: Date.now() };

    feedbackService.publish(feedback);
    fixture.detectChanges();

    expect(component.messages.some(message => message.text === 'No fue posible autocompletar los datos. Puedes completar el formulario manualmente.')).toBeTrue();
    expect(component.messages.some(message => message.text === '¿Necesitas ayuda con algo más?')).toBeFalse();
    tick(30_000);
    fixture.detectChanges();
    expect(component.messages.some(message => message.text === '¿Necesitas ayuda con algo más?')).toBeTrue();
    expect(component.messages.at(-1)).toEqual(jasmine.objectContaining({ type: 'menu', menuId: 'principal' }));
    expect(component.messages.at(-1)?.options?.length).toBe(3);
    expect(component.clinicalHistoryFlow).toEqual({ step: 'idle' });
    expect(asistenteService.preguntar).not.toHaveBeenCalled();
    expect(scrollBottomSpy).not.toHaveBeenCalled();
    expect(scrollNewBlockSpy).not.toHaveBeenCalled();
  }));

  it('debe cancelar la suscripción de feedback en ngOnDestroy', () => {
    const messageCount = component.messages.length;
    fixture.destroy();

    feedbackService.publish({ id: 'feedback-after-destroy', type: 'prefill-success', createdAt: Date.now() });

    expect(component.messages.length).toBe(messageCount);
  });

  it('debe abrir un submenú y conservar la selección en el historial', () => {
    const menuPrincipal = component.messages[1];
    const opcionManejo = menuPrincipal.options![0];
    const scrollNewBlockSpy = spyOn(component as any, 'scrollToNewBlock');

    component.selectHistoricalMenuOption(menuPrincipal, opcionManejo);

    expect(component.messages.some(mensaje => mensaje.sender === 'user' && mensaje.text === 'Manejo del sistema')).toBeTrue();
    expect(component.messages.some(mensaje => mensaje.type === 'menu' && mensaje.menuId === 'manejo')).toBeTrue();
    expect(menuPrincipal.options?.some(opcion => opcion.label === 'Manejo del sistema')).toBeFalse();
    expect(asistenteService.preguntar).not.toHaveBeenCalled();
    expect(scrollNewBlockSpy).toHaveBeenCalledWith(jasmine.any(String));
  });

  it('debe organizar Manejo del sistema por procesos sin mostrar opciones administrativas', () => {
    const menuPrincipal = component.messages[1];
    component.selectHistoricalMenuOption(menuPrincipal, menuPrincipal.options![0]);

    const menuManejo = component.messages.at(-1)!;
    expect(component.messages.at(-2)?.text).toBe('¿Sobre qué proceso del sistema necesitas ayuda? Selecciona una opción o escribe tu pregunta.');
    expect(menuManejo.menuId).toBe('manejo');
    expect(menuManejo.options?.map(opcion => opcion.label)).toEqual(['Pacientes', 'Historias clínicas', 'Consultas médicas']);
    expect(JSON.stringify(component.messages)).not.toContain('¿Cómo gestiono empleados?');
    expect(JSON.stringify(component.messages)).not.toContain('¿Cómo gestiono usuarios y permisos?');
  });

  it('debe mostrar solo las preguntas del proceso seleccionado y conservar los menús históricos', () => {
    const menuPrincipal = component.messages[1];
    component.selectHistoricalMenuOption(menuPrincipal, menuPrincipal.options![0]);
    const menuManejo = component.messages.at(-1)!;
    const opcionPacientes = menuManejo.options![0];

    component.selectHistoricalMenuOption(menuManejo, opcionPacientes);

    const menuPacientes = component.messages.at(-1)!;
    expect(component.messages.at(-2)?.text).toBe('Selecciona una opción o escribe tu pregunta sobre la gestión de pacientes.');
    expect(menuPacientes.menuId).toBe('manejo-pacientes');
    expect(menuPacientes.options?.map(opcion => opcion.label)).toEqual([
      '¿Cómo registro un paciente?',
      '¿Cómo edito los datos de un paciente?',
      '¿Cómo visualizo los datos de un paciente?'
    ]);
    expect(component.messages).toContain(menuManejo);
    expect(menuManejo.options?.map(opcion => opcion.label)).toEqual(['Historias clínicas', 'Consultas médicas']);
    expect(asistenteService.preguntar).not.toHaveBeenCalled();
  });

  it('debe mantener separadas las preguntas de historias clínicas y consultas médicas', () => {
    const menus = (component as any).menus;

    expect(menus['manejo-historias'].options.map((opcion: any) => opcion.label)).toEqual([
      '¿Cómo creo una historia clínica?',
      '¿Cómo edito una historia clínica?',
      '¿Cómo visualizo una historia clínica?'
    ]);
    expect(menus['manejo-consultas'].options.map((opcion: any) => opcion.label)).toEqual([
      '¿Cómo agrego una consulta médica?',
      '¿Cómo comienzo la atención de una consulta médica?',
      '¿Cómo visualizo una consulta médica antes de atenderla?'
    ]);
    expect(JSON.stringify(menus['manejo'])).not.toContain('empleados');
    expect(JSON.stringify(menus['manejo'])).not.toContain('usuarios');
    expect(JSON.stringify(menus['manejo'])).not.toContain('permisos');
  });

  it('debe mostrar localmente las instrucciones de una opción prompt', () => {
    component.quickAsk('Buscar paciente por DNI');

    expect(component.messages.at(-1)?.text).toContain('DNI del paciente');
    expect(asistenteService.preguntar).not.toHaveBeenCalled();
  });

  it('debe dejar en Historias clínicas solo las consultas informativas requeridas', () => {
    const menuHistorias = abrirMenuHistorias();

    expect(menuHistorias.options.map((opcion: any) => opcion.label)).toEqual([
      '¿Cuántas historias clínicas hay registradas?',
      'Buscar si un paciente tiene historia clínica',
      'Historias clínicas creadas hoy',
      'Detectar historias clínicas duplicadas'
    ]);
    expect(menuHistorias.options.find((opcion: any) => opcion.label === 'Detectar historias clínicas duplicadas')?.description).toBeUndefined();
  });

  it('consulta primero las historias duplicadas desde Consultar información', () => {
    asistenteService.preguntar.and.returnValue(of({
      intencion: 'HISTORIAS_CLINICAS_DUPLICADAS', respuesta: 'No existen historias duplicadas.',
      datos: { hayDuplicados: false, totalGrupos: 0, duplicados: [] }
    } as any));
    const menuHistorias = abrirMenuHistorias();
    const opcion = menuHistorias.options.find((item: any) => item.label === 'Detectar historias clínicas duplicadas');

    component.selectHistoricalMenuOption(menuHistorias, opcion);

    expect(asistenteService.preguntar).toHaveBeenCalledOnceWith('Detectar historias clínicas duplicadas');
    expect(component.messages.some(mensaje => mensaje.type === 'clinical-history-duplicate-management')).toBeFalse();
  });

  it('limpia el análisis de historias duplicadas al minimizar', () => {
    const pendiente = new Subject<any>();
    historiasDuplicadasService.detectar.and.returnValue(pendiente.asObservable());
    const menuHistorias = abrirMenuAsistenciaHistorias();
    component.selectHistoricalMenuOption(menuHistorias,
      menuHistorias.options.find((item: any) => item.label === 'Analizar historias clínicas duplicadas'));
    fixture.detectChanges();
    const tarjeta = component.historiasDuplicadasComponents.last;

    component.minimizeChat();

    expect(tarjeta.state.estado).toBe('CANCELADO');
    expect(tarjeta.state.idsSeleccionados).toEqual([]);
    expect(pendiente.observed).toBeFalse();
    expect(component.gestionHistoriasDuplicadasActiva).toBeFalse();
  });

  it('ancla el inicio informativo del flujo y no desplaza el scroll al bloque de grupos', fakeAsync(() => {
    component.openChat();
    const pendiente = new Subject<any>();
    historiasDuplicadasService.detectar.and.returnValue(pendiente.asObservable());
    const menuHistorias = abrirMenuAsistenciaHistorias();
    const scrollSpy = spyOn(component as any, 'scrollToNewBlock');

    component.selectHistoricalMenuOption(menuHistorias,
      menuHistorias.options.find((item: any) => item.label === 'Analizar historias clínicas duplicadas'));
    fixture.detectChanges();
    const inicio = component.messages.find(mensaje => mensaje.text?.startsWith('Consultaré las historias clínicas duplicadas'))!;
    pendiente.next({ hayDuplicados: true, totalGrupos: 1, mensaje: 'Se encontró un grupo.', duplicados: [{
      tipo: 'dni', valorCoincidente: DNI_PRUEBA, cantidad: 2, historiasClinicas: [
        { idHistoriaClinica: 7, idPaciente: 12, nombreCompleto: 'Paciente', cantidadConsultas: 0, estado: 'ACTIVA' },
        { idHistoriaClinica: 8, idPaciente: 12, nombreCompleto: 'Paciente', cantidadConsultas: 0, estado: 'ACTIVA' }
      ]
    }] });
    tick(10_000);
    fixture.detectChanges();
    const grupos = component.messages.find(mensaje => mensaje.duplicateHistoriesView === 'groups')!;

    expect(scrollSpy).toHaveBeenCalledOnceWith(inicio.id);
    expect(scrollSpy).not.toHaveBeenCalledWith(grupos.id);
    expect(grupos.presentationState).toBe('visible');
  }));

  it('ancla el primer mensaje del resultado y la comparación aparece sin apropiarse del scroll', fakeAsync(() => {
    component.openChat();
    const state = crearGestionHistoriasDuplicadasState();
    state.estado = 'ANALIZANDO_HISTORIAS';
    (component as any).addDuplicateHistoriesBlock(state, 'analyzing', true);
    fixture.detectChanges();
    const scrollSpy = spyOn(component as any, 'scrollToNewBlock');

    component.manejarMensajeHistoriasDuplicadas(state, {
      remitente: 'bot', texto: 'He analizado las historias clínicas seleccionadas para Paciente.',
      inicioGrupo: true
    });
    const primerMensaje = component.messages.at(-1)!;
    component.manejarMensajeHistoriasDuplicadas(state, {
      remitente: 'bot', texto: 'Recomiendo conservar la historia clínica 7.'
    });
    component.manejarMensajeHistoriasDuplicadas(state, {
      remitente: 'bot', texto: 'No existen consultas para transferir en este caso.', vistaSiguiente: 'comparison', reemplazarVistaActiva: true
    });
    const comparacion = component.messages.find(mensaje => mensaje.duplicateHistoriesView === 'comparison')!;
    expect(component.messages.filter(mensaje => mensaje.historiasDuplicadas === state).length).toBe(1);

    tick(10_000);
    fixture.detectChanges();

    expect(scrollSpy).toHaveBeenCalledOnceWith(primerMensaje.id);
    expect(scrollSpy).not.toHaveBeenCalledWith(comparacion.id);
    expect(comparacion.presentationState).toBe('visible');
    expect((component as any).interactionScrollAnchorId).toBeUndefined();
    expect((component as any).autoFollowPresentation).toBeFalse();
  }));

  it('mantiene el componente y la petición HTTP activos al cambiar de contraseña a fusión', () => {
    component.openChat();
    const state = crearGestionHistoriasDuplicadasState();
    state.estado = 'SOLICITANDO_CONTRASENA';
    state.idHistoriaPrincipal = 19;
    state.idHistoriaSecundaria = 16;
    state.analisis = {
      tipoDuplicidad: 'MISMO_PACIENTE', idHistoriaClinicaRecomendada: 19, motivosRecomendacion: [],
      resumenComparativo: 'Comparación', futuraFusionPermitida: true, tokenAnalisis: 'token-vigente',
      posiblesCoincidencias: [], advertenciasIntegridad: [], mensaje: 'Listo', historiasComparadas: [
        { idHistoriaClinica: 19, idPaciente: 3, nombreCompleto: 'Paciente de prueba', cantidadConsultas: 0, cantidadConsultasAtendidas: 0,
          cantidadConsultasPendientes: 0, camposClinicosInformados: 0, puntajeRiquezaClinica: 0,
          cantidadConsultasExclusivas: 0, consultasExclusivas: [] },
        { idHistoriaClinica: 16, idPaciente: 3, nombreCompleto: 'Paciente de prueba', cantidadConsultas: 0, cantidadConsultasAtendidas: 0,
          cantidadConsultasPendientes: 0, camposClinicosInformados: 0, puntajeRiquezaClinica: 0,
          cantidadConsultasExclusivas: 0, consultasExclusivas: [] }
      ]
    };
    const respuestaPendiente = new Subject<any>();
    historiasDuplicadasService.fusionar.and.returnValue(respuestaPendiente.asObservable());
    (component as any).addDuplicateHistoriesBlock(state, 'password', true);
    fixture.detectChanges();
    const instanciaInicial = fixture.debugElement.query(By.directive(GestionHistoriasDuplicadasChatComponent))
      .componentInstance as GestionHistoriasDuplicadasChatComponent;
    const destruccionSpy = spyOn(instanciaInicial, 'ngOnDestroy').and.callThrough();

    instanciaInicial.password = 'incorrecta';
    instanciaInicial.fusionar();
    fixture.detectChanges();

    const instanciaFusionando = fixture.debugElement.query(By.directive(GestionHistoriasDuplicadasChatComponent)).componentInstance;
    expect(instanciaFusionando).toBe(instanciaInicial);
    expect(destruccionSpy).not.toHaveBeenCalled();
    expect(respuestaPendiente.observed).toBeTrue();
    expect(state.estado).toBe('FUSIONANDO');

    respuestaPendiente.error({ status: 401, error: {
      resultado: 'CONTRASENA_INCORRECTA', mensaje: 'La contraseña ingresada no es correcta.'
    } });
    fixture.detectChanges();

    expect(state.estado).toBe('SOLICITANDO_CONTRASENA');
    expect(fixture.debugElement.query(By.directive(GestionHistoriasDuplicadasChatComponent)).componentInstance).toBe(instanciaInicial);
    expect(destruccionSpy).not.toHaveBeenCalled();
    expect(component.messages.filter(mensaje => mensaje.historiasDuplicadas === state).length).toBe(1);
    expect(instanciaInicial.password).toBe('');
    const posicionMensajeError = component.messages.findIndex(mensaje => mensaje.text?.includes('Inténtalo nuevamente'));
    const posicionFormulario = component.messages.findIndex(mensaje => mensaje.historiasDuplicadas === state);
    expect(posicionMensajeError).toBeGreaterThanOrEqual(0);
    expect(posicionFormulario).toBeGreaterThan(posicionMensajeError);
  });

  it('reemplaza el loader de análisis sin conservarlo en la conversación', () => {
    const state = crearGestionHistoriasDuplicadasState();
    state.estado = 'MOSTRANDO_COMPARACION';
    (component as any).addDuplicateHistoriesBlock(state, 'analyzing', true);

    component.manejarMensajeHistoriasDuplicadas(state, {
      remitente: 'bot', texto: 'Análisis finalizado.', vistaSiguiente: 'comparison', reemplazarVistaActiva: true
    });

    const tarjetas = component.messages.filter(mensaje => mensaje.historiasDuplicadas === state);
    expect(tarjetas.length).toBe(1);
    expect(tarjetas[0].duplicateHistoriesView).toBe('comparison');
    expect(component.messages.some(mensaje => mensaje.duplicateHistoriesView === 'analyzing')).toBeFalse();
  });

  it('muestra una sola tarjeta tras una fusión exitosa y elimina la carga temporal', () => {
    const state = crearGestionHistoriasDuplicadasState();
    state.estado = 'COMPLETADO';
    state.respuestaFusion = { fusionada: true, resultado: 'HISTORIAS_FUSIONADAS', mensaje: 'Fusión completada' };
    (component as any).addDuplicateHistoriesBlock(state, 'fusing', true);
    const cantidadInicial = component.messages.length;

    component.manejarMensajeHistoriasDuplicadas(state, {
      remitente: 'bot', texto: 'Se conservaron y fusionaron las historias.', vistaSiguiente: 'success', reemplazarVistaActiva: true
    });

    expect(component.messages.length).toBe(cantidadInicial);
    expect(component.messages.filter(mensaje => mensaje.historiasDuplicadas === state).length).toBe(1);
    expect(component.messages.some(mensaje => mensaje.duplicateHistoriesView === 'fusing')).toBeFalse();
    expect(component.messages.some(mensaje => mensaje.text === 'Se conservaron y fusionaron las historias.')).toBeFalse();
  });

  it('representa un error de fusión una sola vez en la tarjeta activa', () => {
    const state = crearGestionHistoriasDuplicadasState();
    state.estado = 'ERROR'; state.mensajeError = 'No se pudo completar la fusión. No se realizaron cambios.';
    (component as any).addDuplicateHistoriesBlock(state, 'fusing', true);
    const cantidadInicial = component.messages.length;

    component.manejarMensajeHistoriasDuplicadas(state, {
      remitente: 'bot', texto: state.mensajeError, vistaSiguiente: 'error', reemplazarVistaActiva: true
    });

    expect(component.messages.length).toBe(cantidadInicial);
    expect(component.messages.filter(mensaje => mensaje.text === state.mensajeError).length).toBe(0);
    expect(component.messages.find(mensaje => mensaje.historiasDuplicadas === state)?.duplicateHistoriesView).toBe('error');
  });

  it('debe mostrar e iniciar el registro desde Excel en Asistencia guiada sin usar Botpress', () => {
    const pacientes = abrirMenuAsistenciaPacientes();
    const opcion = pacientes.options.find((item: any) => item.label === 'Registrar pacientes desde Excel');
    expect(opcion).toBeTruthy();

    component.selectHistoricalMenuOption(pacientes, opcion);
    fixture.detectChanges();

    expect(component.messages.some(mensaje => mensaje.sender === 'user' && mensaje.text === opcion.label)).toBeTrue();
    expect(pacientes.options.includes(opcion)).toBeFalse();
    expect(component.messages.some(mensaje => mensaje.type === 'patient-import')).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('Descargar plantilla oficial');
    expect(asistenteService.preguntar).not.toHaveBeenCalled();
  });

  it('debe agregar los mensajes de importación al historial general y mantener la tarjeta separada', () => {
    component.openChat();
    iniciarImportacion();
    const importacion = component.importacionComponents.first as ImportacionPacientesChatComponent;

    importacion.yaTengoPlantilla();
    fixture.detectChanges();

    const ultimos = component.messages.slice(-3);
    expect(ultimos[0]).toEqual(jasmine.objectContaining({ type: 'text', sender: 'user', text: 'Ya tengo la plantilla' }));
    expect(ultimos[1]).toEqual(jasmine.objectContaining({ type: 'text', sender: 'bot', text: jasmine.stringContaining('Perfecto. Selecciona la plantilla Excel') }));
    expect(ultimos[2]).toEqual(jasmine.objectContaining({ type: 'patient-import' }));
    const tarjeta: HTMLElement = fixture.nativeElement.querySelector('.import-message-block');
    expect(tarjeta.textContent).not.toContain('Ya tengo la plantilla');
    expect(tarjeta.textContent).not.toContain('Perfecto. Selecciona la plantilla Excel');
    expect(asistenteService.preguntar).not.toHaveBeenCalled();

    component.minimizeChat();
    component.openChat();
    expect(component.messages.slice(-3).map(mensaje => mensaje.type)).toEqual(['text', 'text', 'patient-import']);
  });

  it('debe conservar cards progresivas congeladas y mostrar solo la etapa de cada bloque', () => {
    component.openChat();
    iniciarImportacion();
    const scrollSpy = spyOn(component as any, 'scrollToNewBlock');
    let activa = component.importacionComponents.last;

    activa.yaTengoPlantilla();
    fixture.detectChanges();
    const mensajeUsuario = component.messages.find(mensaje => mensaje.text === 'Ya tengo la plantilla')!;
    expect(scrollSpy).toHaveBeenCalledOnceWith(mensajeUsuario.id);
    expect(component.messages.filter(mensaje => mensaje.type === 'patient-import').map(mensaje => mensaje.importView)).toEqual(['template', 'file-selection']);

    activa = component.importacionComponents.last;
    activa.seleccionarArchivo({ target: { files: [new File(['excel'], 'PacientesV2.xlsx')], value: 'x' } } as unknown as Event);
    fixture.detectChanges();
    expect(component.messages.filter(mensaje => mensaje.type === 'patient-import').map(mensaje => mensaje.importView)).toEqual(['template', 'file-selection', 'file-ready']);
    const ordenArchivo = component.messages.slice(-2);
    expect(ordenArchivo[0]).toEqual(jasmine.objectContaining({ sender: 'bot', text: jasmine.stringContaining('He recibido el archivo «PacientesV2.xlsx»') }));
    expect(ordenArchivo[1]).toEqual(jasmine.objectContaining({ type: 'patient-import', importView: 'file-ready' }));
    const cardsArchivo = Array.from<HTMLElement>(fixture.nativeElement.querySelectorAll('.import-message-block'));
    expect(cardsArchivo[1].textContent).not.toContain('PacientesV2.xlsx');
    expect(cardsArchivo[2].textContent).toContain('PacientesV2.xlsx');

    importacionService.validarArchivo.and.returnValue(of({
      importacionId: 'preview-1', estado: 'PREVISUALIZADA', expiraEn: new Date(Date.now() + 60000).toISOString(),
      resumen: { registrosAnalizados: 1, validos: 1, conErrores: 0, filasConDniDuplicado: 0, gruposDniDuplicados: 0, dniExistentes: 0, conAdvertencias: 0, filasVaciasIgnoradas: 0 },
      filas: [{ numeroFila: 2, nombreCompleto: 'PACIENTE DE PRUEBA', dni: DNI_PRUEBA, estado: 'VALIDO', paciente: {}, antecedentes: {}, errores: [], advertencias: [] }]
    } as any));
    activa = component.importacionComponents.last;
    activa.analizarArchivo();
    fixture.detectChanges();

    const vistas = component.messages.filter(mensaje => mensaje.type === 'patient-import').map(mensaje => mensaje.importView);
    expect(vistas).toEqual(['template', 'file-selection', 'file-ready', 'analysis', 'confirmation']);
    const cards = Array.from<HTMLElement>(fixture.nativeElement.querySelectorAll('.import-message-block'));
    expect(cards[0].textContent).toContain('Paso 1 de 4');
    expect(cards[0].textContent).not.toContain('Paso 2 de 4');
    expect(cards[1].textContent).toContain('Paso 2 de 4');
    expect(cards[1].textContent).not.toContain('Paso 1 de 4');
    expect(cards[2].textContent).toContain('Archivo listo para analizar');
    expect(cards[2].textContent).not.toContain('Paso 3 de 4');
    expect(cards[3].textContent).toContain('Paso 3 de 4');
    expect(cards[3].textContent).not.toContain('Paso 2 de 4');
    expect(cards[4].textContent).toContain('Paso 4 de 4');
    expect(cards[4].textContent).not.toContain('Paso 3 de 4');

    importacionService.confirmarImportacion.and.returnValue(of({
      importacionId: 'preview-1', estado: 'CONFIRMADA',
      resumen: { filasValidasEnPrevisualizacion: 1, pacientesRegistrados: 1, omitidosPorDniExistente: 0, erroresAlRegistrar: 0 },
      resultados: []
    }));
    component.importacionComponents.last.confirmar();
    fixture.detectChanges();
    const final = Array.from<HTMLElement>(fixture.nativeElement.querySelectorAll('.import-message-block')).at(-1)!;
    expect(final.textContent).toContain('Importación completada');
    expect(final.textContent).not.toContain('Paso 1 de 4');
    expect(final.textContent).not.toContain('Paso 4 de 4');
    expect(asistenteService.preguntar).not.toHaveBeenCalled();
  });

  it('debe conservar el estado y archivo de importación al minimizar', () => {
    const mensaje = iniciarImportacion();
    mensaje.importacion.plantillaDescargada = true;
    mensaje.importacion.estado = 'ARCHIVO_SELECCIONADO';
    mensaje.importacion.archivo = new File(['excel'], 'pacientes.xlsx');
    mensaje.importView = 'file-ready';
    component.openChat();

    component.minimizeChat();
    fixture.detectChanges();
    component.openChat();
    fixture.detectChanges();

    const restaurado = component.messages.find(item => item.type === 'patient-import')!;
    expect(restaurado.importacion.archivo.name).toBe('pacientes.xlsx');
    expect(fixture.nativeElement.textContent).toContain('pacientes.xlsx');
  });

  it('debe limpiar el flujo de importación al cerrar el chatbot', () => {
    const mensaje = iniciarImportacion();
    const cancelar = jasmine.createSpy('cancelarSolicitud');
    mensaje.importacion.cancelarSolicitud = cancelar;
    expect(component.messages.some(mensaje => mensaje.type === 'patient-import')).toBeTrue();

    component.minimizeChat();
    fixture.detectChanges();
    component.closeChat();

    expect(cancelar).toHaveBeenCalled();
    expect(component.messages.some(mensaje => mensaje.type === 'patient-import')).toBeFalse();
    expect(component.messages.length).toBe(2);
  });

  it('debe conservar el resultado anterior al registrar otro archivo y crear un bloque nuevo', () => {
    const anterior = iniciarImportacion();
    anterior.importacion.estado = 'CONFIRMADA';
    anterior.importacion.confirmacion = {
      importacionId: 'confirmada-1', estado: 'CONFIRMADA',
      resumen: { filasValidasEnPrevisualizacion: 1, pacientesRegistrados: 1, omitidosPorDniExistente: 0, erroresAlRegistrar: 0 },
      resultados: []
    };

    component.registrarOtroArchivo();

    const importaciones = component.messages.filter(mensaje => mensaje.type === 'patient-import');
    expect(importaciones.length).toBe(2);
    expect(importaciones[0].importacion!.confirmacion!.importacionId).toBe('confirmada-1');
    expect(importaciones[1].importacion!.estado).toBe('PLANTILLA_DESCARGADA');
    expect(asistenteService.preguntar).not.toHaveBeenCalled();
  });

  it('debe mostrar solo las consultas informativas requeridas sobre pacientes', () => {
    const etiquetas = abrirMenuPacientes().options.map((opcion: any) => opcion.label);
    expect(etiquetas).toEqual([
      '¿Cuántos pacientes hay registrados?',
      'Muéstrame los últimos pacientes registrados',
      'Buscar paciente por DNI',
      'Buscar paciente por nombre',
      '¿Cuál es la edad promedio de los pacientes?',
      'Detectar posibles pacientes duplicados'
    ]);
    expect(etiquetas).not.toContain('Registrar pacientes desde Excel');
    expect(etiquetas).not.toContain('Gestionar pacientes duplicados');
  });

  it('debe mostrar solo las consultas médicas informativas requeridas', () => {
    const menus = (component as any).menus;
    expect(menus['consultas'].options.map((opcion: any) => opcion.label)).toEqual([
      '¿Cuántas consultas médicas hay registradas?',
      'Buscar si un paciente tiene consultas médicas',
      '¿Cuál fue la última consulta médica de un paciente?',
      '¿Tiene consultas médicas pendientes?',
      'Consultas médicas atendidas hoy'
    ]);
  });

  it('debe mostrar las dos subsecciones de Asistencia guiada y conservar el historial y scroll', () => {
    const principal = component.messages[1];
    const scrollSpy = spyOn(component as any, 'scrollToNewBlock');
    component.selectHistoricalMenuOption(principal, principal.options!.find(opcion => opcion.label === 'Asistencia guiada')!);

    const asistencia = component.messages.at(-1)!;
    expect(component.messages.at(-2)?.text).toBe('¿Qué proceso deseas realizar con ayuda del asistente? Selecciona una opción o escribe tu solicitud.');
    expect(asistencia.menuId).toBe('asistencia');
    expect(asistencia.options?.map(opcion => opcion.label)).toEqual(['Pacientes', 'Historias clínicas']);
    expect(component.messages).toContain(principal);
    expect(principal.options?.some(opcion => opcion.label === 'Asistencia guiada')).toBeFalse();
    expect(scrollSpy).toHaveBeenCalledOnceWith(jasmine.any(String));
  });

  it('debe conservar los tipos especializados de las acciones trasladadas', () => {
    const menus = (component as any).menus;
    expect(menus['asistencia-pacientes'].options).toEqual([
      jasmine.objectContaining({ label: 'Registrar pacientes desde Excel', action: 'patient-import-flow' }),
      jasmine.objectContaining({ label: 'Gestionar pacientes duplicados', action: 'patient-duplicate-flow' })
    ]);
    expect(menus['asistencia-historias'].options).toEqual([
      jasmine.objectContaining({ label: 'Crear una historia clínica con el asistente', action: 'clinical-history-flow' }),
      jasmine.objectContaining({ label: 'Crear historias clínicas faltantes', action: 'missing-clinical-histories-flow' }),
      jasmine.objectContaining({ label: 'Analizar historias clínicas duplicadas', action: 'clinical-history-duplicate-flow' })
    ]);
    expect(menus['pacientes'].options).toContain(jasmine.objectContaining({
      label: 'Detectar posibles pacientes duplicados', action: 'request'
    }));
  });

  it('debe terminar la introducción antes de insertar las opciones de asistencia de historias', fakeAsync(() => {
    const principal = component.messages.find(mensaje => mensaje.menuId === 'principal')!;
    component.selectHistoricalMenuOption(principal, principal.options!.find(opcion => opcion.label === 'Asistencia guiada')!);
    const asistencia = component.messages.find(mensaje => mensaje.menuId === 'asistencia')!;
    const scrollSpy = spyOn(component as any, 'scrollToNewBlock');

    component.selectHistoricalMenuOption(asistencia, asistencia.options!.find(opcion => opcion.label === 'Historias clínicas')!);
    fixture.detectChanges();

    const seleccion = component.messages.find(mensaje => mensaje.sender === 'user' && mensaje.text === 'Historias clínicas')!;
    const introduccion = component.messages.find(mensaje => mensaje.text === 'Selecciona una opción o escribe tu solicitud sobre historias clínicas.')!;
    expect(introduccion.presentationState).not.toBe('visible');
    expect(component.asistenteEscribiendo).toBeTrue();
    expect(component.messages.some(mensaje => mensaje.menuId === 'asistencia-historias')).toBeFalse();
    expect(scrollSpy).toHaveBeenCalledOnceWith(seleccion.id);

    tick(10_000);
    fixture.detectChanges();

    const menu = component.messages.find(mensaje => mensaje.menuId === 'asistencia-historias')!;
    expect(introduccion.presentationState).toBe('visible');
    expect(component.asistenteEscribiendo).toBeFalse();
    expect(menu.options?.map(opcion => opcion.icon)).toEqual(['pi pi-file-plus', 'pi pi-list-check', 'pi pi-clone']);
    expect(scrollSpy).toHaveBeenCalledTimes(1);
  }));

  it('no debe contener datos personales de ejemplo en los textos configurados del frontend', () => {
    const textosMenus = JSON.stringify((component as any).menus);
    expect(textosMenus).not.toMatch(/\b\d{8}\b/);
    expect(textosMenus).not.toMatch(/\bID\s+\d+\b/i);
    expect(textosMenus).toContain('(PONER DNI)');
    expect(textosMenus).toContain('(AGREGAR NOMBRE Y DOS APELLIDOS)');
  });

  it('debe iniciar localmente el flujo y solicitar el DNI sin consultar al asistente', () => {
    iniciarFlujoHistoriaClinica();

    expect(component.clinicalHistoryFlow).toEqual({ step: 'awaitingDni' });
    expect(component.messages.at(-1)?.text).toBe('Ingresa el DNI de ocho dígitos del paciente que deseas utilizar para crear la historia clínica. Puedes cancelar la asistencia en cualquier momento pulsando “Cancelar”.');
    expect(asistenteService.preguntar).not.toHaveBeenCalled();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.clinical-history-flow-actions button')?.textContent).toContain('Cancelar');
  });

  it('debe anclar el scroll en el mensaje DNI sin bajar hasta el paciente encontrado', () => {
    iniciarFlujoHistoriaClinica();
    const scrollBottomSpy = spyOn(component, 'scrollToBottom');
    const scrollNewBlockSpy = spyOn(component as any, 'scrollToNewBlock');

    enviarDni(DNI_PRUEBA);

    const dniMessage = component.messages.find(message => message.sender === 'user' && message.text === DNI_PRUEBA)!;
    expect(scrollNewBlockSpy).toHaveBeenCalledOnceWith(dniMessage.id);
    expect(scrollBottomSpy).not.toHaveBeenCalled();
    expect(component.messages.some(message => message.text?.includes('Paciente encontrado:'))).toBeTrue();
  });

  [
    { name: 'inválido', configure: () => undefined, dni: '12A45678' },
    { name: 'inexistente', configure: () => historiaClinicaService.buscarPacientesPorDni.and.returnValue(of([])), dni: DNI_PRUEBA },
    { name: 'duplicado', configure: () => historiaClinicaService.buscarPacientesPorDni.and.returnValue(of([paciente, { ...paciente, idPaciente: 9 }])), dni: DNI_PRUEBA },
    { name: 'con error HTTP', configure: () => historiaClinicaService.buscarPacientesPorDni.and.returnValue(throwError(() => new Error('error HTTP'))), dni: DNI_PRUEBA }
  ].forEach(testCase => {
    it(`no debe forzar el scroll final para un DNI ${testCase.name}`, () => {
      iniciarFlujoHistoriaClinica();
      testCase.configure();
      const scrollBottomSpy = spyOn(component, 'scrollToBottom');
      const scrollNewBlockSpy = spyOn(component as any, 'scrollToNewBlock');

      enviarDni(testCase.dni);

      const dniMessage = component.messages.find(message => message.sender === 'user' && message.text === testCase.dni)!;
      expect(scrollNewBlockSpy).toHaveBeenCalledOnceWith(dniMessage.id);
      expect(scrollBottomSpy).not.toHaveBeenCalled();
    });
  });

  ['', '12A45678', '1234567', '(PONER DNI)9'].forEach(dni => {
    it(`debe rechazar el DNI inválido ${dni || 'vacío'} sin consultar servicios`, () => {
      iniciarFlujoHistoriaClinica();
      enviarDni(dni);

      expect(component.clinicalHistoryFlow).toEqual({ step: 'awaitingDni' });
      expect(component.messages.at(-1)?.text).toBe('El DNI debe contener exactamente ocho dígitos. Inténtalo nuevamente o cancela la operación.');
      expect(historiaClinicaService.buscarPacientesPorDni).not.toHaveBeenCalled();
      expect(antecedentesService.getByPacienteId).not.toHaveBeenCalled();
    });
  });

  it('debe recortar espacios, conservar el cero inicial y buscar una sola vez', () => {
    iniciarFlujoHistoriaClinica();
    enviarDni('  (PONER DNI)  ');

    expect(historiaClinicaService.buscarPacientesPorDni).toHaveBeenCalledOnceWith(DNI_PRUEBA);
    expect(component.clinicalHistoryFlow.step).toBe('awaitingConfirmation');
    expect((component.clinicalHistoryFlow as any).dni).toBe(DNI_PRUEBA);
    expect(typeof (component.clinicalHistoryFlow as any).dni).toBe('string');
    expect(asistenteService.preguntar).not.toHaveBeenCalled();
  });

  it('debe aceptar solo coincidencias defensivas exactas y permitir reintentar cuando no existen', () => {
    historiaClinicaService.buscarPacientesPorDni.and.returnValue(of([{ ...paciente, dni: DNI_INEXISTENTE, numDocumento: DNI_INEXISTENTE }]));
    iniciarFlujoHistoriaClinica();
    enviarDni(DNI_PRUEBA);

    expect(component.clinicalHistoryFlow).toEqual({ step: 'awaitingDni' });
    expect(component.messages.at(-1)?.text).toBe('No se encontró un paciente registrado con el DNI indicado. Verifica el número e inténtalo nuevamente con el DNI de un paciente existente.');
    expect(antecedentesService.getByPacienteId).not.toHaveBeenCalled();
    expect(historiaClinicaService.getByPaciente).not.toHaveBeenCalled();

    historiaClinicaService.buscarPacientesPorDni.and.returnValue(of([paciente]));
    enviarDni(DNI_PRUEBA);
    expect(historiaClinicaService.buscarPacientesPorDni).toHaveBeenCalledTimes(2);
    expect(component.clinicalHistoryFlow.step).toBe('awaitingConfirmation');
  });

  it('debe mantener la captura activa ante varios pacientes sin seleccionar el primero', () => {
    historiaClinicaService.buscarPacientesPorDni.and.returnValue(of([paciente, { ...paciente, idPaciente: 9 }]));
    iniciarFlujoHistoriaClinica();
    enviarDni(DNI_PRUEBA);

    expect(component.clinicalHistoryFlow).toEqual({ step: 'awaitingDni' });
    expect(JSON.stringify(component.clinicalHistoryFlow)).not.toContain('idPaciente');
    expect(JSON.stringify(component.clinicalHistoryFlow)).not.toContain('prefill');
    expect(component.messages.at(-1)?.text).toBe('Se encontraron varios pacientes con el mismo DNI. Por seguridad, no se puede seleccionar automáticamente uno de ellos. Ingresa otro DNI o cancela la operación.');
    expect(antecedentesService.getByPacienteId).not.toHaveBeenCalled();
    expect(historiaClinicaService.getByPaciente).not.toHaveBeenCalled();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.cancel-action')?.textContent).toContain('Cancelar');
  });

  it('debe permitir buscar otro DNI después del conflicto sin enviarlo al asistente', () => {
    const segundoPaciente = { ...paciente, idPaciente: 10, dni: OTRO_DNI_PRUEBA, numDocumento: OTRO_DNI_PRUEBA };
    historiaClinicaService.buscarPacientesPorDni.and.callFake(dni => dni === DNI_PRUEBA
      ? of([paciente, { ...paciente, idPaciente: 9 }])
      : of([segundoPaciente]));
    iniciarFlujoHistoriaClinica();
    enviarDni(DNI_PRUEBA);

    enviarDni(OTRO_DNI_PRUEBA);

    expect(historiaClinicaService.buscarPacientesPorDni.calls.allArgs()).toEqual([[DNI_PRUEBA], [OTRO_DNI_PRUEBA]]);
    expect(asistenteService.preguntar).not.toHaveBeenCalled();
    expect(antecedentesService.getByPacienteId).toHaveBeenCalledOnceWith(10);
    expect(historiaClinicaService.getByPaciente).toHaveBeenCalledOnceWith(10);
    expect(component.clinicalHistoryFlow.step).toBe('awaitingConfirmation');
    expect((component.clinicalHistoryFlow as any).dni).toBe(OTRO_DNI_PRUEBA);

    component.cancelClinicalHistoryFlow();
    expect(component.clinicalHistoryFlow).toEqual({ step: 'idle' });
  });

  it('debe consultar en paralelo antecedentes e historias para un paciente único', () => {
    iniciarFlujoHistoriaClinica();
    enviarDni(DNI_PRUEBA);

    expect(antecedentesService.getByPacienteId).toHaveBeenCalledOnceWith(8);
    expect(historiaClinicaService.getByPaciente).toHaveBeenCalledOnceWith(8);
    expect(component.clinicalHistoryFlow.step).toBe('awaitingConfirmation');
  });

  it('debe representar antecedentes inexistentes con null sin inventar datos', () => {
    iniciarFlujoHistoriaClinica();
    enviarDni(DNI_PRUEBA);

    const prefill = (component.clinicalHistoryFlow as any).prefill;
    expect(prefill.enfermedadesPrevias).toBeNull();
    expect(prefill.cirugiasPrevias).toBeNull();
    expect(prefill.alergiaMedicamentos).toBeNull();
  });

  it('debe mostrar la orientación completa antes de habilitar los controles de confirmación', fakeAsync(() => {
    antecedentesService.getByPacienteId.and.returnValue(of({ enfermedadesPrevias: 'Asma severa', cirugiasPrevias: 'Apendicectomía', alergiaMedicamentos: 'Penicilina' }));
    iniciarFlujoHistoriaClinica();
    enviarDni(DNI_PRUEBA);
    fixture.detectChanges();
    const resumen = component.messages.find(message => message.text?.startsWith('Paciente encontrado:'))?.text ?? '';
    const orientacion = component.messages.find(message => message.text?.startsWith('Revisa los datos del paciente encontrado.'))!;

    expect(resumen).toContain('Nombre: NOMBRE PRUEBA APELLIDO UNO APELLIDO DOS');
    expect(resumen).toContain('Fecha de nacimiento: 01/01/1992');
    expect(resumen).toContain('Estado civil: Soltero(a)');
    expect(resumen).toContain('Historias clínicas existentes: 0');
    expect(resumen).not.toContain('Asma severa');
    expect(resumen).not.toContain('Apendicectomía');
    expect(resumen).not.toContain('Penicilina');
    expect(orientacion.presentationState).not.toBe('visible');
    expect(fixture.nativeElement.querySelector('.continue-action')).toBeNull();
    expect(fixture.nativeElement.querySelector('.cancel-action')).toBeNull();
    tick(30_000);
    fixture.detectChanges();
    expect(orientacion.presentationState).toBe('visible');
    expect(fixture.nativeElement.querySelector('.continue-action')?.textContent).toContain('Continuar');
    expect(fixture.nativeElement.querySelector('.cancel-action')?.textContent).toContain('Cancelar');
  }));

  it('debe mostrar la cantidad de varias historias existentes', () => {
    historiaClinicaService.getByPaciente.and.returnValue(of([{ idHistoriaClinica: 1 }, { idHistoriaClinica: 2 }, { idHistoriaClinica: 3 }]));
    iniciarFlujoHistoriaClinica();
    enviarDni(DNI_PRUEBA);

    expect(component.messages.some(message => message.text?.includes('Historias clínicas existentes: 3'))).toBeTrue();
  });

  it('debe ignorar Continuar fuera de awaitingConfirmation', () => {
    spyOn(transferService, 'createTransfer').and.callThrough();

    component.continueClinicalHistoryFlow();

    expect(transferService.createTransfer).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('debe crear una transferencia y navegar con state mínimo al continuar', fakeAsync(() => {
    const createTransferSpy = spyOn(transferService, 'createTransfer').and.callThrough();
    iniciarFlujoHistoriaClinica();
    enviarDni(DNI_PRUEBA);
    tick(30_000);
    fixture.detectChanges();
    const scrollBottomSpy = spyOn(component, 'scrollToBottom');
    const scrollNewBlockSpy = spyOn(component as any, 'scrollToNewBlock');

    component.continueClinicalHistoryFlow();
    const transferId = createTransferSpy.calls.mostRecent().returnValue;
    component.continueClinicalHistoryFlow();

    expect(createTransferSpy).toHaveBeenCalledTimes(1);
    const candidate = createTransferSpy.calls.mostRecent().args[0];
    expect(candidate).toEqual(jasmine.objectContaining({
      idPaciente: 8, dni: DNI_PRUEBA, nombres: 'NOMBRE PRUEBA', apellidos: 'APELLIDO UNO APELLIDO DOS',
      fechaIngreso: '2020-03-10', fechaNacimiento: '1992-01-01', estadoCivil: 'SOLTERO'
    }));
    expect(candidate as any).not.toEqual(jasmine.objectContaining({ nombreCompleto: jasmine.anything(), existingClinicalHistoryCount: jasmine.anything() }));
    expect(router.navigate).not.toHaveBeenCalled();
    expect(component.messages.at(-1)?.text).toBe('Abriré Nueva Historia Clínica con los datos del paciente seleccionado.');
    tick(10_000);
    expect(router.navigate).toHaveBeenCalledOnceWith(
      ['/historiaClinica', 'mantenimiento-historias-clinicas', 'nuevo'],
      { state: { source: 'chatbot', transferId } }
    );
    const navigation = router.navigate.calls.mostRecent().args;
    expect(JSON.stringify(navigation)).not.toContain(DNI_PRUEBA);
    expect(asistenteService.preguntar).not.toHaveBeenCalled();
    expect(historiaClinicaService.insert).not.toHaveBeenCalled();
    expect(historiaClinicaService.update).not.toHaveBeenCalled();
    const continueMessage = component.messages.find(message => message.sender === 'user' && message.text === 'Continuar')!;
    expect(scrollNewBlockSpy).toHaveBeenCalledOnceWith(continueMessage.id);
    expect(scrollBottomSpy).not.toHaveBeenCalled();

    tick();

    expect(component.clinicalHistoryFlow).toEqual({ step: 'idle' });
    expect(transferService.peekTransfer(transferId)).not.toBeNull();
  }));

  it('debe revocar la transferencia y conservar la confirmación si falla la navegación', fakeAsync(() => {
    router.navigate.and.returnValue(Promise.resolve(false));
    spyOn(transferService, 'revokeTransfer').and.callThrough();
    iniciarFlujoHistoriaClinica();
    enviarDni(DNI_PRUEBA);
    tick(30_000);
    fixture.detectChanges();

    component.continueClinicalHistoryFlow();
    const transferId = (component.clinicalHistoryFlow as any).transferId;
    tick(10_000);

    expect(transferService.revokeTransfer).toHaveBeenCalledOnceWith(transferId);
    expect(transferService.peekTransfer(transferId)).toBeNull();
    expect(component.clinicalHistoryFlow.step).toBe('awaitingConfirmation');
    expect((component.clinicalHistoryFlow as any).dni).toBe(DNI_PRUEBA);
    expect(component.messages.at(-1)?.text).toBe('No se pudo abrir el formulario de Nueva Historia Clínica. Inténtalo nuevamente.');
  }));

  it('debe cancelar y limpiar todos los datos temporales conservando el historial', () => {
    spyOn(transferService, 'createTransfer').and.callThrough();
    iniciarFlujoHistoriaClinica();
    enviarDni(DNI_PRUEBA);
    const mensajesAntes = component.messages.length;

    component.cancelClinicalHistoryFlow();

    expect(component.clinicalHistoryFlow).toEqual({ step: 'idle' });
    expect(JSON.stringify(component.clinicalHistoryFlow)).not.toContain(DNI_PRUEBA);
    expect(component.messages.length).toBe(mensajesAntes + 2);
    expect(component.messages.at(-1)?.text).toBe('La creación de la historia clínica fue cancelada.');
    expect(transferService.createTransfer).not.toHaveBeenCalled();
  });

  it('debe limpiar el flujo al volver al Menú principal', () => {
    spyOn(transferService, 'createTransfer').and.callThrough();
    iniciarFlujoHistoriaClinica();
    enviarDni(DNI_PRUEBA);

    component.quickAsk('Menú principal');

    expect(component.clinicalHistoryFlow).toEqual({ step: 'idle' });
    expect(component.messages.at(-1)).toEqual(jasmine.objectContaining({ type: 'menu', menuId: 'principal' }));
    expect(transferService.createTransfer).not.toHaveBeenCalled();
  });

  it('debe recuperarse de un error al buscar el paciente', () => {
    historiaClinicaService.buscarPacientesPorDni.and.returnValue(throwError(() => new Error('falló búsqueda')));
    iniciarFlujoHistoriaClinica();
    enviarDni(DNI_PRUEBA);

    expect(component.clinicalHistoryFlow).toEqual({ step: 'awaitingDni' });
    expect(component.messages.at(-1)?.text).toBe('No se pudo consultar la información del paciente en este momento. Inténtalo nuevamente.');
    expect(component.isLoading).toBeFalse();
  });

  it('debe recuperarse si falla la consulta de antecedentes o historias', () => {
    antecedentesService.getByPacienteId.and.returnValue(throwError(() => new Error('falló antecedentes')));
    iniciarFlujoHistoriaClinica();
    enviarDni(DNI_PRUEBA);

    expect(component.clinicalHistoryFlow).toEqual({ step: 'awaitingDni' });
    expect(component.messages.at(-1)?.text).toContain('No se pudo consultar la información');
    expect(component.messages.some(mensaje => mensaje.text?.includes('¿Deseas continuar'))).toBeFalse();
  });

  it('debe recuperarse si falla específicamente la consulta de historias', () => {
    historiaClinicaService.getByPaciente.and.returnValue(throwError(() => new Error('falló historias')));
    iniciarFlujoHistoriaClinica();
    enviarDni(DNI_PRUEBA);

    expect(component.clinicalHistoryFlow).toEqual({ step: 'awaitingDni' });
    expect(component.messages.at(-1)?.text).toBe('No se pudo consultar la información del paciente en este momento. Inténtalo nuevamente.');
    expect(component.messages.some(mensaje => mensaje.text?.includes('¿Deseas continuar'))).toBeFalse();
  });

  it('debe enviar las opciones request al backend y mostrar su respuesta', fakeAsync(() => {
    component.quickAsk('¿Qué preguntas puedo hacer?');
    tick();

    expect(asistenteService.preguntar).toHaveBeenCalledOnceWith('¿Qué preguntas puedo hacer?');
    expect(component.messages.at(-1)?.text).toBe('Respuesta del asistente');
    expect(component.isLoading).toBeFalse();
  }));

  it('debe retirar la recomendación de historia clínica solo de la búsqueda informativa por DNI', () => {
    const respuesta = [
      'ID: 8',
      'Nombre: PACIENTE PRUEBA',
      `DNI: ${DNI_PRUEBA}`,
      'Fecha de registro: 20/08/2026',
      '',
      'No se recomienda crear una nueva historia clínica para este paciente.'
    ].join('\n');

    const resultado = (component as any).formatResponse({ intencion: 'BUSQUEDA_PACIENTE_DNI', respuesta });

    expect(resultado).toContain('ID: 8');
    expect(resultado).toContain(`DNI: ${DNI_PRUEBA}`);
    expect(resultado).toContain('Fecha de registro: 20/08/2026');
    expect(resultado).not.toContain('No se recomienda crear una nueva historia clínica');
  });

  it('debe conservar recomendaciones de historia clínica fuera de la búsqueda informativa por DNI', () => {
    const respuesta = 'No se recomienda crear una nueva historia clínica para este paciente.';

    const resultado = (component as any).formatResponse({ intencion: 'VALIDACION_HISTORIA_CLINICA', respuesta });

    expect(resultado).toBe(respuesta);
  });

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

  it('debe restaurar la posición de scroll guardada después de minimizar', fakeAsync(() => {
    component.openChat();
    fixture.detectChanges();
    const firstBody: HTMLElement = fixture.nativeElement.querySelector('.chatbot-body');
    firstBody.scrollTop = 73;

    component.minimizeChat();
    fixture.detectChanges();
    component.openChat();
    fixture.detectChanges();
    const restoredBody: HTMLElement = fixture.nativeElement.querySelector('.chatbot-body');
    tick(20);

    expect(restoredBody.scrollTop).toBe(73);
  }));

  it('debe conservar awaitingConfirmation y sus datos al minimizar y reabrir', () => {
    component.openChat();
    iniciarFlujoHistoriaClinica();
    enviarDni(DNI_PRUEBA);

    component.minimizeChat();
    component.openChat();

    expect(component.clinicalHistoryFlow.step).toBe('awaitingConfirmation');
    expect((component.clinicalHistoryFlow as any).prefill.idPaciente).toBe(8);
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

  it('debe cancelar la solicitud y limpiar el flujo especializado al cerrar el chat', () => {
    spyOn(transferService, 'createTransfer').and.callThrough();
    const busquedaPendiente = new Subject<any[]>();
    historiaClinicaService.buscarPacientesPorDni.and.returnValue(busquedaPendiente);
    iniciarFlujoHistoriaClinica();
    enviarDni(DNI_PRUEBA);
    expect(busquedaPendiente.observed).toBeTrue();

    component.closeChat();

    expect(component.clinicalHistoryFlow).toEqual({ step: 'idle' });
    expect(busquedaPendiente.observed).toBeFalse();
    expect(transferService.createTransfer).not.toHaveBeenCalled();
  });

  it('debe cancelar la solicitud y reiniciar la conversación al cerrar sesión', () => {
    spyOn(transferService, 'createTransfer').and.callThrough();
    const busquedaPendiente = new Subject<any[]>();
    historiaClinicaService.buscarPacientesPorDni.and.returnValue(busquedaPendiente);
    component.openChat();
    iniciarFlujoHistoriaClinica();
    enviarDni(DNI_PRUEBA);

    logoutSubject.next();

    expect(component.isOpen).toBeFalse();
    expect(component.messages.length).toBe(2);
    expect(component.messages[0].text).toContain('Hola, soy el Asistente IA');
    expect(component.clinicalHistoryFlow).toEqual({ step: 'idle' });
    expect(busquedaPendiente.observed).toBeFalse();
    expect(transferService.createTransfer).not.toHaveBeenCalled();
  });

  it('debe cancelar una solicitud activa desde el botón Cancelar', () => {
    const busquedaPendiente = new Subject<any[]>();
    historiaClinicaService.buscarPacientesPorDni.and.returnValue(busquedaPendiente);
    iniciarFlujoHistoriaClinica();
    enviarDni(DNI_PRUEBA);

    component.cancelClinicalHistoryFlow();

    expect(busquedaPendiente.observed).toBeFalse();
    expect(component.isLoading).toBeFalse();
    expect(component.clinicalHistoryFlow).toEqual({ step: 'idle' });
  });

  it('debe mostrar la gestión guiada de duplicados para administrador y enfermería', () => {
    expect(abrirMenuAsistenciaPacientes().options.some((opcion: any) => opcion.label === 'Gestionar pacientes duplicados')).toBeTrue();

    fixture.destroy();
    authServiceMock.usuario.cargo = ' ENFERMERA(O) ';
    fixture = TestBed.createComponent(InterfazChatComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    expect(abrirMenuAsistenciaPacientes().options.some((opcion: any) => opcion.label === 'Gestionar pacientes duplicados')).toBeTrue();
  });

  it('debe ocultar la opción al doctor y rechazar también la intención escrita', () => {
    authServiceMock.usuario.cargo = 'MÉDICO';
    const pacientes = abrirMenuAsistenciaPacientes();
    expect(pacientes.options.some((opcion: any) => opcion.label === 'Gestionar pacientes duplicados')).toBeFalse();

    component.userMessage = 'Quiero eliminar un paciente duplicado';
    component.sendMessage();

    expect(component.messages.at(-1)?.text).toBe('La gestión de registros duplicados está disponible únicamente para personal autorizado.');
    expect(component.messages.some(mensaje => mensaje.type === 'duplicate-management')).toBeFalse();
    expect(asistenteService.preguntar).not.toHaveBeenCalled();
  });

  [
    'Eliminar paciente duplicado',
    'Eliminar un paciente duplicado',
    'Archivar paciente duplicado',
    'Archivar un registro duplicado',
    'Gestionar paciente duplicado',
    'Gestionar duplicados',
    'Decidir cuál paciente conservar'
  ].forEach(frase => {
    it(`debe iniciar localmente el flujo para la intención: ${frase}`, () => {
      component.userMessage = frase;
      component.sendMessage();
      fixture.detectChanges();

      expect(component.messages.some(mensaje => mensaje.type === 'duplicate-management' && mensaje.duplicateView === 'dni')).toBeTrue();
      expect(component.messages.some(mensaje => mensaje.sender === 'user' && mensaje.text === frase)).toBeTrue();
      expect(asistenteService.preguntar).not.toHaveBeenCalled();
      expect(fixture.nativeElement.querySelector('app-gestion-duplicados-chat')).not.toBeNull();
    });
  });



  [
    '¿Existen pacientes duplicados?',
    'Verifica si hay pacientes repetidos',
    'Analiza posibles duplicados',
    'Busca pacientes duplicados'
  ].forEach(frase => {
    it(`debe enviar al backend la consulta general de duplicados: ${frase}`, fakeAsync(() => {
      asistenteService.preguntar.and.returnValue(of({
        intencion: 'ANALISIS_DUPLICADOS_PACIENTES',
        respuesta: 'Se encontraron posibles pacientes duplicados: ID: 1 DNI: (PONER DNI) ID: 2 DNI: (PONER DNI)',
        datos: { cantidad: 2, resultados: [] }
      } as any));

      component.userMessage = frase;
      component.sendMessage();
      fixture.detectChanges();

      expect(component.messages.some(mensaje => mensaje.type === 'duplicate-management')).toBeFalse();
      expect(component.messages.some(mensaje => mensaje.text === 'Ingresa el DNI de ocho dígitos del paciente duplicado que deseas revisar.')).toBeFalse();
      expect(asistenteService.preguntar).toHaveBeenCalledOnceWith(frase);
      expect(component.messages.some(mensaje => mensaje.text?.includes('Se encontraron posibles pacientes duplicados'))).toBeTrue();
      expect(component.messages.some(mensaje => mensaje.menuId === 'contextual-action')).toBeFalse();
      tick(10_000);
      expect(component.messages.at(-1)?.options?.[0].label).toBe('Gestionar pacientes duplicados');
    }));
  });

  [
    '¿Existen historias clínicas duplicadas?',
    'Busca historias clínicas repetidas',
    'Revisa la duplicidad de historias clínicas',
    'Detecta historias clínicas duplicadas',
    'Busca pacientes con más de una historia clínica',
    '¿El DNI (PONER DNI) tiene historias clínicas duplicadas?',
    'Busca historias repetidas del DNI (PONER DNI)',
    'Verifica historias clínicas del paciente con DNI (PONER DNI)'
  ].forEach(frase => {
    it(`debe consultar historias clínicas duplicadas sin activar archivado: ${frase}`, fakeAsync(() => {
      asistenteService.preguntar.and.returnValue(of({
        intencion: 'HISTORIAS_CLINICAS_DUPLICADAS',
        respuesta: 'Se encontraron 2 posibles historias clínicas duplicadas para el DNI (PONER DNI).\n\nID historia clínica: 12\nConsultas asociadas: 3\nEstado de la historia: ACTIVA\n\nSe recomienda conservar la historia clínica ID 12.',
        datos: { hayDuplicados: true, duplicados: [] }
      } as any));

      component.userMessage = frase;
      component.sendMessage();
      fixture.detectChanges();

      expect(component.messages.some(mensaje => mensaje.type === 'duplicate-management')).toBeFalse();
      expect(component.messages.some(mensaje => mensaje.text?.includes('ID historia clínica: 12'))).toBeTrue();
      expect(component.messages.some(mensaje => mensaje.menuId === 'contextual-action')).toBeFalse();
      tick(10_000);
      expect(component.messages.at(-1)?.options?.[0].label).toBe('Analizar historias clínicas duplicadas');
      expect(asistenteService.preguntar).toHaveBeenCalledOnceWith(frase);
    }));
  });

  it('no ofrece acciones contextuales cuando no existen duplicados', () => {
    asistenteService.preguntar.and.returnValues(
      of({ intencion: 'ANALISIS_DUPLICADOS_SIN_RESULTADOS', respuesta: 'No existen pacientes duplicados.', datos: { cantidad: 0 } } as any),
      of({ intencion: 'HISTORIAS_CLINICAS_DUPLICADAS', respuesta: 'No existen historias duplicadas.', datos: { hayDuplicados: false, duplicados: [] } } as any)
    );

    component.userMessage = '¿Existen pacientes duplicados?';
    component.sendMessage();
    component.userMessage = '¿Existen historias clínicas duplicadas?';
    component.sendMessage();

    const etiquetas = component.messages.flatMap(mensaje => mensaje.options?.map(opcion => opcion.label) ?? []);
    expect(etiquetas).not.toContain('Gestionar pacientes duplicados');
    expect(etiquetas).not.toContain('Analizar historias clínicas duplicadas');
  });

  it('muestra el mensaje progresivo antes del CTA y abre el mismo flujo de pacientes', fakeAsync(() => {
    asistenteService.preguntar.and.returnValue(of({
      intencion: 'ANALISIS_DUPLICADOS_PACIENTES', respuesta: 'Se encontraron dos pacientes.',
      datos: { cantidad: 2, resultados: [{}, {}] }
    } as any));

    component.userMessage = '¿Existen pacientes duplicados?';
    component.sendMessage();
    const ayuda = component.messages.find(mensaje => mensaje.text?.startsWith('Si deseas, puedo ayudarte'))!;
    expect(ayuda.presentationState).not.toBe('visible');
    expect(component.asistenteEscribiendo).toBeTrue();
    expect(component.messages.some(mensaje => mensaje.menuId === 'contextual-action')).toBeFalse();

    tick(10_000);
    const cta = component.messages.find(mensaje => mensaje.menuId === 'contextual-action')!;
    expect(component.messages.indexOf(ayuda)).toBeLessThan(component.messages.indexOf(cta));
    expect(component.asistenteEscribiendo).toBeFalse();
    component.selectHistoricalMenuOption(cta, cta.options![0]);

    expect(cta.presentationState).toBe('visible');
    expect(component.messages.some(mensaje => mensaje.type === 'duplicate-management' && mensaje.duplicateView === 'dni')).toBeTrue();
    expect(duplicadosService.analizar).not.toHaveBeenCalled();
  }));

  it('abre desde el CTA el componente existente de historias y vuelve a validar en backend', fakeAsync(() => {
    asistenteService.preguntar.and.returnValue(of({
      intencion: 'HISTORIAS_CLINICAS_DUPLICADAS', respuesta: 'Se encontraron historias duplicadas.',
      datos: { hayDuplicados: true, duplicados: [{}, {}] }
    } as any));

    component.userMessage = '¿Existen historias clínicas duplicadas?';
    component.sendMessage();
    const ayuda = component.messages.find(mensaje => mensaje.text?.startsWith('Puedo analizar estas historias'))!;
    expect(ayuda.presentationState).not.toBe('visible');
    expect(component.asistenteEscribiendo).toBeTrue();
    expect(component.messages.some(mensaje => mensaje.menuId === 'contextual-action')).toBeFalse();
    tick(10_000);
    const cta = component.messages.find(mensaje => mensaje.menuId === 'contextual-action')!;
    expect(ayuda.presentationState).toBe('visible');
    expect(component.asistenteEscribiendo).toBeFalse();
    component.selectHistoricalMenuOption(cta, cta.options![0]);
    fixture.detectChanges();

    expect(component.messages.some(mensaje => mensaje.type === 'clinical-history-duplicate-management')).toBeTrue();
    expect(historiasDuplicadasService.detectar).toHaveBeenCalledTimes(1);
  }));

  it('mantiene informativa la detección pero bloquea ambos CTA para un cargo no autorizado', fakeAsync(() => {
    authServiceMock.usuario.cargo = 'DOCTOR';
    asistenteService.preguntar.and.returnValue(of({
      intencion: 'ANALISIS_DUPLICADOS_PACIENTES', respuesta: 'Se encontraron dos pacientes.',
      datos: { cantidad: 2, resultados: [{}, {}] }
    } as any));
    component.userMessage = '¿Existen pacientes duplicados?';
    component.sendMessage();
    tick(10_000);
    const cta = component.messages.find(mensaje => mensaje.menuId === 'contextual-action')!;

    component.selectHistoricalMenuOption(cta, cta.options![0]);

    expect(asistenteService.preguntar).toHaveBeenCalled();
    expect(component.messages.at(-1)?.text).toBe('La gestión de registros duplicados está disponible únicamente para personal autorizado.');
    expect(component.messages.some(mensaje => mensaje.type === 'duplicate-management')).toBeFalse();
  }));

  [
    {
      nombre: 'pacientes duplicados generales',
      pregunta: '¿Existen pacientes duplicados?',
      intencion: 'ANALISIS_DUPLICADOS_PACIENTES',
      datos: { cantidad: 2, resultados: [{}, {}] },
      respuesta: 'Se encontraron posibles pacientes duplicados:\n\nID paciente: 1\n\nID paciente: 2'
    },
    {
      nombre: 'pacientes duplicados por DNI',
      pregunta: 'Busca pacientes duplicados con DNI (PONER DNI)',
      intencion: 'BUSQUEDA_DUPLICADO_DNI_MULTIPLE',
      datos: { tipoBusqueda: 'DNI', resultados: [{}, {}] },
      respuesta: 'Se encontraron posibles pacientes duplicados para el DNI (PONER DNI):\n\nID paciente: 1\n\nID paciente: 2'
    },
    {
      nombre: 'historias clínicas duplicadas generales',
      pregunta: '¿Existen historias clínicas duplicadas?',
      intencion: 'HISTORIAS_CLINICAS_DUPLICADAS',
      datos: { hayDuplicados: true, duplicados: [{}, {}] },
      respuesta: 'Se encontraron posibles historias clínicas duplicadas:\n\nID historia clínica: 12\n\nRecomendación: conservar ID 12'
    },
    {
      nombre: 'historias clínicas duplicadas por DNI',
      pregunta: '¿El DNI (PONER DNI) tiene historias clínicas duplicadas?',
      intencion: 'HISTORIAS_CLINICAS_DUPLICADAS',
      datos: { hayDuplicados: true, dniConsultado: DNI_PRUEBA, duplicados: [{}] },
      respuesta: 'Se encontraron posibles historias clínicas duplicadas para el DNI (PONER DNI):\n\nID historia clínica: 12\n\nRecomendación: conservar ID 12'
    }
  ].forEach(caso => {
    it(`debe anclar el inicio del resultado sin saltar al final para ${caso.nombre}`, () => {
      asistenteService.preguntar.and.returnValue(of({
        intencion: caso.intencion,
        respuesta: caso.respuesta,
        datos: caso.datos
      } as any));
      const scrollBottomSpy = spyOn(component, 'scrollToBottom');
      const scrollBlockSpy = spyOn(component as any, 'scrollToNewBlock');

      component.userMessage = caso.pregunta;
      component.sendMessage();
      fixture.detectChanges();

      const preguntaIndex = component.messages.findIndex(mensaje => mensaje.sender === 'user' && mensaje.text === caso.pregunta);
      const resultadoIndex = component.messages.findIndex(mensaje => mensaje.sender === 'bot' && mensaje.text === caso.respuesta);
      const resultado = component.messages[resultadoIndex];
      expect(preguntaIndex).toBeGreaterThanOrEqual(0);
      expect(resultadoIndex).toBe(preguntaIndex + 1);
      expect(scrollBlockSpy).toHaveBeenCalledOnceWith(resultado.id);
      expect(scrollBottomSpy).not.toHaveBeenCalled();
    });
  });

  it('debe usar scrollIntoView sobre el inicio del mensaje de resultado', fakeAsync(() => {
    asistenteService.preguntar.and.returnValue(of({
      intencion: 'ANALISIS_DUPLICADOS_PACIENTES',
      respuesta: 'Se encontraron posibles pacientes duplicados:\n\nPrimer registro\n\nÚltimo registro',
      datos: { cantidad: 2, resultados: [{}, {}] }
    } as any));

    component.userMessage = 'Analiza posibles duplicados';
    component.sendMessage();
    fixture.detectChanges();
    const resultado = component.messages.find(mensaje => mensaje.text?.startsWith('Se encontraron posibles pacientes duplicados'))!;
    const elemento = fixture.nativeElement.querySelector(`[data-block-id="${resultado.id}"]`);
    elemento.scrollIntoView = jasmine.createSpy('scrollIntoView');

    tick(20);

    expect(elemento.scrollIntoView).toHaveBeenCalledOnceWith({ behavior: 'smooth', block: 'start' });
  }));

  it('debe conservar el scroll normal hacia abajo para respuestas cortas', () => {
    asistenteService.preguntar.and.returnValue(of({ intencion: 'AYUDA_USO_SISTEMA', respuesta: 'Respuesta corta.' } as any));
    const scrollBottomSpy = spyOn(component, 'scrollToBottom');
    const scrollBlockSpy = spyOn(component as any, 'scrollToNewBlock');

    component.userMessage = '¿Qué preguntas puedo hacer?';
    component.sendMessage();

    expect(scrollBottomSpy).toHaveBeenCalledTimes(1);
    expect(scrollBlockSpy).not.toHaveBeenCalled();
  });

  it('debe iniciar desde el menú, mantenerlo en el historial y cancelar limpiamente', () => {
    const pacientes = abrirMenuAsistenciaPacientes();
    const opcion = pacientes.options.find((item: any) => item.label === 'Gestionar pacientes duplicados');
    component.selectHistoricalMenuOption(pacientes, opcion);
    fixture.detectChanges();
    expect(component.gestionDuplicadosActiva).toBeTrue();
    expect(component.messages.some(mensaje => mensaje.menuId === 'asistencia-pacientes')).toBeTrue();
    expect(fixture.nativeElement.querySelector('.duplicate-flow-actions')).toBeNull();
    const cancelarContextual = fixture.nativeElement.querySelector('.duplicate-message-block .link-action') as HTMLButtonElement;
    expect(cancelarContextual.textContent).toContain('Cancelar y volver a Pacientes');

    cancelarContextual.click();
    fixture.detectChanges();

    expect(component.gestionDuplicadosActiva).toBeFalse();
    expect(component.messages.at(-1)?.menuId).toBe('asistencia-pacientes');
  });

  it('mantiene el Cancelar inferior para la creación de historia clínica', () => {
    iniciarFlujoHistoriaClinica();
    fixture.detectChanges();

    const cancelarCreacion = fixture.nativeElement.querySelector('.clinical-history-flow-actions .cancel-action') as HTMLButtonElement;
    expect(cancelarCreacion).not.toBeNull();
    expect(cancelarCreacion.textContent).toContain('Cancelar');
  });

  it('debe limpiar la contraseña al minimizar y todo el flujo al cerrar', () => {
    component.userMessage = 'Gestionar duplicados';
    component.sendMessage();
    fixture.detectChanges();
    const tarjeta = component.gestionDuplicadosComponents.last;
    tarjeta.password = 'sensible';
    component.minimizeChat();
    expect(tarjeta.password).toBe('');

    component.openChat();
    component.closeChat();
    expect(component.messages.length).toBe(2);
    expect(component.gestionDuplicadosActiva).toBeFalse();
  });

  it('debe mantener DNI, resultado y orientación en orden y revelar las tarjetas al terminar el mensaje', fakeAsync(() => {
    const respuestaPendiente = new Subject<any>();
    duplicadosService.analizar.and.returnValue(respuestaPendiente);
    component.userMessage = 'Gestionar duplicados';
    component.sendMessage();
    fixture.detectChanges();
    const scrollBottomSpy = spyOn(component, 'scrollToBottom');
    const scrollBlockSpy = spyOn(component as any, 'scrollToNewBlock');
    const tarjeta = component.gestionDuplicadosComponents.last;

    tarjeta.dniInput = DNI_PRUEBA;
    tarjeta.consultarDni();
    fixture.detectChanges();

    const solicitudIndex = component.messages.findIndex(mensaje => mensaje.text === 'Comencemos revisando un grupo específico de pacientes duplicados.');
    const dniIndexAntes = component.messages.findIndex(mensaje => mensaje.sender === 'user' && mensaje.text === DNI_PRUEBA);
    expect(solicitudIndex).toBeLessThan(dniIndexAntes);
    expect(duplicadosService.analizar).toHaveBeenCalledOnceWith(DNI_PRUEBA);

    respuestaPendiente.next({
      dni: DNI_PRUEBA, cantidadPacientesActivos: 2, esDuplicado: true,
      pacientes: [
        { idPaciente: 10, nombreCompleto: 'Paciente principal', dni: DNI_PRUEBA, estadoRegistro: 'ACTIVO', cantidadHistoriasClinicas: 1, cantidadConsultas: 2, cantidadCamposPersonalesCompletos: 8, tieneInformacionClinicaRelevante: true },
        { idPaciente: 13, nombreCompleto: 'Paciente duplicado', dni: DNI_PRUEBA, estadoRegistro: 'ACTIVO', cantidadHistoriasClinicas: 0, cantidadConsultas: 0, cantidadCamposPersonalesCompletos: 5, tieneInformacionClinicaRelevante: false }
      ],
      idPacienteRecomendado: 10, razonesRecomendacion: ['Tiene 2 consultas registradas'],
      permitirArchivadoSimple: true, requiereRevision: false, resultado: 'DUPLICADOS_ENCONTRADOS',
      mensaje: 'Se encontraron 2 pacientes activos con el mismo DNI.'
    });
    respuestaPendiente.complete();
    fixture.detectChanges();

    const dniMensajes = component.messages.filter(mensaje => mensaje.sender === 'user' && mensaje.text === DNI_PRUEBA);
    const resultadoIndex = component.messages.findIndex(mensaje => mensaje.text === 'Se encontraron 2 pacientes activos con el mismo DNI.');
    const orientacionIndex = component.messages.findIndex(mensaje => mensaje.text?.startsWith('Revisa los pacientes encontrados.'));
    expect(dniMensajes.length).toBe(1);
    expect(dniIndexAntes).toBeLessThan(resultadoIndex);
    expect(resultadoIndex).toBeLessThan(orientacionIndex);
    expect(component.messages.some(mensaje => mensaje.type === 'duplicate-management' && mensaje.duplicateView === 'results')).toBeFalse();
    expect(component.messages[orientacionIndex].presentationState).not.toBe('visible');
    expect(component.asistenteEscribiendo).toBeTrue();
    expect(fixture.nativeElement.textContent).not.toContain('Pacientes encontrados');

    tick(10_000);
    fixture.detectChanges();
    const tarjetasIndex = component.messages.findIndex(mensaje => mensaje.type === 'duplicate-management' && mensaje.duplicateView === 'results');
    expect(orientacionIndex).toBeLessThan(tarjetasIndex);
    expect(component.messages[orientacionIndex].presentationState).toBe('visible');
    expect(component.asistenteEscribiendo).toBeFalse();
    expect(fixture.nativeElement.textContent).toContain('Pacientes encontrados');
    expect(fixture.nativeElement.textContent).toContain('¿Qué debes hacer?');
    expect(fixture.nativeElement.textContent).toContain('Recomendado para archivar');
    expect(fixture.nativeElement.textContent).toContain('Tiene 2 consultas registradas');
    expect(scrollBlockSpy).toHaveBeenCalledOnceWith(component.messages[dniIndexAntes].id);
    expect(scrollBottomSpy).not.toHaveBeenCalled();
  }));

  it('debe conservar el orden solicitud, DNI y mensaje cuando solo existe un paciente', () => {
    duplicadosService.analizar.and.returnValue(of({
      dni: DNI_PRUEBA, cantidadPacientesActivos: 1, esDuplicado: false, pacientes: [],
      razonesRecomendacion: [], permitirArchivadoSimple: false, requiereRevision: false,
      resultado: 'SIN_DUPLICADOS', mensaje: 'El DNI corresponde a un único paciente activo.'
    } as any));
    component.userMessage = 'Gestionar duplicados';
    component.sendMessage();
    fixture.detectChanges();
    const tarjeta = component.gestionDuplicadosComponents.last;
    tarjeta.dniInput = DNI_PRUEBA;
    tarjeta.consultarDni();
    fixture.detectChanges();

    const solicitudIndex = component.messages.findIndex(mensaje => mensaje.text === 'Comencemos revisando un grupo específico de pacientes duplicados.');
    const dniIndex = component.messages.findIndex(mensaje => mensaje.sender === 'user' && mensaje.text === DNI_PRUEBA);
    const resultadoIndex = component.messages.findIndex(mensaje => mensaje.text === 'Solo existe un paciente activo con ese DNI. No hay duplicados para gestionar.');
    expect(solicitudIndex).toBeLessThan(dniIndex);
    expect(dniIndex).toBeLessThan(resultadoIndex);
    expect(component.messages.filter(mensaje => mensaje.sender === 'user' && mensaje.text === DNI_PRUEBA).length).toBe(1);
  });

  it('presenta introducción, búsqueda y selección en orden con un único GET y sin saltar el scroll', fakeAsync(() => {
    const scrollSpy = spyOn(component as any, 'scrollToNewBlock');
    const opcion = (component as any).menus['asistencia-historias'].options
      .find((item: any) => item.label === 'Crear historias clínicas faltantes');
    const seleccionInicial = (component as any).addUserMessage(opcion.label);

    (component as any).executeMenuOption(opcion, seleccionInicial.id);
    fixture.detectChanges();

    expect(historiaClinicaService.getHistoriasClinicasFaltantes).not.toHaveBeenCalled();
    expect(component.messages.some(mensaje => mensaje.type === 'missing-clinical-histories')).toBeFalse();
    expect(scrollSpy).toHaveBeenCalledOnceWith(seleccionInicial.id);

    tick(3_000);
    fixture.detectChanges();
    expect(historiaClinicaService.getHistoriasClinicasFaltantes).toHaveBeenCalledTimes(1);
    expect(fixture.nativeElement.textContent).toContain('Buscando historias clínicas faltantes...');
    expect(component.messages.some(mensaje => mensaje.missingHistoriesView === 'selection')).toBeFalse();

    tick(6_001);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain('Buscando historias clínicas faltantes...');
    expect(component.messages.some(mensaje => mensaje.text?.startsWith('Se encontraron 2 pacientes activos'))).toBeTrue();
    expect(component.messages.some(mensaje => mensaje.missingHistoriesView === 'selection')).toBeFalse();

    tick(3_000);
    fixture.detectChanges();
    const tarjeta = component.messages.find(mensaje => mensaje.missingHistoriesView === 'selection')!;

    expect(historiaClinicaService.getHistoriasClinicasFaltantes).toHaveBeenCalledTimes(1);
    expect(tarjeta.historiasFaltantes.preview?.pacientes.length).toBe(2);
    expect(tarjeta.missingHistoriesView).toBe('selection');
    expect(component.clinicalHistoryFlow.step).toBe('idle');
    expect(fixture.nativeElement.textContent).toContain('******00');
    expect(fixture.nativeElement.textContent).not.toContain(DNI_PRUEBA);
    expect(scrollSpy).toHaveBeenCalledOnceWith(seleccionInicial.id);
  }));
  it('mantiene el scroll actual al continuar hacia la confirmación', fakeAsync(() => {
    iniciarFlujoHistoriasFaltantes();
    const scrollSpy = spyOn(component as any, 'scrollToNewBlock');
    const tarjeta = component.historiasFaltantesComponents.last;
    tarjeta.cambiarSeleccion(8, { target: { checked: true } } as unknown as Event);

    tarjeta.continuar();

    const mensajeContinuar = component.messages.find(mensaje => mensaje.sender === 'user'
      && mensaje.text === 'Continuar con 1 pacientes')!;
    expect(scrollSpy).toHaveBeenCalledOnceWith(mensajeContinuar.id);
  }));
  it('conserva selección al minimizar y reabrir sin repetir el GET', fakeAsync(() => {
    iniciarFlujoHistoriasFaltantes();
    const tarjeta = component.historiasFaltantesComponents.last;
    tarjeta.cambiarSeleccion(8, { target: { checked: true } } as unknown as Event);
    expect(tarjeta.state.idsSeleccionados).toEqual([8]);

    component.minimizeChat();
    fixture.detectChanges();
    component.openChat();
    fixture.detectChanges();

    const tarjetaRestaurada = component.historiasFaltantesComponents.last;
    expect(tarjetaRestaurada.state.idsSeleccionados).toEqual([8]);
    expect(tarjetaRestaurada.estaSeleccionado(8)).toBeTrue();
    expect(historiaClinicaService.getHistoriasClinicasFaltantes).toHaveBeenCalledTimes(1);
  }));
  it('mantiene la selección al confirmar y volver, dejando las tarjetas históricas inactivas', fakeAsync(() => {
    iniciarFlujoHistoriasFaltantes();
    let tarjeta = component.historiasFaltantesComponents.last;
    tarjeta.cambiarSeleccion(8, { target: { checked: true } } as unknown as Event);
    tarjeta.continuar();
    fixture.detectChanges();

    const tarjetasTrasConfirmar = component.messages.filter(mensaje => mensaje.type === 'missing-clinical-histories');
    expect(tarjetasTrasConfirmar.length).toBe(2);
    expect(tarjetasTrasConfirmar[0].missingHistoriesActive).toBeFalse();
    expect(tarjetasTrasConfirmar[1].missingHistoriesView).toBe('confirmation');
    expect(tarjetasTrasConfirmar[1].historiasFaltantes?.idsConfirmados).toEqual([8]);

    tarjeta = component.historiasFaltantesComponents.last;
    tarjeta.volverASeleccionar();
    fixture.detectChanges();
    const tarjetasFinales = component.messages.filter(mensaje => mensaje.type === 'missing-clinical-histories');
    expect(tarjetasFinales.at(-2)?.missingHistoriesActive).toBeFalse();
    expect(tarjetasFinales.at(-1)?.missingHistoriesView).toBe('selection');
    expect(tarjetasFinales.at(-1)?.historiasFaltantes?.idsSeleccionados).toEqual([8]);
  }));
  it('cancela sin POST, limpia la selección y vuelve a Asistencia guiada Historias clínicas', fakeAsync(() => {
    iniciarFlujoHistoriasFaltantes();
    const tarjeta = component.historiasFaltantesComponents.last;
    tarjeta.cambiarSeleccion(8, { target: { checked: true } } as unknown as Event);
    tarjeta.continuar();
    fixture.detectChanges();
    component.historiasFaltantesComponents.last.cancelar();
    fixture.detectChanges();

    const estado = component.messages.find(mensaje => mensaje.type === 'missing-clinical-histories')?.historiasFaltantes;
    expect(estado?.idsSeleccionados).toEqual([]);
    expect(estado?.idsConfirmados).toEqual([]);
    expect(component.messages.some(mensaje => mensaje.text === 'La creación de historias clínicas faltantes fue cancelada.')).toBeTrue();
    expect(component.messages.at(-1)?.menuId).toBe('asistencia-historias');
    expect(component.messages.filter(mensaje => mensaje.type === 'missing-clinical-histories' && mensaje.missingHistoriesActive).length).toBe(0);
  }));
  it('cierra y reinicia limpiando por completo el nuevo flujo', fakeAsync(() => {
    iniciarFlujoHistoriasFaltantes();
    component.historiasFaltantesComponents.last.cambiarSeleccion(8, { target: { checked: true } } as unknown as Event);

    component.closeChat();

    expect(component.messages.length).toBe(2);
    expect(component.messages.some(mensaje => mensaje.type === 'missing-clinical-histories')).toBeFalse();
  }));
  it('envía una sola vez exclusivamente los ids confirmados y deja las tarjetas previas inactivas', fakeAsync(() => {
    iniciarFlujoHistoriasFaltantes();
    let tarjeta = component.historiasFaltantesComponents.last;
    tarjeta.cambiarSeleccion(8, { target: { checked: true } } as unknown as Event);
    tarjeta.continuar();
    fixture.detectChanges();
    tarjeta = component.historiasFaltantesComponents.last;
    tarjeta.state.idsSeleccionados = [9];

    tarjeta.confirmarCreacion();
    tarjeta.confirmarCreacion();
    fixture.detectChanges();

    expect(historiaClinicaService.crearHistoriasClinicasFaltantes).toHaveBeenCalledOnceWith([8]);
    expect(historiaClinicaService.insert).not.toHaveBeenCalled();
    const crear = component.messages.find(mensaje => mensaje.sender === 'user'
      && mensaje.text === 'Crear las 1 historias clínicas')!;
    expect(component.messages.at(-1)?.missingHistoriesView).toBe('creating');
    expect(fixture.nativeElement.textContent).toContain('Creando las historias clínicas seleccionadas...');
    expect(component.messages.some(mensaje => mensaje.missingHistoriesView === 'result')).toBeFalse();

    tick(10_000);
    fixture.detectChanges();
    const tarjetas = component.messages.filter(mensaje => mensaje.type === 'missing-clinical-histories');
    expect(tarjetas.at(-1)?.missingHistoriesView).toBe('result');
    expect(tarjetas.slice(0, -1).every(mensaje => !mensaje.missingHistoriesActive)).toBeTrue();
    expect(tarjetas.at(-1)?.historiasFaltantes?.resultado?.creadas).toBe(1);
    expect(component.messages.filter(mensaje => mensaje.text?.includes('Proceso finalizado')).length).toBe(0);
    expect(component.messages.filter(mensaje => mensaje.text === '✓ Procesamiento completado').length).toBe(1);
    expect(fixture.nativeElement.textContent).toContain('Resumen del proceso');
    expect(fixture.nativeElement.textContent).toContain('1 Historias clínicas creadas');
    expect(fixture.nativeElement.textContent).not.toContain('Creando las historias clínicas seleccionadas...');
    expect((component as any).interactionScrollAnchorId).toBe(crear.id);
  }));
  it('impide doble POST mientras procesa y conserva resultados parciales del backend', fakeAsync(() => {
    const respuesta = new Subject<any>();
    historiaClinicaService.crearHistoriasClinicasFaltantes.and.returnValue(respuesta);
    iniciarFlujoHistoriasFaltantes();
    let tarjeta = component.historiasFaltantesComponents.last;
    tarjeta.cambiarSeleccion(8, { target: { checked: true } } as unknown as Event);
    tarjeta.cambiarSeleccion(9, { target: { checked: true } } as unknown as Event);
    tarjeta.continuar();
    fixture.detectChanges();
    tarjeta = component.historiasFaltantesComponents.last;
    tarjeta.confirmarCreacion();
    tarjeta.confirmarCreacion();
    fixture.detectChanges();

    expect(historiaClinicaService.crearHistoriasClinicasFaltantes).toHaveBeenCalledTimes(1);
    expect(component.messages.at(-1)?.missingHistoriesView).toBe('creating');
    expect(fixture.nativeElement.querySelectorAll('.missing-step button').length).toBe(0);

    tick(7_000);
    fixture.detectChanges();
    expect(component.messages.at(-1)?.missingHistoriesView).toBe('creating');
    expect(component.messages.some(mensaje => mensaje.missingHistoriesView === 'result')).toBeFalse();

    respuesta.next({ totalSolicitados: 2, totalProcesados: 2, creadas: 1, omitidas: 1,
      noEncontrados: 0, inactivos: 0, errores: 0,
      resultados: [{ idPaciente: 8, estado: 'CREADA' }, { idPaciente: 9, estado: 'OMITIDA_YA_TIENE_HISTORIA' }] });
    respuesta.complete();
    fixture.detectChanges();

    expect(component.messages.at(-1)?.text).toBe('✓ Procesamiento completado');
    tick(3_000);
    fixture.detectChanges();
    const resultado = component.messages.at(-1)?.historiasFaltantes?.resultado;
    expect(resultado).toEqual(jasmine.objectContaining({ creadas: 1, omitidas: 1, errores: 0 }));
    expect(historiaClinicaService.crearHistoriasClinicasFaltantes).toHaveBeenCalledTimes(1);
  }));
  it('ante error HTTP no reintenta y revisar nuevamente ejecuta un GET nuevo con estado nuevo', fakeAsync(() => {
    historiaClinicaService.crearHistoriasClinicasFaltantes.and.returnValue(throwError(() => new Error('red')));
    iniciarFlujoHistoriasFaltantes();
    const estadoAnterior = component.historiasFaltantesComponents.last.state;
    component.historiasFaltantesComponents.last.cambiarSeleccion(8, { target: { checked: true } } as unknown as Event);
    component.historiasFaltantesComponents.last.continuar();
    fixture.detectChanges();
    component.historiasFaltantesComponents.last.confirmarCreacion();
    fixture.detectChanges();

    expect(historiaClinicaService.crearHistoriasClinicasFaltantes).toHaveBeenCalledTimes(1);
    tick(5_000);
    fixture.detectChanges();
    expect(component.messages.at(-1)?.missingHistoriesView).toBe('creation-error');
    component.historiasFaltantesComponents.last.revisarNuevamente();
    tick(20_000);
    fixture.detectChanges();

    expect(historiaClinicaService.getHistoriasClinicasFaltantes).toHaveBeenCalledTimes(2);
    expect(component.historiasFaltantesComponents.last.state).not.toBe(estadoAnterior);
    expect(historiaClinicaService.crearHistoriasClinicasFaltantes).toHaveBeenCalledTimes(1);
  }));

  it('muestra el resumen guiado a administrador y realiza una sola petición al endpoint', fakeAsync(() => {
    const opcion = (component as any).createMenuOptions('asistencia-consultas')
      .find((item: any) => item.action === 'patient-consultation-summary-flow');
    expect(opcion.label).toBe('Resumen de consultas del paciente');
    const seleccion = (component as any).addUserMessage(opcion.label);
    (component as any).executeMenuOption(opcion, seleccion.id);
    tick(10_000);

    component.userMessage = DNI_PRUEBA;
    component.sendMessage();
    tick(5_000);
    expect(component.resumenConsultasState?.vista).toBe('confirmation');

    component.generarResumenConsultas();
    expect(resumenConsultasService.obtener).toHaveBeenCalledOnceWith(8);
    expect(component.messages.some(mensaje => mensaje.summaryView === 'loading')).toBeTrue();
    tick(5_000);
    fixture.detectChanges();

    expect(resumenConsultasService.obtener).toHaveBeenCalledTimes(1);
    expect(component.messages.some(mensaje => mensaje.summaryView === 'summary')).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('Resumen del paciente');
    expect((component as any).interactionScrollAnchorId).toBeDefined();
    tick(20_000);
  }));

  it('oculta la opción de resumen al personal de enfermería', () => {
    authServiceMock.usuario = { idUsuario: 9, tipoUsuario: 'ENFERMERO', cargo: 'ENFERMERO' };
    const opciones = (component as any).createMenuOptions('asistencia-consultas');
    expect(opciones.some((item: any) => item.action === 'patient-consultation-summary-flow')).toBeFalse();
  });

  it('muestra la opción de resumen al doctor', () => {
    authServiceMock.usuario = { idUsuario: 10, tipoUsuario: 'DOCTOR', cargo: 'DOCTOR' };
    const opciones = (component as any).createMenuOptions('asistencia-consultas');
    expect(opciones.some((item: any) => item.action === 'patient-consultation-summary-flow')).toBeTrue();
  });

  it('abre directamente el resumen contextual por idPaciente con una sola petición', fakeAsync(() => {
    const navigation = TestBed.inject(ChatbotNavigationService);
    navigation.abrirResumenConsultas({ idPaciente: 8, nombreCompleto: 'NOMBRE PRUEBA', dni: DNI_PRUEBA, cantidadConsultasAtendidas: 2 });
    expect(component.isOpen).toBeTrue();
    expect(component.messages.some(mensaje => mensaje.text?.includes('Prepararé el resumen'))).toBeTrue();
    expect(resumenConsultasService.obtener).not.toHaveBeenCalled();
    tick(10_000);
    expect(resumenConsultasService.obtener).toHaveBeenCalledOnceWith(8);
    expect(historiaClinicaService.buscarPacientesPorDni).not.toHaveBeenCalled();
    expect(historiaClinicaService.buscarPacientesPorNombre).not.toHaveBeenCalled();
    expect(component.messages.some(mensaje => mensaje.summaryView === 'summary')).toBeTrue();
    expect(component.messages.some(mensaje => mensaje.text?.includes('Ingresa el DNI'))).toBeFalse();
    tick(20_000);
  }));

  it('cancela el contexto pendiente al cerrar y no lo reutiliza al abrir normalmente', fakeAsync(() => {
    const respuesta = new Subject<any>();
    resumenConsultasService.obtener.and.returnValue(respuesta);
    const navigation = TestBed.inject(ChatbotNavigationService);
    navigation.abrirResumenConsultas({ idPaciente: 23, dni: DNI_PRUEBA });
    tick(10_000);
    expect(resumenConsultasService.obtener).toHaveBeenCalledOnceWith(23);
    component.minimizeChat();
    expect(component.resumenConsultasState).toBeUndefined();
    component.openChat();
    tick(5_000);
    expect(resumenConsultasService.obtener).toHaveBeenCalledTimes(1);
  }));

  it('muestra el error existente y permite reintentar el resumen contextual', fakeAsync(() => {
    resumenConsultasService.obtener.and.returnValue(throwError(() => ({ status: 500 })));
    const navigation = TestBed.inject(ChatbotNavigationService);
    navigation.abrirResumenConsultas({ idPaciente: 31, dni: DNI_PRUEBA });
    tick(10_000);
    expect(resumenConsultasService.obtener).toHaveBeenCalledOnceWith(31);
    expect(component.resumenConsultasState?.vista).toBe('error');
    expect(component.resumenConsultasState?.mensajeError).toContain('No se pudo generar');

    resumenConsultasService.obtener.and.returnValue(of({
      paciente: { idPaciente: 31, nombreCompleto: 'PACIENTE CONTEXTUAL', dni: DNI_PRUEBA, estado: 'ACTIVO', cantidadHistoriasClinicas: 1, idsHistoriasClinicas: [4] },
      antecedentes: {}, resumenAtencion: { totalConsultasAtendidas: 1, proximasCitas: [] }, tiposEnfermedad: [], especialidades: [], funcionesVitales: {}, evaluacionesRecientes: [], consultasRecientes: [],
      calidadDatos: { consultasSinFecha: 0, consultasSinTipoEnfermedad: 0, consultasSinEspecialidad: 0, valoresVitalesDescartados: 0, consultasConRelacionInconsistente: 0 }
    }));
    component.generarResumenConsultas();
    tick(5_000);
    expect(resumenConsultasService.obtener).toHaveBeenCalledTimes(2);
    expect(component.messages.some(mensaje => mensaje.summaryView === 'summary')).toBeTrue();
    tick(20_000);
  }));
});
