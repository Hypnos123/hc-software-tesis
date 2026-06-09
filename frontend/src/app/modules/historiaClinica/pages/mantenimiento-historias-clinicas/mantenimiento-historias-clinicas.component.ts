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
import { MensajesSwalService } from '@app/shared/services/mensajes-swal.service';


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

  pacienteCargado = false;
  buscandoPaciente = false;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private readonly servicioMensajesSwal: MensajesSwalService
  ) {
    this.frm = this.fb.group({
      idHistoriaClinica: [{ value: 215, disabled: true }],
      nombrePacienteSel: [''],
      dniSel: [''],

    fechaIngreso: [{ value: null, disabled: true }, Validators.required],
    apellidos: [{ value: '', disabled: true }, Validators.required],
    nombres: [{ value: '', disabled: true }, Validators.required],
    estadoCivil: [{ value: null, disabled: true }, Validators.required],
    edad: [{ value: null, disabled: true }, [Validators.required, Validators.min(0), Validators.max(120)]],
    dni: [{ value: '', disabled: true }, Validators.required],
    enfPrevias: [{ value: '', disabled: true }, Validators.required],
    cirugiasPrevias: [{ value: '', disabled: true }, Validators.required],
    alergiasMedicamentos: [{ value: '', disabled: true }, Validators.required]
    });
  }

  buscarPaciente(): void {
  if (this.modo === 'ver') return;

  const nombre = this.frm.get('nombrePacienteSel')?.value?.trim();
  const dniSel = this.frm.get('dniSel')?.value?.trim();

  if (!nombre && !dniSel) {
    this.limpiarDatosPaciente();
    return;
  }

  this.buscandoPaciente = true;

  // Pintado de inputs - mock
  const pacienteMock = this.obtenerPacienteMock(nombre, dniSel);

  setTimeout(() => {
    this.buscandoPaciente = false;

    if (pacienteMock) {
      this.frm.patchValue({
        fechaIngreso: new Date(),
        apellidos: pacienteMock.apellidos,
        nombres: pacienteMock.nombres,
        estadoCivil: pacienteMock.estadoCivil,
        edad: pacienteMock.edad,
        dni: pacienteMock.dni,
        enfPrevias: pacienteMock.enfPrevias,
        cirugiasPrevias: pacienteMock.cirugiasPrevias,
        alergiasMedicamentos: pacienteMock.alergiasMedicamentos
      });

      this.pacienteCargado = true;
    } else {
      this.limpiarDatosPaciente();
      this.servicioMensajesSwal.mensajeAdvertencia?.('No se encontró información del paciente.');
    }
  }, 400);
}

  limpiarDatosPaciente(): void {
    this.frm.patchValue({
      fechaIngreso: null,
      apellidos: '',
      nombres: '',
      estadoCivil: null,
      edad: null,
      dni: '',
      enfPrevias: '',
      cirugiasPrevias: '',
      alergiasMedicamentos: ''
    });

  this.pacienteCargado = false;
}

  obtenerPacienteMock(nombre?: string, dni?: string) {
    const pacientes = [
    {
      nombreCompleto: 'Josefina Vera',
      dni: '74526981',
      apellidos: 'Mendoza Davalos',
      nombres: 'Josefina Vera',
      estadoCivil: 'SOLTERO',
      edad: 41,
      enfPrevias: 'Hipertensión',
      cirugiasPrevias: 'Apendicectomía',
      alergiasMedicamentos: 'Penicilina'
    },
    {
      nombreCompleto: 'Luis Alberto',
      dni: '42831659',
      apellidos: 'Quispe Huamán',
      nombres: 'Luis Alberto',
      estadoCivil: 'CASADO',
      edad: 29,
      enfPrevias: 'Ninguna',
      cirugiasPrevias: 'Ninguna',
      alergiasMedicamentos: 'Ninguna'
    }
  ];

  return pacientes.find(p =>
    (dni && p.dni === dni) ||
    (nombre && p.nombreCompleto.toLowerCase().includes(nombre.toLowerCase()))
    );
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

  get datosBusquedaCompleto(): boolean {
  const v = this.frm.getRawValue();

  return !!v.fechaIngreso &&
         !!v.apellidos &&
         !!v.nombres &&
         !!v.estadoCivil &&
         v.edad !== null &&
         !!v.dni;
}

  get antecedentesCompleto(): boolean {
    const v = this.frm.getRawValue();

    return !!v.enfPrevias &&
         !!v.cirugiasPrevias &&
         !!v.alergiasMedicamentos;
  }

  get puedeGuardar(): boolean {
    return this.datosBusquedaCompleto && this.antecedentesCompleto;
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

  guardar(): void {
  if (this.modo === 'ver') return;

  if (this.frm.invalid) {
    this.frm.markAllAsTouched();
    return;
  }

  this.servicioMensajesSwal
    .mensajePregunta('¿Está seguro de guardar la nueva historia clínica?')
    .then((response) => {
      if (response.isConfirmed) {
        const payload = {
          ...this.frm.getRawValue()
        };

        console.log('HC a guardar', payload);

        // Aquí luego llamas a tu servicio real
        // this.historiaClinicaService.guardar(payload).subscribe((res) => {
        //   this.servicioMensajesSwal.mensajeRegistroGuardado();
        // });
      }
    });
}
}