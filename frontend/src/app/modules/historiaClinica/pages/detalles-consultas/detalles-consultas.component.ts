import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { HistoriaClinicaService } from '../../services/consultas.service';
import { INuevaConsultaRequest, IEmpleadoDoctor } from '../../models/historiaClinica';
import { MensajesSwalService } from '@app/shared/services/mensajes-swal.service';

@Component({ selector: 'app-detalles-consultas', standalone: true, imports: [CommonModule, ReactiveFormsModule, InputTextModule, InputTextareaModule, DropdownModule, CalendarModule, ButtonModule], templateUrl: './detalles-consultas.component.html', styleUrl: './detalles-consultas.component.scss' })
export class DetallesConsultasComponent implements OnInit {
  modo: 'ver' | 'nuevo' | 'editar' = 'ver';
  esNuevo = false;
  idHistoriaClinica!: number;
  idConsulta?: number;
  frm!: FormGroup;
  doctores: IEmpleadoDoctor[] = [];
  tiposEnfermedad = [
    { label: 'Respiratoria', value: 'RESPIRATORIA' }, { label: 'Digestiva', value: 'DIGESTIVA' }, { label: 'Cardiovascular', value: 'CARDIOVASCULAR' },
    { label: 'Neurológica', value: 'NEUROLOGICA' }, { label: 'Dermatológica', value: 'DERMATOLOGICA' }, { label: 'Musculoesquelética', value: 'MUSCULOESQUELETICA' },
    { label: 'Infecciosa', value: 'INFECCIOSA' }, { label: 'Alérgica', value: 'ALERGICA' }, { label: 'Urinaria', value: 'URINARIA' }, { label: 'Otra', value: 'OTRA' }
  ];
  especialidades = [
    { label: 'Medicina General', value: 'MEDICINA_GENERAL' }, { label: 'Pediatría', value: 'PEDIATRIA' }, { label: 'Ginecología', value: 'GINECOLOGIA' },
    { label: 'Cardiología', value: 'CARDIOLOGIA' }, { label: 'Dermatología', value: 'DERMATOLOGIA' }, { label: 'Traumatología', value: 'TRAUMATOLOGIA' },
    { label: 'Neurología', value: 'NEUROLOGIA' }, { label: 'Gastroenterología', value: 'GASTROENTEROLOGIA' }, { label: 'Otorrinolaringología', value: 'OTORRINOLARINGOLOGIA' }, { label: 'Otra', value: 'OTRA' }
  ];

  constructor(private fb: FormBuilder, private route: ActivatedRoute, private router: Router, private service: HistoriaClinicaService, private swal: MensajesSwalService) {}

  ngOnInit(): void {
    this.idHistoriaClinica = Number(this.route.snapshot.paramMap.get('id'));
    this.modo = (this.route.snapshot.paramMap.get('modo') as 'ver' | 'nuevo' | 'editar') || 'ver';
    this.idConsulta = Number(this.route.snapshot.queryParamMap.get('idConsulta')) || undefined;
    this.esNuevo = this.modo === 'nuevo';
    this.initForm();
    this.cargarDoctores();
    if (this.esNuevo) this.cargarDatosHistoria(); else if (this.idConsulta) this.cargarDetalleConsulta(this.idConsulta);
    if (this.modo === 'ver') this.frm.disable(); else this.deshabilitarDatosPaciente();
  }

  initForm(): void {
    this.frm = this.fb.group({
      nombreCompleto: [{ value: '', disabled: true }], dni: [{ value: '', disabled: true }], edad: [{ value: '', disabled: true }],
      enfermedadesPrevias: [{ value: '', disabled: true }], cirugiasPrevias: [{ value: '', disabled: true }], alergiaMedicamentos: [{ value: '', disabled: true }],
      presionArterial: ['', [Validators.maxLength(20)]], frecuenciaCardiaca: ['', [Validators.min(20), Validators.max(250)]], frecuenciaRespiratoria: ['', [Validators.min(5), Validators.max(80)]],
      talla: ['', [Validators.min(0.3), Validators.max(2.5)]], temperatura: ['', [Validators.min(30), Validators.max(45)]], peso: ['', [Validators.min(1), Validators.max(400)]],
      fechaConsulta: [new Date(), Validators.required], tiempoEnfermedad: ['', Validators.required], tipoEnfermedad: [null, Validators.required], especialidadRequerida: [null, Validators.required], idEmpleadoDoctor: [null, Validators.required], relatoPaciente: [''],
      diagnostico: [''], examenesRecetados: [''], receta: [''], tratamiento: [''], proximaCita: [null]
    });
  }

