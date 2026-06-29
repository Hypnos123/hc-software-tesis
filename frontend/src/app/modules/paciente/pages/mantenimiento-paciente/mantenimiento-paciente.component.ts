import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { ButtonComponent } from '@app/shared/components';
import { IPaciente } from '../../models/paciente';
import { PacienteService } from '../../services/paciente.service';
import { AntecedentesService } from '../../services/antecedentes.service';
import { ActivatedRoute, Router } from '@angular/router';
import { AbstractControl, FormBuilder, FormGroup, ValidationErrors, Validators, ReactiveFormsModule } from '@angular/forms';
import { TabViewModule } from 'primeng/tabview';
import { InputTextModule } from 'primeng/inputtext';
import { CalendarModule } from 'primeng/calendar';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { DialogModule } from 'primeng/dialog';
import { switchMap } from 'rxjs/operators';
import Swal from 'sweetalert2';

export interface IOption {
  label: string;
  value: string;
}

const EDAD_MAXIMA_PACIENTE = 120;
@Component({
  selector: 'app-mantenimiento-paciente',
  standalone: true,
  imports: [
    CommonModule,
    ButtonComponent,
    ReactiveFormsModule,
    TabViewModule,
    InputTextModule,
    CalendarModule,
    DropdownModule,
    ButtonModule,
    InputTextareaModule,
    DialogModule,
  ],
  templateUrl: './mantenimiento-paciente.component.html',
  styleUrl: './mantenimiento-paciente.component.scss',
})
export class MantenimientoPacienteComponent {
  activeIndex = 0;
  mostrarConfirmacion = false;

  modo: 'nuevo' | 'ver' | 'editar' = 'nuevo';
  titulo = 'Nuevo Paciente';
  pacienteId: number | null = null;
  antecedenteId: number | null = null;
  fechaMaximaNacimiento = new Date();

  sexo_Opcion: IOption[] = [];
  estadoCivil_Opcion: IOption[] = [];
  educacion_Opcion: IOption[] = [];

  constructor(
    private fb: FormBuilder,
    private pacienteService: PacienteService,
    private antecedentesService: AntecedentesService,
    private router: Router,
    private route: ActivatedRoute
  ) { }

  frm: FormGroup = this.fb.group({
    datos: this.fb.group({
      nPaciente: [{ value: 5, disabled: true }],
      apellidos: ['', Validators.required],
      nombres: ['', Validators.required],
      fechaIngreso: [new Date(), Validators.required],
      fechaNac: [null, [Validators.required, this.validarFechaNacimiento.bind(this)]],
      estadoCivil: [null, Validators.required],
      edad: [{ value: null, disabled: true }, [Validators.required, Validators.min(0), Validators.max(EDAD_MAXIMA_PACIENTE)]],
      dni: ['', [Validators.required, Validators.minLength(8)]],
      sexo: [null, Validators.required],
      direccion: ['', Validators.required],
      distrito: ['', Validators.required],
      traidoPor: [''],
    }),
    antecedentes: this.fb.group({
      alimentacion: ['', Validators.required],
      habitos: ['', Validators.required],
      vivienda: ['', Validators.required],
      desarrolloPsico: ['', Validators.required],
      vacunas: ['', Validators.required],
      educacion: ['', Validators.required],
      enfermedadesPrev: ['', Validators.required],
      cirugiasPrevias: ['', Validators.required],
      alergiasMedicamentos: ['', Validators.required],
    }),
  });

  ngOnInit(): void {
    this.obtenerModo();
    this.listarDropdown();
    this.inicializarCalculoEdad();

    if (this.modo === 'ver' || this.modo === 'editar') {
      this.pacienteId = Number(this.route.snapshot.paramMap.get('id'));

      if (this.pacienteId) {
        this.cargarPaciente(this.pacienteId);
      }
    } else {
      this.actualizarEdadDesdeFechaNacimiento(this.frm.get('datos.fechaNac')?.value);
    }

    if (this.modo === 'ver') {
      this.frm.disable();
    }
  }

