import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { PacientesArchivadosComponent } from './pacientes-archivados.component';
import { AuditoriaAdminService } from '../../services/auditoria-admin.service';
import { PacienteArchivadoDetalle, PacienteArchivadoResumen } from '../../models/paciente-archivado-admin';

describe('PacientesArchivadosComponent', () => {
  let fixture: ComponentFixture<PacientesArchivadosComponent>;
  let component: PacientesArchivadosComponent;
  let service: jasmine.SpyObj<AuditoriaAdminService>;
  const row: PacienteArchivadoResumen = { idPaciente: 13, nombreCompleto: 'Ana Archivada', dni: '12345678',
    fechaArchivado: '2026-08-10T12:00:00', usuarioResponsable: 'admin', motivoArchivado: 'DUPLICADO', estadoRegistro: 'ARCHIVADO' };

  beforeEach(async () => {
    service = jasmine.createSpyObj('AuditoriaAdminService', ['listarPacientesArchivados', 'obtenerPacienteArchivado']);
    service.listarPacientesArchivados.and.returnValue(of({ content: [row], page: 0, size: 10, totalElements: 1, totalPages: 1 }));
    await TestBed.configureTestingModule({ imports: [PacientesArchivadosComponent],
      providers: [{ provide: AuditoriaAdminService, useValue: service }] }).compileComponents();
    fixture = TestBed.createComponent(PacientesArchivadosComponent); component = fixture.componentInstance;
  });

  it('carga inicialmente y muestra la tabla con estado ARCHIVADO sin restauración', () => {
    fixture.detectChanges();
    expect(service.listarPacientesArchivados).toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Ana Archivada');
    expect(fixture.nativeElement.textContent).toContain('ARCHIVADO');
    expect(fixture.nativeElement.textContent).not.toContain('Restaurar');
  });

  it('mantiene el indicador de carga mientras la petición está pendiente', () => {
    const pending = new Subject<any>(); service.listarPacientesArchivados.and.returnValue(pending);
    fixture.detectChanges();
    expect(component.loading).toBeTrue();
    pending.next({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 }); pending.complete();
    expect(component.loading).toBeFalse();
  });

  it('envía búsqueda y filtros reiniciando la página', () => {
    fixture.detectChanges(); service.listarPacientesArchivados.calls.reset();
    component.page = 3; component.searchValue = 'Ana'; component.dni = '12345678'; component.idPaciente = 13;
    component.desde = '2026-08-01'; component.hasta = '2026-08-31'; component.applyFilters();
    expect(service.listarPacientesArchivados).toHaveBeenCalledWith(jasmine.objectContaining({ page: 0, search: 'Ana',
      dni: '12345678', idPaciente: 13, desde: '2026-08-01T00:00:00', hasta: '2026-08-31T23:59:59' }));
  });

  it('solicita la página seleccionada por el paginador', () => {
    fixture.detectChanges(); service.listarPacientesArchivados.calls.reset();
    component.onLazyLoad({ first: 20, rows: 10 });
    expect(service.listarPacientesArchivados).toHaveBeenCalledWith(jasmine.objectContaining({ page: 2, size: 10 }));
  });

  it('distingue vacío general y vacío por filtros', () => {
    service.listarPacientesArchivados.and.returnValue(of({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 }));
    fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('No se encontraron pacientes archivados');
    component.searchValue = 'nadie'; component.applyFilters(); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No se encontraron resultados con los filtros aplicados');
  });

  it('muestra el error y permite reintentar', () => {
    service.listarPacientesArchivados.and.returnValue(throwError(() => ({ error: { mensaje: 'Servidor no disponible' } })));
    fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('Servidor no disponible');
    service.listarPacientesArchivados.and.returnValue(of({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 }));
    component.load(); expect(service.listarPacientesArchivados).toHaveBeenCalledTimes(2);
  });

  it('abre el diálogo y carga el detalle', () => {
    const detail = { ...row, nombres: 'Ana', apellidos: 'Archivada', detalleMotivoArchivado: 'Registro repetido',
      requirioRevisionClinica: true, confirmoRevisionClinica: true, cantidadHistoriasClinicas: 1,
      cantidadConsultas: 2, cantidadAntecedentes: 1 } as PacienteArchivadoDetalle;
    service.obtenerPacienteArchivado.and.returnValue(of(detail)); fixture.detectChanges();
    component.openDetail(13); fixture.detectChanges();
    expect(service.obtenerPacienteArchivado).toHaveBeenCalledWith(13);
    expect(component.detailVisible).toBeTrue(); expect(fixture.nativeElement.textContent).toContain('Registro repetido');
    expect(fixture.nativeElement.textContent).toContain('El archivado es lógico');
  });
});
