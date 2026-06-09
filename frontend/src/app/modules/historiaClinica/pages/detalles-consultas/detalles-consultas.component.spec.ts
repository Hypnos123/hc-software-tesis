import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DetallesConsultasComponent } from './detalles-consultas.component';

describe('DetallesConsultasComponent', () => {
  let component: DetallesConsultasComponent;
  let fixture: ComponentFixture<DetallesConsultasComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DetallesConsultasComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(DetallesConsultasComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
