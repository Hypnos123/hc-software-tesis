import { Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { IEmpleado } from '../../models/empleado';
import { EmpleadoService } from '../../services/empleado.service';
import { PATTERNS } from '@app/global/pattern';
import { IColumnasTabla } from '@app/shared/models/columnas';
import { MensajesSwalService } from '@app/shared/services/mensajes-swal.service';
import { documentoValidator } from '@app/shared/validators/validators';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonComponent } from '@app/shared/components';
import { CommonModule } from '@angular/common';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-mantenimiento-empleado',
  templateUrl: './mantenimiento-empleado.component.html',
  styleUrls: ['./mantenimiento-empleado.component.scss'],
  imports: [CommonModule, DropdownModule, ButtonComponent, ReactiveFormsModule],
  standalone: true
})
export class MantenimientoEmpleadoComponent implements OnInit {

  isGuardar: boolean = false;
  titulo: string = 'Crear Empleado';
  id!: string;
  isEditar: boolean = false;

  colsEmpleado: IColumnasTabla[] = [];
  colsEmpleadoVisibles: IColumnasTabla[] = [];
  listaElementos: IEmpleado[] = [];

  tipoDocumentos: any[] = [];
  tipoCargos: any[] = [];
  listaCargos: any[] = [];

  constructor(
    private fb: FormBuilder,
    private serviceEmpleado: EmpleadoService,
    private router: Router,
    private _ActivatedRoute: ActivatedRoute,
    private servicioMensajesSwal: MensajesSwalService
  ) { }

  empleadoForm = this.fb.group({
    tipoDocumento: [null, [Validators.required]],
    numDocumento: ['', [Validators.required, documentoValidator()]],
    nombre: [null, [Validators.required]],
    apellido: [null, [Validators.required]],
    direccion: [null, [Validators.required]],
    telefono: [null, [Validators.required, Validators.minLength(6), Validators.maxLength(6)]],
    celular: [null, [Validators.required, Validators.pattern(PATTERNS.CELULAR)]],
    cargo: [null, [Validators.required]],
  });

  ngOnInit(): void {
    this.empleadoForm.get('tipoDocumento')?.valueChanges.subscribe(() => {
      this.empleadoForm.get('numDocumento')?.updateValueAndValidity();
    });

    const id = this._ActivatedRoute.snapshot.paramMap.get('id');
    if (id) {
      this.titulo = 'Editar Empleado';
      this.id = id;
      this.isEditar = true;
      this.buscarIdElemento();
    }

    this.listarDropdown();
    this.tipoDocumento?.setValue(this.tipoDocumentos[0]);
  }

  get tipoDocumento() {
    return this.empleadoForm.get('tipoDocumento');
  }

  get numDocumento() {
    return this.empleadoForm.get('numDocumento');
  }

  get nombre() {
    return this.empleadoForm.get('nombre');
  }

  get apellido() {
    return this.empleadoForm.get('apellido');
  }

  get direccion() {
    return this.empleadoForm.get('direccion');
  }

  get telefono() {
    return this.empleadoForm.get('telefono');
  }

  get celular() {
    return this.empleadoForm.get('celular');
  }

  get cargo() {
    return this.empleadoForm.get('cargo');
  }

  listarDropdown() {
    this.tipoDocumentos = [
      {
        tipo: 'DNI',
      },
      {
        tipo: 'CE',
      },
    ];

    this.tipoCargos = [
      {
        tipo: 'Doctor',
      },
      {
        tipo: 'Enfermera(o)',
      },
      {
        tipo: 'Administrador',
      },
      {
        tipo: 'Otros',
      },
    ];
  }

  guardarElemento() {
    if (this.empleadoForm.invalid) {
    this.empleadoForm.markAllAsTouched();
    return;
  }

    const {
      tipoDocumento,
      numDocumento,
      nombre,
      apellido,
      direccion,
      telefono,
      celular,
      cargo,
    } = this.empleadoForm.value;

    const params: IEmpleado = {
      tipoDocumento: (tipoDocumento as any).tipo,
      numDocumento: numDocumento ?? '',
      nombre: nombre,
      apellido: apellido,
      direccion: direccion,
      telefono: telefono,
      celular: celular,
      cargo: (cargo as any).tipo,
    };

    const titulo = this.isEditar ? '¿Actualizar empleado?' : '¿Guardar empleado?';
    const texto = this.isEditar
    ? 'Se guardarán los cambios del empleado.'
    : 'Se registrará un nuevo empleado.';

    Swal.fire({
    title: titulo,
    text: texto,
    icon: 'question',
    showCancelButton: true,
    confirmButtonText: 'Sí, confirmar',
    cancelButtonText: 'Cancelar',
    confirmButtonColor: '#1179c4',
    cancelButtonColor: '#6c757d',
    reverseButtons: true,
  }).then((result) => {
    if (!result.isConfirmed) return;

    if (this.isEditar) {
      this.editarElemento(params);
    } else {
      this.crearElemento(params);
    }
  });
  }





  crearElemento(params: IEmpleado) {
  this.serviceEmpleado.insert(params).subscribe({
    next: (response) => {
      if (response) {
        Swal.fire({ icon: 'success', title: 'Guardado', timer: 1200, showConfirmButton: false });
        this.router.navigateByUrl('/empleados');
      }
    },
    error: () => Swal.fire({ icon: 'error', title: 'Error', text: 'No se pudo guardar.' })
  });
}

  editarElemento(params: IEmpleado) {
    this.serviceEmpleado
      .update(+this.id, params)
      .subscribe((response) => {
        if (response) this.router.navigateByUrl('/empleados');
      });
  }

  buscarIdElemento() {
    this.serviceEmpleado.getFindById(+this.id).subscribe((res) => {
      const resultado = res[0];
      this.mostrarValoresInput(resultado);
    });
  }


  mostrarValoresInput(resultado: any) {
    const tipoDocumento = this.tipoDocumentos.find((e) => e.tipo === resultado.tipoDocumento);
    const cargo = this.tipoCargos.find((e) => e.tipo === resultado.cargo);

    this.empleadoForm.patchValue({
      tipoDocumento: tipoDocumento,
      numDocumento: resultado.numDocumento,
      nombre: resultado.nombre,
      apellido: resultado.apellido,
      direccion: resultado.direccion,
      telefono: resultado.telefono,
      celular: resultado.celular,
      cargo: cargo,
    });
  }
}
