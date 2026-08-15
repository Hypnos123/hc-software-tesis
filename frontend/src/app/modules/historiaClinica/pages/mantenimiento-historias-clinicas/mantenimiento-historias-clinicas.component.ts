import { Component, OnInit } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { ButtonComponent } from '@app/shared/components';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { FieldsetModule } from 'primeng/fieldset';
import { InputTextModule } from 'primeng/inputtext';
import { CalendarModule } from 'primeng/calendar';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { ActivatedRoute, Router } from '@angular/router';
import { MensajesSwalService } from '@app/shared/services/mensajes-swal.service';
import { HistoriaClinicaService } from '../../services/consultas.service';
import { IHistoriaClinicaCreateRequest, IHistoriaClinicaUpdateRequest } from '../../models/historiaClinica';
import { ClinicalHistoryTransferService } from '@app/shared/services/clinical-history-transfer.service';
import { ClinicalHistoryTransferCandidate } from '@app/shared/models/clinical-history-transfer';
import { ClinicalHistoryFlowFeedbackService } from '@app/shared/services/clinical-history-flow-feedback.service';

interface ClinicalHistoryNavigationState {
  source?: unknown;
  transferId?: unknown;
}

function noSoloEspacios(control: AbstractControl): ValidationErrors | null {
  return typeof control.value === 'string' && control.value.trim().length === 0
    ? { soloEspacios: true }
    : null;
}

function fechaNacimientoValida(control: AbstractControl): ValidationErrors | null {
  if (!control.value) return null;
  const fecha = control.value instanceof Date ? control.value : new Date(control.value);
  if (Number.isNaN(fecha.getTime())) return { fechaInvalida: true };

  const hoy = new Date();
  const fechaSinHora = new Date(fecha.getFullYear(), fecha.getMonth(), fecha.getDate());
  const hoySinHora = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate());
  return fechaSinHora > hoySinHora ? { fechaFutura: true } : null;
}

@Component({
  selector: 'app-mantenimiento-historias-clinicas',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FieldsetModule, ButtonComponent, InputTextModule, CalendarModule, DropdownModule, ButtonModule, InputTextareaModule],
  templateUrl: './mantenimiento-historias-clinicas.component.html',
  styleUrl: './mantenimiento-historias-clinicas.component.scss'
})
export class MantenimientoHistoriasClinicasComponent implements OnInit {
  frm: FormGroup;
  modo: 'nuevo' | 'ver' | 'editar' = 'nuevo';
  titulo = 'Nueva Historia Clinica';
  historiaId: number | null = null;
  historiaCargada = false;
  fechaMaximaNacimiento = new Date();
  readonly mensajeGuardadoManual = 'Revisa los datos ingresados. La historia clínica se guardará únicamente cuando pulses Guardar.';
  mensajePrecargaChatbot: string | null = null;
  mensajeErrorPrecargaChatbot: string | null = null;

