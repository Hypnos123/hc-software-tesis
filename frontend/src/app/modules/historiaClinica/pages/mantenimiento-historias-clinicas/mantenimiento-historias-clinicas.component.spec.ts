import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { Location } from '@angular/common';
import { of } from 'rxjs';

import { MensajesSwalService } from '@app/shared/services/mensajes-swal.service';
import { HistoriaClinicaService } from '../../services/consultas.service';
import { MantenimientoHistoriasClinicasComponent } from './mantenimiento-historias-clinicas.component';
import { ClinicalHistoryTransferService } from '@app/shared/services/clinical-history-transfer.service';
import { ClinicalHistoryTransferCandidate } from '@app/shared/models/clinical-history-transfer';
import { ClinicalHistoryFlowFeedbackService } from '@app/shared/services/clinical-history-flow-feedback.service';
import { AuthService } from '@app/auth/services/auth.service';

describe('MantenimientoHistoriasClinicasComponent', () => {
  let component: MantenimientoHistoriasClinicasComponent;
  let fixture: ComponentFixture<MantenimientoHistoriasClinicasComponent>;
  let modoRuta: 'nuevo' | 'ver' | 'editar';
  let idRuta: string | null;
  let historiaService: jasmine.SpyObj<HistoriaClinicaService>;
  let mensajes: jasmine.SpyObj<MensajesSwalService>;
  let router: jasmine.SpyObj<Router>;
  let location: jasmine.SpyObj<Location>;
  let transferService: ClinicalHistoryTransferService;
  let consumeTransferSpy: jasmine.Spy;
  let navigationState: Record<string, unknown> | null;
  let feedbackService: jasmine.SpyObj<ClinicalHistoryFlowFeedbackService>;

  const historiaExistente = {
    idHistoriaClinica: 25,
    fechaIngreso: '2026-07-01',
    fechaNacimiento: '1996-01-01',
    apellidos: 'Pérez Díaz',
    nombres: 'Ana María',
    estadoCivil: 'SOLTERO',
    edad: 30,
    numDocumento: '12345678',
    enfermedadesPrevias: 'Asma',
    cirugiasPrevias: 'Ninguna',
    alergiaMedicamentos: 'Penicilina'
  };

  const candidatoChatbot: ClinicalHistoryTransferCandidate = {
    idPaciente: 8,
    dni: '01234567',
    nombres: 'Andrea Lucía',
    apellidos: 'Quispe Ramírez',
    fechaIngreso: '2020-03-10',
    fechaNacimiento: '1992-02-17',
    estadoCivil: 'SOLTERO',
    enfermedadesPrevias: null,
    cirugiasPrevias: 'Apendicectomía',
    alergiaMedicamentos: null
  };

  beforeEach(async () => {
    modoRuta = 'nuevo';
    idRuta = null;
    navigationState = null;
    window.history.replaceState({ navigationId: 17 }, '', window.location.href);
    historiaService = jasmine.createSpyObj<HistoriaClinicaService>('HistoriaClinicaService', [
      'getById',
      'insert',
      'update',
      'buscarPacientesPorNombre',
      'buscarPacientesPorDni',
      'getAntecedentesByPaciente'
    ]);
    historiaService.getById.and.returnValue(of(historiaExistente));
    historiaService.insert.and.returnValue(of({ idGenerado: 101, mensaje: 'Registro guardado correctamente.' }));
    historiaService.update.and.returnValue(of({ idGenerado: 25, mensaje: 'Registro actualizado correctamente.' }));

    mensajes = jasmine.createSpyObj<MensajesSwalService>('MensajesSwalService', [
      'mensajeAdvertencia', 'mensajePregunta', 'mensajeExito', 'mensajeError'
    ]);
    mensajes.mensajePregunta.and.returnValue(Promise.resolve({ isConfirmed: true } as any));
    router = jasmine.createSpyObj<Router>('Router', ['navigate', 'getCurrentNavigation'], { url: '/historiaClinica/mantenimiento-historias-clinicas/nuevo' });
    router.getCurrentNavigation.and.callFake(() => navigationState ? ({ extras: { state: navigationState } } as any) : null);
    location = jasmine.createSpyObj<Location>('Location', ['replaceState']);
    feedbackService = jasmine.createSpyObj<ClinicalHistoryFlowFeedbackService>('ClinicalHistoryFlowFeedbackService', ['emit']);
    feedbackService.emit.and.callFake(type => ({ id: `feedback-${type}`, type, createdAt: Date.now() }));

    await TestBed.configureTestingModule({
      imports: [MantenimientoHistoriasClinicasComponent],
      providers: [
        { provide: HistoriaClinicaService, useValue: historiaService },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: (parametro: string) => convertToParamMap({ modo: modoRuta, id: idRuta }).get(parametro)
              }
            }
          }
        },
        { provide: Router, useValue: router },
        { provide: Location, useValue: location },
        { provide: ClinicalHistoryFlowFeedbackService, useValue: feedbackService },
        { provide: AuthService, useValue: { puedeCrearHistoriasClinicas: () => true } },
        {
          provide: MensajesSwalService,
          useValue: mensajes
        }
      ]
    }).compileComponents();

    transferService = TestBed.inject(ClinicalHistoryTransferService);
    transferService.clearAll();
    consumeTransferSpy = spyOn(transferService, 'consumeTransfer').and.callThrough();
    crearComponente();
  });

  function crearComponente(): void {
    fixture = TestBed.createComponent(MantenimientoHistoriasClinicasComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  function recrearComponente(): void {
    fixture.destroy();
    crearComponente();
  }

  function prepararNavegacionChatbot(candidate: ClinicalHistoryTransferCandidate = candidatoChatbot): string {
    const transferId = transferService.createTransfer(candidate);
    navigationState = { source: 'chatbot', transferId };
    return transferId;
  }

  it('debe mostrar una captura manual sin buscadores ni sugerencias en modo nuevo', () => {
    const elemento: HTMLElement = fixture.nativeElement;

    expect(elemento.textContent).not.toContain('Seleccionar Paciente');
    expect(elemento.querySelector('[formControlName="nombrePacienteSel"]')).toBeNull();
    expect(elemento.querySelector('[formControlName="dniSel"]')).toBeNull();
    expect(elemento.querySelectorAll('.autocomplete-list').length).toBe(0);
    expect(elemento.querySelectorAll('[formControlName="dni"]').length).toBe(1);
    expect(historiaService.buscarPacientesPorNombre).not.toHaveBeenCalled();
    expect(historiaService.buscarPacientesPorDni).not.toHaveBeenCalled();
    expect(historiaService.getAntecedentesByPaciente).not.toHaveBeenCalled();
    expect(consumeTransferSpy).not.toHaveBeenCalled();
    expect(component.mensajePrecargaChatbot).toBeNull();
    expect(component.mensajeErrorPrecargaChatbot).toBeNull();
    expect(feedbackService.emit).not.toHaveBeenCalled();
  });

  it('debe habilitar los campos manuales y mantener deshabilitado el ID', () => {
    const controlesManuales = [
      'fechaIngreso', 'fechaNacimiento', 'apellidos', 'nombres', 'estadoCivil', 'dni',
      'enfPrevias', 'cirugiasPrevias', 'alergiasMedicamentos'
    ];

    controlesManuales.forEach(nombre => expect(component.frm.get(nombre)?.enabled).toBeTrue());
    expect(component.frm.get('idHistoriaClinica')?.disabled).toBeTrue();
    expect(component.frm.get('edad')?.disabled).toBeTrue();
    expect(fixture.nativeElement.querySelector('[formControlName="idHistoriaClinica"]')).toBeNull();
  });

  it('debe consumir una transferencia válida una sola vez y autocompletar los campos', () => {
    const transferId = prepararNavegacionChatbot();
    recrearComponente();

    expect(consumeTransferSpy).toHaveBeenCalledOnceWith(transferId);
    expect(component.frm.getRawValue()).toEqual(jasmine.objectContaining({
      idHistoriaClinica: '',
      fechaIngreso: new Date(2020, 2, 10),
      fechaNacimiento: new Date(1992, 1, 17),
      apellidos: 'Quispe Ramírez',
      nombres: 'Andrea Lucía',
      estadoCivil: 'SOLTERO',
      dni: '01234567',
      enfPrevias: '',
      cirugiasPrevias: 'Apendicectomía',
      alergiasMedicamentos: ''
    }));
    expect(component.frm.contains('idPaciente')).toBeFalse();
    expect(component.frm.get('edad')?.value).toBe(component.calcularEdad(new Date(1992, 1, 17)));
    expect(component.frm.get('edad')?.disabled).toBeTrue();
    expect(component.frm.get('dni')?.disabled).toBeTrue();
    expect(transferService.consumeTransfer(transferId)).toBeNull();
    expect(historiaService.insert).not.toHaveBeenCalled();
    expect(historiaService.update).not.toHaveBeenCalled();
    expect(feedbackService.emit).toHaveBeenCalledOnceWith('prefill-success');
    const feedback = feedbackService.emit.calls.mostRecent().returnValue;
    expect(Object.keys(feedback).sort()).toEqual(['createdAt', 'id', 'type']);
    expect(JSON.stringify(feedback)).not.toContain(candidatoChatbot.dni);
    expect(JSON.stringify(feedback)).not.toContain(candidatoChatbot.nombres);
    expect(JSON.stringify(feedback)).not.toContain(String(candidatoChatbot.idPaciente));
  });

  it('debe mostrar los mensajes de precarga y de guardado manual', () => {
    prepararNavegacionChatbot();
    recrearComponente();

    expect(fixture.nativeElement.textContent).toContain('Los datos del paciente fueron cargados desde el chatbot. Revísalos antes de guardar.');
    expect(fixture.nativeElement.textContent).toContain('La historia clínica se guardará únicamente cuando pulses Guardar.');
    expect(component.mensajeErrorPrecargaChatbot).toBeNull();
  });

  it('debe limpiar source y transferId del estado sin modificar la URL', () => {
    prepararNavegacionChatbot();
    recrearComponente();

    expect(location.replaceState).toHaveBeenCalledOnceWith(
      '/historiaClinica/mantenimiento-historias-clinicas/nuevo',
      '',
      { navigationId: 17 }
    );
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('debe mantener el DNI editable en el acceso manual', () => {
    expect(component.frm.get('dni')?.enabled).toBeTrue();
    expect((fixture.nativeElement.querySelector('[formControlName="dni"]') as HTMLInputElement).disabled).toBeFalse();
    expect(consumeTransferSpy).not.toHaveBeenCalled();
  });

  it('debe ignorar transferencias en modo editar y visualizar', () => {
    const transferId = prepararNavegacionChatbot();
    modoRuta = 'editar';
    idRuta = '25';
    recrearComponente();
    expect(consumeTransferSpy).not.toHaveBeenCalled();
    expect(transferService.peekTransfer(transferId)).not.toBeNull();
    expect(feedbackService.emit).not.toHaveBeenCalled();

    modoRuta = 'ver';
    recrearComponente();
    expect(consumeTransferSpy).not.toHaveBeenCalled();
    expect(transferService.peekTransfer(transferId)).not.toBeNull();
    expect(feedbackService.emit).not.toHaveBeenCalled();
  });

  it('debe ignorar un transferId cuando source no es chatbot', () => {
    const transferId = transferService.createTransfer(candidatoChatbot);
    navigationState = { source: 'otro-origen', transferId };
    recrearComponente();

    expect(consumeTransferSpy).not.toHaveBeenCalled();
    expect(component.frm.get('dni')?.value).toBe('');
    expect(component.mensajeErrorPrecargaChatbot).toBeNull();
    expect(location.replaceState).not.toHaveBeenCalled();
  });

  it('debe permitir captura manual si falta el transferId', () => {
    navigationState = { source: 'chatbot' };
    recrearComponente();

    expect(consumeTransferSpy).not.toHaveBeenCalled();
    expect(component.mensajeErrorPrecargaChatbot).toBe('No fue posible recuperar los datos enviados por el chatbot. Puedes completar el formulario manualmente.');
    expect(component.frm.get('dni')?.enabled).toBeTrue();
    expect(component.frm.get('dni')?.value).toBe('');
    expect(location.replaceState).toHaveBeenCalled();
    expect(feedbackService.emit).toHaveBeenCalledOnceWith('prefill-failure');
  });

  it('debe explicar cómo resolver un DNI asociado a pacientes duplicados', () => {
    const mensaje = (component as any).obtenerMensajeError({
      status: 409,
      error: {
        codigo: 'DNI_AMBIGUO',
        mensaje: 'El DNI está asociado a varios pacientes y no se puede resolver automáticamente.'
      }
    });

    expect(mensaje).toContain('No es posible crear la historia clínica.');
    expect(mensaje).toContain('uno de los registros ya cuenta con una historia clínica.');
    expect(mensaje).toContain('debes revisar los pacientes duplicados y definir cuál registro se conservará.');
    expect(mensaje).toContain('El Chatbot puede ayudarte a gestionar este problema.');
    expect(mensaje).not.toContain('no se puede resolver automáticamente');
  });

  it('debe manejar una transferencia inexistente sin completar datos parciales', () => {
    navigationState = { source: 'chatbot', transferId: 'transfer-inexistente' };
    recrearComponente();

    expect(consumeTransferSpy).toHaveBeenCalledOnceWith('transfer-inexistente');
    expect(component.frm.getRawValue()).toEqual(jasmine.objectContaining({ nombres: '', apellidos: '', dni: '', fechaNacimiento: null }));
    expect(component.mensajeErrorPrecargaChatbot).toContain('No fue posible recuperar');
    expect(component.frm.get('dni')?.enabled).toBeTrue();
    expect(historiaService.insert).not.toHaveBeenCalled();
    expect(historiaService.update).not.toHaveBeenCalled();
    expect(feedbackService.emit).toHaveBeenCalledOnceWith('prefill-failure');
  });

  it('debe manejar una transferencia vencida usando history.state como respaldo', () => {
    const now = Date.now();
    const transferId = transferService.createTransfer(candidatoChatbot);
    navigationState = null;
    window.history.replaceState({ navigationId: 18, source: 'chatbot', transferId }, '', window.location.href);
    spyOn(Date, 'now').and.returnValue(now + ClinicalHistoryTransferService.TTL_MS + 1);
    recrearComponente();

    expect(consumeTransferSpy).toHaveBeenCalledOnceWith(transferId);
    expect(component.mensajeErrorPrecargaChatbot).toContain('No fue posible recuperar');
    expect(component.frm.get('dni')?.enabled).toBeTrue();
    expect(transferService.peekTransfer(transferId)).toBeNull();
  });

  it('debe impedir que una transferencia consumida vuelva a aplicarse', () => {
    const transferId = prepararNavegacionChatbot();
    recrearComponente();
    expect(component.frm.get('dni')?.value).toBe('01234567');

    recrearComponente();

    expect(consumeTransferSpy).toHaveBeenCalledTimes(2);
    expect(consumeTransferSpy.calls.mostRecent().args[0]).toBe(transferId);
    expect(component.frm.get('dni')?.value).toBe('');
    expect(component.mensajeErrorPrecargaChatbot).toContain('No fue posible recuperar');
  });

  it('debe guardar manualmente el prefill sin IDs ni edad', fakeAsync(() => {
    prepararNavegacionChatbot();
    recrearComponente();
    expect(historiaService.insert).not.toHaveBeenCalled();

    component.guardar();
    tick();

    expect(historiaService.insert).toHaveBeenCalledTimes(1);
    const request = historiaService.insert.calls.mostRecent().args[0] as any;
    expect(request).toEqual({
      fechaIngreso: '2020-03-10', fechaNacimiento: '1992-02-17',
      apellidos: 'Quispe Ramírez', nombres: 'Andrea Lucía', estadoCivil: 'SOLTERO', dni: '01234567',
      enfermedadesPrevias: undefined, cirugiasPrevias: 'Apendicectomía', alergiaMedicamentos: undefined
    });
    expect(request.idPaciente).toBeUndefined();
    expect(request.idHistoriaClinica).toBeUndefined();
    expect(request.edad).toBeUndefined();
  }));

  it('debe validar DNI, nombres y apellidos requeridos sin aceptar espacios', () => {
    component.frm.patchValue({ dni: '12A', nombres: '   ', apellidos: '' });
    component.frm.get('dni')?.markAsTouched();
    component.frm.get('nombres')?.markAsTouched();
    component.frm.get('apellidos')?.markAsTouched();
    fixture.detectChanges();

    expect(component.frm.get('dni')?.hasError('pattern')).toBeTrue();
    expect(component.frm.get('nombres')?.hasError('soloEspacios')).toBeTrue();
    expect(component.frm.get('apellidos')?.hasError('required')).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('El DNI debe contener exactamente ocho dígitos.');
    expect(fixture.nativeElement.textContent).toContain('Los nombres son obligatorios');
    expect(fixture.nativeElement.textContent).toContain('Los apellidos son obligatorios');
  });

  it('debe habilitar el guardado cuando el formulario manual es válido', () => {
    const botonGuardar: HTMLButtonElement = fixture.nativeElement.querySelector('.footer-actions button[title]');

    expect(botonGuardar.disabled).toBeTrue();

    component.frm.patchValue({
      fechaIngreso: new Date(2026, 6, 26),
      fechaNacimiento: new Date(1996, 0, 1),
      apellidos: 'Pérez Díaz',
      nombres: 'Ana María',
      estadoCivil: 'SOLTERO',
      edad: component.calcularEdad(new Date(1996, 0, 1)),
      dni: '12345678'
    });
    fixture.detectChanges();

    expect(botonGuardar.disabled).toBeFalse();
  });

  it('debe confirmar y enviar el contrato manual sin ID de historia', fakeAsync(() => {
    component.frm.patchValue({
      fechaIngreso: new Date(2026, 6, 26),
      fechaNacimiento: new Date(1996, 0, 1),
      apellidos: ' Pérez Díaz ',
      nombres: ' Ana María ',
      estadoCivil: 'SOLTERO',
      edad: component.calcularEdad(new Date(1996, 0, 1)),
      dni: '12345678',
      enfPrevias: ' Asma ',
      cirugiasPrevias: 'Ninguna',
      alergiasMedicamentos: 'Penicilina'
    });

    component.guardar();
    tick();

    const request = historiaService.insert.calls.mostRecent().args[0] as any;
    expect(request).toEqual(jasmine.objectContaining({
      fechaIngreso: '2026-07-26', fechaNacimiento: '1996-01-01',
      apellidos: 'Pérez Díaz', nombres: 'Ana María', dni: '12345678',
      enfermedadesPrevias: 'Asma', cirugiasPrevias: 'Ninguna', alergiaMedicamentos: 'Penicilina'
    }));
    expect(request.edad).toBeUndefined();
    expect(request.idHistoriaClinica).toBeUndefined();
    expect(request.idPaciente).toBeUndefined();
  }));

  it('debe recalcular la edad y rechazar una fecha de nacimiento futura', () => {
    component.frm.get('fechaNacimiento')?.setValue(new Date(1996, 0, 1));
    expect(component.frm.get('edad')?.value).toBe(component.calcularEdad(new Date(1996, 0, 1)));

    const futura = new Date();
    futura.setDate(futura.getDate() + 1);
    component.frm.get('fechaNacimiento')?.setValue(futura);
    component.frm.get('fechaNacimiento')?.markAsTouched();
    fixture.detectChanges();

    expect(component.frm.get('fechaNacimiento')?.hasError('fechaFutura')).toBeTrue();
    expect(component.frm.get('edad')?.value).toBeUndefined();
    expect(fixture.nativeElement.textContent).toContain('La fecha de nacimiento no puede ser futura.');
  });

  it('debe calcular la edad correctamente antes y después del cumpleaños', () => {
    const hoy = new Date();
    const nacimientoCumplido = new Date(hoy.getFullYear() - 20, hoy.getMonth(), hoy.getDate());
    const nacimientoPorCumplir = new Date(hoy.getFullYear() - 20, hoy.getMonth(), hoy.getDate());
    nacimientoPorCumplir.setDate(nacimientoPorCumplir.getDate() + 1);

    expect(component.calcularEdad(nacimientoCumplido)).toBe(20);
    expect(component.calcularEdad(nacimientoPorCumplir)).toBe(19);
  });

  it('debe habilitar solo los campos permitidos en modo editar', () => {
    fixture.destroy();
    modoRuta = 'editar';
    idRuta = '25';
    crearComponente();

    expect(historiaService.getById).toHaveBeenCalledWith(25);
    expect(component.historiaCargada).toBeTrue();
    expect(component.frm.get('nombres')?.value).toBe('Ana María');
    expect(component.frm.get('fechaNacimiento')?.value).toEqual(new Date(1996, 0, 1));
    const editables = [
      'fechaIngreso', 'fechaNacimiento', 'apellidos', 'nombres', 'estadoCivil',
      'enfPrevias', 'cirugiasPrevias', 'alergiasMedicamentos'
    ];
    editables.forEach(nombre => expect(component.frm.get(nombre)?.enabled).toBeTrue());
    ['idHistoriaClinica', 'dni', 'edad'].forEach(nombre => expect(component.frm.get(nombre)?.disabled).toBeTrue());
    expect(component.frm.get('idHistoriaClinica')?.value).toBe(25);
    expect((fixture.nativeElement.querySelector('.footer-actions button[title]') as HTMLButtonElement).disabled).toBeFalse();
  });

  it('debe conservar el formulario y no actualizar cuando se cancela la confirmación', fakeAsync(() => {
    fixture.destroy();
    modoRuta = 'editar';
    idRuta = '25';
    mensajes.mensajePregunta.and.returnValue(Promise.resolve({ isConfirmed: false } as any));
    crearComponente();
    component.frm.get('nombres')?.setValue('Nombre editado');

    component.guardar();
    tick();

    expect(historiaService.update).not.toHaveBeenCalled();
    expect(component.frm.get('nombres')?.value).toBe('Nombre editado');
  }));

  it('debe confirmar y enviar el contrato de actualización sin identificadores, DNI ni edad', fakeAsync(() => {
    fixture.destroy();
    modoRuta = 'editar';
    idRuta = '25';
    crearComponente();
    component.frm.patchValue({
      fechaIngreso: new Date(2026, 6, 27),
      fechaNacimiento: new Date(1995, 4, 10),
      apellidos: ' Pérez Actualizado ',
      nombres: ' Ana Actualizada ',
      estadoCivil: 'CASADO',
      enfPrevias: ' Asma controlada ',
      cirugiasPrevias: '',
      alergiasMedicamentos: ' Penicilina '
    });

    component.guardar();
    tick();

    expect(mensajes.mensajePregunta).toHaveBeenCalledWith('Los datos de la historia clínica se modificara. ¿Desea continuar?');
    const [id, request] = historiaService.update.calls.mostRecent().args as [number, any];
    expect(id).toBe(25);
    expect(request).toEqual({
      fechaIngreso: '2026-07-27', fechaNacimiento: '1995-05-10',
      apellidos: 'Pérez Actualizado', nombres: 'Ana Actualizada', estadoCivil: 'CASADO',
      enfermedadesPrevias: 'Asma controlada', cirugiasPrevias: undefined,
      alergiaMedicamentos: 'Penicilina'
    });
    expect(request.idHistoriaClinica).toBeUndefined();
    expect(request.idPaciente).toBeUndefined();
    expect(request.dni).toBeUndefined();
    expect(request.edad).toBeUndefined();
  }));

  it('debe mantener deshabilitado todo el formulario en modo ver', () => {
    fixture.destroy();
    modoRuta = 'ver';
    idRuta = '25';
    crearComponente();

    Object.values(component.frm.controls).forEach(control => expect(control.disabled).toBeTrue());
    expect(fixture.nativeElement.querySelector('.footer-actions button[title]')).toBeNull();
  });
});