  obtenerModo(): void {
    const modoParam = this.route.snapshot.paramMap.get('modo');

    if (modoParam === 'ver' || modoParam === 'editar' || modoParam === 'nuevo') {
      this.modo = modoParam;
    } else {
      this.modo = 'nuevo';
    }

    switch (this.modo) {
      case 'nuevo':
        this.titulo = 'Nuevo Paciente';
        break;
      case 'ver':
        this.titulo = 'Visualizar Paciente';
        break;
      case 'editar':
        this.titulo = 'Editar Paciente';
        break;
    }
  }

  parseFecha(fecha: string | Date | undefined): Date | null {
    if (!fecha) return null;
    if (fecha instanceof Date) return fecha;

    if (fecha.includes('/')) {
      const partes = fecha.split('/');
      if (partes.length !== 3) return null;

      const [dia, mes, anio] = partes.map(Number);
      return new Date(anio, mes - 1, dia);
    }

    const date = new Date(fecha);
    return isNaN(date.getTime()) ? null : date;
  }


  private inicializarCalculoEdad(): void {
    this.frm.get('datos.fechaNac')?.valueChanges.subscribe((fechaNacimiento) => {
      this.actualizarEdadDesdeFechaNacimiento(fechaNacimiento);
    });
  }

  private actualizarEdadDesdeFechaNacimiento(fechaNacimiento: Date | string | null): void {
    const edadControl = this.frm.get('datos.edad');
    const edad = this.calcularEdad(this.parseFecha(fechaNacimiento ?? undefined));

    edadControl?.setValue(edad, { emitEvent: false });
    edadControl?.markAsTouched();
    edadControl?.updateValueAndValidity({ emitEvent: false });
  }

  private calcularEdad(fechaNacimiento: Date | null): number | null {
    if (!fechaNacimiento || isNaN(fechaNacimiento.getTime()) || fechaNacimiento > this.inicioDelDia(new Date())) {
      return null;
    }

    const hoy = this.inicioDelDia(new Date());
    let edad = hoy.getFullYear() - fechaNacimiento.getFullYear();
    const mesActual = hoy.getMonth();
    const diaActual = hoy.getDate();
    const mesNacimiento = fechaNacimiento.getMonth();
    const diaNacimiento = fechaNacimiento.getDate();

    if (mesActual < mesNacimiento || (mesActual === mesNacimiento && diaActual < diaNacimiento)) {
      edad--;
    }

    return edad >= 0 ? edad : null;
  }

  private validarFechaNacimiento(control: AbstractControl): ValidationErrors | null {
    const fecha = this.parseFecha(control.value);

    if (!fecha) return { fechaNacimientoInvalida: true };
    if (fecha > this.inicioDelDia(new Date())) return { fechaNacimientoFutura: true };

    const edad = this.calcularEdad(fecha);
    if (edad === null || edad < 0) return { edadNegativa: true };
    if (edad > EDAD_MAXIMA_PACIENTE) return { edadMaxima: true };

    return null;
  }

  mensajeErrorFechaNacimiento(): string {
    const control = this.frm.get('datos.fechaNac');
    if (!control || !control.errors || !control.touched) return '';

    if (control.errors['required']) return 'La fecha de nacimiento es obligatoria.';
    if (control.errors['fechaNacimientoFutura']) return 'La fecha de nacimiento no puede ser futura.';
    if (control.errors['edadMaxima']) return `La edad máxima permitida es ${EDAD_MAXIMA_PACIENTE} años.`;

    return 'Ingresa una fecha de nacimiento válida.';
  }

  private inicioDelDia(fecha: Date): Date {
    return new Date(fecha.getFullYear(), fecha.getMonth(), fecha.getDate());
  }

