import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FusionesHistoriasComponent } from './fusiones-historias.component';

describe('FusionesHistoriasComponent', () => {
  let fixture: ComponentFixture<FusionesHistoriasComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [FusionesHistoriasComponent] }).compileComponents();
    fixture = TestBed.createComponent(FusionesHistoriasComponent);
    fixture.detectChanges();
  });

  it('muestra la nota irreversible y la estructura futura', () => {
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('no disponen de una acción de restauración');
    expect(text).toContain('El historial de fusiones se implementará en una fase posterior');
    expect(fixture.nativeElement.querySelectorAll('th').length).toBe(10);
  });
});
