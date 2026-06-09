import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';

import { HistoriaClinicaService } from '../../services/consultas.service';
import { INuevaConsultaRequest } from '../../models/historiaClinica';
import { MensajesSwalService } from '@app/shared/services/mensajes-swal.service';

@Component({
  selector: 'app-detalles-consultas',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    InputTextModule,
    InputTextareaModule,
    DropdownModule,
    CalendarModule,
    ButtonModule
  ],
  templateUrl: './detalles-consultas.component.html',
  styleUrl: './detalles-consultas.component.scss'
})
export class DetallesConsultasComponent implements OnInit {

  modo: 'ver' | 'nuevo' = 'ver';
  esNuevo = false;

  idHistoriaClinica!: number;
  frm!: FormGroup;

  tiposEnfermedad = [
    { label: 'Dolor Abdominal', value: 'Dolor Abdominal' },
    { label: 'Fiebre', value: 'Fiebre' },
    { label: 'Tos', value: 'Tos' },
    { label: 'Dolor de cabeza', value: 'Dolor de cabeza' }
  ];

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private historiaClinicaService: HistoriaClinicaService,
    private readonly servicioMensajesSwal: MensajesSwalService
  ) {}

  ngOnInit(): void {
    this.idHistoriaClinica = Number(this.route.snapshot.paramMap.get('id'));
    this.modo = (this.route.snapshot.paramMap.get('modo') as 'ver' | 'nuevo') || 'ver';
    this.esNuevo = this.modo === 'nuevo';

    this.initForm();

    if (this.esNuevo) {
      this.cargarDatosPaciente();
      this.deshabilitarDatosPaciente();
    } else {
      this.cargarDetalleConsulta();
      this.frm.disable();
    }
  }

  initForm(): void {
    this.frm = this.fb.group({
      nombreCompleto: [''],
      dni: [''],
      edad: [''],
      enfermedadesPrevias: [''],
      cirugiasPrevias: [''],
      alergiaMedicamentos: [''],
      presionArterial: [''],
      frecuenciaCardiaca: [''],
      frecuenciaRespiratoria: [''],
      talla: [''],
      temperatura: [''],
      peso: [''],
      fechaConsulta: [new Date()],
      tiempoEnfermedad: [''],
      tipoEnfermedad: [''],
      relatoPaciente: [''],
      diagnostico: [''],
      examenesRecetados: [''],
      receta: [''],
      tratamiento: [''],
      proximaCita: [null]
    });
  }

  cargarDatosPaciente(): void {
    this.frm.patchValue({
      nombreCompleto: 'Mendozaaa Davalos Josefina Vera',
      dni: '74526981',
      edad: '31',
      enfermedadesPrevias: 'Asma',
      cirugiasPrevias: 'No presenta',
      alergiaMedicamentos: 'No presenta',
      presionArterial: '',
      frecuenciaCardiaca: '',
      frecuenciaRespiratoria: '',
      talla: '',
      temperatura: '',
      peso: '',
      fechaConsulta: new Date(),
      tiempoEnfermedad: '',
      tipoEnfermedad: '',
      relatoPaciente: '',
      diagnostico: '',
      examenesRecetados: '',
      receta: '',
      tratamiento: '',
      proximaCita: null
    });
  }

  deshabilitarDatosPaciente(): void {
  this.frm.get('nombreCompleto')?.disable();
  this.frm.get('dni')?.disable();
  this.frm.get('edad')?.disable();

  this.frm.get('enfermedadesPrevias')?.disable();
  this.frm.get('cirugiasPrevias')?.disable();
  this.frm.get('alergiaMedicamentos')?.disable();
}

  cargarDetalleConsulta(): void {
    this.frm.patchValue({
      nombreCompleto: 'Mendozaaa Davalos Josefina Vera',
      dni: '74526981',
      edad: '31',
      enfermedadesPrevias: 'Asma',
      cirugiasPrevias: 'No presenta',
      alergiaMedicamentos: 'No presenta',
      presionArterial: '120/80',
      frecuenciaCardiaca: '61',
      frecuenciaRespiratoria: '20',
      talla: '1.65',
      temperatura: '36.5',
      peso: '72',
      fechaConsulta: new Date(),
      tiempoEnfermedad: '5 días',
      tipoEnfermedad: 'Dolor Abdominal',
      relatoPaciente: 'Paciente acude...',
      diagnostico: 'Gastritis',
      examenesRecetados: 'Tomografia abdominal',
      receta: 'Pastillas',
      tratamiento: 'Por la mañana',
      proximaCita: new Date()
    });
  }

  volver(): void {
    this.router.navigate(['/historiaClinica/ver-consultas', this.idHistoriaClinica]);
  }

  guardar(): void {
  this.servicioMensajesSwal
    .mensajePregunta('¿Está seguro de guardar la nueva consulta?')
    .then((response) => {
      if (response.isConfirmed) {
        const raw = this.frm.getRawValue();

        const request: INuevaConsultaRequest = {
          idHistoriaClinica: this.idHistoriaClinica,
          fechaConsulta: raw.fechaConsulta,
          tiempoEnfermedad: raw.tiempoEnfermedad,
          tipoEnfermedad: raw.tipoEnfermedad,
          relatoPaciente: raw.relatoPaciente,
          diagnostico: raw.diagnostico,
          examenesRecetados: raw.examenesRecetados,
          receta: raw.receta,
          tratamiento: raw.tratamiento,
          proximaCita: raw.proximaCita,
          presionArterial: raw.presionArterial,
          frecuenciaCardiaca: raw.frecuenciaCardiaca,
          frecuenciaRespiratoria: raw.frecuenciaRespiratoria,
          talla: raw.talla,
          temperatura: raw.temperatura,
          peso: raw.peso
        };

        console.log(request);

        // this.consultaService.insert(request).subscribe(() => {
        //   this.router.navigate(['/historiaClinica/ver-consultas', this.idHistoriaClinica]);
        // });

        this.router.navigate(['/historiaClinica/ver-consultas', this.idHistoriaClinica]);
      }
    });
}
}