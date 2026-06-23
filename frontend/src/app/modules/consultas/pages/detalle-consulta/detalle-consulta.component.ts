import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { FieldsetModule } from 'primeng/fieldset';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { ActivatedRoute, Router } from '@angular/router';
import { DialogModule } from 'primeng/dialog';
import { ConsultaService } from '../../services/consultas.service';
import { MensajesSwalService } from '@app/shared/services/mensajes-swal.service';

@Component({ selector: 'app-detalle-consulta', standalone: true, imports: [CommonModule, ReactiveFormsModule, FieldsetModule, InputTextModule, CalendarModule, ButtonModule, InputTextareaModule, DialogModule], templateUrl: './detalle-consulta.component.html', styleUrl: './detalle-consulta.component.scss' })
export class DetalleConsultaComponent implements OnInit {
  modo = 'ver'; mostrarGuardar = false; mostrarConfirmacionGuardar = false; frm!: FormGroup; idConsulta!: number;
  constructor(private fb: FormBuilder, private route: ActivatedRoute, private router: Router, private consultaService: ConsultaService, private swal: MensajesSwalService) {
    this.frm = this.fb.group({ nombreCompleto: [''], edad: [null], dni: [''], enfermedadesPrevias: [''], cirugiasPrevias: [''], alergiasMedicamentos: [''], presion: [''], frecuenciaCardiaca: [''], frecuenciaRespiratoria: [''], talla: [''], temperatura: [''], peso: [''], fechaConsulta: [null], tiempoEnfermedad: [''], tipoEnfermedad: [''], especialidadRequerida: [''], doctorResponsable: [''], relato: [''], diagnostico: ['', Validators.required], examenesRecetados: [''], receta: [''], tratamiento: ['', Validators.required], proximaCita: [null] });
  }
  ngOnInit(): void { this.idConsulta = Number(this.route.snapshot.paramMap.get('id')); this.modo = this.route.snapshot.queryParamMap.get('modo') || 'ver'; this.cargarDetalleConsulta(); }
  regresar() { this.router.navigateByUrl('consultas'); }
  cargarDetalleConsulta() { this.consultaService.getFindById(this.idConsulta).subscribe({ next: c => { if (!c) { this.swal.mensajeError('No tiene permiso para visualizar esta consulta o no existe.'); this.regresar(); return; } this.frm.patchValue({ nombreCompleto: [c.apellidos, c.nombres].filter(Boolean).join(' '), dni: c.numDocumento, edad: c.edad, enfermedadesPrevias: c.enfermedadesPrevias, cirugiasPrevias: c.cirugiasPrevias, alergiasMedicamentos: c.alergiaMedicamentos, presion: c.presionArterial, frecuenciaCardiaca: c.frecuenciaCardiaca, frecuenciaRespiratoria: c.frecuenciaRespiratoria, talla: c.talla, temperatura: c.temperatura, peso: c.peso, fechaConsulta: this.toDate(c.fechaConsulta), tiempoEnfermedad: c.tiempoEnfermedad, tipoEnfermedad: c.tipoEnfermedad, especialidadRequerida: c.especialidadRequerida, doctorResponsable: c.doctorResponsable, relato: c.relatoPaciente, diagnostico: c.diagnostico, examenesRecetados: c.examenesRecetados, receta: c.receta, tratamiento: c.tratamiento, proximaCita: this.toDate(c.proximaCita) }); if (this.modo === 'atender' && this.normalizarEstado(c.estado) !== 'PENDIENTE') { this.swal.mensajeError('La consulta ya fue atendida o no está pendiente.'); this.modo = 'ver'; } this.aplicarModoPantalla(); }, error: e => { this.swal.mensajeError(e?.error?.error || 'No se pudo cargar la consulta.'); this.regresar(); } }); }
  aplicarModoPantalla() { this.frm.disable(); this.mostrarGuardar = this.modo === 'atender'; if (this.mostrarGuardar) ['diagnostico','examenesRecetados','receta','tratamiento','proximaCita'].forEach(c => this.frm.get(c)?.enable()); }
  toDate(fecha: any): Date | null { return fecha ? new Date(fecha) : null; }
  abrirConfirmacionGuardar() { if (this.frm.invalid) { this.frm.markAllAsTouched(); return; } this.mostrarConfirmacionGuardar = true; }
  confirmarGuardar() { this.mostrarConfirmacionGuardar = false; this.guardar(); }
  cancelarGuardar() { this.mostrarConfirmacionGuardar = false; }
  guardar() { const raw = this.frm.getRawValue(); this.consultaService.finalizarAtencion(this.idConsulta, { diagnostico: raw.diagnostico, examenesRecetados: raw.examenesRecetados, receta: raw.receta, tratamiento: raw.tratamiento, proximaCita: raw.proximaCita }).subscribe({ next: r => { if (r.error) { this.swal.mensajeError(r.error); return; } this.swal.mensajeExito('Atención médica finalizada correctamente.'); this.regresar(); }, error: e => this.swal.mensajeError(e?.error?.error || 'No se pudo finalizar la atención.') }); }
  private normalizarEstado(e?: string): string { return (e ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim().toUpperCase().replace(/ /g, '_'); }
}
