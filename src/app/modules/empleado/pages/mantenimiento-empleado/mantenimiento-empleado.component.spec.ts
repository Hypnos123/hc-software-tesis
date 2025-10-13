import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MantenimientoEmpleadoComponent } from './mantenimiento-empleado.component';

describe('MantenimientoEmpleadoComponent', () => {
  let component: MantenimientoEmpleadoComponent;
  let fixture: ComponentFixture<MantenimientoEmpleadoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ MantenimientoEmpleadoComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(MantenimientoEmpleadoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
