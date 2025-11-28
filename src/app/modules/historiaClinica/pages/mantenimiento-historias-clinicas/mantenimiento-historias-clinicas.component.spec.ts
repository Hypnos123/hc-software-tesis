import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MantenimientoHistoriasClinicasComponent } from './mantenimiento-historias-clinicas.component';

describe('MantenimientoHistoriasClinicasComponent', () => {
  let component: MantenimientoHistoriasClinicasComponent;
  let fixture: ComponentFixture<MantenimientoHistoriasClinicasComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MantenimientoHistoriasClinicasComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(MantenimientoHistoriasClinicasComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
