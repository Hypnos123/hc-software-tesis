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


export interface IHistoriaClinica {
  idHistoriaClinica?: number;

  nombrePacienteSel?: string;
  dniSel?: string;

  fechaIngreso?: string;
  apellidos?: string;
  nombres?: string;
  estadoCivil?: string;
  edad?: string | number;
  dni?: string;

  enfPrevias?: string;
  cirugiasPrevias?: string;
  alergiasMedicamentos?: string;
}