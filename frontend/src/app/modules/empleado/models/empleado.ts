export interface IEmpleado {
  idEmpleado?: number;
  tipoDocumento: string;
  numDocumento: string;
  nombres: string;
  apellidos: string;
  direccion: string;
  telefono: string;
  celular: string;
  cargo: string;
  estado?: boolean;
}

export interface IResponseModelGet<T> {
  data?: T[];
  mensaje?: string;
  error?: string;
}