  cargarDoctores(): void { this.service.getDoctoresActivos().subscribe(d => this.doctores = d); }
  cargarDatosHistoria(): void { this.service.getById(this.idHistoriaClinica).subscribe(h => { if (!h) return; this.frm.patchValue({ nombreCompleto: [h.apellidos, h.nombres].filter(Boolean).join(' '), dni: h.numDocumento, edad: h.edad, enfermedadesPrevias: h.enfermedadesPrevias, cirugiasPrevias: h.cirugiasPrevias, alergiaMedicamentos: h.alergiaMedicamentos }); }); }
  deshabilitarDatosPaciente(): void { ['nombreCompleto','dni','edad','enfermedadesPrevias','cirugiasPrevias','alergiaMedicamentos'].forEach(c => this.frm.get(c)?.disable()); }
  cargarDetalleConsulta(id: number): void { this.service.getConsultaById(id).subscribe({ next: c => { if (!c) { this.swal.mensajeError('No tiene permiso para visualizar esta consulta o no existe.'); this.volver(); return; } this.frm.patchValue({ nombreCompleto: [c.apellidos, c.nombres].filter(Boolean).join(' '), dni: c.numDocumento, edad: c.edad, enfermedadesPrevias: c.enfermedadesPrevias, cirugiasPrevias: c.cirugiasPrevias, alergiaMedicamentos: c.alergiaMedicamentos, presionArterial: c.presionArterial, frecuenciaCardiaca: c.frecuenciaCardiaca, frecuenciaRespiratoria: c.frecuenciaRespiratoria, talla: c.talla, temperatura: c.temperatura, peso: c.peso, fechaConsulta: this.toDate(c.fechaConsulta), tiempoEnfermedad: c.tiempoEnfermedad, tipoEnfermedad: c.tipoEnfermedad, especialidadRequerida: c.especialidadRequerida, idEmpleadoDoctor: c.idEmpleadoDoctor, relatoPaciente: c.relatoPaciente, diagnostico: c.diagnostico, examenesRecetados: c.examenesRecetados, receta: c.receta, tratamiento: c.tratamiento, proximaCita: this.toDate(c.proximaCita) }); if (this.modo === 'ver') this.frm.disable(); else this.deshabilitarDatosPaciente(); }, error: () => { this.swal.mensajeError('No se pudo cargar la consulta. Inicia sesión nuevamente o verifica tus permisos.'); this.volver(); } }); }
  volver(): void { this.router.navigate(['/historiaClinica/ver-consultas', this.idHistoriaClinica]); }
  guardar(): void { if (this.modo === 'ver') return; if (this.frm.invalid) { this.frm.markAllAsTouched(); return; } this.swal.mensajePregunta(this.esNuevo ? '¿Está seguro de guardar la nueva consulta?' : '¿Está seguro de actualizar la consulta?').then(r => { if (!r.isConfirmed) return; const request = this.buildRequest(); const obs = this.esNuevo ? this.service.insertConsulta(request) : this.service.updateConsulta(this.idConsulta!, request); obs.subscribe({ next: () => { this.swal.mensajeExito(this.esNuevo ? 'Consulta registrada correctamente.' : 'Consulta actualizada correctamente.'); this.volver(); }, error: () => this.swal.mensajeError('No se pudo guardar la consulta.') }); }); }
  buildRequest(): INuevaConsultaRequest { const raw = this.frm.getRawValue(); return { idConsulta: this.idConsulta, idHistoriaClinica: this.idHistoriaClinica, fechaConsulta: raw.fechaConsulta, tiempoEnfermedad: raw.tiempoEnfermedad, tipoEnfermedad: raw.tipoEnfermedad, especialidadRequerida: raw.especialidadRequerida, idEmpleadoDoctor: raw.idEmpleadoDoctor, relatoPaciente: raw.relatoPaciente, diagnostico: raw.diagnostico, examenesRecetados: raw.examenesRecetados, receta: raw.receta, tratamiento: raw.tratamiento, proximaCita: raw.proximaCita, presionArterial: raw.presionArterial, frecuenciaCardiaca: raw.frecuenciaCardiaca, frecuenciaRespiratoria: raw.frecuenciaRespiratoria, talla: raw.talla, temperatura: raw.temperatura, peso: raw.peso }; }
  toDate(v: any): Date | null { return v ? new Date(v) : null; }
}
