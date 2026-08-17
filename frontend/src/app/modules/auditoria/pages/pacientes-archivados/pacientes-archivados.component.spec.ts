import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PacientesArchivadosComponent } from './pacientes-archivados.component';

describe('PacientesArchivadosComponent', () => {
  let fixture: ComponentFixture<PacientesArchivadosComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [PacientesArchivadosComponent] }).compileComponents();
    fixture = TestBed.createComponent(PacientesArchivadosComponent);
    fixture.detectChanges();
  });

  it('muestra la estructura vacía de la siguiente fase', () => {
    expect(fixture.nativeElement.textContent).toContain('No hay datos cargados todavía');
    expect(fixture.nativeElement.querySelectorAll('th').length).toBe(8);
  });
});
