import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse, HttpHeaders, HttpResponse } from '@angular/common/http';
import { Subject, of, throwError } from 'rxjs';
import { ImportacionPacientesComponent } from './importacion-pacientes.component';
import { PacienteImportacionService } from '../../services/paciente-importacion.service';
import { IPacienteImportacionPrevisualizacion } from '../../models/paciente-importacion';

describe('ImportacionPacientesComponent', () => {
  let fixture: ComponentFixture<ImportacionPacientesComponent>;
  let component: ImportacionPacientesComponent;
  let service: jasmine.SpyObj<PacienteImportacionService>;

  beforeEach(async () => {
    service = jasmine.createSpyObj<PacienteImportacionService>(
      'PacienteImportacionService',
      ['descargarPlantilla', 'obtenerNombreArchivo', 'validarArchivo', 'obtenerPrevisualizacion', 'confirmarImportacion'],
      { nombrePlantillaPredeterminado: 'plantilla-importacion-pacientes-v1.0.xlsx' }
    );
    service.obtenerNombreArchivo.and.returnValue(service.nombrePlantillaPredeterminado);
    await TestBed.configureTestingModule({
      imports: [ImportacionPacientesComponent],
      providers: [{ provide: PacienteImportacionService, useValue: service }]
    }).compileComponents();
    fixture = TestBed.createComponent(ImportacionPacientesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => fixture.destroy());

  it('rechaza archivos que no sean xlsx, vacíos o mayores de 2 MB', () => {
    component.estado = 'PLANTILLA_DESCARGADA';
    seleccionar(new File(['texto'], 'pacientes.csv'));
    expect(component.mensajeError).toContain('.xlsx');

    seleccionar(new File([], 'vacio.xlsx'));
    expect(component.mensajeError).toContain('vacío');

    seleccionar(new File([new Uint8Array(2 * 1024 * 1024 + 1)], 'grande.xlsx'));
    expect(component.mensajeError).toContain('2 MB');
    expect(component.archivoSeleccionado).toBeUndefined();
  });

  it('selecciona un archivo válido y muestra nombre y tamaño', () => {
    component.estado = 'PLANTILLA_DESCARGADA';
    seleccionar(new File(['excel'], 'pacientes.xlsx'));
    fixture.detectChanges();

    expect(component.estado).toBe('ARCHIVO_SELECCIONADO');
    expect(fixture.nativeElement.textContent).toContain('pacientes.xlsx');
    expect(fixture.nativeElement.textContent).toContain('Quitar archivo');
  });

  it('renderiza resumen y filas válidas, duplicadas y con errores', () => {
    service.validarArchivo.and.returnValue(of(preview()));
    component.estado = 'ARCHIVO_SELECCIONADO';
    component.archivoSeleccionado = new File(['excel'], 'pacientes.xlsx');

    component.analizarArchivo();
    fixture.detectChanges();

    const texto = fixture.nativeElement.textContent;
    expect(component.estado).toBe('PREVISUALIZADA');
    expect(texto).toContain('Registros analizados');
    expect(texto).toContain('Listos para registrar');
    expect(texto).toContain('Ana Pérez');
    expect(texto).toContain('DNI DUPLICADO');
    expect(texto).toContain('ERROR');
    expect(texto).toContain('El DNI no es válido');
  });

  it('confirma correctamente y bloquea una segunda confirmación', () => {
    const respuesta = {
      importacionId: 'import-1', estado: 'CONFIRMADA' as const,
      resumen: { filasValidasEnPrevisualizacion: 1, pacientesRegistrados: 1, omitidosPorDniExistente: 0, erroresAlRegistrar: 0 },
      resultados: [{ numeroFila: 2, dni: '01234567', estado: 'REGISTRADO' as const, idPaciente: 25, errores: [] }]
    };
    service.confirmarImportacion.and.returnValue(of(respuesta));
    component.previsualizacion = preview();
    component.estado = 'PREVISUALIZADA';

    component.confirmar();
    component.confirmar();
    fixture.detectChanges();

    expect(service.confirmarImportacion).toHaveBeenCalledTimes(1);
    expect(component.estado).toBe('CONFIRMADA');
    expect(fixture.nativeElement.textContent).toContain('Importación completada');
    expect(fixture.nativeElement.textContent).toContain('Pacientes registrados');
    expect(fixture.nativeElement.textContent).toContain('Registrar otro archivo');
  });

  it('impide doble confirmación mientras la primera solicitud está pendiente', () => {
    const pendiente = new Subject<any>();
    service.confirmarImportacion.and.returnValue(pendiente);
    component.previsualizacion = preview();
    component.estado = 'PREVISUALIZADA';

    component.confirmar();
    component.confirmar();

    expect(component.estado).toBe('CONFIRMANDO');
    expect(service.confirmarImportacion).toHaveBeenCalledTimes(1);
    pendiente.complete();
  });

  it('marca como expirada una previsualización vencida', () => {
    const vencida = preview();
    vencida.expiraEn = new Date(Date.now() - 1000).toISOString();
    service.validarArchivo.and.returnValue(of(vencida));
    component.estado = 'ARCHIVO_SELECCIONADO';
    component.archivoSeleccionado = new File(['excel'], 'pacientes.xlsx');

    component.analizarArchivo();
    fixture.detectChanges();

    expect(component.estado).toBe('EXPIRADA');
    expect(component.puedeConfirmar).toBeFalse();
    expect(fixture.nativeElement.textContent).toContain('previsualización expiró');
  });

  it('muestra el mensaje funcional seguro y traduce errores HTTP conocidos', () => {
    service.validarArchivo.and.returnValue(throwError(() => new HttpErrorResponse({
      status: 413, error: { mensaje: 'El archivo excede el límite autorizado.' }
    })));
    component.estado = 'ARCHIVO_SELECCIONADO';
    component.archivoSeleccionado = new File(['excel'], 'pacientes.xlsx');
    component.analizarArchivo();
    expect(component.mensajeError).toBe('El archivo excede el límite autorizado.');

    service.validarArchivo.and.returnValue(throwError(() => new HttpErrorResponse({ status: 410 })));
    component.estado = 'ARCHIVO_SELECCIONADO';
    component.analizarArchivo();
    expect(component.estado).toBe('EXPIRADA');
    expect(component.mensajeError).toContain('expiró');
  });

  function seleccionar(archivo: File): void {
    component.seleccionarArchivo({ target: { files: [archivo], value: 'x' } } as unknown as Event);
  }

  function preview(): IPacienteImportacionPrevisualizacion {
    const paciente = { apellidos: 'Pérez', nombres: 'Ana', fechaNacimiento: '1990-01-01', estadoCivil: 'SOLTERO', dni: '01234567', sexo: 'F', direccion: 'Lima', distrito: 'Lima', traidoPor: '' };
    const antecedentes = { alimentacion: 'Normal', habitos: 'Ninguno', vivienda: 'Casa', desarrolloPsicomotor: 'Normal', vacunas: 'Completas', educacion: 'S1', enfermedadesPrevias: 'Ninguna', cirugiasPrevias: 'Ninguna', alergiasMedicamentos: 'Ninguna' };
    return {
      importacionId: 'import-1', estado: 'PREVISUALIZADA', expiraEn: new Date(Date.now() + 60000).toISOString(),
      resumen: { registrosAnalizados: 3, validos: 1, conErrores: 1, filasConDniDuplicado: 1, gruposDniDuplicados: 1, dniExistentes: 0, conAdvertencias: 0, filasVaciasIgnoradas: 2 },
      filas: [
        { numeroFila: 2, nombreCompleto: 'Ana Pérez', dni: '01234567', estado: 'VALIDO', paciente, antecedentes, errores: [], advertencias: [] },
        { numeroFila: 3, nombreCompleto: 'Luis Paz', dni: '12345678', estado: 'DNI_DUPLICADO_ARCHIVO', paciente: { ...paciente, dni: '12345678' }, antecedentes, errores: [{ codigo: 'DNI_DUPLICADO_ARCHIVO', mensaje: 'DNI repetido' }], advertencias: [] },
        { numeroFila: 4, nombreCompleto: 'María Sol', dni: 'ABC', estado: 'ERROR_DATOS', paciente: { ...paciente, dni: 'ABC' }, antecedentes, errores: [{ codigo: 'DNI_INVALIDO', mensaje: 'El DNI no es válido' }], advertencias: [] }
      ]
    };
  }
});
