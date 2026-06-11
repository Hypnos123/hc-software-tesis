export interface IPaciente {
  idAntecedentes?: number;
  idPaciente?: number;

  nombreApellidos?: string;
  apellidos?: string;
  nombres?: string;
  edad?: string | number;
  dni?: string;
  fechaRegistro?: string;
  fechaIngreso?: string | Date;
  fechaNacimiento?: string | Date;
  estadoCivil?: string;
  numDocumento?: string;
  sexo?: string;
  direccion?: string;
  distrito?: string;
  traidoPor?: string;

  alimentacion?: string;
  habitos?: string;
  vivienda?: string;
  desarrolloPsicomotor?: string;
  vacunas?: string;
  educacion?: string;
  enfermedadesPrevias?: string;
  cirugiasPrevias?: string;
  alergiaMedicamentos?: string;
}

export interface IResponseModelGet<T> {
  data?: T[];
  mensaje?: string;
  error?: string;
}
