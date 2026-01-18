import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ConsultaService } from '../../services/consultas.service';
import { ActivatedRoute, Router } from '@angular/router';
import { MensajesSwalService } from '@app/shared/services/mensajes-swal.service';
import { CommonModule } from '@angular/common';
import { ButtonComponent } from '@app/shared/components';
import { ButtonModule } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { TabViewModule } from 'primeng/tabview';

@Component({
  selector: 'app-mantenimiento-consultas',
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
  templateUrl: './mantenimiento-consultas.component.html',
  styleUrl: './mantenimiento-consultas.component.scss'
})
export class MantenimientoConsultasComponent {
  activeIndex = 0;
  mostrarConfirmacion = false;

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
    private consultaService: ConsultaService,
    private router: Router,
    private route: ActivatedRoute,
    private readonly servicioMensajesSwal: MensajesSwalService
  ) { }


  onTabChange(e: any) {
    if (e.index === 1 && this.frm.get('datos')?.invalid) {
      this.frm.get('datos')?.markAllAsTouched();
      this.activeIndex = 0;
    }
  }

    abrirConfirmacion() {
    if (this.frm.invalid) {
      this.frm.markAllAsTouched();
      return;
    }
    this.mostrarConfirmacion = true;
  }

  confirmar() {
    this.mostrarConfirmacion = false;
    // this.registrarPaciente();
  }

  cancelar() {
    this.mostrarConfirmacion = false;
  }
}
