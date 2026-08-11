import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
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
  let authServiceMock: any;
  const paciente = {
    idPaciente: 8, dni: DNI_PRUEBA, numDocumento: DNI_PRUEBA, nombres: 'NOMBRE PRUEBA',
    apellidos: 'APELLIDO UNO APELLIDO DOS', fechaIngreso: '2020-03-10', fechaNacimiento: '1992-01-01', estadoCivil: 'SOLTERO'
  };

  beforeEach(async () => {
    logoutSubject = new Subject<void>();
    sessionChangedSubject = new Subject<boolean>();
    asistenteService = jasmine.createSpyObj<AsistenteService>('AsistenteService', ['preguntar']);
    asistenteService.preguntar.and.returnValue(of({ intencion: 'ayuda', respuesta: 'Respuesta del asistente' }));
    historiaClinicaService = jasmine.createSpyObj<HistoriaClinicaService>('HistoriaClinicaService', ['buscarPacientesPorDni', 'getByPaciente', 'insert', 'update', 'getHistoriasClinicasFaltantes', 'crearHistoriasClinicasFaltantes']);
    historiaClinicaService.buscarPacientesPorDni.and.returnValue(of([paciente]));
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
    authServiceMock = {
      logout$: logoutSubject.asObservable(),
      sessionChanged$: sessionChangedSubject.asObservable(),
      usuario: { idUsuario: 7, cargo: 'ADMINISTRADOR' }
    };

    await TestBed.configureTestingModule({
      imports: [InterfazChatComponent],
      providers: [
        { provide: AsistenteService, useValue: asistenteService },
        { provide: HistoriaClinicaService, useValue: historiaClinicaService },
        { provide: AntecedentesService, useValue: antecedentesService },
        { provide: Router, useValue: router },
        { provide: PacienteImportacionService, useValue: importacionService },
        { provide: PacienteDuplicadoChatService, useValue: duplicadosService },
        { provide: PacienteListRefreshService, useValue: jasmine.createSpyObj('PacienteListRefreshService', ['solicitarActualizacion']) },
        { provide: AuthService, useValue: authServiceMock }
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
    const principal = component.messages.find(mensaje => mensaje.menuId === 'principal')!;
    component.selectHistoricalMenuOption(principal, principal.options!.find(opcion => opcion.label === 'Asistencia guiada')!);
    const asistencia = component.messages.find(mensaje => mensaje.menuId === 'asistencia')!;
    component.selectHistoricalMenuOption(asistencia, asistencia.options!.find(opcion => opcion.label === 'Historias clínicas')!);
    const menuHistorias = component.messages.find(mensaje => mensaje.menuId === 'asistencia-historias')!;
    const opcionCrear = menuHistorias.options.find((opcion: any) => opcion.label === 'Crear una historia clínica con el asistente');
    component.selectHistoricalMenuOption(menuHistorias, opcionCrear);
  }

  function iniciarFlujoHistoriasFaltantes(): any {
    const principal = component.messages.find(mensaje => mensaje.menuId === 'principal')!;
    component.selectHistoricalMenuOption(principal, principal.options!.find(opcion => opcion.label === 'Asistencia guiada')!);
    const asistencia = component.messages.find(mensaje => mensaje.menuId === 'asistencia')!;
    component.selectHistoricalMenuOption(asistencia, asistencia.options!.find(opcion => opcion.label === 'Historias clínicas')!);
    const menuHistorias = component.messages.find(mensaje => mensaje.menuId === 'asistencia-historias')!;
    const opcion = menuHistorias.options.find((item: any) => item.label === 'Crear historias clínicas faltantes');
    component.selectHistoricalMenuOption(menuHistorias, opcion);
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

  it('debe procesar una sola vez el feedback de precarga exitosa y volver al menú principal', () => {
    iniciarFlujoHistoriaClinica();
    enviarDni(DNI_PRUEBA);
    const scrollBottomSpy = spyOn(component, 'scrollToBottom');
    const scrollNewBlockSpy = spyOn(component as any, 'scrollToNewBlock');
    const feedback: ClinicalHistoryFlowFeedback = { id: 'feedback-success-1', type: 'prefill-success', createdAt: Date.now() };

    feedbackService.publish(feedback);
    feedbackService.publish(feedback);
    fixture.detectChanges();

    const successMessage = 'Los datos del paciente se autocompletaron correctamente en Nueva Historia Clínica. Revísalos y pulsa Guardar para registrar la historia.';
    expect(component.messages.filter(message => message.text === successMessage).length).toBe(1);
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
  });

  it('debe mostrar ayuda manual y el menú principal ante un fallo de precarga', () => {
    const scrollBottomSpy = spyOn(component, 'scrollToBottom');
    const scrollNewBlockSpy = spyOn(component as any, 'scrollToNewBlock');
    const feedback: ClinicalHistoryFlowFeedback = { id: 'feedback-failure-1', type: 'prefill-failure', createdAt: Date.now() };

    feedbackService.publish(feedback);
    fixture.detectChanges();

    expect(component.messages.some(message => message.text === 'No fue posible autocompletar los datos. Puedes completar el formulario manualmente.')).toBeTrue();
    expect(component.messages.some(message => message.text === '¿Necesitas ayuda con algo más?')).toBeTrue();
    expect(component.messages.at(-1)).toEqual(jasmine.objectContaining({ type: 'menu', menuId: 'principal' }));
    expect(component.messages.at(-1)?.options?.length).toBe(3);
    expect(component.clinicalHistoryFlow).toEqual({ step: 'idle' });
    expect(asistenteService.preguntar).not.toHaveBeenCalled();
    expect(scrollBottomSpy).not.toHaveBeenCalled();
    expect(scrollNewBlockSpy).not.toHaveBeenCalled();
  });

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
    expect(etiquetas).not.toContain('Gestionar y eliminar pacientes duplicados');
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
      jasmine.objectContaining({ label: 'Gestionar y eliminar pacientes duplicados', action: 'patient-duplicate-flow' })
    ]);
    expect(menus['asistencia-historias'].options).toEqual([
      jasmine.objectContaining({ label: 'Crear una historia clínica con el asistente', action: 'clinical-history-flow' }),
      jasmine.objectContaining({ label: 'Crear historias clínicas faltantes', action: 'missing-clinical-histories-flow' })
    ]);
    expect(menus['pacientes'].options).toContain(jasmine.objectContaining({
      label: 'Detectar posibles pacientes duplicados', action: 'request'
    }));
  });

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
    expect(component.messages.at(-1)?.text).toBe('Ingresa el DNI de ocho dígitos del paciente existente.');
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
    expect(component.messages.at(-1)?.text).toContain('Paciente encontrado:');
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
    expect(component.messages.at(-1)?.text).toBe('No existe un paciente registrado con el DNI indicado.');
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

  it('debe mostrar cero historias, el resumen limitado y los botones estructurados', () => {
    antecedentesService.getByPacienteId.and.returnValue(of({ enfermedadesPrevias: 'Asma severa', cirugiasPrevias: 'Apendicectomía', alergiaMedicamentos: 'Penicilina' }));
    iniciarFlujoHistoriaClinica();
    enviarDni(DNI_PRUEBA);
    fixture.detectChanges();
    const resumen = component.messages.at(-1)?.text ?? '';

    expect(resumen).toContain('Nombre: NOMBRE PRUEBA APELLIDO UNO APELLIDO DOS');
    expect(resumen).toContain('Fecha de nacimiento: 01/01/1992');
    expect(resumen).toContain('Estado civil: Soltero(a)');
    expect(resumen).toContain('Historias clínicas existentes: 0');
    expect(resumen).not.toContain('Asma severa');
    expect(resumen).not.toContain('Apendicectomía');
    expect(resumen).not.toContain('Penicilina');
    expect(fixture.nativeElement.querySelector('.continue-action')?.textContent).toContain('Continuar');
    expect(fixture.nativeElement.querySelector('.cancel-action')?.textContent).toContain('Cancelar');
  });

  it('debe mostrar la cantidad de varias historias existentes', () => {
    historiaClinicaService.getByPaciente.and.returnValue(of([{ idHistoriaClinica: 1 }, { idHistoriaClinica: 2 }, { idHistoriaClinica: 3 }]));
    iniciarFlujoHistoriaClinica();
    enviarDni(DNI_PRUEBA);

    expect(component.messages.at(-1)?.text).toContain('Historias clínicas existentes: 3');
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

    component.continueClinicalHistoryFlow();
    const transferId = (component.clinicalHistoryFlow as any).transferId;
    tick();

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
    expect(abrirMenuAsistenciaPacientes().options.some((opcion: any) => opcion.label === 'Gestionar y eliminar pacientes duplicados')).toBeTrue();

    fixture.destroy();
    authServiceMock.usuario.cargo = ' ENFERMERA(O) ';
    fixture = TestBed.createComponent(InterfazChatComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    expect(abrirMenuAsistenciaPacientes().options.some((opcion: any) => opcion.label === 'Gestionar y eliminar pacientes duplicados')).toBeTrue();
  });

  it('debe ocultar la opción al doctor y rechazar también la intención escrita', () => {
    authServiceMock.usuario.cargo = 'MÉDICO';
    const pacientes = abrirMenuAsistenciaPacientes();
    expect(pacientes.options.some((opcion: any) => opcion.label === 'Gestionar y eliminar pacientes duplicados')).toBeFalse();

    component.userMessage = 'Quiero eliminar un paciente duplicado';
    component.sendMessage();

    expect(component.messages.at(-1)?.text).toBe('Tu cargo no tiene permiso para archivar pacientes.');
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
    it(`debe enviar al backend la consulta general de duplicados: ${frase}`, () => {
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
      expect(component.messages.at(-1)?.text).toContain('Se encontraron posibles pacientes duplicados');
    });
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
    it(`debe consultar historias clínicas duplicadas sin activar archivado: ${frase}`, () => {
      asistenteService.preguntar.and.returnValue(of({
        intencion: 'HISTORIAS_CLINICAS_DUPLICADAS',
        respuesta: 'Se encontraron 2 posibles historias clínicas duplicadas para el DNI (PONER DNI).\n\nID historia clínica: 12\nConsultas asociadas: 3\nEstado de la historia: ACTIVA\n\nSe recomienda conservar la historia clínica ID 12.',
        datos: { hayDuplicados: true, duplicados: [] }
      } as any));

      component.userMessage = frase;
      component.sendMessage();
      fixture.detectChanges();

      expect(component.messages.some(mensaje => mensaje.type === 'duplicate-management')).toBeFalse();
      expect(component.messages.at(-1)?.text).toContain('ID historia clínica: 12');
      expect(component.messages.at(-1)?.text).not.toContain('Se encontraron posibles pacientes duplicados');
      expect(asistenteService.preguntar).toHaveBeenCalledOnceWith(frase);
    });
  });

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
    const resultado = component.messages.at(-1)!;
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
    const opcion = pacientes.options.find((item: any) => item.label === 'Gestionar y eliminar pacientes duplicados');
    component.selectHistoricalMenuOption(pacientes, opcion);
    fixture.detectChanges();
    expect(component.gestionDuplicadosActiva).toBeTrue();
    expect(component.messages.some(mensaje => mensaje.menuId === 'asistencia-pacientes')).toBeTrue();

    component.cancelarGestionDuplicados();
    fixture.detectChanges();

    expect(component.gestionDuplicadosActiva).toBeFalse();
    expect(component.messages.at(-1)?.menuId).toBe('asistencia-pacientes');
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

  it('debe mantener solicitud, DNI, resultado y tarjetas en orden cronológico y anclar el resultado', () => {
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

    const solicitudIndex = component.messages.findIndex(mensaje => mensaje.text === 'Ingresa el DNI de ocho dígitos del paciente duplicado que deseas revisar.');
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
    const tarjetasIndex = component.messages.findIndex(mensaje => mensaje.type === 'duplicate-management' && mensaje.duplicateView === 'results');
    const mensajeResultado = component.messages[resultadoIndex];
    expect(dniMensajes.length).toBe(1);
    expect(dniIndexAntes).toBeLessThan(resultadoIndex);
    expect(resultadoIndex).toBeLessThan(tarjetasIndex);
    expect(fixture.nativeElement.textContent).toContain('Pacientes encontrados');
    expect(fixture.nativeElement.textContent).toContain('Tiene 2 consultas registradas');
    expect(scrollBlockSpy).toHaveBeenCalledWith(mensajeResultado.id);
    expect(scrollBottomSpy).not.toHaveBeenCalled();
  });

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

    const solicitudIndex = component.messages.findIndex(mensaje => mensaje.text === 'Ingresa el DNI de ocho dígitos del paciente duplicado que deseas revisar.');
    const dniIndex = component.messages.findIndex(mensaje => mensaje.sender === 'user' && mensaje.text === DNI_PRUEBA);
    const resultadoIndex = component.messages.findIndex(mensaje => mensaje.text === 'Solo existe un paciente activo con ese DNI. No hay duplicados para gestionar.');
    expect(solicitudIndex).toBeLessThan(dniIndex);
    expect(dniIndex).toBeLessThan(resultadoIndex);
    expect(component.messages.filter(mensaje => mensaje.sender === 'user' && mensaje.text === DNI_PRUEBA).length).toBe(1);
  });

  it('inicia el flujo especializado de historias faltantes con un único GET y sin alterar el flujo individual', () => {
    const scrollSpy = spyOn(component as any, 'scrollToNewBlock');
    const tarjeta = iniciarFlujoHistoriasFaltantes();
    const seleccionInicial = component.messages.find(mensaje => mensaje.sender === 'user'
      && mensaje.text === 'Crear historias clínicas faltantes')!;

    expect(historiaClinicaService.getHistoriasClinicasFaltantes).toHaveBeenCalledTimes(1);
    expect(tarjeta.historiasFaltantes.preview?.pacientes.length).toBe(2);
    expect(tarjeta.missingHistoriesView).toBe('selection');
    expect(component.clinicalHistoryFlow.step).toBe('idle');
    expect(fixture.nativeElement.textContent).toContain('******00');
    expect(fixture.nativeElement.textContent).not.toContain(DNI_PRUEBA);
    expect(scrollSpy).toHaveBeenCalledTimes(3);
    expect(scrollSpy.calls.mostRecent().args).toEqual([seleccionInicial.id]);
  });

  it('mantiene el scroll actual al continuar hacia la confirmación', () => {
    iniciarFlujoHistoriasFaltantes();
    const scrollSpy = spyOn(component as any, 'scrollToNewBlock');
    const tarjeta = component.historiasFaltantesComponents.last;
    tarjeta.cambiarSeleccion(8, { target: { checked: true } } as unknown as Event);

    tarjeta.continuar();

    const mensajeContinuar = component.messages.find(mensaje => mensaje.sender === 'user'
      && mensaje.text === 'Continuar con 1 pacientes')!;
    expect(scrollSpy).toHaveBeenCalledOnceWith(mensajeContinuar.id);
  });

  it('conserva selección al minimizar y reabrir sin repetir el GET', () => {
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
  });

  it('mantiene la selección al confirmar y volver, dejando las tarjetas históricas inactivas', () => {
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
  });

  it('cancela sin POST, limpia la selección y vuelve a Asistencia guiada Historias clínicas', () => {
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
  });

  it('cierra y reinicia limpiando por completo el nuevo flujo', () => {
    iniciarFlujoHistoriasFaltantes();
    component.historiasFaltantesComponents.last.cambiarSeleccion(8, { target: { checked: true } } as unknown as Event);

    component.closeChat();

    expect(component.messages.length).toBe(2);
    expect(component.messages.some(mensaje => mensaje.type === 'missing-clinical-histories')).toBeFalse();
  });

  it('envía una sola vez exclusivamente los ids confirmados y deja las tarjetas previas inactivas', () => {
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
    const tarjetas = component.messages.filter(mensaje => mensaje.type === 'missing-clinical-histories');
    expect(tarjetas.at(-1)?.missingHistoriesView).toBe('result');
    expect(tarjetas.slice(0, -1).every(mensaje => !mensaje.missingHistoriesActive)).toBeTrue();
    expect(tarjetas.at(-1)?.historiasFaltantes?.resultado?.creadas).toBe(1);
  });

  it('impide doble POST mientras procesa y conserva resultados parciales del backend', () => {
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

    respuesta.next({ totalSolicitados: 2, totalProcesados: 2, creadas: 1, omitidas: 1,
      noEncontrados: 0, inactivos: 0, errores: 0,
      resultados: [{ idPaciente: 8, estado: 'CREADA' }, { idPaciente: 9, estado: 'OMITIDA_YA_TIENE_HISTORIA' }] });
    respuesta.complete();
    fixture.detectChanges();

    const resultado = component.messages.at(-1)?.historiasFaltantes?.resultado;
    expect(resultado).toEqual(jasmine.objectContaining({ creadas: 1, omitidas: 1, errores: 0 }));
    expect(historiaClinicaService.crearHistoriasClinicasFaltantes).toHaveBeenCalledTimes(1);
  });

  it('ante error HTTP no reintenta y revisar nuevamente ejecuta un GET nuevo con estado nuevo', () => {
    historiaClinicaService.crearHistoriasClinicasFaltantes.and.returnValue(throwError(() => new Error('red')));
    iniciarFlujoHistoriasFaltantes();
    const estadoAnterior = component.historiasFaltantesComponents.last.state;
    component.historiasFaltantesComponents.last.cambiarSeleccion(8, { target: { checked: true } } as unknown as Event);
    component.historiasFaltantesComponents.last.continuar();
    fixture.detectChanges();
    component.historiasFaltantesComponents.last.confirmarCreacion();
    fixture.detectChanges();

    expect(historiaClinicaService.crearHistoriasClinicasFaltantes).toHaveBeenCalledTimes(1);
    expect(component.messages.at(-1)?.missingHistoriesView).toBe('creation-error');
    component.historiasFaltantesComponents.last.revisarNuevamente();
    fixture.detectChanges();

    expect(historiaClinicaService.getHistoriasClinicasFaltantes).toHaveBeenCalledTimes(2);
    expect(component.historiasFaltantesComponents.last.state).not.toBe(estadoAnterior);
    expect(historiaClinicaService.crearHistoriasClinicasFaltantes).toHaveBeenCalledTimes(1);
  });
});