  listarDropdown() {
    this.sexo_Opcion = [
      { label: 'Masculino', value: 'M' },
      { label: 'Femenino', value: 'F' },
    ];

    this.estadoCivil_Opcion = [
      { label: 'Soltero(a)', value: 'SOLTERO' },
      { label: 'Casado(a)', value: 'CASADO' },
      { label: 'Divorciado(a)', value: 'DIVORCIADO' },
      { label: 'Viudo(a)', value: 'VIUDO' },
    ];

    this.educacion_Opcion = [
      { label: 'Primaria', value: 'P' },
      { label: 'Secundaria', value: 'S' },
      { label: 'Tecnico', value: 'T' },
      { label: 'Superior', value: 'S1' },
    ];
  }

  cargarPaciente(id: number): void {
    this.pacienteService.getById(id).subscribe((paciente) => {
      if (!paciente) return;

      const sexoFiltrado = this.sexo_Opcion.find((sexo) => sexo.value === paciente.sexo || sexo.label === paciente.sexo);
      const estadoCivilFiltrado = this.estadoCivil_Opcion.find((estadoCivil) =>
        estadoCivil.value === this.normalizarEstadoCivil(paciente.estadoCivil) || estadoCivil.label === paciente.estadoCivil
      );

      this.frm.patchValue({
        datos: {
          nPaciente: paciente.idPaciente ?? null,
          apellidos: paciente.apellidos ?? '',
          nombres: paciente.nombres ?? '',
          fechaIngreso: this.parseFecha(paciente.fechaIngreso),
          fechaNac: this.parseFecha(paciente.fechaNacimiento),
          estadoCivil: estadoCivilFiltrado ?? this.normalizarEstadoCivil(paciente.estadoCivil) ?? null,
          edad: this.calcularEdad(this.parseFecha(paciente.fechaNacimiento)),
          dni: paciente.numDocumento ?? paciente.dni ?? '',
          sexo: sexoFiltrado ?? paciente.sexo ?? null,
          direccion: paciente.direccion ?? '',
          distrito: paciente.distrito ?? '',
          traidoPor: paciente.traidoPor ?? '',
        },
        antecedentes: {
          alimentacion: '',
          habitos: '',
          vivienda: '',
          desarrolloPsico: '',
          vacunas: '',
          educacion: '',
          enfermedadesPrev: '',
          cirugiasPrevias: '',
          alergiasMedicamentos: '',
        }
      });

      this.actualizarEdadDesdeFechaNacimiento(this.frm.get('datos.fechaNac')?.value);

      this.cargarAntecedentes(id);

      if (this.modo === 'ver') {
        this.frm.disable();
      }
    });
  }

  cargarAntecedentes(idPaciente: number): void {
    this.antecedentesService.getByPacienteId(idPaciente).subscribe((antecedente) => {
      if (!antecedente) return;

      this.antecedenteId = antecedente.idAntecedentes ?? null;
      const educacionFiltrada = this.educacion_Opcion.find(
        (educacion) => educacion.value === antecedente.educacion || educacion.label === antecedente.educacion
      );

      this.frm.patchValue({
        antecedentes: {
          alimentacion: antecedente.alimentacion ?? '',
          habitos: antecedente.habitos ?? '',
          vivienda: antecedente.vivienda ?? '',
          desarrolloPsico: antecedente.desarrolloPsicomotor ?? '',
          vacunas: antecedente.vacunas ?? '',
          educacion: educacionFiltrada ?? antecedente.educacion ?? '',
          enfermedadesPrev: antecedente.enfermedadesPrevias ?? '',
          cirugiasPrevias: antecedente.cirugiasPrevias ?? '',
          alergiasMedicamentos: antecedente.alergiaMedicamentos ?? '',
        }
      });
    });
  }

