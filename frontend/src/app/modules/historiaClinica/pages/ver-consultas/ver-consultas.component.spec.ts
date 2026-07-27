import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConsultasHistoriaClinicaComponent } from './ver-consultas.component';

describe('ConsultasHistoriaClinicaComponent', () => {
  let component: ConsultasHistoriaClinicaComponent;
  let fixture: ComponentFixture<ConsultasHistoriaClinicaComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConsultasHistoriaClinicaComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(ConsultasHistoriaClinicaComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
