import { Component, OnInit } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { Observable } from 'rxjs';
import { IUsuario } from '../../models/usuario';
import { UsuarioService } from '../../services/usuario.service';
import { IColumnasTabla } from '@app/shared/models/columnas';
import { MensajesSwalService } from '@app/shared/services/mensajes-swal.service';
import { CommonModule } from '@angular/common';
import { ButtonComponent, TableComponent } from '@app/shared/components';
import { InputTextModule } from 'primeng/inputtext';

@Component({
  selector: 'app-usuarios',
  templateUrl: './usuarios.component.html',
  styleUrls: ['./usuarios.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    ButtonComponent,
    TableComponent,
    InputTextModule
  ]
})
export class UsuariosComponent implements OnInit {

  listaElementos: IUsuario[] = [];
  cols: IColumnasTabla[] = [];
  colsVisibles: IColumnasTabla[] = [];
  isCargado: boolean = false;

  constructor(
    private usuarioService: UsuarioService,
    private router: Router,
    private readonly servicioMensajesSwal: MensajesSwalService
  ) { }


  ngOnInit(): void {
    this.getAllElementos();
  }

  getColumnasTabla() {
    this.cols = [
      { field: 'usuario', header: 'Usuario', visibility: true, formatoFecha: '' },
      { field: 'contrasena', header: 'Contraseña', visibility: true, formatoFecha: '' },
      { field: 'tipoUsuario', header: 'Tipo Usuario', visibility: true, formatoFecha: '' },
      { field: 'apellidoYNombre', header: 'Apellidos y Nombres', visibility: true, formatoFecha: '' },
      { field: 'estado', header: 'Estado', visibility: true, formatoFecha: '' },
    ];

    this.colsVisibles = this.cols.filter((x) => x.visibility == true);
  }


  getAllElementos() {
    const obs = new Observable<boolean>((observer) => {
      this.usuarioService.getAll().subscribe((resp) => {
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

  eliminarElementos(data: any) {
    this.servicioMensajesSwal
      .mensajePregunta('¿Está seguro de eliminar el registro?')
      .then((response) => {
        if (response.isConfirmed) {
          this.usuarioService
            .setInactive(data.idUsuario)
            .subscribe((res) => {
              this.getAllElementos();
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
        this.eliminarElementos(data);
        break;

      default:
        console.log('Acción no aplicada');
        break;
    }
  }

  editarElemento(data: any) {
    const id = data.idUsuario;
    this.router.navigateByUrl(`usuarios/mantenimiento-usuario/${id}`);
  }
}
