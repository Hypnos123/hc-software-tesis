import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import {
  AnalisisHistoriasClinicasDuplicadas,
  crearGestionHistoriasDuplicadasState,
  DeteccionHistoriasClinicasDuplicadasResponse,
  GestionHistoriasDuplicadasEvento,
  HistoriaClinicaAnalisisDetallado
} from '../../models/historia-clinica-duplicada-chat';
import { HistoriaClinicaDuplicadaChatService } from '../../services/historia-clinica-duplicada-chat.service';
import { GestionHistoriasDuplicadasChatComponent } from './gestion-historias-duplicadas-chat.component';

describe('GestionHistoriasDuplicadasChatComponent', () => {
  let fixture: ComponentFixture<GestionHistoriasDuplicadasChatComponent>;
  let component: GestionHistoriasDuplicadasChatComponent;
  let service: jasmine.SpyObj<HistoriaClinicaDuplicadaChatService>;
  const deteccion: DeteccionHistoriasClinicasDuplicadasResponse = {
    hayDuplicados: true, totalGrupos: 1, mensaje: 'Se encontró un grupo.',
    duplicados: [{ tipo: 'dni', valorCoincidente: '74281635', cantidad: 2, historiasClinicas: [
      { idHistoriaClinica: 7, idPaciente: 12, dni: '74281635', nombreCompleto: 'Andrea Quispe', fechaCreacion: '2026-01-01T10:00:00', cantidadConsultas: 0, estado: 'ACTIVA' },
      { idHistoriaClinica: 8, idPaciente: 12, dni: '74281635', nombreCompleto: 'Andrea Quispe', fechaCreacion: '2026-02-01T10:00:00', cantidadConsultas: 0, estado: 'ACTIVA' }
    ] }]
  };

  beforeEach(async () => {
    service = jasmine.createSpyObj('HistoriaClinicaDuplicadaChatService', ['detectar', 'analizar', 'fusionar']);
    service.detectar.and.returnValue(of(deteccion));
    await TestBed.configureTestingModule({
      imports: [GestionHistoriasDuplicadasChatComponent],
      providers: [{ provide: HistoriaClinicaDuplicadaChatService, useValue: service }]
    }).compileComponents();
    fixture = TestBed.createComponent(GestionHistoriasDuplicadasChatComponent);
    component = fixture.componentInstance;
    component.state = crearGestionHistoriasDuplicadasState();
    component.view = 'loading';
    component.active = true;
  });

  it('detecta, muestra el grupo y permite seleccionar todas sus historias', () => {
    const eventos: GestionHistoriasDuplicadasEvento[] = [];
    component.mensajeConversacional.subscribe(evento => eventos.push(evento));
    fixture.detectChanges();

    expect(component.state.estado).toBe('MOSTRANDO_HISTORIAS');
    expect(eventos.at(-1)?.vistaSiguiente).toBe('groups');
    component.seleccionarGrupo(deteccion.duplicados[0]);
    expect(component.state.idsSeleccionados).toEqual([7, 8]);
    expect(component.todasSeleccionadas).toBeTrue();
  });

  it('muestra dos historias vacías, la recomendación, el motivo y la aptitud futura', () => {
    prepararComparacion(analisis());

    expect(fixture.nativeElement.textContent).toContain('Historia clínica 7');
    expect(fixture.nativeElement.textContent).toContain('Historia clínica 8');
    expect(fixture.nativeElement.textContent).toContain('Recomendada para conservar');
    expect(fixture.nativeElement.textContent).toContain('Todos los criterios están empatados');
    expect(component.state.analisis?.futuraFusionPermitida).toBeTrue();
  });

  it('respeta la secuencia: emite mensajes antes de solicitar la vista comparativa', () => {
    fixture.detectChanges();
    component.seleccionarGrupo(deteccion.duplicados[0]);
    service.analizar.and.returnValue(of(analisis()));
    const eventos: GestionHistoriasDuplicadasEvento[] = [];
    component.mensajeConversacional.subscribe(evento => eventos.push(evento));

    component.analizar();

    const eventosBot = eventos.filter(evento => evento.remitente === 'bot');
    expect(eventosBot.length).toBe(3);
    expect(eventosBot[0].vistaSiguiente).toBeUndefined();
    expect(eventosBot[1].vistaSiguiente).toBeUndefined();
    expect(eventosBot[2].vistaSiguiente).toBe('comparison');
  });

  it('muestra consultas exclusivas y posibles coincidencias como advertencia informativa', () => {
    const respuesta = analisis();
    respuesta.historiasComparadas[1].cantidadConsultas = 1;
    respuesta.historiasComparadas[1].cantidadConsultasExclusivas = 1;
    respuesta.posiblesCoincidencias = [{ clasificacion: 'POSIBLE_COINCIDENCIA', idConsultaA: 1, idHistoriaClinicaA: 7,
      idConsultaB: 2, idHistoriaClinicaB: 8, criteriosCoincidentes: ['MISMA_FECHA'], advertencia: 'Revisión' }];
    prepararComparacion(respuesta);

    const texto = fixture.nativeElement.textContent;
    expect(texto).toContain('Consultas exclusivas');
    expect(texto).toContain('posibles coincidencias');
    expect(texto).toContain('requieren revisión');
  });

  it('bloquea visualmente pacientes diferentes y muestra el motivo', () => {
    const respuesta = analisis();
    respuesta.tipoDuplicidad = 'MISMO_DNI_DIFERENTE_PACIENTE';
    respuesta.futuraFusionPermitida = false;
    respuesta.motivoBloqueo = 'Primero deben gestionarse los pacientes duplicados.';
    prepararComparacion(respuesta);

    expect(fixture.nativeElement.textContent).toContain('No aptas para fusión automática');
    expect(fixture.nativeElement.textContent).toContain('paciente diferentes');
    expect(fixture.nativeElement.textContent).toContain('pacientes duplicados');
  });

  it('hace visibles las advertencias de integridad', () => {
    const respuesta = analisis();
    respuesta.futuraFusionPermitida = false;
    respuesta.advertenciasIntegridad = ['La consulta ID 20 pertenece a otro paciente.'];
    prepararComparacion(respuesta);

    expect(fixture.nativeElement.textContent).toContain('Advertencias de integridad');
    expect(fixture.nativeElement.textContent).toContain('consulta ID 20');
    expect(fixture.nativeElement.textContent).toContain('no son aptas');
  });

  it('soporta más de dos historias y selección parcial de al menos dos', () => {
    fixture.detectChanges();
    const grupo = structuredClone(deteccion.duplicados[0]);
    grupo.historiasClinicas.push({ ...grupo.historiasClinicas[0], idHistoriaClinica: 9 });
    grupo.cantidad = 3;
    component.seleccionarGrupo(grupo);
    component.cambiarSeleccion(9, { target: { checked: false } } as unknown as Event);

    expect(component.state.idsSeleccionados).toEqual([7, 8]);
    expect(component.todasSeleccionadas).toBeFalse();
  });

  it('cancela solicitudes y limpia selección y análisis', () => {
    const pendiente = new Subject<AnalisisHistoriasClinicasDuplicadas>();
    fixture.detectChanges();
    component.seleccionarGrupo(deteccion.duplicados[0]);
    service.analizar.and.returnValue(pendiente.asObservable());
    component.analizar();

    component.cancelar();

    expect(pendiente.observed).toBeFalse();
    expect(component.state.estado).toBe('CANCELADO');
    expect(component.state.idsSeleccionados).toEqual([]);
    expect(component.state.analisis).toBeUndefined();
  });

  it('no presenta contraseña ni eliminación antes de confirmar la vista previa', () => {
    prepararComparacion(analisis());
    const botones = Array.from(fixture.nativeElement.querySelectorAll('button')).map((boton: unknown) => (boton as HTMLButtonElement).textContent?.trim());

    expect(botones).not.toContain('Eliminar');
    expect(botones).not.toContain('Confirmar eliminación');
    expect(fixture.nativeElement.querySelector('input[type="password"]')).toBeNull();
  });

  it('preselecciona la recomendada y muestra la contraseña solo después de confirmar', () => {
    prepararComparacion(analisis());
    component.continuarConFusion();
    expect(component.state.idHistoriaPrincipal).toBe(7);
    expect(component.state.idHistoriaSecundaria).toBe(8);
    component.mostrarVistaPrevia();
    component.confirmarVistaPrevia();
    component.view = 'password'; fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('input[type="password"]')).not.toBeNull();
  });

  it('limita la contraseña a tres intentos y nunca la emite como mensaje', () => {
    prepararComparacion(analisis()); component.continuarConFusion(); component.mostrarVistaPrevia(); component.confirmarVistaPrevia();
    service.fusionar.and.returnValue(throwError(() => new HttpErrorResponse({ status: 401, error: { resultado: 'CONTRASENA_INCORRECTA' } })));
    const textos: string[] = []; component.mensajeConversacional.subscribe(e => textos.push(e.texto));
    for (let i=0;i<3;i++) { component.password='secreta'; component.fusionar(); }
    expect(component.state.estado).toBe('CANCELADO'); expect(component.password).toBe(''); expect(textos).not.toContain('secreta');
  });

  it('envía snapshot y conserva idPaciente de solo lectura en frontend', () => {
    const respuesta=analisis(); respuesta.historiasComparadas[1].consultasExclusivas=[{idConsulta:20,estado:'ATENDIDO',camposClinicosInformados:1,puntajeRiquezaClinica:3}]; respuesta.historiasComparadas[1].cantidadConsultas=1;
    prepararComparacion(respuesta); component.continuarConFusion(); component.mostrarVistaPrevia(); component.confirmarVistaPrevia();
    service.fusionar.and.returnValue(of({fusionada:true,resultado:'HISTORIAS_FUSIONADAS',mensaje:'OK'})); component.password='clave'; component.fusionar();
    const body=service.fusionar.calls.mostRecent().args[1]; expect(body.idsConsultasEsperadasSecundaria).toEqual([20]); expect(body).not.toEqual(jasmine.objectContaining({idPaciente:12})); expect(component.password).toBe('');
  });

  it('limpiarFlujo cancela y elimina el estado al minimizar o cerrar desde el padre', () => {
    prepararComparacion(analisis());
    component.limpiarFlujo();

    expect(component.state.estado).toBe('CANCELADO');
    expect(component.state.deteccion).toBeUndefined();
    expect(component.state.idsSeleccionados).toEqual([]);
    expect(component.state.analisis).toBeUndefined();
  });

  function prepararComparacion(respuesta: AnalisisHistoriasClinicasDuplicadas): void {
    component.state.estado = 'MOSTRANDO_COMPARACION';
    component.state.deteccion = deteccion;
    component.state.analisis = respuesta;
    component.view = 'comparison';
    fixture.detectChanges();
  }

  function analisis(): AnalisisHistoriasClinicasDuplicadas {
    return {
      tipoDuplicidad: 'MISMO_PACIENTE', idHistoriaClinicaRecomendada: 7,
      motivosRecomendacion: ['Todos los criterios están empatados y tiene el ID menor.'],
      resumenComparativo: 'Comparación completa', futuraFusionPermitida: true,
      tokenAnalisis: 'token',
      posiblesCoincidencias: [], advertenciasIntegridad: [], mensaje: 'No existen consultas para transferir.',
      historiasComparadas: [historia(7, '2026-01-01T10:00:00'), historia(8, '2026-02-01T10:00:00')]
    };
  }

  function historia(id: number, fecha: string): HistoriaClinicaAnalisisDetallado {
    return { idHistoriaClinica: id, idPaciente: 12, dni: '74281635', nombreCompleto: 'Andrea Quispe', fechaCreacion: fecha,
      cantidadConsultas: 0, cantidadConsultasAtendidas: 0, cantidadConsultasPendientes: 0, camposClinicosInformados: 0,
      puntajeRiquezaClinica: 0, cantidadConsultasExclusivas: 0, consultasExclusivas: [] };
  }
});
