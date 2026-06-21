export interface IHistoriaClinica {
  idHistoriaClinica?: number;
  fechaCreacion?: string | Date;
  ultimaActualizacion?: string | Date;
  idPaciente?: number;
  nombres?: string;
  apellidos?: string;
  fechaIngreso?: string | Date;
  fechaNacimiento?: string | Date;
  estadoCivil?: string;
  numDocumento?: string;
  edad?: string | number;
  enfermedadesPrevias?: string;
  cirugiasPrevias?: string;
  alergiaMedicamentos?: string;
}

export interface IHistoriaClinicaRequest { idPaciente?: number; }

export interface IPacienteBusqueda {
  idPaciente?: number;
  nombres?: string;
  apellidos?: string;
  fechaIngreso?: string | Date;
  fechaNacimiento?: string | Date;
  estadoCivil?: string;
  numDocumento?: string;
  edad?: string | number;
}

export interface IEmpleadoDoctor {
  idEmpleado?: number;
  nombres?: string;
  apellidos?: string;
  cargo?: string;
  estado?: boolean;
  nombreCompleto?: string;
}
export interface IResponseModelGet<T> { data?: T[]; mensaje?: string; error?: string; }
export interface IResponseModelSet { mensaje?: string; error?: string; idGenerado?: number; }

export interface IDetalleConsulta {
  idConsulta?: number;
  idHistoriaClinica?: number;
  fechaCreacion?: string | Date;
  estado?: string;
  paciente?: string;
  dni?: string;
  edad?: string | number;
  nombres?: string;
  apellidos?: string;
  numDocumento?: string;
  enfermedadesPrevias?: string;
  cirugiasPrevias?: string;
  alergiaMedicamentos?: string;
  presionArterial?: string;
  frecuenciaCardiaca?: string | number;
  frecuenciaRespiratoria?: string | number;
  talla?: string | number;
  temperatura?: string | number;
  peso?: string | number;
  fechaConsulta?: string | Date;
  tiempoEnfermedad?: string;
  tipoEnfermedad?: string;
  especialidadRequerida?: string;
  idEmpleadoDoctor?: number;
  doctorResponsable?: string;
  relatoPaciente?: string;
  diagnostico?: string;
  examenesRecetados?: string;
  receta?: string;
  tratamiento?: string;
  proximaCita?: string | Date | null;
}

export interface INuevaConsultaRequest {
  idConsulta?: number;
  idHistoriaClinica?: number;
  fechaConsulta?: string | Date;
  tiempoEnfermedad?: string;
  tipoEnfermedad?: string;
  especialidadRequerida?: string;
  idEmpleadoDoctor?: number;
  relatoPaciente?: string;
  diagnostico?: string;
  examenesRecetados?: string;
  receta?: string;
  tratamiento?: string;
  proximaCita?: string | Date | null;
  presionArterial?: string;
  frecuenciaCardiaca?: string | number;
  frecuenciaRespiratoria?: string | number;
  talla?: string | number;
  temperatura?: string | number;
  peso?: string | number;
}
