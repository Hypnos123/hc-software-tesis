import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { IEmpleado } from '../../models/empleado';
import { EmpleadoService } from '../../services/empleado.service';
import { IColumnasTabla } from '../../../../shared/models/columnas';
import { MensajesSwalService } from '../../../../shared/services/mensajes-swal.service';
import { CommonModule } from '@angular/common';
import { ButtonComponent, TableComponent } from '@app/shared/components';
import { IButton } from '@app/shared/components/table/models/table';
import { InputTextModule } from 'primeng/inputtext';

@Component({
  selector: 'app-empleados',
  templateUrl: './empleados.component.html',
  styleUrls: ['./empleados.component.scss'],
  imports: [
    CommonModule,
    ButtonComponent,
    TableComponent,
    InputTextModule
  ],
  standalone: true
})
export class EmpleadosComponent implements OnInit {
  listaElementos: IEmpleado[] = [];
  cols: IColumnasTabla[] = [];
  colsVisibles: IColumnasTabla[] = [];
  isCargado: boolean = false;
  acciones: IButton[] = [];

  constructor(
    private empleadoService: EmpleadoService,
    private router: Router,
    private readonly servicioMensajesSwal: MensajesSwalService
  ) { }

  ngOnInit(): void {
    //this.getAllActivosElementos();

    this.acciones = [
      {
        icono: 'pi pi-pencil',
        clase: 'rounded',
        evento: 'editar',
        estado: true,
        tooltip: 'Editar'
      },
      {
        icono: 'pi pi-trash',
        clase: 'rounded',
        evento: 'eliminar',
        estado: true,
        tooltip: 'Eliminar'
      }
    ]

    this.getAllActivosElementos();
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
        field: 'telefono',
        header: 'Telefono',
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


