import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonComponent, TableComponent } from '@app/shared/components';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { FieldsetModule } from 'primeng/fieldset';
import { InputTextModule } from 'primeng/inputtext';
import { CalendarModule } from 'primeng/calendar';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { ActivatedRoute } from '@angular/router';

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
    ButtonComponent,
    TableComponent,
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

  modo: 'nuevo' | 'ver' = 'nuevo';
  titulo = 'Nueva Historia Clinica';
  historiaId: number | null = null;

  estadosCiviles: Opcion[] = [
    { label: 'Soltero(a)', value: 'SOLTERO' },
    { label: 'Casado(a)', value: 'CASADO' },
    { label: 'Divorciado(a)', value: 'DIVORCIADO' },
    { label: 'Viudo(a)', value: 'VIUDO' }
  ];

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute
  ) {
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

  ngOnInit(): void {
    this.obtenerModo();

    if (this.modo === 'ver') {
      this.historiaId = Number(this.route.snapshot.paramMap.get('id'));

      if (this.historiaId) {
        this.cargarHistoria(this.historiaId);
      }

      this.frm.disable();
    }
  }

  obtenerModo(): void {
    const modoParam = this.route.snapshot.paramMap.get('modo');

    if (modoParam === 'ver' || modoParam === 'nuevo') {
      this.modo = modoParam;
    } else {
      this.modo = 'nuevo';
    }

    this.titulo =
      this.modo === 'ver'
        ? 'Visualizar Historia Clinica'
        : 'Nueva Historia Clinica';
  }

  cargarHistoria(id: number): void {
    // Mock temporal
    const historiaMock = {
      idHistoriaClinica: id,
      nombrePacienteSel: 'Mendozaas Davalos Josefina Vera',
      dniSel: '74526981',
      fechaIngreso: this.parseFecha('25/03/2026'),
      apellidos: 'Mendoza Davalos',
      nombres: 'Josefina Vera',
      estadoCivil: 'SOLTERO',
      edad: 41,
      dni: '74526981',
      enfPrevias: 'Hipertensión',
      cirugiasPrevias: 'Apendicectomía',
      alergiasMedicamentos: 'Penicilina'
    };

    this.frm.patchValue(historiaMock);

    if (this.modo === 'ver') {
      this.frm.disable();
    }
  }

  parseFecha(fecha: string | undefined): Date | null {
    if (!fecha) return null;

    const partes = fecha.split('/');
    if (partes.length !== 3) return null;

    const [dia, mes, anio] = partes.map(Number);
    return new Date(anio, mes - 1, dia);
  }

  guardar() {
    if (this.modo === 'ver') return;

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