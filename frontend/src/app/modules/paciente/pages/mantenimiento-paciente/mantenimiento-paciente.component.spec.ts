import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MantenimientoPacienteComponent } from './mantenimiento-paciente.component';

describe('MantenimientoPacienteComponent', () => {
  let component: MantenimientoPacienteComponent;
  let fixture: ComponentFixture<MantenimientoPacienteComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MantenimientoPacienteComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(MantenimientoPacienteComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
