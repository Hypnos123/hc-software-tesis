import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, Subject } from 'rxjs';
import { PacientesComponent } from './pacientes.component';
import { PacienteService } from '../../services/paciente.service';
import { PacienteListRefreshService } from '../../services/paciente-list-refresh.service';

describe('PacientesComponent', () => {
  let component: PacientesComponent;
  let fixture: ComponentFixture<PacientesComponent>;
  let pacienteService: jasmine.SpyObj<PacienteService>;
  let refresh: Subject<void>;

  beforeEach(async () => {
    pacienteService = jasmine.createSpyObj('PacienteService', ['getAllActivos']);
    pacienteService.getAllActivos.and.returnValue(of([]));
    refresh = new Subject<void>();
    await TestBed.configureTestingModule({
      imports: [PacientesComponent],
      providers: [
        { provide: PacienteService, useValue: pacienteService },
        { provide: Router, useValue: jasmine.createSpyObj('Router', ['navigate']) },
        { provide: PacienteListRefreshService, useValue: { refresh$: refresh.asObservable() } }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(PacientesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe crear y cargar pacientes', () => {
    expect(component).toBeTruthy();
    expect(pacienteService.getAllActivos).toHaveBeenCalledTimes(1);
  });

  it('debe volver a consultar pacientes después de una importación confirmada', () => {
    refresh.next();
    expect(pacienteService.getAllActivos).toHaveBeenCalledTimes(2);
  });
});
