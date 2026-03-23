import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { ButtonComponent } from '@app/shared/components';
import { IPaciente } from '../../models/paciente';
import { IColumnasTabla } from '@app/shared/models/columnas';
import { PacienteService } from '../../services/paciente.service';
import { MensajesSwalService } from '@app/shared/services/mensajes-swal.service';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule,} from '@angular/forms';
import { TabViewModule } from 'primeng/tabview';
import { InputTextModule } from 'primeng/inputtext';
import { CalendarModule } from 'primeng/calendar';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { DialogModule } from 'primeng/dialog';
import { ActivatedRoute } from '@angular/router';
import Swal from 'sweetalert2';


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

  readonly sexo_Opcion = [
    { label: 'Masculino', value: 'M' },
    { label: 'Femenino', value: 'F' },
  ];

  readonly educacion_Opcion = [
    { label: 'Primaria', value: 'P' },
    { label: 'Secundaria', value: 'S' },
    { label: 'Tecnico', value: 'T' },
    { label: 'Superior', value: 'S1' },
  ];

  constructor(
    private fb: FormBuilder,
    private pacienteService: PacienteService,
    private router: Router,
    private route: ActivatedRoute,
    private readonly servicioMensajesSwal: MensajesSwalService
  ) {}

  ngOnInit(): void {
    this.getAllActivosElementos();
  }

  frm: FormGroup = this.fb.group({
    datos: this.fb.group({
      nPaciente: [{ value: 5, disabled: true }],
      apellidos: ['', Validators.required],
      nombres: ['', Validators.required],
      fechaIngreso: [new Date(), Validators.required],
      fechaNac: [new Date(), Validators.required],
      estadoCivil: ['', Validators.required],
      edad: [
        null,
        [Validators.required, Validators.min(0), Validators.max(120)],
      ],
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

  onTabChange(e: any) {
    if (e.index === 1 && this.frm.get('datos')?.invalid) {
      this.frm.get('datos')?.markAllAsTouched();
      this.activeIndex = 0;
    }
  }


  confirmar() {
    this.mostrarConfirmacion = false;
    // this.registrarPaciente();
  }

  confirmarGuardar() {
  if (this.frm.invalid) {
    this.frm.markAllAsTouched();
    return;
  }

  Swal.fire({
    title: '¿Guardar paciente?',
    text: 'Se registrará un nuevo paciente.',
    icon: 'question',
    showCancelButton: true,
    confirmButtonText: 'Sí, confirmar',
    cancelButtonText: 'Cancelar',
    reverseButtons: true,
    confirmButtonColor: '#1179c4',
    cancelButtonColor: '#6c757d'
  }).then((result) => {
    if (!result.isConfirmed) return;

    this.registrarPaciente(); 
  });
}

registrarPaciente() {
  const payload = this.frm.getRawValue(); // incluye disabled como nPaciente
  // arma tu objeto según tu backend
  // this.pacienteService.insert(payload).subscribe(...)
  console.log('payload', payload);

  // opcional: mensaje de éxito
  Swal.fire({
    icon: 'success',
    title: 'Guardado',
    timer: 1200,
    showConfirmButton: false
  });

  this.router.navigateByUrl('/paciente');
}

  cancelar() {
    this.mostrarConfirmacion = false;
  }

  listaPacientes: IPaciente[] = [];
  cols: IColumnasTabla[] = [];
  colsVisibles: IColumnasTabla[] = [];
  isCargado: boolean = false;
  

  next() {
    if (this.frm.get('datos')?.valid) this.activeIndex = 1;
    else this.frm.get('datos')?.markAllAsTouched();
  }

  back() {
    this.activeIndex = 0;
  }

  getAllActivosElementos() {}

  guardar() {
    if (this.frm.invalid) {
      this.frm.markAllAsTouched();
      return;
    }
    const payload = this.frm.getRawValue();
  }
}
