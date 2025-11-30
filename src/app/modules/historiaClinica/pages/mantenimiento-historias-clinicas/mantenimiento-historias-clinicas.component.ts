import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { FieldsetModule } from 'primeng/fieldset';
import { InputTextModule } from 'primeng/inputtext';
import { CalendarModule } from 'primeng/calendar';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { InputTextareaModule } from 'primeng/inputtextarea';
interface Opcion {
  label: string; 
  value: string;
}
@Component({
  selector: 'app-mantenimiento-historias-clinicas',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FieldsetModule,
    InputTextModule,
    CalendarModule,
    DropdownModule,
    ButtonModule,
    InputTextareaModule
  ],
  templateUrl: './mantenimiento-historias-clinicas.component.html',
  styleUrl: './mantenimiento-historias-clinicas.component.scss'
})
export class MantenimientoHistoriasClinicasComponent {

  frm!: FormGroup;

  estadosCiviles: Opcion[] = [
    { label: 'Soltero(a)', value: 'SOLTERO' },
    { label: 'Casado(a)',  value: 'CASADO' },
    { label: 'Divorciado(a)', value: 'DIVORCIADO' },
    { label: 'Viudo(a)', value: 'VIUDO' }
  ];

  constructor(private fb: FormBuilder) {
    this.frm = this.fb.group({
  
      idHistoriaClinica: [{ value: 215, disabled: true }],
      nombrePacienteSel: [''],
      dniSel: [''],

      fechaIngreso: [new Date(), Validators.required],
      apellidos: ['', Validators.required],
      nombres: ['', Validators.required],
      estadoCivil: [null],
      edad: [null, [Validators.min(0), Validators.max(120)]],
      dni: [''],

      enfPrevias: [''],
      cirugiasPrevias: [''],
      alergiasMedicamentos: ['']
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

  volver() {
   
  }
}
