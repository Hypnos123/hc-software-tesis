import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { AuditoriaAdminService } from '../../services/auditoria-admin.service';
import { FusionHistoriaAuditoriaDetalle, FusionHistoriaAuditoriaResumen } from '../../models/fusion-historia-auditoria';
import { FusionesHistoriasComponent } from './fusiones-historias.component';

describe('FusionesHistoriasComponent', () => {
  let fixture: ComponentFixture<FusionesHistoriasComponent>;
  let component: FusionesHistoriasComponent;
  let service: jasmine.SpyObj<AuditoriaAdminService>;
  const row: FusionHistoriaAuditoriaResumen = { idAuditoria: 3, idPaciente: 4, nombrePaciente: 'Ana Paciente',
    dni: '12345678', idHistoriaPrincipal: 19, idHistoriaEliminada: 16, consultasTransferidas: 1,
    fecha: '2026-08-14T10:00:00', usuarioResponsable: 'admin', resultado: 'HISTORIAS_FUSIONADAS' };

  beforeEach(async () => {
    service = jasmine.createSpyObj('AuditoriaAdminService', ['listarFusionesHistorias', 'obtenerFusionHistoria']);
    service.listarFusionesHistorias.and.returnValue(of({ content: [row], page: 0, size: 10, totalElements: 1, totalPages: 1 }));
    await TestBed.configureTestingModule({ imports: [FusionesHistoriasComponent],
      providers: [{ provide: AuditoriaAdminService, useValue: service }] }).compileComponents();
    fixture = TestBed.createComponent(FusionesHistoriasComponent); component = fixture.componentInstance;
  });

  it('carga inicialmente y muestra tabla, resultado y aviso irreversible sin restauración', () => {
    fixture.detectChanges();
    expect(service.listarFusionesHistorias).toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('HISTORIAS_FUSIONADAS');
    expect(fixture.nativeElement.textContent).toContain('La historia clínica fusionada no puede restaurarse');
    expect(fixture.nativeElement.textContent).not.toContain('Restaurar');
  });

  it('mantiene carga mientras la petición está pendiente', () => {
    const pending = new Subject<any>(); service.listarFusionesHistorias.and.returnValue(pending);
    fixture.detectChanges(); expect(component.loading).toBeTrue();
    pending.next({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 }); pending.complete();
    expect(component.loading).toBeFalse();
  });

  it('envía filtros y reinicia la página', () => {
    fixture.detectChanges(); service.listarFusionesHistorias.calls.reset(); component.page = 2;
    component.searchValue = 'Ana';
    component.idHistoriaPrincipal = 19; component.idHistoriaEliminada = 16;
    component.desde = '2026-08-01'; component.hasta = '2026-08-31'; component.applyFilters();
    expect(service.listarFusionesHistorias).toHaveBeenCalledWith(jasmine.objectContaining({ page: 0, search: 'Ana',
      idHistoriaPrincipal: 19, idHistoriaEliminada: 16,
      desde: '2026-08-01T00:00:00', hasta: '2026-08-31T23:59:59' }));
    const filtros = service.listarFusionesHistorias.calls.mostRecent().args[0];
    expect(filtros.dni).toBeUndefined(); expect(filtros.idPaciente).toBeUndefined();
  });

  it('solicita la página elegida', () => {
    fixture.detectChanges(); service.listarFusionesHistorias.calls.reset(); component.onLazyLoad({ first: 20, rows: 10 });
    expect(service.listarFusionesHistorias).toHaveBeenCalledWith(jasmine.objectContaining({ page: 2, size: 10 }));
  });

  it('distingue vacío general y sin resultados', () => {
    service.listarFusionesHistorias.and.returnValue(of({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 }));
    fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('No se encontraron fusiones registradas');
    component.searchValue = '00000000'; component.applyFilters(); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No se encontraron resultados con los filtros aplicados');
  });

  it('no muestra filtros separados de DNI ni ID paciente', () => {
    fixture.detectChanges();
    const filtros = fixture.nativeElement.querySelector('form');
    expect(filtros.querySelector('input[name="dni"]')).toBeNull();
    expect(filtros.querySelector('input[name="idPaciente"]')).toBeNull();
    expect(filtros.querySelector('input[name="search"]').placeholder).toBe('Paciente, DNI o usuario');
  });

  it('muestra error y permite reintentar', () => {
    service.listarFusionesHistorias.and.returnValue(throwError(() => ({ error: { mensaje: 'Error de historial' } })));
    fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('Error de historial');
    service.listarFusionesHistorias.and.returnValue(of({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 }));
    component.load(); expect(service.listarFusionesHistorias).toHaveBeenCalledTimes(2);
  });

  it('abre el detalle con narrativa, conteos y aviso irreversible', () => {
    const detail: FusionHistoriaAuditoriaDetalle = { ...row, origen: 'CHATBOT', motivo: 'DUPLICIDAD',
      consultasAntesPrincipal: 2, consultasAntesSecundaria: 1, consultasDespuesPrincipal: 3,
      idUsuario: 7, idEmpleado: 6, empleadoResponsable: 'Ada Admin', cargo: 'ADMINISTRADOR',
      explicacion: 'La HC 16 fue fusionada en la HC 19. Se conservó la HC 19 y se transfirió 1 consulta desde la HC 16.' };
    service.obtenerFusionHistoria.and.returnValue(of(detail)); fixture.detectChanges(); component.openDetail(3); fixture.detectChanges();
    expect(service.obtenerFusionHistoria).toHaveBeenCalledWith(3);
    expect(component.detailVisible).toBeTrue(); expect(fixture.nativeElement.textContent).toContain(detail.explicacion);
    expect(fixture.nativeElement.textContent).toContain('Esta operación es histórica e irreversible');
    expect(fixture.nativeElement.textContent).not.toContain('Restaurar');
  });
});
