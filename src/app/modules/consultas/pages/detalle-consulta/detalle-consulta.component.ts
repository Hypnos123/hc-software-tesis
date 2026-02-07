import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonComponent } from '@app/shared/components';
import { ButtonModule } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { DropdownModule } from 'primeng/dropdown';
import { FieldsetModule } from 'primeng/fieldset';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';

interface Opcion {
  label: string;
  value: string;
}

@Component({
  selector: 'app-detalle-consulta',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FieldsetModule,
    ButtonComponent,
    InputTextModule,
    CalendarModule,
    DropdownModule,
    ButtonModule,
    InputTextareaModule
  ],
  templateUrl: './detalle-consulta.component.html',
  styleUrl: './detalle-consulta.component.scss'
})
export class DetalleConsultaComponent {

  frm!: FormGroup;

  estadosCiviles: Opcion[] = [
    { label: 'Soltero(a)', value: 'SOLTERO' },
    { label: 'Casado(a)', value: 'CASADO' },
    { label: 'Divorciado(a)', value: 'DIVORCIADO' },
    { label: 'Viudo(a)', value: 'VIUDO' }
  ];

  constructor(private fb: FormBuilder) {
    this.frm = this.fb.group({

      nombreCompleto: ['', Validators.required],
      edad: [null, [Validators.min(0), Validators.max(120)]],
      dni: [''],

      enfermedadesPrevias: ['', Validators.required],
      cirugiasPrevias: ['', Validators.required],
      alergiasMedicamentos: [''],

      presion: [''],
      frecuenciaCardiaca: [''],
      frecuenciaRespiratoria: [''],
      talla: [''],
      temperatura: [''],
      peso: [''],


      fechaConsulta: [new Date(), Validators.required],
      tiempoEnfermedad: ['', Validators.required],
      tipoEnfermedad: ['', Validators.required],

      relato: [''],
      diagnostico: ['', Validators.required],
      examenesRecetados: ['', Validators.required],
      receta: ['', Validators.required],
      tratamiento: ['', Validators.required],
    });
  }

  guardar() {
    if (this.frm.invalid) {
      this.frm.markAllAsTouched();
      return;
    }
    const payload = {
      ...this.frm.getRawValue()
    };

    console.log('HC a guardar', payload);
  }
}
