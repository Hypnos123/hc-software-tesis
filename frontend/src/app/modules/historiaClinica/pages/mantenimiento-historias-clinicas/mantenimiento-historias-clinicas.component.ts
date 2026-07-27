import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonComponent } from '@app/shared/components';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { FieldsetModule } from 'primeng/fieldset';
import { InputTextModule } from 'primeng/inputtext';
import { CalendarModule } from 'primeng/calendar';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { ActivatedRoute, Router } from '@angular/router';
import { MensajesSwalService } from '@app/shared/services/mensajes-swal.service';
import { HistoriaClinicaService } from '../../services/consultas.service';
import { IHistoriaClinicaCreateRequest } from '../../models/historiaClinica';

function noSoloEspacios(control: AbstractControl): ValidationErrors | null {
  return typeof control.value === 'string' && control.value.trim().length === 0
    ? { soloEspacios: true }
    : null;
}

function fechaNacimientoValida(control: AbstractControl): ValidationErrors | null {
  if (!control.value) return null;
  const fecha = control.value instanceof Date ? control.value : new Date(control.value);
  if (Number.isNaN(fecha.getTime())) return { fechaInvalida: true };

  const hoy = new Date();
  const fechaSinHora = new Date(fecha.getFullYear(), fecha.getMonth(), fecha.getDate());
  const hoySinHora = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate());
  return fechaSinHora > hoySinHora ? { fechaFutura: true } : null;
}

@Component({
  selector: 'app-mantenimiento-historias-clinicas',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FieldsetModule, ButtonComponent, InputTextModule, CalendarModule, DropdownModule, ButtonModule, InputTextareaModule],
  templateUrl: './mantenimiento-historias-clinicas.component.html',
  styleUrl: './mantenimiento-historias-clinicas.component.scss'
})
export class MantenimientoHistoriasClinicasComponent implements OnInit {
  frm: FormGroup;
  modo: 'nuevo' | 'ver' | 'editar' = 'nuevo';
  titulo = 'Nueva Historia Clinica';
  historiaId: number | null = null;
  historiaCargada = false;
  fechaMaximaNacimiento = new Date();

