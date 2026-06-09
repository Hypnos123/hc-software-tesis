import { CommonModule } from '@angular/common';
import { Component, OnInit  } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonComponent } from '@app/shared/components';
import { ButtonModule } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { DropdownModule } from 'primeng/dropdown';
import { FieldsetModule } from 'primeng/fieldset';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { ActivatedRoute, Router  } from '@angular/router';
import { getDetalleConsultas } from '@app/mocks/mocks';
import { DialogModule } from 'primeng/dialog';


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
    InputTextareaModule,
    DialogModule
  ],
  templateUrl: './detalle-consulta.component.html',
  styleUrl: './detalle-consulta.component.scss'
})
export class DetalleConsultaComponent implements OnInit {


  
  modo: string = 'ver';
  mostrarGuardar: boolean = false;
  mostrarConfirmacionGuardar: boolean = false;
  frm!: FormGroup;

  idConsulta!: number;
  idHistoriaClinica!: number;
  detalleConsultaSeleccionada: any;

  estadosCiviles: Opcion[] = [
    { label: 'Soltero(a)', value: 'SOLTERO' },
    { label: 'Casado(a)', value: 'CASADO' },
    { label: 'Divorciado(a)', value: 'DIVORCIADO' },
    { label: 'Viudo(a)', value: 'VIUDO' }
  ];

  constructor(private fb: FormBuilder, private route: ActivatedRoute, private router: Router) {
    

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
      proximaCita: ['', Validators.required],
    });
  }

  ngOnInit(): void {
  this.idConsulta = Number(this.route.snapshot.paramMap.get('id'));
  this.modo = this.route.snapshot.queryParamMap.get('modo') || 'ver';
  this.idHistoriaClinica = Number(this.route.snapshot.queryParamMap.get('idHistoriaClinica'));
  this.cargarDetalleConsulta();
}

regresar() {
  if (this.idHistoriaClinica) {
    this.router.navigateByUrl(`consultas/lista-consultas/${this.idHistoriaClinica}`);
  } else {
    this.router.navigateByUrl('consultas');
  }
}


cargarDetalleConsulta() {
  const detalles = getDetalleConsultas();

  this.detalleConsultaSeleccionada = detalles.find(
    x => x.id === this.idConsulta
  );

  if (!this.detalleConsultaSeleccionada) {
    console.warn('No se encontró detalle de consulta con ID:', this.idConsulta);
    return;
  }

  this.frm.patchValue({
    nombreCompleto: this.detalleConsultaSeleccionada.nombreCompleto,
    dni: this.detalleConsultaSeleccionada.dni,
    edad: this.detalleConsultaSeleccionada.edad,

    enfermedadesPrevias: this.detalleConsultaSeleccionada.enfermedadesPrevias,
    cirugiasPrevias: this.detalleConsultaSeleccionada.cirugiasPrevias,
    alergiasMedicamentos: this.detalleConsultaSeleccionada.alergiasMedicamentos,

    presion: this.detalleConsultaSeleccionada.presion,
    frecuenciaCardiaca: this.detalleConsultaSeleccionada.frecuenciaCardiaca,
    frecuenciaRespiratoria: this.detalleConsultaSeleccionada.frecuenciaRespiratoria,
    talla: this.detalleConsultaSeleccionada.talla,
    temperatura: this.detalleConsultaSeleccionada.temperatura,
    peso: this.detalleConsultaSeleccionada.peso,

    fechaConsulta: this.convertirFecha(this.detalleConsultaSeleccionada.fechaConsulta),
    tiempoEnfermedad: this.detalleConsultaSeleccionada.tiempoEnfermedad,
    tipoEnfermedad: this.detalleConsultaSeleccionada.tipoEnfermedad,
    relato: this.detalleConsultaSeleccionada.relato,

    diagnostico: this.detalleConsultaSeleccionada.diagnostico,
    examenesRecetados: this.detalleConsultaSeleccionada.examenesRecetados,
    receta: this.detalleConsultaSeleccionada.receta,
    tratamiento: this.detalleConsultaSeleccionada.tratamiento,
    proximaCita: this.convertirFecha(this.detalleConsultaSeleccionada.proximaCita),
  });

  // Modo solo visualización
  this.aplicarModoPantalla();
}



aplicarModoPantalla() {
  this.frm.disable();

  if (this.modo === 'atender') {
    this.mostrarGuardar = true;

    this.frm.get('diagnostico')?.enable();
    this.frm.get('examenesRecetados')?.enable();
    this.frm.get('receta')?.enable();
    this.frm.get('tratamiento')?.enable();
    this.frm.get('proximaCita')?.enable();
  } else {
    this.mostrarGuardar = false;
  }
}

convertirFecha(fecha: string): Date | null {
  if (!fecha) return null;

  const partes = fecha.split('/');
  const dia = Number(partes[0]);
  const mes = Number(partes[1]) - 1;
  const anio = Number(partes[2]);

  return new Date(anio, mes, dia);
}

abrirConfirmacionGuardar() {
  if (this.frm.invalid) {
    this.frm.markAllAsTouched();
    return;
  }

  this.mostrarConfirmacionGuardar = true;
}

confirmarGuardar() {
  this.mostrarConfirmacionGuardar = false;
  this.guardar();
}

cancelarGuardar() {
  this.mostrarConfirmacionGuardar = false;
}

  guardar() {
  if (this.frm.invalid) {
    this.frm.markAllAsTouched();
    return;
  }

  const payload = {
    ...this.frm.getRawValue()
  };

  console.log('Consulta a guardar', payload);
}
}