  onTabChange(e: any) {
    if (this.modo === 'ver') {
      this.activeIndex = e.index;
      return;
    }

    if (e.index === 1 && this.frm.get('datos')?.invalid) {
      this.frm.get('datos')?.markAllAsTouched();
      this.activeIndex = 0;
    }
  }

  confirmarGuardar() {
    if (this.modo === 'ver') return;

    if (this.frm.get('datos')?.invalid) {
      this.frm.get('datos')?.markAllAsTouched();
      return;
    }

    const texto =
      this.modo === 'editar'
        ? 'Se actualizarán los datos del paciente.'
        : 'Se registrará un nuevo paciente.';

    const titulo =
      this.modo === 'editar'
        ? '¿Actualizar paciente?'
        : '¿Guardar paciente?';

    Swal.fire({
      title: titulo,
      text: texto,
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Sí, confirmar',
      cancelButtonText: 'Cancelar',
      reverseButtons: true,
      confirmButtonColor: '#1179c4',
      cancelButtonColor: '#6c757d'
    }).then((result) => {
      if (!result.isConfirmed) return;

      if (this.modo === 'editar') {
        this.actualizarPaciente();
      } else {
        this.registrarPaciente();
      }
    });
  }

  registrarPaciente() {
    const params = this.buildPacienteRequest();

    this.pacienteService.insert(params).pipe(
      switchMap((response) => {
        const idPaciente = response.idGenerado;
        if (!idPaciente) throw new Error('No se recibió el id del paciente generado.');

        return this.antecedentesService.insert(this.buildAntecedentesRequest(idPaciente));
      })
    ).subscribe({
      next: (response) => {
        if (response) {
          Swal.fire({ icon: 'success', title: 'Guardado', timer: 1200, showConfirmButton: false });
          this.router.navigateByUrl('/paciente');
        }
      },
      error: () => Swal.fire({ icon: 'error', title: 'Error', text: 'No se pudo guardar.' })
    });
  }

  actualizarPaciente() {
    if (!this.pacienteId) return;

    const params = this.buildPacienteRequest(this.pacienteId);

    this.pacienteService.update(this.pacienteId, params).pipe(
      switchMap(() => this.guardarAntecedentes(this.pacienteId!))
    ).subscribe({
      next: (response) => {
        if (response) {
          Swal.fire({ icon: 'success', title: 'Actualizado', timer: 1200, showConfirmButton: false });
          this.router.navigateByUrl('/paciente');
        }
      },
      error: () => Swal.fire({ icon: 'error', title: 'Error', text: 'No se pudo actualizar.' })
    });
  }

  private buildPacienteRequest(idPaciente?: number): IPaciente {
    const datos = this.frm.getRawValue().datos;
    const sexo = datos.sexo && typeof datos.sexo === 'object' ? datos.sexo.value : datos.sexo;
    const estadoCivil = datos.estadoCivil && typeof datos.estadoCivil === 'object' ? datos.estadoCivil.value : datos.estadoCivil;

    return {
      idPaciente,
      nombres: datos.nombres,
      apellidos: datos.apellidos,
      fechaIngreso: datos.fechaIngreso,
      fechaNacimiento: datos.fechaNac,
      edad: datos.edad,
      estadoCivil: this.normalizarEstadoCivil(estadoCivil) ?? estadoCivil,
      numDocumento: datos.dni,
      sexo,
      direccion: datos.direccion,
      distrito: datos.distrito,
      traidoPor: datos.traidoPor,
    };
  }


  private guardarAntecedentes(idPaciente: number) {
    const params = this.buildAntecedentesRequest(idPaciente, this.antecedenteId ?? undefined);

    return this.antecedenteId
      ? this.antecedentesService.update(this.antecedenteId, params)
      : this.antecedentesService.insert(params);
  }

