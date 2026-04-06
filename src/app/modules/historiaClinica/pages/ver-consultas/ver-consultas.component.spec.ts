import { ComponentFixture, TestBed } from '@angular/core/testing';

import { VerConsultasComponent } from './ver-consultas.component';

describe('VerConsultasComponent', () => {
  let component: VerConsultasComponent;
  let fixture: ComponentFixture<VerConsultasComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VerConsultasComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(VerConsultasComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
