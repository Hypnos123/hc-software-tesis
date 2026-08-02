import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MantenimientoPacienteComponent } from './mantenimiento-paciente.component';
import { of, throwError } from 'rxjs';
import Swal from 'sweetalert2';

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

  it('should hide the DNI assistant by default', () => {
    const assistant = fixture.nativeElement.querySelector('.asistente-dni');

    expect(component.mostrarAsistenteDni).toBeFalse();
    expect(assistant).toBeNull();
  });

  it('no debe mostrar éxito ni navegar cuando falla el guardado de antecedentes', () => {
    const pacienteService = jasmine.createSpyObj('PacienteService', ['insert']);
    const antecedentesService = jasmine.createSpyObj('AntecedentesService', ['insert']);
    const router = jasmine.createSpyObj('Router', ['navigateByUrl']);
    pacienteService.insert.and.returnValue(of({ idGenerado: 20, mensaje: 'OK' }));
    antecedentesService.insert.and.returnValue(throwError(() => new Error('fallo backend')));
    (component as any).pacienteService = pacienteService;
    (component as any).antecedentesService = antecedentesService;
    (component as any).router = router;
    const alerta = spyOn(Swal, 'fire');

    component.registrarPaciente();

    expect(router.navigateByUrl).not.toHaveBeenCalled();
    expect(alerta).toHaveBeenCalledWith(jasmine.objectContaining({ icon: 'error' }));
    expect(alerta).not.toHaveBeenCalledWith(jasmine.objectContaining({ icon: 'success' }));
  });

  it('no debe navegar cuando el backend devuelve una respuesta funcional de error', () => {
    const pacienteService = jasmine.createSpyObj('PacienteService', ['update']);
    const antecedentesService = jasmine.createSpyObj('AntecedentesService', ['insert']);
    const router = jasmine.createSpyObj('Router', ['navigateByUrl']);
    pacienteService.update.and.returnValue(of({ mensaje: '¡ERROR AL REGISTRAR LA INFORMACION!' }));
    (component as any).pacienteService = pacienteService;
    (component as any).antecedentesService = antecedentesService;
    (component as any).router = router;
    component.pacienteId = 20;
    const alerta = spyOn(Swal, 'fire');

    component.actualizarPaciente();

    expect(antecedentesService.insert).not.toHaveBeenCalled();
    expect(router.navigateByUrl).not.toHaveBeenCalled();
    expect(alerta).toHaveBeenCalledWith(jasmine.objectContaining({ icon: 'error' }));
  });
});
