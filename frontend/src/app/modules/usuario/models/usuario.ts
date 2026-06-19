export interface IUsuario {
  idUsuario?: number,
  usuario: string;
  contrasena: string;
  tipoUsuario: string;
  estado?: boolean,
  idEmpleado: number,
  apellidoYNombre?: string;
}

export interface IUsuarioLogged {
  idUsuario: number;
  usuario?: string;
  tipoUsuario: string;
  estadoUsuario?: boolean;
  idEmpleado?: number;
  nombres?: string;
  apellidos?: string;
  cargo?: string;
  estadoEmpleado?: boolean;
  nombre?: string;
  apellido?: string;
}

export interface ITipoUsuario {
  tipoUsuario: string,
}

export interface IMenu {
  estado: boolean,
  idMenu: number,
  imagen: string,
  nombre: string,
  ruta: string
}

export interface IDetallePermiso {
  idMenu: number,
  idUsuario: number
}
