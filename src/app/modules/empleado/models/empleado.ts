export interface IEmpleado {
  idEmpleado?: number;
  tipoDocumento: string;
  numDocumento: string;
  nombre: string;
  apellido: string;
  direccion: string;
  telefono: string;
  celular: string;
  cargo: string;
  estado?: boolean;
}
