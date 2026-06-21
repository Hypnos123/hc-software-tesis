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

export interface IResponseModelGet<T> { data?: T[]; mensaje?: string; error?: string; }
export interface IResponseModelSet { mensaje?: string; error?: string; idGenerado?: number; }

export interface IDetalleConsulta {
  id?: number;
  paciente?: string;
  dni?: string;
  edad?: string | number;
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
  relatoPaciente?: string;
  diagnostico?: string;
  examenesRecetados?: string;
  receta?: string;
  tratamiento?: string;
  proximaCita?: string | Date | null;
}

export interface INuevaConsultaRequest {
  idHistoriaClinica?: number;
  fechaConsulta?: string | Date;
  tiempoEnfermedad?: string;
  tipoEnfermedad?: string;
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
