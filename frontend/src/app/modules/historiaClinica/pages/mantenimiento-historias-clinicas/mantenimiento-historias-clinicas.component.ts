import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonComponent } from '@app/shared/components';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { FieldsetModule } from 'primeng/fieldset';
import { InputTextModule } from 'primeng/inputtext';
import { CalendarModule } from 'primeng/calendar';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { ActivatedRoute, Router } from '@angular/router';
import { MensajesSwalService } from '@app/shared/services/mensajes-swal.service';
import { Subject, debounceTime, distinctUntilChanged, filter, switchMap, takeUntil, tap } from 'rxjs';
import { HistoriaClinicaService } from '../../services/consultas.service';
import { IHistoriaClinica, IPacienteBusqueda } from '../../models/historiaClinica';

@Component({ selector: 'app-mantenimiento-historias-clinicas', standalone: true, imports: [CommonModule, ReactiveFormsModule, FieldsetModule, ButtonComponent, InputTextModule, CalendarModule, DropdownModule, ButtonModule, InputTextareaModule], templateUrl: './mantenimiento-historias-clinicas.component.html', styleUrl: './mantenimiento-historias-clinicas.component.scss' })
export class MantenimientoHistoriasClinicasComponent implements OnInit, OnDestroy {
  frm: FormGroup; modo: 'nuevo' | 'ver' | 'editar' = 'nuevo'; titulo = 'Nueva Historia Clinica'; historiaId: number | null = null; pacienteSeleccionado?: IPacienteBusqueda; pacienteCargado = false;
  sugerenciasNombre: IPacienteBusqueda[] = []; sugerenciasDni: IPacienteBusqueda[] = []; activeNombre = 0; activeDni = 0;
  private nombre$ = new Subject<string>(); private dni$ = new Subject<string>(); private destroy$ = new Subject<void>();
  estadosCiviles = [{ label: 'Soltero(a)', value: 'SOLTERO' }, { label: 'Casado(a)', value: 'CASADO' }, { label: 'Divorciado(a)', value: 'DIVORCIADO' }, { label: 'Viudo(a)', value: 'VIUDO' }];
  constructor(private fb: FormBuilder, private route: ActivatedRoute, private router: Router, private swal: MensajesSwalService, private service: HistoriaClinicaService) {
    this.frm = this.fb.group({ idHistoriaClinica: [{ value: '', disabled: true }], nombrePacienteSel: [''], dniSel: [''], fechaIngreso: [{ value: null, disabled: true }], apellidos: [{ value: '', disabled: true }], nombres: [{ value: '', disabled: true }], estadoCivil: [{ value: null, disabled: true }], edad: [{ value: null, disabled: true }], dni: [{ value: '', disabled: true }], enfPrevias: [{ value: '', disabled: true }], cirugiasPrevias: [{ value: '', disabled: true }], alergiasMedicamentos: [{ value: '', disabled: true }] });
  }
  ngOnInit(): void { this.obtenerModo(); this.configurarBusquedas(); if (this.modo !== 'nuevo') { this.historiaId = Number(this.route.snapshot.paramMap.get('id')); if (this.historiaId) this.cargarHistoria(this.historiaId); } if (this.modo === 'ver') this.frm.disable(); }
  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }
  obtenerModo(): void { const m = this.route.snapshot.paramMap.get('modo'); this.modo = m === 'ver' || m === 'editar' ? m : 'nuevo'; this.titulo = this.modo === 'ver' ? 'Visualizar Historia Clinica' : this.modo === 'editar' ? 'Editar Historia Clinica' : 'Nueva Historia Clinica'; }
  configurarBusquedas(): void { this.nombre$.pipe(tap(() => this.limpiarSeleccion()), debounceTime(400), distinctUntilChanged(), filter(v => (v || '').trim().length >= 2), switchMap(v => this.service.buscarPacientesPorNombre(v.trim())), takeUntil(this.destroy$)).subscribe(r => { this.sugerenciasNombre = r; this.activeNombre = 0; }); this.dni$.pipe(tap(() => this.limpiarSeleccion()), debounceTime(400), distinctUntilChanged(), filter(v => (v || '').trim().length >= 2), switchMap(v => this.service.buscarPacientesPorDni(v.trim())), takeUntil(this.destroy$)).subscribe(r => { this.sugerenciasDni = r; this.activeDni = 0; }); }
  onNombreInput(v: string): void { if (this.modo !== 'nuevo') return; this.sugerenciasDni = []; this.nombre$.next(v); }
  onDniInput(v: string): void { if (this.modo !== 'nuevo') return; this.sugerenciasNombre = []; this.dni$.next(v); }
  seleccionarPaciente(p: IPacienteBusqueda): void { this.pacienteSeleccionado = p; this.pacienteCargado = true; this.sugerenciasNombre = []; this.sugerenciasDni = []; this.frm.patchValue({ nombrePacienteSel: this.nombreCompleto(p), dniSel: p.numDocumento, fechaIngreso: this.toDate(p.fechaIngreso), apellidos: p.apellidos, nombres: p.nombres, estadoCivil: p.estadoCivil, edad: p.edad ?? this.calcularEdad(p.fechaNacimiento), dni: p.numDocumento }); this.service.getAntecedentesByPaciente(p.idPaciente!).subscribe(a => this.frm.patchValue({ enfPrevias: a?.enfermedadesPrevias ?? '', cirugiasPrevias: a?.cirugiasPrevias ?? '', alergiasMedicamentos: a?.alergiaMedicamentos ?? '' })); }
  seleccionarActiva(lista: IPacienteBusqueda[], index: number): void { if (lista[index]) this.seleccionarPaciente(lista[index]); }
  limpiarSeleccion(): void { if (this.pacienteSeleccionado && this.modo === 'nuevo') { this.pacienteSeleccionado = undefined; this.pacienteCargado = false; } }
  cargarHistoria(id: number): void { this.service.getById(id).subscribe(h => { if (!h) return; this.pacienteCargado = true; this.frm.patchValue({ idHistoriaClinica: h.idHistoriaClinica, nombrePacienteSel: [h.apellidos, h.nombres].filter(Boolean).join(' '), dniSel: h.numDocumento, fechaIngreso: this.toDate(h.fechaIngreso), apellidos: h.apellidos, nombres: h.nombres, estadoCivil: h.estadoCivil, edad: h.edad ?? this.calcularEdad(h.fechaNacimiento), dni: h.numDocumento, enfPrevias: h.enfermedadesPrevias, cirugiasPrevias: h.cirugiasPrevias, alergiasMedicamentos: h.alergiaMedicamentos }); }); }
  guardar(): void { if (this.modo === 'ver') return; if (this.modo === 'nuevo' && !this.pacienteSeleccionado?.idPaciente) { this.swal.mensajeAdvertencia('Debe seleccionar un paciente de la lista.'); return; } this.swal.mensajePregunta(this.modo === 'editar' ? '¿Está seguro de guardar los cambios?' : '¿Está seguro de guardar la nueva historia clínica?').then(r => { if (!r.isConfirmed) return; if (this.modo === 'nuevo') { const idPaciente = this.pacienteSeleccionado!.idPaciente!; this.service.getByPaciente(idPaciente).subscribe(existente => { if (existente) { this.swal.mensajeAdvertencia('El paciente seleccionado ya cuenta con una historia clínica.'); return; } this.service.insert({ idPaciente }).subscribe(res => this.finalizar(res.mensaje)); }); } else if (this.historiaId) { this.service.update(this.historiaId, {}).subscribe(res => this.finalizar(res.mensaje)); } }); }
  finalizar(mensaje?: string): void { this.swal.mensajeExito(mensaje || 'Operación realizada correctamente.'); this.router.navigate(['/historiaClinica']); }
  nombreCompleto(p: IPacienteBusqueda): string { return [p.apellidos, p.nombres].filter(Boolean).join(' '); }
  toDate(f: any): Date | null { return f ? new Date(f) : null; }
  calcularEdad(f: any): number | undefined { const d = this.toDate(f); if (!d) return undefined; const h = new Date(); let e = h.getFullYear() - d.getFullYear(); if (h.getMonth() < d.getMonth() || (h.getMonth() === d.getMonth() && h.getDate() < d.getDate())) e--; return e; }
}