  estadosCiviles = [
    { label: 'Soltero(a)', value: 'SOLTERO' },
    { label: 'Casado(a)', value: 'CASADO' },
    { label: 'Divorciado(a)', value: 'DIVORCIADO' },
    { label: 'Viudo(a)', value: 'VIUDO' }
  ];

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private swal: MensajesSwalService,
    private service: HistoriaClinicaService
  ) {
    this.frm = this.fb.group({
      idHistoriaClinica: [{ value: '', disabled: true }],
      fechaIngreso: [{ value: null, disabled: true }, Validators.required],
      fechaNacimiento: [{ value: null, disabled: true }, [Validators.required, fechaNacimientoValida]],
      apellidos: [{ value: '', disabled: true }, [Validators.required, noSoloEspacios, Validators.maxLength(120)]],
      nombres: [{ value: '', disabled: true }, [Validators.required, noSoloEspacios, Validators.maxLength(120)]],
      estadoCivil: [{ value: null, disabled: true }, Validators.required],
      edad: [{ value: null, disabled: true }, [Validators.required, Validators.min(0)]],
      dni: [{ value: '', disabled: true }, [Validators.required, noSoloEspacios, Validators.pattern(/^\d{8}$/), Validators.maxLength(8)]],
      enfPrevias: [{ value: '', disabled: true }, Validators.maxLength(120)],
      cirugiasPrevias: [{ value: '', disabled: true }, Validators.maxLength(120)],
      alergiasMedicamentos: [{ value: '', disabled: true }, Validators.maxLength(120)]
    });
  }

  ngOnInit(): void {
    this.obtenerModo();
    this.inicializarCalculoEdad();

    if (this.modo === 'nuevo') {
      this.habilitarCapturaManual();
      return;
    }

    this.historiaId = Number(this.route.snapshot.paramMap.get('id'));
    if (this.historiaId) this.cargarHistoria(this.historiaId);
    if (this.modo === 'ver') this.frm.disable();
  }

  obtenerModo(): void {
    const modoRuta = this.route.snapshot.paramMap.get('modo');
    this.modo = modoRuta === 'ver' || modoRuta === 'editar' ? modoRuta : 'nuevo';
    this.titulo = this.modo === 'ver'
      ? 'Visualizar Historia Clinica'
      : this.modo === 'editar'
        ? 'Editar Historia Clinica'
        : 'Nueva Historia Clinica';
  }

  private habilitarCapturaManual(): void {
    Object.entries(this.frm.controls)
      .filter(([nombreControl]) => nombreControl !== 'idHistoriaClinica' && nombreControl !== 'edad')
      .forEach(([, control]) => control.enable({ emitEvent: false }));
  }

  private inicializarCalculoEdad(): void {
    this.frm.get('fechaNacimiento')?.valueChanges.subscribe(fecha => {
      this.frm.get('edad')?.setValue(this.calcularEdad(fecha), { emitEvent: false });
    });
  }

  cargarHistoria(id: number): void {
    this.service.getById(id).subscribe(historia => {
      if (!historia) return;

      this.historiaCargada = true;
      this.frm.patchValue({
        idHistoriaClinica: historia.idHistoriaClinica,
        fechaIngreso: this.toDate(historia.fechaIngreso),
        fechaNacimiento: this.toDate(historia.fechaNacimiento),
        apellidos: historia.apellidos,
        nombres: historia.nombres,
        estadoCivil: historia.estadoCivil,
        edad: this.calcularEdad(historia.fechaNacimiento),
        dni: historia.numDocumento,
        enfPrevias: historia.enfermedadesPrevias,
        cirugiasPrevias: historia.cirugiasPrevias,
        alergiasMedicamentos: historia.alergiaMedicamentos
      });
    });
  }

  guardar(): void {
    if (this.modo === 'ver') return;

    if (this.modo === 'nuevo') {
      this.frm.markAllAsTouched();
      if (this.frm.invalid) return;
      this.confirmarCreacion();
      return;
    }

    if (!this.historiaId) return;

    this.swal.mensajePregunta('¿Está seguro de guardar los cambios?').then(resultado => {
      if (!resultado.isConfirmed) return;
      this.service.update(this.historiaId!, {}).subscribe(respuesta => this.finalizar(respuesta.mensaje));
    });
  }

  private confirmarCreacion(): void {
    this.swal.mensajePregunta('¿Está seguro de guardar la nueva historia clínica?').then(resultado => {
      if (!resultado.isConfirmed) return;
      this.service.insert(this.crearRequest()).subscribe({
        next: respuesta => {
          if (!respuesta.idGenerado) {
            this.swal.mensajeError('El servidor no confirmó la creación de la historia clínica.');
            return;
          }
          this.finalizar(respuesta.mensaje);
        },
        error: error => this.swal.mensajeError(this.obtenerMensajeError(error))
      });
    });
  }

  private crearRequest(): IHistoriaClinicaCreateRequest {
    const datos = this.frm.getRawValue();
    return {
      fechaIngreso: this.formatearFechaSinZona(datos.fechaIngreso),
      fechaNacimiento: this.formatearFechaSinZona(datos.fechaNacimiento),
      apellidos: datos.apellidos.trim(),
      nombres: datos.nombres.trim(),
      estadoCivil: datos.estadoCivil,
      dni: datos.dni.trim(),
      enfermedadesPrevias: datos.enfPrevias?.trim() || undefined,
      cirugiasPrevias: datos.cirugiasPrevias?.trim() || undefined,
      alergiaMedicamentos: datos.alergiasMedicamentos?.trim() || undefined
    };
  }

  private obtenerMensajeError(error: any): string {
    if (error?.status === 400) return error?.error?.mensaje || 'El DNI ingresado es inválido.';
    if (error?.status === 404) return error?.error?.mensaje || 'No existe un paciente registrado con el DNI ingresado.';
    if (error?.status === 409) return error?.error?.mensaje || 'El DNI está asociado a varios pacientes y no se puede resolver automáticamente.';
    return error?.error?.mensaje || error?.error?.error || 'No se pudo crear la historia clínica.';
  }

  mostrarError(nombreControl: string, error?: string): boolean {
    const control = this.frm.get(nombreControl);
    if (!control || !(control.touched || control.dirty) || !control.errors) return false;
    return error ? control.hasError(error) : true;
  }

  finalizar(mensaje?: string): void {
    this.swal.mensajeExito(mensaje || 'Operación realizada correctamente.');
    this.router.navigate(['/historiaClinica']);
  }

  toDate(fecha: unknown): Date | null {
    if (!fecha) return null;
    if (fecha instanceof Date) return fecha;
    if (typeof fecha === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(fecha)) {
      const [anio, mes, dia] = fecha.split('-').map(Number);
      return new Date(anio, mes - 1, dia);
    }
    return new Date(fecha as string | number);
  }

  private formatearFechaSinZona(valor: unknown): string {
    const fecha = this.toDate(valor);
    if (!fecha || Number.isNaN(fecha.getTime())) return '';
    const anio = fecha.getFullYear();
    const mes = String(fecha.getMonth() + 1).padStart(2, '0');
    const dia = String(fecha.getDate()).padStart(2, '0');
    return `${anio}-${mes}-${dia}`;
  }

  calcularEdad(fecha: unknown): number | undefined {
    const nacimiento = this.toDate(fecha);
    if (!nacimiento || Number.isNaN(nacimiento.getTime())) return undefined;
    const hoy = new Date();
    if (nacimiento > hoy) return undefined;
    let edad = hoy.getFullYear() - nacimiento.getFullYear();
    if (hoy.getMonth() < nacimiento.getMonth() || (hoy.getMonth() === nacimiento.getMonth() && hoy.getDate() < nacimiento.getDate())) edad--;
    return edad;
  }
}
