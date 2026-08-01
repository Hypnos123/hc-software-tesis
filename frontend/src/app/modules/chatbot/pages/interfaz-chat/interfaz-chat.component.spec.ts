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

describe('InterfazChatComponent', () => {
  let component: InterfazChatComponent;
  let fixture: ComponentFixture<InterfazChatComponent>;
  let asistenteService: jasmine.SpyObj<AsistenteService>;
  let historiaClinicaService: jasmine.SpyObj<HistoriaClinicaService>;
  let antecedentesService: jasmine.SpyObj<AntecedentesService>;
  let router: jasmine.SpyObj<Router>;
  let transferService: ClinicalHistoryTransferService;
  let feedbackService: ClinicalHistoryFlowFeedbackService;
  let logoutSubject: Subject<void>;
  let importacionService: jasmine.SpyObj<PacienteImportacionService>;
  const paciente = {
    idPaciente: 8, dni: '01234567', numDocumento: '01234567', nombres: 'Andrea Lucía',
    apellidos: 'Quispe Ramírez', fechaIngreso: '2020-03-10', fechaNacimiento: '1992-01-01', estadoCivil: 'SOLTERO'
  };

  beforeEach(async () => {
    logoutSubject = new Subject<void>();
    asistenteService = jasmine.createSpyObj<AsistenteService>('AsistenteService', ['preguntar']);
    asistenteService.preguntar.and.returnValue(of({ intencion: 'ayuda', respuesta: 'Respuesta del asistente' }));
    historiaClinicaService = jasmine.createSpyObj<HistoriaClinicaService>('HistoriaClinicaService', ['buscarPacientesPorDni', 'getByPaciente', 'insert', 'update']);
    historiaClinicaService.buscarPacientesPorDni.and.returnValue(of([paciente]));
    historiaClinicaService.getByPaciente.and.returnValue(of([]));
    antecedentesService = jasmine.createSpyObj<AntecedentesService>('AntecedentesService', ['getByPacienteId']);
    antecedentesService.getByPacienteId.and.returnValue(of(undefined));
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    router.navigate.and.returnValue(Promise.resolve(true));
    importacionService = jasmine.createSpyObj('PacienteImportacionService', ['descargarPlantilla', 'obtenerNombreArchivo', 'validarArchivo', 'confirmarImportacion']);

    await TestBed.configureTestingModule({
      imports: [InterfazChatComponent],
      providers: [
        { provide: AsistenteService, useValue: asistenteService },
        { provide: HistoriaClinicaService, useValue: historiaClinicaService },
        { provide: AntecedentesService, useValue: antecedentesService },
        { provide: Router, useValue: router },
        { provide: PacienteImportacionService, useValue: importacionService },
        { provide: PacienteListRefreshService, useValue: jasmine.createSpyObj('PacienteListRefreshService', ['solicitarActualizacion']) },
        { provide: AuthService, useValue: { logout$: logoutSubject.asObservable() } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(InterfazChatComponent);
    component = fixture.componentInstance;
    transferService = TestBed.inject(ClinicalHistoryTransferService);
    feedbackService = TestBed.inject(ClinicalHistoryFlowFeedbackService);
    transferService.clearAll();
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

  function abrirMenuPacientes(): any {
    const principal = component.messages.find(mensaje => mensaje.menuId === 'principal')!;
    component.selectHistoricalMenuOption(principal, principal.options!.find(opcion => opcion.label === 'Consultar información')!);
    const consultar = component.messages.find(mensaje => mensaje.menuId === 'consultar')!;
    component.selectHistoricalMenuOption(consultar, consultar.options!.find(opcion => opcion.label === 'Pacientes')!);
    return component.messages.find(mensaje => mensaje.menuId === 'pacientes')!;
  }

  function iniciarImportacion(): any {
    const pacientes = abrirMenuPacientes();
    const opcion = pacientes.options.find((item: any) => item.label === 'Registrar pacientes de forma masiva');
    component.selectHistoricalMenuOption(pacientes, opcion);
    fixture.detectChanges();
    return component.messages.find(mensaje => mensaje.type === 'patient-import')!;
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
    expect(component.messages[1].options?.length).toBe(4);
  });

  it('debe procesar una sola vez el feedback de precarga exitosa y volver al menú principal', () => {
    iniciarFlujoHistoriaClinica();
    enviarDni('01234567');
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
      'Manejo del sistema', 'Consultar información', 'Verificar datos', 'Soporte y ayuda'
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
    expect(component.messages.at(-1)?.options?.length).toBe(4);
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

  it('debe mostrar localmente las instrucciones de una opción prompt', () => {
    component.quickAsk('Buscar paciente por DNI');

    expect(component.messages.at(-1)?.text).toContain('DNI de 8 dígitos');
    expect(asistenteService.preguntar).not.toHaveBeenCalled();
  });

  it('debe mostrar Crear historia clínica en el menú de historias', () => {
    const menuHistorias = abrirMenuHistorias();

    expect(menuHistorias.options.some((opcion: any) => opcion.label === 'Crear historia clínica')).toBeTrue();
  });

  it('debe mostrar e iniciar el registro masivo desde Pacientes sin usar Botpress', () => {
    const pacientes = abrirMenuPacientes();
    const opcion = pacientes.options.find((item: any) => item.label === 'Registrar pacientes de forma masiva');
    expect(opcion).toBeTruthy();

    component.selectHistoricalMenuOption(pacientes, opcion);
    fixture.detectChanges();

    expect(component.messages.some(mensaje => mensaje.sender === 'user' && mensaje.text === opcion.label)).toBeTrue();
    expect(pacientes.options.includes(opcion)).toBeFalse();
    expect(component.messages.some(mensaje => mensaje.type === 'patient-import')).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('Descargar plantilla oficial');
    expect(asistenteService.preguntar).not.toHaveBeenCalled();
  });

  it('debe conservar el estado y archivo de importación al minimizar', () => {
    const mensaje = iniciarImportacion();
    mensaje.importacion.plantillaDescargada = true;
    mensaje.importacion.estado = 'ARCHIVO_SELECCIONADO';
    mensaje.importacion.archivo = new File(['excel'], 'pacientes.xlsx');
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

  it('debe conservar todas las opciones anteriores del menú Pacientes', () => {
    const etiquetas = abrirMenuPacientes().options.map((opcion: any) => opcion.label);
    expect(etiquetas).toContain('¿Cuántos pacientes hay registrados?');
    expect(etiquetas).toContain('Muéstrame los últimos pacientes registrados');
    expect(etiquetas).toContain('Buscar paciente por DNI');
    expect(etiquetas).toContain('Buscar paciente por nombre');
    expect(etiquetas).toContain('Consulta el paciente por ID');
    expect(etiquetas).toContain('¿Cuál es la edad promedio de los pacientes?');
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

    enviarDni('01234567');

    const dniMessage = component.messages.find(message => message.sender === 'user' && message.text === '01234567')!;
    expect(scrollNewBlockSpy).toHaveBeenCalledOnceWith(dniMessage.id);
    expect(scrollBottomSpy).not.toHaveBeenCalled();
    expect(component.messages.at(-1)?.text).toContain('Paciente encontrado:');
  });

  [
    { name: 'inválido', configure: () => undefined, dni: '12A45678' },
    { name: 'inexistente', configure: () => historiaClinicaService.buscarPacientesPorDni.and.returnValue(of([])), dni: '01234567' },
    { name: 'duplicado', configure: () => historiaClinicaService.buscarPacientesPorDni.and.returnValue(of([paciente, { ...paciente, idPaciente: 9 }])), dni: '01234567' },
    { name: 'con error HTTP', configure: () => historiaClinicaService.buscarPacientesPorDni.and.returnValue(throwError(() => new Error('error HTTP'))), dni: '01234567' }
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

  ['', '12A45678', '1234567', '123456789'].forEach(dni => {
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
    enviarDni('  01234567  ');

    expect(historiaClinicaService.buscarPacientesPorDni).toHaveBeenCalledOnceWith('01234567');
    expect(component.clinicalHistoryFlow.step).toBe('awaitingConfirmation');
    expect((component.clinicalHistoryFlow as any).dni).toBe('01234567');
    expect(typeof (component.clinicalHistoryFlow as any).dni).toBe('string');
    expect(asistenteService.preguntar).not.toHaveBeenCalled();
  });

  it('debe aceptar solo coincidencias defensivas exactas y permitir reintentar cuando no existen', () => {
    historiaClinicaService.buscarPacientesPorDni.and.returnValue(of([{ ...paciente, dni: '99999999', numDocumento: '99999999' }]));
    iniciarFlujoHistoriaClinica();
    enviarDni('01234567');

    expect(component.clinicalHistoryFlow).toEqual({ step: 'awaitingDni' });
    expect(component.messages.at(-1)?.text).toBe('No existe un paciente registrado con el DNI indicado.');
    expect(antecedentesService.getByPacienteId).not.toHaveBeenCalled();
    expect(historiaClinicaService.getByPaciente).not.toHaveBeenCalled();

    historiaClinicaService.buscarPacientesPorDni.and.returnValue(of([paciente]));
    enviarDni('01234567');
    expect(historiaClinicaService.buscarPacientesPorDni).toHaveBeenCalledTimes(2);
    expect(component.clinicalHistoryFlow.step).toBe('awaitingConfirmation');
  });

  it('debe mantener la captura activa ante varios pacientes sin seleccionar el primero', () => {
    historiaClinicaService.buscarPacientesPorDni.and.returnValue(of([paciente, { ...paciente, idPaciente: 9 }]));
    iniciarFlujoHistoriaClinica();
    enviarDni('01234567');

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
    const segundoPaciente = { ...paciente, idPaciente: 10, dni: '87654321', numDocumento: '87654321' };
    historiaClinicaService.buscarPacientesPorDni.and.callFake(dni => dni === '01234567'
      ? of([paciente, { ...paciente, idPaciente: 9 }])
      : of([segundoPaciente]));
    iniciarFlujoHistoriaClinica();
    enviarDni('01234567');

    enviarDni('87654321');

    expect(historiaClinicaService.buscarPacientesPorDni.calls.allArgs()).toEqual([['01234567'], ['87654321']]);
    expect(asistenteService.preguntar).not.toHaveBeenCalled();
    expect(antecedentesService.getByPacienteId).toHaveBeenCalledOnceWith(10);
    expect(historiaClinicaService.getByPaciente).toHaveBeenCalledOnceWith(10);
    expect(component.clinicalHistoryFlow.step).toBe('awaitingConfirmation');
    expect((component.clinicalHistoryFlow as any).dni).toBe('87654321');

    component.cancelClinicalHistoryFlow();
    expect(component.clinicalHistoryFlow).toEqual({ step: 'idle' });
  });

  it('debe consultar en paralelo antecedentes e historias para un paciente único', () => {
    iniciarFlujoHistoriaClinica();
    enviarDni('01234567');

    expect(antecedentesService.getByPacienteId).toHaveBeenCalledOnceWith(8);
    expect(historiaClinicaService.getByPaciente).toHaveBeenCalledOnceWith(8);
    expect(component.clinicalHistoryFlow.step).toBe('awaitingConfirmation');
  });

  it('debe representar antecedentes inexistentes con null sin inventar datos', () => {
    iniciarFlujoHistoriaClinica();
    enviarDni('01234567');

    const prefill = (component.clinicalHistoryFlow as any).prefill;
    expect(prefill.enfermedadesPrevias).toBeNull();
    expect(prefill.cirugiasPrevias).toBeNull();
    expect(prefill.alergiaMedicamentos).toBeNull();
  });

  it('debe mostrar cero historias, el resumen limitado y los botones estructurados', () => {
    antecedentesService.getByPacienteId.and.returnValue(of({ enfermedadesPrevias: 'Asma severa', cirugiasPrevias: 'Apendicectomía', alergiaMedicamentos: 'Penicilina' }));
    iniciarFlujoHistoriaClinica();
    enviarDni('01234567');
    fixture.detectChanges();
    const resumen = component.messages.at(-1)?.text ?? '';

    expect(resumen).toContain('Nombre: Andrea Lucía Quispe Ramírez');
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
    enviarDni('01234567');

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
    enviarDni('01234567');
    const scrollBottomSpy = spyOn(component, 'scrollToBottom');
    const scrollNewBlockSpy = spyOn(component as any, 'scrollToNewBlock');

    component.continueClinicalHistoryFlow();
    const transferId = createTransferSpy.calls.mostRecent().returnValue;
    component.continueClinicalHistoryFlow();

    expect(createTransferSpy).toHaveBeenCalledTimes(1);
    const candidate = createTransferSpy.calls.mostRecent().args[0];
    expect(candidate).toEqual(jasmine.objectContaining({
      idPaciente: 8, dni: '01234567', nombres: 'Andrea Lucía', apellidos: 'Quispe Ramírez',
      fechaIngreso: '2020-03-10', fechaNacimiento: '1992-01-01', estadoCivil: 'SOLTERO'
    }));
    expect(candidate as any).not.toEqual(jasmine.objectContaining({ nombreCompleto: jasmine.anything(), existingClinicalHistoryCount: jasmine.anything() }));
    expect(router.navigate).toHaveBeenCalledOnceWith(
      ['/historiaClinica', 'mantenimiento-historias-clinicas', 'nuevo'],
      { state: { source: 'chatbot', transferId } }
    );
    const navigation = router.navigate.calls.mostRecent().args;
    expect(JSON.stringify(navigation)).not.toContain('01234567');
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
    enviarDni('01234567');

    component.continueClinicalHistoryFlow();
    const transferId = (component.clinicalHistoryFlow as any).transferId;
    tick();

    expect(transferService.revokeTransfer).toHaveBeenCalledOnceWith(transferId);
    expect(transferService.peekTransfer(transferId)).toBeNull();
    expect(component.clinicalHistoryFlow.step).toBe('awaitingConfirmation');
    expect((component.clinicalHistoryFlow as any).dni).toBe('01234567');
    expect(component.messages.at(-1)?.text).toBe('No se pudo abrir el formulario de Nueva Historia Clínica. Inténtalo nuevamente.');
  }));

  it('debe cancelar y limpiar todos los datos temporales conservando el historial', () => {
    spyOn(transferService, 'createTransfer').and.callThrough();
    iniciarFlujoHistoriaClinica();
    enviarDni('01234567');
    const mensajesAntes = component.messages.length;

    component.cancelClinicalHistoryFlow();

    expect(component.clinicalHistoryFlow).toEqual({ step: 'idle' });
    expect(JSON.stringify(component.clinicalHistoryFlow)).not.toContain('01234567');
    expect(component.messages.length).toBe(mensajesAntes + 2);
    expect(component.messages.at(-1)?.text).toBe('La creación de la historia clínica fue cancelada.');
    expect(transferService.createTransfer).not.toHaveBeenCalled();
  });

  it('debe limpiar el flujo al volver al Menú principal', () => {
    spyOn(transferService, 'createTransfer').and.callThrough();
    iniciarFlujoHistoriaClinica();
    enviarDni('01234567');

    component.quickAsk('Menú principal');

    expect(component.clinicalHistoryFlow).toEqual({ step: 'idle' });
    expect(component.messages.at(-1)).toEqual(jasmine.objectContaining({ type: 'menu', menuId: 'principal' }));
    expect(transferService.createTransfer).not.toHaveBeenCalled();
  });

  it('debe recuperarse de un error al buscar el paciente', () => {
    historiaClinicaService.buscarPacientesPorDni.and.returnValue(throwError(() => new Error('falló búsqueda')));
    iniciarFlujoHistoriaClinica();
    enviarDni('01234567');

    expect(component.clinicalHistoryFlow).toEqual({ step: 'awaitingDni' });
    expect(component.messages.at(-1)?.text).toBe('No se pudo consultar la información del paciente en este momento. Inténtalo nuevamente.');
    expect(component.isLoading).toBeFalse();
  });

  it('debe recuperarse si falla la consulta de antecedentes o historias', () => {
    antecedentesService.getByPacienteId.and.returnValue(throwError(() => new Error('falló antecedentes')));
    iniciarFlujoHistoriaClinica();
    enviarDni('01234567');

    expect(component.clinicalHistoryFlow).toEqual({ step: 'awaitingDni' });
    expect(component.messages.at(-1)?.text).toContain('No se pudo consultar la información');
    expect(component.messages.some(mensaje => mensaje.text?.includes('¿Deseas continuar'))).toBeFalse();
  });

  it('debe recuperarse si falla específicamente la consulta de historias', () => {
    historiaClinicaService.getByPaciente.and.returnValue(throwError(() => new Error('falló historias')));
    iniciarFlujoHistoriaClinica();
    enviarDni('01234567');

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
    enviarDni('01234567');

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
    enviarDni('01234567');
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
    enviarDni('01234567');

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
    enviarDni('01234567');

    component.cancelClinicalHistoryFlow();

    expect(busquedaPendiente.observed).toBeFalse();
    expect(component.isLoading).toBeFalse();
    expect(component.clinicalHistoryFlow).toEqual({ step: 'idle' });
  });
});
