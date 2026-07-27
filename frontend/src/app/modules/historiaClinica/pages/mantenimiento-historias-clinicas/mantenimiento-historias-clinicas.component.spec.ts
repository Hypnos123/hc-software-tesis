import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';

import { MensajesSwalService } from '@app/shared/services/mensajes-swal.service';
import { HistoriaClinicaService } from '../../services/consultas.service';
import { MantenimientoHistoriasClinicasComponent } from './mantenimiento-historias-clinicas.component';

describe('MantenimientoHistoriasClinicasComponent', () => {
  let component: MantenimientoHistoriasClinicasComponent;
  let fixture: ComponentFixture<MantenimientoHistoriasClinicasComponent>;
  let modoRuta: 'nuevo' | 'ver' | 'editar';
  let idRuta: string | null;
  let historiaService: jasmine.SpyObj<HistoriaClinicaService>;

  const historiaExistente = {
    idHistoriaClinica: 25,
    fechaIngreso: '2026-07-01',
    fechaNacimiento: '1996-01-01',
    apellidos: 'Pérez Díaz',
    nombres: 'Ana María',
    estadoCivil: 'SOLTERO',
    edad: 30,
    numDocumento: '12345678',
    enfermedadesPrevias: 'Asma',
    cirugiasPrevias: 'Ninguna',
    alergiaMedicamentos: 'Penicilina'
  };

  beforeEach(async () => {
    modoRuta = 'nuevo';
    idRuta = null;
    historiaService = jasmine.createSpyObj<HistoriaClinicaService>('HistoriaClinicaService', [
      'getById',
      'insert',
      'update',
      'buscarPacientesPorNombre',
      'buscarPacientesPorDni',
      'getAntecedentesByPaciente'
    ]);
    historiaService.getById.and.returnValue(of(historiaExistente));
    historiaService.insert.and.returnValue(of({ idGenerado: 101, mensaje: 'Registro guardado correctamente.' }));

    const mensajes = jasmine.createSpyObj<MensajesSwalService>('MensajesSwalService', [
      'mensajeAdvertencia', 'mensajePregunta', 'mensajeExito', 'mensajeError'
    ]);
    mensajes.mensajePregunta.and.returnValue(Promise.resolve({ isConfirmed: true } as any));

    await TestBed.configureTestingModule({
      imports: [MantenimientoHistoriasClinicasComponent],
      providers: [
        { provide: HistoriaClinicaService, useValue: historiaService },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: (parametro: string) => convertToParamMap({ modo: modoRuta, id: idRuta }).get(parametro)
              }
            }
          }
        },
        { provide: Router, useValue: jasmine.createSpyObj<Router>('Router', ['navigate']) },
        {
          provide: MensajesSwalService,
          useValue: mensajes
        }
      ]
    }).compileComponents();

    crearComponente();
  });

  function crearComponente(): void {
    fixture = TestBed.createComponent(MantenimientoHistoriasClinicasComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('debe mostrar una captura manual sin buscadores ni sugerencias en modo nuevo', () => {
    const elemento: HTMLElement = fixture.nativeElement;

    expect(elemento.textContent).not.toContain('Seleccionar Paciente');
    expect(elemento.querySelector('[formControlName="nombrePacienteSel"]')).toBeNull();
    expect(elemento.querySelector('[formControlName="dniSel"]')).toBeNull();
    expect(elemento.querySelectorAll('.autocomplete-list').length).toBe(0);
    expect(elemento.querySelectorAll('[formControlName="dni"]').length).toBe(1);
    expect(historiaService.buscarPacientesPorNombre).not.toHaveBeenCalled();
    expect(historiaService.buscarPacientesPorDni).not.toHaveBeenCalled();
    expect(historiaService.getAntecedentesByPaciente).not.toHaveBeenCalled();
  });

  it('debe habilitar los campos manuales y mantener deshabilitado el ID', () => {
    const controlesManuales = [
      'fechaIngreso', 'fechaNacimiento', 'apellidos', 'nombres', 'estadoCivil', 'dni',
      'enfPrevias', 'cirugiasPrevias', 'alergiasMedicamentos'
    ];

    controlesManuales.forEach(nombre => expect(component.frm.get(nombre)?.enabled).toBeTrue());
    expect(component.frm.get('idHistoriaClinica')?.disabled).toBeTrue();
    expect(component.frm.get('edad')?.disabled).toBeTrue();
    expect(fixture.nativeElement.querySelector('[formControlName="idHistoriaClinica"]')).toBeNull();
  });

  it('debe validar DNI, nombres y apellidos requeridos sin aceptar espacios', () => {
    component.frm.patchValue({ dni: '12A', nombres: '   ', apellidos: '' });
    component.frm.get('dni')?.markAsTouched();
    component.frm.get('nombres')?.markAsTouched();
    component.frm.get('apellidos')?.markAsTouched();
    fixture.detectChanges();

    expect(component.frm.get('dni')?.hasError('pattern')).toBeTrue();
    expect(component.frm.get('nombres')?.hasError('soloEspacios')).toBeTrue();
    expect(component.frm.get('apellidos')?.hasError('required')).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('El DNI debe contener exactamente ocho dígitos.');
    expect(fixture.nativeElement.textContent).toContain('Los nombres son obligatorios');
    expect(fixture.nativeElement.textContent).toContain('Los apellidos son obligatorios');
  });

  it('debe habilitar el guardado cuando el formulario manual es válido', () => {
    const botonGuardar: HTMLButtonElement = fixture.nativeElement.querySelector('.footer-actions button[title]');

    expect(botonGuardar.disabled).toBeTrue();

    component.frm.patchValue({
      fechaIngreso: new Date(2026, 6, 26),
      fechaNacimiento: new Date(1996, 0, 1),
      apellidos: 'Pérez Díaz',
      nombres: 'Ana María',
      estadoCivil: 'SOLTERO',
      edad: component.calcularEdad(new Date(1996, 0, 1)),
      dni: '12345678'
    });
    fixture.detectChanges();

    expect(botonGuardar.disabled).toBeFalse();
  });

  it('debe confirmar y enviar el contrato manual sin ID de historia', fakeAsync(() => {
    component.frm.patchValue({
      fechaIngreso: new Date(2026, 6, 26),
      fechaNacimiento: new Date(1996, 0, 1),
      apellidos: ' Pérez Díaz ',
      nombres: ' Ana María ',
      estadoCivil: 'SOLTERO',
      edad: component.calcularEdad(new Date(1996, 0, 1)),
      dni: '12345678',
      enfPrevias: ' Asma ',
      cirugiasPrevias: 'Ninguna',
      alergiasMedicamentos: 'Penicilina'
    });

    component.guardar();
    tick();

    const request = historiaService.insert.calls.mostRecent().args[0] as any;
    expect(request).toEqual(jasmine.objectContaining({
      fechaIngreso: '2026-07-26', fechaNacimiento: '1996-01-01',
      apellidos: 'Pérez Díaz', nombres: 'Ana María', dni: '12345678',
      enfermedadesPrevias: 'Asma', cirugiasPrevias: 'Ninguna', alergiaMedicamentos: 'Penicilina'
    }));
    expect(request.edad).toBeUndefined();
    expect(request.idHistoriaClinica).toBeUndefined();
    expect(request.idPaciente).toBeUndefined();
  }));

  it('debe recalcular la edad y rechazar una fecha de nacimiento futura', () => {
    component.frm.get('fechaNacimiento')?.setValue(new Date(1996, 0, 1));
    expect(component.frm.get('edad')?.value).toBe(component.calcularEdad(new Date(1996, 0, 1)));

    const futura = new Date();
    futura.setDate(futura.getDate() + 1);
    component.frm.get('fechaNacimiento')?.setValue(futura);
    component.frm.get('fechaNacimiento')?.markAsTouched();
    fixture.detectChanges();

    expect(component.frm.get('fechaNacimiento')?.hasError('fechaFutura')).toBeTrue();
    expect(component.frm.get('edad')?.value).toBeUndefined();
    expect(fixture.nativeElement.textContent).toContain('La fecha de nacimiento no puede ser futura.');
  });

  it('debe calcular la edad correctamente antes y después del cumpleaños', () => {
    const hoy = new Date();
    const nacimientoCumplido = new Date(hoy.getFullYear() - 20, hoy.getMonth(), hoy.getDate());
    const nacimientoPorCumplir = new Date(hoy.getFullYear() - 20, hoy.getMonth(), hoy.getDate());
    nacimientoPorCumplir.setDate(nacimientoPorCumplir.getDate() + 1);

    expect(component.calcularEdad(nacimientoCumplido)).toBe(20);
    expect(component.calcularEdad(nacimientoPorCumplir)).toBe(19);
  });

  it('debe cargar una historia existente en modo editar sin habilitar sus datos', () => {
    fixture.destroy();
    modoRuta = 'editar';
    idRuta = '25';
    crearComponente();

    expect(historiaService.getById).toHaveBeenCalledWith(25);
    expect(component.historiaCargada).toBeTrue();
    expect(component.frm.get('nombres')?.value).toBe('Ana María');
    expect(component.frm.get('fechaNacimiento')?.value).toEqual(new Date(1996, 0, 1));
    expect(component.frm.get('nombres')?.disabled).toBeTrue();
    expect(component.frm.get('idHistoriaClinica')?.value).toBe(25);
  });

  it('debe mantener deshabilitado todo el formulario en modo ver', () => {
    fixture.destroy();
    modoRuta = 'ver';
    idRuta = '25';
    crearComponente();

    Object.values(component.frm.controls).forEach(control => expect(control.disabled).toBeTrue());
    expect(fixture.nativeElement.querySelector('.footer-actions button[title]')).toBeNull();
  });
});
