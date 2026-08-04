import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { of, Subject, throwError } from 'rxjs';
import { GestionDuplicadosChatComponent } from './gestion-duplicados-chat.component';
import { PacienteDuplicadoChatService } from '../../services/paciente-duplicado-chat.service';
import { PacienteListRefreshService } from '@app/modules/paciente/services/paciente-list-refresh.service';
import { crearGestionDuplicadosState, PacienteDuplicadoAnalisisResponse, PacienteDuplicadoDetalle } from '../../models/paciente-duplicado-chat';

describe('GestionDuplicadosChatComponent', () => {
  let fixture: ComponentFixture<GestionDuplicadosChatComponent>;
  let component: GestionDuplicadosChatComponent;
  let service: jasmine.SpyObj<PacienteDuplicadoChatService>;
  let refresh: jasmine.SpyObj<PacienteListRefreshService>;

  const principal: PacienteDuplicadoDetalle = {
    idPaciente: 10, nombreCompleto: 'Patricia Cárdenas', dni: '12345678', estadoRegistro: 'ACTIVO',
    cantidadHistoriasClinicas: 1, cantidadConsultas: 2, cantidadAntecedentes: 1,
    cantidadCamposPersonalesCompletos: 8, cantidadGruposClinicosCompletos: 2,
    tieneInformacionClinicaRelevante: true
  };
  const archivado: PacienteDuplicadoDetalle = {
    ...principal, idPaciente: 13, nombreCompleto: 'Miguel Torres', cantidadHistoriasClinicas: 0,
    cantidadConsultas: 0, cantidadAntecedentes: 0, cantidadGruposClinicosCompletos: 0,
    tieneInformacionClinicaRelevante: false
  };

  beforeEach(async () => {
    service = jasmine.createSpyObj('PacienteDuplicadoChatService', ['analizar', 'archivar']);
    refresh = jasmine.createSpyObj('PacienteListRefreshService', ['solicitarActualizacion']);
    await TestBed.configureTestingModule({
      imports: [GestionDuplicadosChatComponent],
      providers: [
        { provide: PacienteDuplicadoChatService, useValue: service },
        { provide: PacienteListRefreshService, useValue: refresh }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(GestionDuplicadosChatComponent);
    component = fixture.componentInstance;
    component.state = crearGestionDuplicadosState();
    component.view = 'dni';
    component.active = true;
    fixture.detectChanges();
  });

  it('rechaza DNI inválido sin llamar al backend', () => {
    component.dniInput = '12A45678';
    component.consultarDni();
    expect(component.state.mensajeError).toBe('El DNI debe contener exactamente ocho números.');
    expect(service.analizar).not.toHaveBeenCalled();
  });

  it('consulta DNI válido y presenta tarjetas y razones del backend', () => {
    service.analizar.and.returnValue(of(analisis(false)));
    component.dniInput = '12345678';
    component.consultarDni();
    component.view = 'results';
    fixture.detectChanges();
    expect(service.analizar).toHaveBeenCalledOnceWith('12345678');
    expect(fixture.nativeElement.querySelectorAll('.patient-card').length).toBe(2);
    expect(fixture.nativeElement.textContent).toContain('Recomendado para conservar');
    expect(fixture.nativeElement.textContent).toContain('Tiene 2 consultas registradas');
  });

  it('emite el DNI del usuario antes de iniciar la llamada HTTP', () => {
    const eventos: string[] = [];
    const pendiente = new Subject<PacienteDuplicadoAnalisisResponse>();
    component.mensajeConversacional.subscribe(evento => eventos.push(evento.texto));
    service.analizar.and.callFake(() => {
      expect(eventos).toEqual(['01234567']);
      return pendiente.asObservable();
    });

    component.dniInput = '01234567';
    component.consultarDni();

    expect(service.analizar).toHaveBeenCalledOnceWith('01234567');
    expect(eventos.filter(texto => texto === '01234567').length).toBe(1);
    pendiente.complete();
  });

  it('informa correctamente cuando no hay pacientes o solo existe uno', () => {
    const eventos: string[] = [];
    component.mensajeConversacional.subscribe(evento => eventos.push(evento.texto));
    service.analizar.and.returnValue(of({ ...analisis(false), esDuplicado: false, cantidadPacientesActivos: 0, pacientes: [] }));
    component.dniInput = '12345678';
    component.consultarDni();
    expect(eventos).toContain('No se encontraron pacientes activos con ese DNI.');

    component.state.estado = 'SOLICITANDO_DNI';
    service.analizar.and.returnValue(of({ ...analisis(false), esDuplicado: false, cantidadPacientesActivos: 1, pacientes: [principal] }));
    component.consultarDni();
    expect(eventos).toContain('Solo existe un paciente activo con ese DNI. No hay duplicados para gestionar.');
  });

  it('selecciona pacientes distintos y permite cambiar la selección', () => {
    prepararResultados(false);
    component.seleccionarParaArchivar(archivado);
    expect(component.state.pacienteArchivado?.idPaciente).toBe(13);
    expect(component.state.pacientePrincipal?.idPaciente).toBe(10);
    expect(component.state.pacienteArchivado?.idPaciente).not.toBe(component.state.pacientePrincipal?.idPaciente);
    component.cambiarSeleccion();
    expect(component.state.pacienteArchivado).toBeUndefined();
    expect(component.state.estado).toBe('MOSTRANDO_RESULTADOS');
  });

  it('exige revisión clínica sin marcarla automáticamente', () => {
    prepararResultados(true);
    component.seleccionarParaArchivar(archivado);
    component.confirmarSeleccion();
    expect(component.state.estado).toBe('REQUIERE_REVISION_CLINICA');
    expect(component.state.revisionClinicaConfirmada).toBeFalse();
    component.confirmarRevision();
    expect(component.state.revisionClinicaConfirmada).toBeTrue();
    expect(component.state.estado).toBe('SOLICITANDO_CONTRASENA');
  });

  it('cancela desde la advertencia y limpia selección e intentos', () => {
    prepararResultados(true);
    component.seleccionarParaArchivar(archivado);
    component.confirmarSeleccion();
    component.cancelar();
    expect(component.state.estado).toBe('CANCELADO');
    expect(component.state.pacienteArchivado).toBeUndefined();
    expect(component.state.intentosRestantes).toBe(3);
  });

  it('muestra un input password integrado y permite mostrar u ocultar', () => {
    prepararPassword(false);
    component.view = 'password';
    fixture.detectChanges();
    const input: HTMLInputElement = fixture.nativeElement.querySelector('input[aria-label="Contraseña para confirmar el archivado"]');
    expect(input.type).toBe('password');
    component.alternarPassword();
    fixture.detectChanges();
    expect(input.type).toBe('text');
  });

  it('no agrega la contraseña al historial y la limpia al enviar', () => {
    prepararPassword(false);
    const textos: string[] = [];
    component.mensajeConversacional.subscribe(evento => textos.push(evento.texto));
    service.archivar.and.returnValue(of({ archivado: true, resultado: 'PACIENTE_ARCHIVADO', mensaje: 'OK', idAuditoria: 1 }));
    component.password = 'secreto-no-historial';
    component.confirmarArchivado();
    expect(component.password).toBe('');
    expect(textos.join(' ')).not.toContain('secreto-no-historial');
    expect(service.archivar.calls.mostRecent().args[1].origen).toBe('CHATBOT');
    expect((service.archivar.calls.mostRecent().args[1] as any).cargo).toBeUndefined();
    expect((service.archivar.calls.mostRecent().args[1] as any).idUsuario).toBeUndefined();
  });

  it('limita a tres contraseñas incorrectas y un flujo nuevo inicia con tres', () => {
    prepararPassword(false);
    service.archivar.and.returnValue(throwError(() => new HttpErrorResponse({ status: 401, error: { resultado: 'CONTRASENA_INCORRECTA' } })));
    for (const restantes of [2, 1]) {
      component.password = 'incorrecta';
      component.confirmarArchivado();
      expect(component.state.intentosRestantes).toBe(restantes);
      expect(component.state.estado).toBe('SOLICITANDO_CONTRASENA');
    }
    component.password = 'incorrecta';
    component.confirmarArchivado();
    expect(component.state.estado).toBe('CANCELADO');
    expect(component.state.intentosRestantes).toBe(3);
    expect(crearGestionDuplicadosState().intentosRestantes).toBe(3);
  });

  it('actualiza el listado y muestra resumen tras archivar correctamente', () => {
    prepararPassword(false);
    service.archivar.and.returnValue(of({ archivado: true, resultado: 'PACIENTE_ARCHIVADO', mensaje: 'OK', idAuditoria: 5 }));
    component.password = 'correcta';
    component.confirmarArchivado();
    component.view = 'success';
    fixture.detectChanges();
    expect(refresh.solicitarActualizacion).toHaveBeenCalledTimes(1);
    expect(fixture.nativeElement.textContent).toContain('Paciente archivado correctamente');
    expect(fixture.nativeElement.textContent).toContain('Auditoría N.° 5');
  });

  it('evita doble envío mientras el archivado está en curso', () => {
    prepararPassword(false);
    const pendiente = new Subject<any>();
    service.archivar.and.returnValue(pendiente.asObservable());
    component.password = 'correcta';
    component.confirmarArchivado();
    component.password = 'correcta';
    component.confirmarArchivado();
    expect(service.archivar).toHaveBeenCalledTimes(1);
    pendiente.complete();
  });

  it('limpia contraseña al cancelar, minimizar mediante API pública y destruir', () => {
    prepararPassword(false);
    component.password = 'temporal';
    component.limpiarPassword();
    expect(component.password).toBe('');
    component.password = 'otra';
    component.cancelar();
    expect(component.password).toBe('');
    component.password = 'ultima';
    fixture.destroy();
    expect(component.password).toBe('');
  });

  it('ante conflicto 409 reinicia la consulta de duplicados', () => {
    prepararPassword(false);
    service.archivar.and.returnValue(throwError(() => new HttpErrorResponse({ status: 409, error: { resultado: 'CONFLICTO_VERSION' } })));
    component.password = 'correcta';
    component.confirmarArchivado();
    expect(component.state.estado).toBe('SOLICITANDO_DNI');
    expect(component.state.analisis).toBeUndefined();
  });

  function analisis(requiereRevision: boolean): PacienteDuplicadoAnalisisResponse {
    return {
      dni: '12345678', cantidadPacientesActivos: 2, esDuplicado: true, pacientes: [principal, archivado],
      idPacienteRecomendado: 10, razonesRecomendacion: ['Tiene 2 consultas registradas'],
      permitirArchivadoSimple: !requiereRevision, requiereRevision,
      resultado: requiereRevision ? 'REQUIERE_REVISION_O_FUSION' : 'DUPLICADOS_ENCONTRADOS', mensaje: 'Se encontraron dos pacientes.'
    };
  }

  function prepararResultados(requiereRevision: boolean): void {
    component.state.analisis = analisis(requiereRevision);
    component.state.estado = 'MOSTRANDO_RESULTADOS';
  }

  function prepararPassword(requiereRevision: boolean): void {
    prepararResultados(requiereRevision);
    component.seleccionarParaArchivar(archivado);
    if (requiereRevision) {
      component.confirmarSeleccion();
      component.confirmarRevision();
    } else {
      component.confirmarSeleccion();
    }
  }
});