  estadosCiviles = [
    { label: 'Soltero(a)', value: 'SOLTERO' },
    { label: 'Casado(a)', value: 'CASADO' },
    { label: 'Divorciado(a)', value: 'DIVORCIADO' },
    { label: 'Viudo(a)', value: 'VIUDO' }
  ];

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private swal: MensajesSwalService,
    private service: HistoriaClinicaService,
    private transferService: ClinicalHistoryTransferService,
    private location: Location,
    private feedbackService: ClinicalHistoryFlowFeedbackService
  ) {
    this.frm = this.fb.group({
      idHistoriaClinica: [{ value: '', disabled: true }],
      fechaIngreso: [{ value: null, disabled: true }, Validators.required],
      fechaNacimiento: [{ value: null, disabled: true }, [Validators.required, fechaNacimientoValida]],
      apellidos: [{ value: '', disabled: true }, [Validators.required, noSoloEspacios, Validators.maxLength(120)]],
      nombres: [{ value: '', disabled: true }, [Validators.required, noSoloEspacios, Validators.maxLength(120)]],
      estadoCivil: [{ value: null, disabled: true }, Validators.required],
      edad: [{ value: null, disabled: true }, [Validators.required, Validators.min(0)]],
      dni: [{ value: '', disabled: true }, [Validators.required, noSoloEspacios, Validators.pattern(/^\d{8}$/), Validators.maxLength(8)]],
      enfPrevias: [{ value: '', disabled: true }, Validators.maxLength(120)],
      cirugiasPrevias: [{ value: '', disabled: true }, Validators.maxLength(120)],
      alergiasMedicamentos: [{ value: '', disabled: true }, Validators.maxLength(120)]
    });
  }

  ngOnInit(): void {
    this.obtenerModo();
    this.inicializarCalculoEdad();

    if (this.modo === 'nuevo') {
      this.habilitarCapturaManual();
      this.intentarPrecargaDesdeChatbot();
      return;
    }

    this.historiaId = Number(this.route.snapshot.paramMap.get('id'));
    if (this.historiaId) this.cargarHistoria(this.historiaId);
    if (this.modo === 'ver') this.frm.disable();
  }

  obtenerModo(): void {
    const modoRuta = this.route.snapshot.paramMap.get('modo');
    this.modo = modoRuta === 'ver' || modoRuta === 'editar' ? modoRuta : 'nuevo';
    this.titulo = this.modo === 'ver'
      ? 'Visualizar Historia Clinica'
      : this.modo === 'editar'
        ? 'Editar Historia Clinica'
        : 'Nueva Historia Clinica';
  }

  private habilitarCapturaManual(): void {
    Object.entries(this.frm.controls)
      .filter(([nombreControl]) => nombreControl !== 'idHistoriaClinica' && nombreControl !== 'edad')
      .forEach(([, control]) => control.enable({ emitEvent: false }));
  }

  private inicializarCalculoEdad(): void {
    this.frm.get('fechaNacimiento')?.valueChanges.subscribe(fecha => {
      this.frm.get('edad')?.setValue(this.calcularEdad(fecha), { emitEvent: false });
    });
  }

  private intentarPrecargaDesdeChatbot(): void {
    const navigationState = (this.router.getCurrentNavigation()?.extras.state ?? window.history.state) as ClinicalHistoryNavigationState | null;
    if (navigationState?.source !== 'chatbot') return;

    const transferId = typeof navigationState.transferId === 'string' ? navigationState.transferId.trim() : '';
    if (!transferId) {
      this.mostrarErrorRecuperacionChatbot();
      this.limpiarEstadoNavegacionChatbot();
      return;
    }

    const transfer = this.transferService.consumeTransfer(transferId);
    this.limpiarEstadoNavegacionChatbot();
    if (!transfer || transfer.source !== 'chatbot' || !this.esCandidatoValido(transfer.candidate)) {
      this.mostrarErrorRecuperacionChatbot();
      return;
    }

    this.aplicarPrecargaChatbot(transfer.candidate);
  }

  private aplicarPrecargaChatbot(candidate: ClinicalHistoryTransferCandidate): void {
    this.frm.patchValue({
      fechaIngreso: this.toDate(candidate.fechaIngreso),
      fechaNacimiento: this.toDate(candidate.fechaNacimiento),
      apellidos: candidate.apellidos,
      nombres: candidate.nombres,
      estadoCivil: this.normalizarEstadoCivil(candidate.estadoCivil),
      dni: candidate.dni,
      enfPrevias: candidate.enfermedadesPrevias ?? '',
      cirugiasPrevias: candidate.cirugiasPrevias ?? '',
      alergiasMedicamentos: candidate.alergiaMedicamentos ?? ''
    });
    this.frm.get('dni')?.disable({ emitEvent: false });
    this.mensajePrecargaChatbot = 'Los datos del paciente fueron cargados desde el chatbot. Revísalos antes de guardar.';
    this.mensajeErrorPrecargaChatbot = null;
    this.feedbackService.emit('prefill-success');
  }

  private esCandidatoValido(candidate: ClinicalHistoryTransferCandidate | null | undefined): candidate is ClinicalHistoryTransferCandidate {
    if (!candidate || !candidate.idPaciente || !/^\d{8}$/.test(candidate.dni)) return false;
    if (!candidate.nombres?.trim() || !candidate.apellidos?.trim()) return false;
    if (!/^\d{4}-\d{2}-\d{2}$/.test(candidate.fechaIngreso) || !/^\d{4}-\d{2}-\d{2}$/.test(candidate.fechaNacimiento)) return false;
    return this.normalizarEstadoCivil(candidate.estadoCivil) !== null;
  }

  private normalizarEstadoCivil(estadoCivil: string): string | null {
    const normalizado = (estadoCivil ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim().toUpperCase();
    const value = normalizado.replace(/\(A\)$/, '');
    return this.estadosCiviles.some(estado => estado.value === value) ? value : null;
  }

  private mostrarErrorRecuperacionChatbot(): void {
    this.mensajePrecargaChatbot = null;
    this.mensajeErrorPrecargaChatbot = 'No fue posible recuperar los datos enviados por el chatbot. Puedes completar el formulario manualmente.';
    this.feedbackService.emit('prefill-failure');
  }

  private limpiarEstadoNavegacionChatbot(): void {
    const estadoActual = { ...(window.history.state ?? {}) };
    delete estadoActual.source;
    delete estadoActual.transferId;
    this.location.replaceState(this.router.url, '', estadoActual);
  }

  cargarHistoria(id: number): void {
    this.service.getById(id).subscribe(historia => {
      if (!historia) return;

      this.historiaCargada = true;
      this.frm.patchValue({
        idHistoriaClinica: historia.idHistoriaClinica,
        fechaIngreso: this.toDate(historia.fechaIngreso),
        fechaNacimiento: this.toDate(historia.fechaNacimiento),
        apellidos: historia.apellidos,
        nombres: historia.nombres,
        estadoCivil: historia.estadoCivil,
        edad: this.calcularEdad(historia.fechaNacimiento),
        dni: historia.numDocumento,
        enfPrevias: historia.enfermedadesPrevias,
        cirugiasPrevias: historia.cirugiasPrevias,
        alergiasMedicamentos: historia.alergiaMedicamentos
      });
      if (this.modo === 'editar') this.habilitarEdicion();
    });
  }

  private habilitarEdicion(): void {
    const controlesEditables = [
      'fechaIngreso', 'fechaNacimiento', 'apellidos', 'nombres', 'estadoCivil',
      'enfPrevias', 'cirugiasPrevias', 'alergiasMedicamentos'
    ];
    controlesEditables.forEach(nombre => this.frm.get(nombre)?.enable({ emitEvent: false }));
  }

  guardar(): void {
    if (this.modo === 'ver') return;

    if (this.modo === 'nuevo') {
      this.frm.markAllAsTouched();
      if (this.frm.invalid) return;
      this.confirmarCreacion();
      return;
    }

    if (!this.historiaId) return;

    this.frm.markAllAsTouched();
    if (this.frm.invalid) return;

    this.swal.mensajePregunta('Los datos de la historia clínica se modificara. ¿Desea continuar?').then(resultado => {
      if (!resultado.isConfirmed) return;
      this.service.update(this.historiaId!, this.crearRequestActualizacion()).subscribe({
        next: respuesta => {
          if (!respuesta.idGenerado) {
            this.swal.mensajeError('El servidor no confirmó la actualización de la historia clínica.');
            return;
          }
          this.finalizar(respuesta.mensaje);
        },
        error: error => this.swal.mensajeError(this.obtenerMensajeError(error))
      });
    });
  }

  private confirmarCreacion(): void {
    this.swal.mensajePregunta('¿Está seguro de guardar la nueva historia clínica?').then(resultado => {
      if (!resultado.isConfirmed) return;
      this.service.insert(this.crearRequest()).subscribe({
        next: respuesta => {
          if (!respuesta.idGenerado) {
            this.swal.mensajeError('El servidor no confirmó la creación de la historia clínica.');
            return;
          }
          this.finalizar(respuesta.mensaje);
        },
        error: error => this.swal.mensajeError(this.obtenerMensajeError(error))
      });
    });
  }

  private crearRequest(): IHistoriaClinicaCreateRequest {
    const datos = this.frm.getRawValue();
    return {
      fechaIngreso: this.formatearFechaSinZona(datos.fechaIngreso),
      fechaNacimiento: this.formatearFechaSinZona(datos.fechaNacimiento),
      apellidos: datos.apellidos.trim(),
      nombres: datos.nombres.trim(),
      estadoCivil: datos.estadoCivil,
      dni: datos.dni.trim(),
      enfermedadesPrevias: datos.enfPrevias?.trim() || undefined,
      cirugiasPrevias: datos.cirugiasPrevias?.trim() || undefined,
      alergiaMedicamentos: datos.alergiasMedicamentos?.trim() || undefined
    };
  }

  private crearRequestActualizacion(): IHistoriaClinicaUpdateRequest {
    const datos = this.frm.getRawValue();
    return {
      fechaIngreso: this.formatearFechaSinZona(datos.fechaIngreso),
      fechaNacimiento: this.formatearFechaSinZona(datos.fechaNacimiento),
      apellidos: datos.apellidos.trim(),
      nombres: datos.nombres.trim(),
      estadoCivil: datos.estadoCivil,
      enfermedadesPrevias: datos.enfPrevias?.trim() || undefined,
      cirugiasPrevias: datos.cirugiasPrevias?.trim() || undefined,
      alergiaMedicamentos: datos.alergiasMedicamentos?.trim() || undefined
    };
  }

  private obtenerMensajeError(error: any): string {
    if (error?.status === 400) return error?.error?.mensaje || 'Los datos ingresados no son válidos.';
    if (error?.status === 404) return error?.error?.mensaje || 'No se encontró el registro solicitado.';
    if (error?.status === 409 && error?.error?.codigo === 'DNI_AMBIGUO') {
      return '<strong>No es posible crear la historia clínica.</strong><br><br>' +
        'Se detectaron pacientes duplicados asociados a este DNI y uno de los registros ya cuenta con una historia clínica.<br><br>' +
        'Antes de continuar, debes revisar los pacientes duplicados y definir cuál registro se conservará.<br><br>' +
        'El Chatbot puede ayudarte a gestionar este problema.';
    }
    if (error?.status === 409) return error?.error?.mensaje || 'El DNI está asociado a varios pacientes y no se puede resolver automáticamente.';
    return error?.error?.mensaje || error?.error?.error || 'No se pudo crear la historia clínica.';
  }

  mostrarError(nombreControl: string, error?: string): boolean {
    const control = this.frm.get(nombreControl);
    if (!control || !(control.touched || control.dirty) || !control.errors) return false;
    return error ? control.hasError(error) : true;
  }

  finalizar(mensaje?: string): void {
    this.swal.mensajeExito(mensaje || 'Operación realizada correctamente.');
    this.router.navigate(['/historiaClinica']);
  }

  toDate(fecha: unknown): Date | null {
    if (!fecha) return null;
    if (fecha instanceof Date) return fecha;
    if (typeof fecha === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(fecha)) {
      const [anio, mes, dia] = fecha.split('-').map(Number);
      return new Date(anio, mes - 1, dia);
    }
    return new Date(fecha as string | number);
  }

  private formatearFechaSinZona(valor: unknown): string {
    const fecha = this.toDate(valor);
    if (!fecha || Number.isNaN(fecha.getTime())) return '';
    const anio = fecha.getFullYear();
    const mes = String(fecha.getMonth() + 1).padStart(2, '0');
    const dia = String(fecha.getDate()).padStart(2, '0');
    return `${anio}-${mes}-${dia}`;
  }

  calcularEdad(fecha: unknown): number | undefined {
    const nacimiento = this.toDate(fecha);
    if (!nacimiento || Number.isNaN(nacimiento.getTime())) return undefined;
    const hoy = new Date();
    if (nacimiento > hoy) return undefined;
    let edad = hoy.getFullYear() - nacimiento.getFullYear();
    if (hoy.getMonth() < nacimiento.getMonth() || (hoy.getMonth() === nacimiento.getMonth() && hoy.getDate() < nacimiento.getDate())) edad--;
    return edad;
  }
}
