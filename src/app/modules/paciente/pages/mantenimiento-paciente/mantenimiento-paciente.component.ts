import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { ButtonComponent } from '@app/shared/components';
import { IPaciente } from '../../models/paciente';
import { IColumnasTabla } from '@app/shared/models/columnas';
import { PacienteService } from '../../services/paciente.service';
import { MensajesSwalService } from '@app/shared/services/mensajes-swal.service';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { TabViewModule } from 'primeng/tabview';
import { InputTextModule } from 'primeng/inputtext';
import { CalendarModule } from 'primeng/calendar';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { DialogModule } from 'primeng/dialog';
import Swal from 'sweetalert2';

export interface IOption {
  label: string;
  value: string;
}
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

  sexo_Opcion: IOption[] = [];
  educacion_Opcion: IOption[] = [];

  constructor(
    private fb: FormBuilder,
    private pacienteService: PacienteService,
    private router: Router,
    private route: ActivatedRoute,
    private readonly servicioMensajesSwal: MensajesSwalService
  ) { }

  frm: FormGroup = this.fb.group({
    datos: this.fb.group({
      nPaciente: [{ value: 5, disabled: true }],
      apellidos: ['', Validators.required],
      nombres: ['', Validators.required],
      fechaIngreso: [new Date(), Validators.required],
      fechaNac: [new Date(), Validators.required],
      estadoCivil: ['', Validators.required],
      edad: [null, [Validators.required, Validators.min(0), Validators.max(120)]],
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

    if (this.modo === 'ver' || this.modo === 'editar') {
      this.pacienteId = Number(this.route.snapshot.paramMap.get('id'));

      if (this.pacienteId) {
        this.cargarPaciente(this.pacienteId);
      }
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

  parseFecha(fecha: string | undefined): Date | null {
    if (!fecha) return null;

    const partes = fecha.split('/');
    if (partes.length !== 3) return null;

    const [dia, mes, anio] = partes.map(Number);
    return new Date(anio, mes - 1, dia);
  }

  listarDropdown() {
    this.sexo_Opcion = [
      { label: 'Masculino', value: 'M' },
      { label: 'Femenino', value: 'F' },
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

      const sexoFiltrado = this.sexo_Opcion.find((sexo) => sexo.label === paciente.sexo);
      const educacionFiltrada = this.educacion_Opcion.find((e) => e.label === paciente.educacion);

      this.frm.patchValue({
        datos: {
          nPaciente: paciente.idPaciente ?? null,
          apellidos: paciente.apellidos ?? '',
          nombres: paciente.nombres ?? '',
          fechaIngreso: this.parseFecha(paciente.fechaIngreso),
          fechaNac: this.parseFecha(paciente.fechaNacimiento),
          estadoCivil: paciente.estadoCivil ?? '',
          edad: paciente.edad ?? null,
          dni: paciente.dni ?? '',
          sexo: sexoFiltrado ?? null,
          direccion: paciente.direccion ?? '',
          distrito: paciente.distrito ?? '',
          traidoPor: paciente.traidoPor ?? '',
        },
        antecedentes: {
          alimentacion: paciente.alimentacion ?? '',
          habitos: paciente.habitos ?? '',
          vivienda: paciente.vivienda ?? '',
          desarrolloPsico: paciente.desarrolloPsicomotor ?? '',
          vacunas: paciente.vacunas ?? '',
          educacion: educacionFiltrada ?? '',
          enfermedadesPrev: paciente.enfermedadesPrevias ?? '',
          cirugiasPrevias: paciente.cirugiasPrevias ?? '',
          alergiasMedicamentos: paciente.alergiaMedicamentos ?? '',
        }
      });

      if (this.modo === 'ver') {
        this.frm.disable();
      }
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

    if (this.frm.invalid) {
      this.frm.markAllAsTouched();
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
    const payload = this.frm.getRawValue();

    const params: IPaciente = {
      ...payload.datos,
      ...payload.antecedentes
    };


    this.pacienteService.insert(params).subscribe({
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
    const payload = {
      idPaciente: this.pacienteId,
      ...this.frm.getRawValue()
    };

    console.log('payload actualizar', payload);

    const params: IPaciente = {
      ...payload.datos,
      ...payload.antecedentes
    };


    this.pacienteService.update(this.pacienteId, params).subscribe({
      next: (response) => {
        if (response) {
          Swal.fire({ icon: 'success', title: 'Actualizado', timer: 1200, showConfirmButton: false });
          this.router.navigateByUrl('/paciente');
        }
      },
      error: () => Swal.fire({ icon: 'error', title: 'Error', text: 'No se pudo actualizar.' })
    });
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
}
