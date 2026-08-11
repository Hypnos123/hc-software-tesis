import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';

import { HistoriaClinicaService } from '@app/modules/historiaClinica/services/consultas.service';
import {
  crearHistoriasClinicasFaltantesState,
  HistoriasClinicasFaltantesChatState,
  HistoriasClinicasFaltantesEvento
} from '../../models/historias-clinicas-faltantes-chat';
import { HistoriasClinicasFaltantesChatComponent } from './historias-clinicas-faltantes-chat.component';

describe('HistoriasClinicasFaltantesChatComponent', () => {
  let component: HistoriasClinicasFaltantesChatComponent;
  let fixture: ComponentFixture<HistoriasClinicasFaltantesChatComponent>;
  let service: jasmine.SpyObj<HistoriaClinicaService>;
  let state: HistoriasClinicasFaltantesChatState;
  const preview = {
    cantidad: 3,
    pacientes: [
      { idPaciente: 1, nombreCompleto: 'Ana Pérez', dniEnmascarado: '******42' },
      { idPaciente: 2, nombreCompleto: 'Luis Soto', dniEnmascarado: '******18' },
      { idPaciente: 3, nombreCompleto: 'Eva Ramos', dniEnmascarado: '******90' }
    ]
  };

  beforeEach(async () => {
    service = jasmine.createSpyObj<HistoriaClinicaService>('HistoriaClinicaService', ['getHistoriasClinicasFaltantes']);
    service.getHistoriasClinicasFaltantes.and.returnValue(of(preview));
    await TestBed.configureTestingModule({
      imports: [HistoriasClinicasFaltantesChatComponent],
      providers: [{ provide: HistoriaClinicaService, useValue: service }]
    }).compileComponents();
    fixture = TestBed.createComponent(HistoriasClinicasFaltantesChatComponent);
    component = fixture.componentInstance;
    state = crearHistoriasClinicasFaltantesState();
    fixture.componentRef.setInput('state', state);
    fixture.componentRef.setInput('view', 'loading');
    fixture.componentRef.setInput('active', true);
  });

  it('realiza un único GET, conserva el preview y muestra solo nombre y DNI enmascarado', () => {
    fixture.detectChanges();
    fixture.componentRef.setInput('view', 'selection');
    fixture.detectChanges();

    expect(service.getHistoriasClinicasFaltantes).toHaveBeenCalledTimes(1);
    expect(state.preview).toEqual(preview);
    const texto = fixture.nativeElement.textContent;
    expect(texto).toContain('Ana Pérez');
    expect(texto).toContain('******42');
    expect(texto).not.toContain('fecha');
    expect(texto).not.toContain('antecedente');
  });

  it('selecciona y deselecciona pacientes sin duplicar ni admitir ids externos', () => {
    fixture.detectChanges();
    component.cambiarSeleccion(1, eventoCheckbox(true));
    component.cambiarSeleccion(1, eventoCheckbox(true));
    component.cambiarSeleccion(999, eventoCheckbox(true));
    expect(state.idsSeleccionados).toEqual([1]);
    expect(component.cantidadSeleccionados).toBe(1);

    component.cambiarSeleccion(1, eventoCheckbox(false));
    expect(state.idsSeleccionados).toEqual([]);
  });

  it('selecciona todos, informa selección parcial y deselecciona todos', () => {
    fixture.detectChanges();
    component.cambiarSeleccion(1, eventoCheckbox(true));
    expect(component.seleccionParcial).toBeTrue();
    expect(component.todosSeleccionados).toBeFalse();

    component.alternarTodos();
    expect(state.idsSeleccionados).toEqual([1, 2, 3]);
    expect(component.todosSeleccionados).toBeTrue();

    component.alternarTodos();
    expect(state.idsSeleccionados).toEqual([]);
  });

  it('bloquea continuar sin selección y confirma una copia exacta sin ejecutar POST', () => {
    const eventos: HistoriasClinicasFaltantesEvento[] = [];
    component.mensajeConversacional.subscribe(evento => eventos.push(evento));
    fixture.detectChanges();
    component.continuar();
    expect(state.estado).toBe('SELECCIONANDO');

    component.cambiarSeleccion(1, eventoCheckbox(true));
    component.cambiarSeleccion(3, eventoCheckbox(true));
    component.continuar();

    expect(state.idsConfirmados).toEqual([1, 3]);
    expect(state.idsConfirmados).not.toBe(state.idsSeleccionados);
    expect(state.estado).toBe('CONFIRMANDO');
    expect(eventos.at(-1)).toEqual(jasmine.objectContaining({ vistaSiguiente: 'confirmation' }));
    expect(service.getHistoriasClinicasFaltantes).toHaveBeenCalledTimes(1);
  });

  it('vuelve a seleccionar conservando exactamente la selección', () => {
    fixture.detectChanges();
    component.cambiarSeleccion(2, eventoCheckbox(true));
    component.continuar();
    component.volverASeleccionar();

    expect(state.estado).toBe('SELECCIONANDO');
    expect(state.idsSeleccionados).toEqual([2]);
    expect(state.idsConfirmados).toEqual([2]);
  });

  it('cancela sin escribir, limpia ids y solicita volver al menú de historias', () => {
    const eventos: HistoriasClinicasFaltantesEvento[] = [];
    component.mensajeConversacional.subscribe(evento => eventos.push(evento));
    fixture.detectChanges();
    component.cambiarSeleccion(1, eventoCheckbox(true));
    component.continuar();
    component.cancelar();

    expect(state.estado).toBe('CANCELADO');
    expect(state.idsSeleccionados).toEqual([]);
    expect(state.idsConfirmados).toEqual([]);
    expect(eventos.at(-1)).toEqual(jasmine.objectContaining({ volverHistorias: true }));
  });

  it('maneja preview vacío y error de consulta sin crear registros', () => {
    service.getHistoriasClinicasFaltantes.and.returnValue(of({ cantidad: 0, pacientes: [] }));
    fixture.detectChanges();
    expect(state.estado).toBe('SIN_CANDIDATOS');

    const segundaFixture = TestBed.createComponent(HistoriasClinicasFaltantesChatComponent);
    const segundoEstado = crearHistoriasClinicasFaltantesState();
    service.getHistoriasClinicasFaltantes.and.returnValue(throwError(() => new Error('fallo')));
    segundaFixture.componentRef.setInput('state', segundoEstado);
    segundaFixture.componentRef.setInput('view', 'loading');
    segundaFixture.componentRef.setInput('active', true);
    segundaFixture.detectChanges();
    expect(segundoEstado.estado).toBe('ERROR');
  });

  it('cancela la solicitud GET al limpiar explícitamente el flujo', () => {
    const solicitud = new Subject<typeof preview>();
    service.getHistoriasClinicasFaltantes.and.returnValue(solicitud);
    fixture.detectChanges();
    expect(solicitud.observed).toBeTrue();

    component.limpiarFlujo();
    expect(solicitud.observed).toBeFalse();
    expect(state.idsSeleccionados).toEqual([]);
  });

  function eventoCheckbox(checked: boolean): Event {
    return { target: { checked } } as unknown as Event;
  }
});
