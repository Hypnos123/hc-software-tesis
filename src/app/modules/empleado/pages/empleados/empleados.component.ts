import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { IEmpleado } from '../../models/empleado';
import { EmpleadoService } from '../../services/empleado.service';
import { IColumnasTabla } from '../../../../shared/models/columnas';
import { MensajesSwalService } from '../../../../shared/services/mensajes-swal.service';
import { CommonModule } from '@angular/common';
import { ButtonComponent, TableComponent } from '@app/shared/components';

@Component({
  selector: 'app-empleados',
  templateUrl: './empleados.component.html',
  styleUrls: ['./empleados.component.scss'],
  imports: [CommonModule, ButtonComponent, TableComponent],
  standalone: true
})
export class EmpleadosComponent implements OnInit {
  listaElementos: IEmpleado[] = [];
  cols: IColumnasTabla[] = [];
  colsVisibles: IColumnasTabla[] = [];
  isCargado: boolean = false;

  constructor(
    private empleadoService: EmpleadoService,
    private router: Router,
    private readonly servicioMensajesSwal: MensajesSwalService
  ) { }

  ngOnInit(): void {
   //this.getAllActivosElementos();
    this.cargarDatosMock();

  }

  cargarDatosMock() {
  this.listaElementos = [
    {
      idEmpleado: 1,
      tipoDocumento: 'DNI',
      numDocumento: '74526981',
      nombre: 'Josefina',
      apellido: 'Mendoza Davalos',
      direccion: 'Av. San José 123',
      telefono: '965874321',
      celular: '987654321',
      cargo: 'Enfermera'
    },
    {
      idEmpleado: 2,
      tipoDocumento: 'DNI',
      numDocumento: '78945612',
      nombre: 'Carlos',
      apellido: 'Ramírez Soto',
      direccion: 'Jr. Lima 456',
      telefono: '964123789',
      celular: '912345678',
      cargo: 'Médico'
    }
  ];

  this.getColumnasTabla();
  this.isCargado = true;
}

  getColumnasTabla() {
    this.cols = [
      {
        field: 'tipoDocumento',
        header: 'Tipo de Documento',
        visibility: true,
        formatoFecha: '',
      },
      {
        field: 'numDocumento',
        header: 'Número de Documento',
        visibility: true,
        formatoFecha: '',
      },
      { field: 'nombre', header: 'Nombre', visibility: true, formatoFecha: '' },
      {
        field: 'apellido',
        header: 'Apellidos',
        visibility: true,
        formatoFecha: '',
      },
      {
        field: 'direccion',
        header: 'Dirección',
        visibility: true,
        formatoFecha: '',
      },
      {
        field: 'telefono',
        header: 'Telefono',
        visibility: true,
        formatoFecha: '',
      },
      {
        field: 'celular',
        header: 'Celular',
        visibility: true,
        formatoFecha: '',
      },
      { field: 'cargo', header: 'Cargo', visibility: true, formatoFecha: '' },
    ];

    this.colsVisibles = this.cols.filter((x) => x.visibility == true);
  }

  getAllActivosElementos() {
    const obs = new Observable<boolean>((observer) => {
      this.empleadoService.getAllActivos().subscribe((resp) => {
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

  eliminarElemento(data: any) {
    this.servicioMensajesSwal
      .mensajePregunta('¿Está seguro de eliminar el registro?')
      .then((response) => {
        if (response.isConfirmed) {
          this.empleadoService.setInactive(data.idEmpleado).subscribe((res) => {
            this.getAllActivosElementos();
            this.servicioMensajesSwal.mensajeRegistroEliminado();
          });
        }
      });
  }

  eventoAccion(datos: any) {
    const { tipo, data } = datos;
    switch (tipo) {
      case 'editar':
        this.editarElemento(data);
        break;

      case 'eliminar':
        this.eliminarElemento(data);
        break;

      default:
        console.log('Acción no aplicada');
        break;
    }
  }



  editarElemento(data: any) {
    const id = data.idEmpleado;
    this.router.navigateByUrl(`empleados/mantenimiento-empleado/${id}`);
  }
}


