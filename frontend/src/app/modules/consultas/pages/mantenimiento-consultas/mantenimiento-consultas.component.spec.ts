import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MantenimientoConsultasComponent } from './mantenimiento-consultas.component';

describe('MantenimientoConsultasComponent', () => {
  let component: MantenimientoConsultasComponent;
  let fixture: ComponentFixture<MantenimientoConsultasComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MantenimientoConsultasComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(MantenimientoConsultasComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