  private buildAntecedentesRequest(idPaciente: number, idAntecedentes?: number): IPaciente {
    const antecedentes = this.frm.getRawValue().antecedentes;
    const educacion = antecedentes.educacion && typeof antecedentes.educacion === 'object'
      ? antecedentes.educacion.value
      : antecedentes.educacion;

    return {
      idAntecedentes,
      idPaciente,
      alimentacion: antecedentes.alimentacion,
      habitos: antecedentes.habitos,
      vivienda: antecedentes.vivienda,
      desarrolloPsicomotor: antecedentes.desarrolloPsico,
      vacunas: antecedentes.vacunas,
      educacion,
      enfermedadesPrevias: antecedentes.enfermedadesPrev,
      cirugiasPrevias: antecedentes.cirugiasPrevias,
      alergiaMedicamentos: antecedentes.alergiasMedicamentos,
    };
  }

  private normalizarEstadoCivil(valor?: string): string | undefined {
    if (!valor) return undefined;

    const limpio = valor
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .trim()
      .toUpperCase();

    if (limpio.startsWith('SOLTER')) return 'SOLTERO';
    if (limpio.startsWith('CASAD')) return 'CASADO';
    if (limpio.startsWith('DIVORCIAD')) return 'DIVORCIADO';
    if (limpio.startsWith('VIUD')) return 'VIUDO';

    return limpio;
  }


  back() {
    this.activeIndex = 0;
  }

  next() {
    if (this.modo === 'ver') {
      this.activeIndex = 1;
      return;
    }

    if (this.frm.get('datos')?.valid) this.activeIndex = 1;
    else this.frm.get('datos')?.markAllAsTouched();
  }



  tiempoTranscurridoMs: number = 0;
tiempoFormateado: string = '00:00:00.000';
cronometroActivo: boolean = false;
intervaloCronometro: any = null;

private inicioCronometro: number = 0;
private tiempoAcumuladoMs: number = 0;

iniciarCronometro(): void {
  if (this.cronometroActivo) return;

  this.cronometroActivo = true;

  // Si estaba pausado, continúa desde lo acumulado
  this.inicioCronometro = Date.now() - this.tiempoAcumuladoMs;

  this.intervaloCronometro = setInterval(() => {
    this.tiempoTranscurridoMs = Date.now() - this.inicioCronometro;
    this.actualizarTiempoFormateado();
  }, 10); // actualiza cada 10 ms
}

pausarCronometro(): void {
  if (!this.cronometroActivo) return;

  this.cronometroActivo = false;

  if (this.intervaloCronometro) {
    clearInterval(this.intervaloCronometro);
    this.intervaloCronometro = null;
  }

  this.tiempoAcumuladoMs = this.tiempoTranscurridoMs;
}

reiniciarCronometro(): void {
  this.cronometroActivo = false;

  if (this.intervaloCronometro) {
    clearInterval(this.intervaloCronometro);
    this.intervaloCronometro = null;
  }

  this.tiempoTranscurridoMs = 0;
  this.tiempoAcumuladoMs = 0;
  this.tiempoFormateado = '00:00:00.000';
}

actualizarTiempoFormateado(): void {
  const horas = Math.floor(this.tiempoTranscurridoMs / 3600000);
  const minutos = Math.floor((this.tiempoTranscurridoMs % 3600000) / 60000);
  const segundos = Math.floor((this.tiempoTranscurridoMs % 60000) / 1000);
  const milisegundos = this.tiempoTranscurridoMs % 1000;

  this.tiempoFormateado =
    `${this.formatoDosDigitos(horas)}:` +
    `${this.formatoDosDigitos(minutos)}:` +
    `${this.formatoDosDigitos(segundos)}.` +
    `${this.formatoTresDigitos(milisegundos)}`;
}

formatoDosDigitos(valor: number): string {
  return valor.toString().padStart(2, '0');
}

formatoTresDigitos(valor: number): string {
  return valor.toString().padStart(3, '0');
}

ngOnDestroy(): void {
  if (this.intervaloCronometro) {
    clearInterval(this.intervaloCronometro);
  }
}

}
