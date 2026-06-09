import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ConsultaService } from '../../services/consultas.service';
import { ActivatedRoute, Router } from '@angular/router';
import { MensajesSwalService } from '@app/shared/services/mensajes-swal.service';
import { CommonModule } from '@angular/common';
import { ButtonComponent, TableComponent } from '@app/shared/components';
import { ButtonModule } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { TabViewModule } from 'primeng/tabview';
import { IColumnasTabla } from '@app/shared/models/columnas';
import { IButton } from '@app/shared/components/table/models/table';
import { Observable } from 'rxjs';
import { IConsulta } from '../../models/consultas';
import { TooltipModule } from 'primeng/tooltip';
import { getHistoriasClinicas, getConsultas } from '@app/mocks/mocks';


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
    TableComponent
  ],
  templateUrl: './mantenimiento-consultas.component.html',
  styleUrl: './mantenimiento-consultas.component.scss'
})
export class MantenimientoConsultasComponent implements OnInit {
  activeIndex = 0;
  mostrarConfirmacion = false;
  isCargado: boolean = false;
  cols: IColumnasTabla[] = [];
  colsVisibles: IColumnasTabla[] = [];
  acciones: IButton[] = [];
  listaElementos: IConsulta[] = [];
  idHistoriaClinica!: number;
  nombrePaciente: string = '';
  historiaClinicaSeleccionada: any;

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

  ngOnInit(): void {
  this.idHistoriaClinica = Number(this.route.snapshot.paramMap.get('id'));

  this.cargarHistoriaClinica();
  this.getColumnasTabla();

  this.acciones = [
    {
      icono: 'pi pi-eye',
      clase: 'rounded',
      evento: 'ver',
      estado: true,
      tooltip: 'Ver consulta'
    },
  ];
}

cargarHistoriaClinica() {
  const historias = getHistoriasClinicas();

  this.historiaClinicaSeleccionada = historias.find(
    x => x.idHistoriaClinica === this.idHistoriaClinica
  );

  if (!this.historiaClinicaSeleccionada) {
    console.warn('No se encontró historia clínica con ID:', this.idHistoriaClinica);
    return;
  }

  this.nombrePaciente = `${this.historiaClinicaSeleccionada.nombres} ${this.historiaClinicaSeleccionada.apellidos}`;

  this.frm.patchValue({
    datos: {
      fechaIngreso: this.convertirFecha(this.historiaClinicaSeleccionada.fechaIngreso),
      dni: this.historiaClinicaSeleccionada.dni,
      apellidos: this.historiaClinicaSeleccionada.apellidos,
      nombres: this.historiaClinicaSeleccionada.nombres,
      estadoCivil: this.historiaClinicaSeleccionada.estadoCivil,
      edad: this.historiaClinicaSeleccionada.edad,
    },
    antecedentes: {
      alimentacion: this.historiaClinicaSeleccionada.alimentacion,
      habitos: this.historiaClinicaSeleccionada.habitos,
      vivienda: this.historiaClinicaSeleccionada.vivienda,
      desarrolloPsico: this.historiaClinicaSeleccionada.desarrolloPsicomotor,
      vacunas: this.historiaClinicaSeleccionada.vacunas,
      educacion: this.historiaClinicaSeleccionada.educacion,
      enfermedadesPrev: this.historiaClinicaSeleccionada.enfPrevias,
      cirugiasPrevias: this.historiaClinicaSeleccionada.cirugiasPrevias,
      alergiasMedicamentos: this.historiaClinicaSeleccionada.alergiasMedicamentos,
    }
  });

  this.cargarConsultasDelPaciente();
  this.frm.disable();

}

convertirFecha(fecha: string): Date | null {
  if (!fecha) return null;

  const partes = fecha.split('/');
  const dia = Number(partes[0]);
  const mes = Number(partes[1]) - 1;
  const anio = Number(partes[2]);

  return new Date(anio, mes, dia);
}


cargarConsultasDelPaciente() {
  const consultas = getConsultas();

  this.listaElementos = consultas.filter(
    x => x.dni === this.historiaClinicaSeleccionada.dni
  );

  this.isCargado = true;
}


  getColumnasTabla() {
    this.cols = [
      { field: 'id', header: 'ID', visibility: true, formatoFecha: '' },
      { field: 'paciente', header: 'Paciente', visibility: true, formatoFecha: '' },
      { field: 'dni', header: 'DNI', visibility: true, formatoFecha: '' },
      { field: 'especialidad', header: 'Especialidad Requerida', visibility: true, formatoFecha: '' },
      { field: 'doctor', header: 'Doctor Responsable', visibility: true, formatoFecha: '' },
      { field: 'fechaCreacion', header: 'Fecha creación', visibility: true, formatoFecha: '' },
    ];

    this.colsVisibles = this.cols.filter((x) => x.visibility == true);
  }


  getAllElementos() {
    const obs = new Observable<boolean>((observer) => {
      this.consultaService.getAllActivos().subscribe((resp) => {
        console.log('Elementos:', resp);
        this.listaElementos = resp;
        observer.next(true);
      });
    });

    obs.subscribe((res) => {
      if (res) {
        this.isCargado = res;
        this.getColumnasTabla();
      }
    });
  }

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

    eventoAccion(datos: any) {

    const { tipo, data } = datos;
    switch (tipo) {
      case 'ver':
        this.verElemento(data);
        break;

      default:
        console.log('Acción no aplicada');
        break;
    }
  }

  verElemento(data: any) {
  const id = data.id;

  this.router.navigate(
    ['consultas/lista-consultas/detalle', id],
    {
      queryParams: {
        idHistoriaClinica: this.idHistoriaClinica
      }
    }
  );
}
}
