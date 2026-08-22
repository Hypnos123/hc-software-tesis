import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';

import { AuthService } from '@app/auth/services/auth.service';
import { HistoriaClinicaService } from '../../services/consultas.service';
import { ConsultasHistoriaClinicaComponent } from './ver-consultas.component';

describe('ConsultasHistoriaClinicaComponent', () => {
  let component: ConsultasHistoriaClinicaComponent;
  let fixture: ComponentFixture<ConsultasHistoriaClinicaComponent>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    await TestBed.configureTestingModule({
      imports: [ConsultasHistoriaClinicaComponent],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: 5 }) } } },
        { provide: Router, useValue: router },
        { provide: HistoriaClinicaService, useValue: {
          getById: () => of({ apellidos: 'Pérez', nombres: 'Ana' }),
          getConsultasByHistoria: () => of([{ idConsulta: 12, estado: 'ATENDIDO' }])
        } },
        { provide: AuthService, useValue: {
          puedeCrearConsultas: () => false,
          puedeVisualizarConsultas: () => true
        } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ConsultasHistoriaClinicaComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('permite al doctor abrir una consulta existente sin habilitar su creación', () => {
    expect(component.puedeAgregarConsulta).toBeFalse();
    expect(component.puedeVisualizarConsulta).toBeTrue();

    component.verConsulta({ idConsulta: 12 } as any);

    expect(router.navigate).toHaveBeenCalledWith(
      ['/historiaClinica/ver-consultas', 5, 'ver'],
      { queryParams: { idConsulta: 12 } }
    );
    expect(fixture.nativeElement.textContent).not.toContain('Agregar Consulta');
  });
});
