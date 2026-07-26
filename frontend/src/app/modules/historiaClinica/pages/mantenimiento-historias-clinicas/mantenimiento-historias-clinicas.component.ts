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

function noSoloEspacios(control: AbstractControl): ValidationErrors | null {
  return typeof control.value === 'string' && control.value.trim().length === 0
    ? { soloEspacios: true }
    : null;
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
  guardadoManualHabilitado = false;
  readonly mensajeGuardadoManual = 'El guardado manual se habilitará al completar la integración con el backend.';

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
      .filter(([nombreControl]) => nombreControl !== 'idHistoriaClinica')
      .forEach(([, control]) => control.enable({ emitEvent: false }));
  }

  cargarHistoria(id: number): void {
    this.service.getById(id).subscribe(historia => {
      if (!historia) return;

      this.historiaCargada = true;
      this.frm.patchValue({
        idHistoriaClinica: historia.idHistoriaClinica,
        fechaIngreso: this.toDate(historia.fechaIngreso),
        apellidos: historia.apellidos,
        nombres: historia.nombres,
        estadoCivil: historia.estadoCivil,
        edad: historia.edad ?? this.calcularEdad(historia.fechaNacimiento),
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
      this.swal.mensajeAdvertencia(this.mensajeGuardadoManual);
      return;
    }

    if (!this.historiaId) return;

    this.swal.mensajePregunta('¿Está seguro de guardar los cambios?').then(resultado => {
      if (!resultado.isConfirmed) return;
      this.service.update(this.historiaId!, {}).subscribe(respuesta => this.finalizar(respuesta.mensaje));
    });
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
    return new Date(fecha as string | number);
  }

  calcularEdad(fecha: unknown): number | undefined {
    const nacimiento = this.toDate(fecha);
    if (!nacimiento) return undefined;
    const hoy = new Date();
    let edad = hoy.getFullYear() - nacimiento.getFullYear();
    if (hoy.getMonth() < nacimiento.getMonth() || (hoy.getMonth() === nacimiento.getMonth() && hoy.getDate() < nacimiento.getDate())) edad--;
    return edad;
  }
}
