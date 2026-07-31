import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { HttpErrorResponse, HttpHeaders, HttpResponse } from '@angular/common/http';
import { Subject, of, throwError } from 'rxjs';
import { ImportacionPacientesChatComponent, crearPacienteImportacionChatState } from './importacion-pacientes-chat.component';
import { PacienteImportacionService } from '../../services/paciente-importacion.service';
import { PacienteListRefreshService } from '../../services/paciente-list-refresh.service';
import { IPacienteImportacionPrevisualizacion } from '../../models/paciente-importacion';

describe('ImportacionPacientesChatComponent', () => {
  let fixture: ComponentFixture<ImportacionPacientesChatComponent>;
  let component: ImportacionPacientesChatComponent;
  let service: jasmine.SpyObj<PacienteImportacionService>;
  let refreshService: jasmine.SpyObj<PacienteListRefreshService>;

  beforeEach(async () => {
    service = jasmine.createSpyObj('PacienteImportacionService', ['descargarPlantilla', 'obtenerNombreArchivo', 'validarArchivo', 'confirmarImportacion']);
    service.obtenerNombreArchivo.and.returnValue('plantilla.xlsx');
    refreshService = jasmine.createSpyObj('PacienteListRefreshService', ['solicitarActualizacion']);
    await TestBed.configureTestingModule({
      imports: [ImportacionPacientesChatComponent],
      providers: [
        { provide: PacienteImportacionService, useValue: service },
        { provide: PacienteListRefreshService, useValue: refreshService }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(ImportacionPacientesChatComponent);
    component = fixture.componentInstance;
    component.state = crearPacienteImportacionChatState();
    fixture.detectChanges();
  });

  it('descarga la plantilla una vez y habilita el selector', () => {
    service.descargarPlantilla.and.returnValue(of(new HttpResponse({ body: new Blob(['excel']), headers: new HttpHeaders() })));
    component.descargarPlantilla();
    component.descargarPlantilla();
    fixture.detectChanges();

    expect(service.descargarPlantilla).toHaveBeenCalledTimes(1);
    expect(component.state.plantillaDescargada).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('La plantilla se descargó correctamente');
    expect(fixture.nativeElement.querySelector('input[type=file]').disabled).toBeFalse();
  });

  it('rechaza extensiones distintas de xlsx y archivos mayores de 2 MB', () => {
    component.state.plantillaDescargada = true;
    seleccionar(new File(['pdf'], 'pacientes.pdf'));
    expect(component.state.mensaje).toContain('.xlsx');
    seleccionar(new File([new Uint8Array(2 * 1024 * 1024 + 1)], 'pacientes.xlsx'));
    expect(component.state.mensaje).toContain('2 MB');
    expect(component.state.archivo).toBeUndefined();
  });

  it('analiza y renderiza el resumen, DNI existente y duplicado', () => {
    service.validarArchivo.and.returnValue(of(preview()));
    component.state.plantillaDescargada = true;
    component.state.archivo = new File(['excel'], 'pacientes.xlsx');
    component.analizarArchivo();
    fixture.detectChanges();

    expect(component.state.estado).toBe('PREVISUALIZADA');
    expect(fixture.nativeElement.textContent).toContain('Análisis completado');
    expect(fixture.nativeElement.textContent).toContain('DNI EXISTENTE');
    expect(fixture.nativeElement.textContent).toContain('DNI DUPLICADO');
  });

  it('deshabilita confirmación cuando no hay filas válidas', () => {
    const sinValidos = preview();
    sinValidos.filas = sinValidos.filas.filter(fila => fila.estado !== 'VALIDO');
    sinValidos.resumen.validos = 0;
    component.state.previsualizacion = sinValidos;
    component.state.estado = 'PREVISUALIZADA';
    fixture.detectChanges();

    expect(component.puedeConfirmar).toBeFalse();
    expect(fixture.nativeElement.textContent).toContain('No hay pacientes disponibles');
  });

  it('confirma una vez, muestra el resultado y solicita actualizar pacientes', () => {
    service.confirmarImportacion.and.returnValue(of({
      importacionId: 'id-1', estado: 'CONFIRMADA',
      resumen: { filasValidasEnPrevisualizacion: 1, pacientesRegistrados: 1, omitidosPorDniExistente: 0, erroresAlRegistrar: 0 },
      resultados: [{ numeroFila: 2, dni: '01234567', estado: 'REGISTRADO', idPaciente: 25, errores: [] }]
    }));
    component.state.previsualizacion = preview();
    component.state.estado = 'PREVISUALIZADA';
    component.confirmar();
    component.confirmar();
    fixture.detectChanges();

    expect(service.confirmarImportacion).toHaveBeenCalledTimes(1);
    expect(refreshService.solicitarActualizacion).toHaveBeenCalledTimes(1);
    expect(fixture.nativeElement.textContent).toContain('Importación completada');
    expect(fixture.nativeElement.textContent).toContain('ID 25');
  });

  it('bloquea doble confirmación mientras la solicitud está pendiente', () => {
    const pendiente = new Subject<any>();
    service.confirmarImportacion.and.returnValue(pendiente);
    component.state.previsualizacion = preview();
    component.state.estado = 'PREVISUALIZADA';
    component.confirmar();
    component.confirmar();
    expect(service.confirmarImportacion).toHaveBeenCalledTimes(1);
    expect(component.state.estado).toBe('CONFIRMANDO');
  });

  it('marca expiración y permite volver a analizar', fakeAsync(() => {
    const corta = preview();
    corta.expiraEn = new Date(Date.now() + 10).toISOString();
    service.validarArchivo.and.returnValue(of(corta));
    component.state.archivo = new File(['excel'], 'pacientes.xlsx');
    component.analizarArchivo();
    tick(20);
    fixture.detectChanges();
    expect(component.state.estado).toBe('EXPIRADA');
    expect(component.puedeConfirmar).toBeFalse();
    expect(fixture.nativeElement.textContent).toContain('Volver a analizar');
  }));

  it('muestra errores HTTP seguros', () => {
    service.validarArchivo.and.returnValue(throwError(() => new HttpErrorResponse({ status: 413 })));
    component.state.archivo = new File(['excel'], 'pacientes.xlsx');
    component.analizarArchivo();
    expect(component.state.mensaje).toBe('El archivo supera los 2 MB.');
  });

  function seleccionar(archivo: File): void {
    component.seleccionarArchivo({ target: { files: [archivo], value: 'archivo' } } as unknown as Event);
  }

  function preview(): IPacienteImportacionPrevisualizacion {
    const paciente = { apellidos: 'Pérez', nombres: 'Ana', fechaNacimiento: '1990-01-01', estadoCivil: 'SOLTERO', dni: '01234567', sexo: 'F', direccion: 'Lima', distrito: 'Lima', traidoPor: '' };
    const antecedentes = { alimentacion: '', habitos: '', vivienda: '', desarrolloPsicomotor: '', vacunas: '', educacion: '', enfermedadesPrevias: '', cirugiasPrevias: '', alergiasMedicamentos: '' };
    return {
      importacionId: 'id-1', estado: 'PREVISUALIZADA', expiraEn: new Date(Date.now() + 60000).toISOString(),
      resumen: { registrosAnalizados: 3, validos: 1, conErrores: 2, filasConDniDuplicado: 1, gruposDniDuplicados: 1, dniExistentes: 1, conAdvertencias: 0, filasVaciasIgnoradas: 2 },
      filas: [
        { numeroFila: 2, nombreCompleto: 'Ana Pérez', dni: '01234567', estado: 'VALIDO', paciente, antecedentes, errores: [], advertencias: [] },
        { numeroFila: 3, nombreCompleto: 'Luis Paz', dni: '12345678', estado: 'DNI_EXISTENTE', paciente, antecedentes, errores: [{ codigo: 'DNI_EXISTENTE', mensaje: 'DNI ya registrado' }], advertencias: [] },
        { numeroFila: 4, nombreCompleto: 'María Sol', dni: '87654321', estado: 'DNI_DUPLICADO_ARCHIVO', paciente, antecedentes, errores: [{ codigo: 'DNI_DUPLICADO_ARCHIVO', mensaje: 'DNI repetido' }], advertencias: [] }
      ]
    };
  }
});
