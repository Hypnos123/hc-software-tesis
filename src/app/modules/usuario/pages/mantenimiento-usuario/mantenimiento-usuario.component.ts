import { Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { IEmpleado } from '@app/modules/empleado/models/empleado';
import { EmpleadoService } from '@app/modules/empleado/services/empleado.service';
import { IColumnasTabla } from '@app/shared/models/columnas';
import { Observable } from 'rxjs';
import { ITipoUsuario, IMenu, IUsuario, IDetallePermiso } from '../../models/usuario';
import { UsuarioService } from '../../services/usuario.service';
import { DropdownModule } from 'primeng/dropdown';
import { PickListModule } from 'primeng/picklist';
import { ButtonComponent } from '@app/shared/components';
import { DialogModule } from 'primeng/dialog';
import { TableModule } from 'primeng/table';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

@Component({
  selector: 'app-mantenimiento-usuario',
  templateUrl: './mantenimiento-usuario.component.html',
  styleUrls: ['./mantenimiento-usuario.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    DropdownModule,
    PickListModule,
    ReactiveFormsModule,
    ButtonModule,
    DialogModule,
    TableModule,
    InputTextModule,
    ButtonComponent
  ]
})
export class MantenimientoUsuarioComponent implements OnInit {

  isGuardar: boolean = false;
  titulo: string = 'Crear Usuario';
  id!: string;
  isEditar: boolean = false;

  listaTipoUsuario: ITipoUsuario[] = [];
  isDNIEmpleadoInvalid: boolean = false;
  isBuscadorDeEmpleado: boolean = false;

  colsEmpleado: IColumnasTabla[] = [];
  colsEmpleadoVisibles: IColumnasTabla[] = [];
  listaEmpleados: IEmpleado[] = [];
  rowSeleccionado!: IEmpleado;

  listaPorAsignar: IMenu[] = [];
  listaAsignados: IMenu[] = [];

  disabledCaja: boolean = false;
  idUsuario!: number;

  constructor(
    private fb: FormBuilder,
    private serviceUsuario: UsuarioService,
    private serviceEmpleado: EmpleadoService,
    private router: Router,
    private _ActivatedRoute: ActivatedRoute,
  ) { }

  usuarioForm = this.fb.group({
    usuario: ['', [Validators.required, Validators.maxLength(20)]],
    contrasena: ['', [Validators.required, Validators.maxLength(20)]],
    tipoUsuario: [{} as ITipoUsuario, [Validators.required]],
    empleado: [{} as IEmpleado],
    numEmpleado: ['', [Validators.required]],
    datosEmpleado: [{ value: '', disabled: true }],
  });

  ngOnInit(): void {
    this.getMenus();

    const id = this._ActivatedRoute.snapshot.paramMap.get('id');
    if (id) {
      this.titulo = 'Editar Usuario';
      this.id = id;
      this.isEditar = true;

      this.serviceUsuario.getFindById(+this.id).subscribe((resultado) => {
        this.mostrarValoresInput(resultado[0]);
      });
    }

    this.listaTipoUsuario = [
      { tipoUsuario: 'ADMINISTRADOR' },
      { tipoUsuario: 'DOCTOR' },
      { tipoUsuario: 'ENFERMERO' }
    ]

  }

  get usuario() {
    return this.usuarioForm.get('usuario');
  }

  get contrasena() {
    return this.usuarioForm.get('contrasena');
  }

  get tipoUsuario() {
    return this.usuarioForm.get('tipoUsuario');
  }

  get descripcion() {
    return this.usuarioForm.get('descripcion');
  }

  get numEmpleado() {
    return this.usuarioForm.get('numEmpleado');
  }

  getMenus() {
    this.getEmpleados();
    this.serviceUsuario.getMenuAllActive().subscribe(res => {
      if (this.isEditar) {
        this.listaPorAsignar = res;
        this.buscarIdDetallePermiso().subscribe((resultado: any) => {
          resultado.forEach((e: any) => {
            this.listaPorAsignar = this.listaPorAsignar.filter((el) => el.idMenu != e.idMenu);
          });
        })
      } else {
        this.listaPorAsignar = res;
      }
    })
  }

  guardarElemento() {
    const { usuario, contrasena, tipoUsuario, empleado } = this.usuarioForm.value;
    const params: IUsuario = {
      contrasena: contrasena,
      idEmpleado: (empleado as any).idEmpleado,
      tipoUsuario: (tipoUsuario as any).tipoUsuario,
      usuario: usuario,
    };

    if (this.isEditar) {
      this.editarElemento(params);
    } else {
      this.crearElemento(params).subscribe((idUsuario: any) => {
        if (idUsuario) {
          this.guardarDetallePermiso(idUsuario);
        }
      });
    }
  }

  guardarDetallePermiso(idUsuario: number) {
    let listaDetallePermiso: IDetallePermiso[] = [];

    this.listaAsignados.forEach(res => {
      const detallePermiso: IDetallePermiso = {
        idMenu: res.idMenu,
        idUsuario: idUsuario
      }

      listaDetallePermiso.push(detallePermiso);
    });

    if (this.isEditar) {
      this.editarDetallePermiso(listaDetallePermiso);
    } else {
      this.crearDetallePermiso(listaDetallePermiso);
    }

  }

  crearDetallePermiso(params: IDetallePermiso[]) {
    this.serviceUsuario.insertDetallePermiso(params).subscribe((response) => {
      if (response) this.router.navigateByUrl('/usuarios');
    });
  }

  editarDetallePermiso(params: IDetallePermiso[]) {
    this.serviceUsuario.updateDetallePermiso(+this.id, params).subscribe((res) => {
      this.router.navigateByUrl('/usuarios');
    });
  }


  crearElemento(params: IUsuario) {
    const obs = new Observable((observer) => {
      this.serviceUsuario
        .insert(params)
        .subscribe((response: IUsuario) => {
          if (response.idGenerado) {
            this.idUsuario = response.idGenerado as number;
            observer.next(this.idUsuario);
          }
        });
    });

    return obs;

  }

  editarElemento(params: IUsuario) {
    this.serviceUsuario
      .update(+this.id, params)
      .subscribe((response) => {
        if (!response) return

        if (response) {
          this.guardarDetallePermiso(+this.id);
        }
      });
  }

  buscarIdElemento() {
    this.serviceUsuario.getFindById(+this.id).subscribe((res) => {
      const resultado = res;
      this.mostrarValoresInput(resultado[0]);
    });
  }

  buscarIdDetallePermiso() {
    const obs = new Observable((observer) => {
      this.serviceUsuario.getFindByIdDetallePermiso(+this.id).subscribe((res) => {
        const resultado = res;
        observer.next(resultado);
        this.mostrarPermisosAsignados(resultado);
      });

    });

    return obs;
  }

  mostrarValoresInput(resultado: any) {
    const tipoUsuario = this.listaTipoUsuario.find((e => e.tipoUsuario === resultado.tipoUsuario));
    const empleado = this.listaEmpleados.find((e => e.idEmpleado === resultado.idEmpleado)) as IEmpleado;

    this.usuarioForm.patchValue({
      usuario: resultado.usuario,
      contrasena: resultado.contrasena,
      tipoUsuario: tipoUsuario,
      empleado: empleado,
      numEmpleado: empleado?.numDocumento,
      datosEmpleado: `${empleado?.nombre} ${empleado?.apellido}`,
    });
  }

  mostrarPermisosAsignados(resultado: any) {
    this.listaAsignados = resultado;
  }



  showBuscadorDeEmpleado(event: any) {
    this.isBuscadorDeEmpleado = true;
    this.getColumnasTablaEmpleado();
  }


  getColumnasTablaEmpleado() {
    this.colsEmpleado = [
      { field: 'tipoDocumento', header: 'Tipo de Documento', visibility: true, formatoFecha: '' },
      { field: 'numDocumento', header: 'Nro de Documento', visibility: true, formatoFecha: '' },
      { field: 'nombre', header: 'Nombres', visibility: true, formatoFecha: '' },
      { field: 'apellido', header: 'Apellidos', visibility: true, formatoFecha: '' },
    ];

    this.colsEmpleadoVisibles = this.colsEmpleado.filter(
      (x) => x.visibility == true
    );
  }

  getEmpleados(): void {
    this.serviceEmpleado.getAllActivos().subscribe((res) => {
      this.listaEmpleados = res;
    })
  }

  buscarEmpleado(event: any) {
    this.isDNIEmpleadoInvalid = false;
    const valorActual = this.usuarioForm.value.numEmpleado;
    if (valorActual && valorActual.length != 9) {
      this.usuarioForm.controls['datosEmpleado'].reset();
    }

    if (event.keyCode == 13) {
      const valorEncontrado = this.listaEmpleados.find(
        (x) => x.numDocumento === valorActual
      );
      if (valorEncontrado) {
        this.usuarioForm.patchValue(
          {
            datosEmpleado: ` ${valorEncontrado.nombre} ${valorEncontrado.apellido}`,
            empleado: valorEncontrado
          });
      } else {
        this.isDNIEmpleadoInvalid = true;
        this.usuarioForm.controls['datosEmpleado'].reset();
      }
    }
  }

  onRowEmpleadoSelected(event: any) {
    this.rowSeleccionado = event.data;
  }

  putEmpleadoSeleccionado() {
    if (this.rowSeleccionado) {
      const { numDocumento, nombre, apellido } = this.rowSeleccionado;
      this.usuarioForm.patchValue({
        numEmpleado: numDocumento,
        datosEmpleado: `${nombre}  ${apellido}`,
        empleado: this.rowSeleccionado
      });
      this.isDNIEmpleadoInvalid = false;
      this.isBuscadorDeEmpleado = false;
    }
  }

}
