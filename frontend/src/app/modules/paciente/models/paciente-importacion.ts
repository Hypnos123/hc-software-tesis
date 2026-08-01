export type EstadoImportacionBackend = 'PREVISUALIZADA' | 'CONFIRMANDO' | 'CONFIRMADA' | 'CANCELADA' | 'EXPIRADA';
export type EstadoFilaImportacion = 'VALIDO' | 'ERROR_DATOS' | 'DNI_EXISTENTE' | 'DNI_DUPLICADO_ARCHIVO' | 'REGISTRADO' | 'NO_REGISTRADO';

export type EstadoFlujoImportacion =
  | 'INICIAL'
  | 'PLANTILLA_DESCARGADA'
  | 'ARCHIVO_SELECCIONADO'
  | 'VALIDANDO'
  | 'PREVISUALIZADA'
  | 'CONFIRMANDO'
  | 'CONFIRMADA'
  | 'CANCELADA'
  | 'EXPIRADA'
  | 'ERROR';

export interface IPacienteImportacionError {
  numeroFila?: number;
  codigo: string;
  campo?: string;
  mensaje: string;
}

export interface IPacienteImportacionAdvertencia {
  numeroFila?: number;
  codigo: string;
  campo?: string;
  mensaje: string;
}

export interface IPacienteImportacionDatos {
  apellidos: string;
  nombres: string;
  fechaNacimiento: string | null;
  estadoCivil: string;
  dni: string;
  sexo: string;
  direccion: string;
  distrito: string;
  traidoPor: string;
}

export interface IPacienteImportacionAntecedentes {
  alimentacion: string;
  habitos: string;
  vivienda: string;
  desarrolloPsicomotor: string;
  vacunas: string;
  educacion: string;
  enfermedadesPrevias: string;
  cirugiasPrevias: string;
  alergiasMedicamentos: string;
}

export interface IPacienteImportacionFila {
  numeroFila: number;
  nombreCompleto: string;
  dni: string;
  estado: EstadoFilaImportacion;
  paciente: IPacienteImportacionDatos;
  antecedentes: IPacienteImportacionAntecedentes;
  errores: IPacienteImportacionError[];
  advertencias: IPacienteImportacionAdvertencia[];
}

export interface IPacienteImportacionResumen {
  registrosAnalizados: number;
  validos: number;
  conErrores: number;
  filasConDniDuplicado: number;
  gruposDniDuplicados: number;
  dniExistentes: number;
  conAdvertencias: number;
  filasVaciasIgnoradas: number;
}

export interface IPacienteImportacionPrevisualizacion {
  importacionId: string;
  estado: EstadoImportacionBackend;
  expiraEn: string;
  resumen: IPacienteImportacionResumen;
  filas: IPacienteImportacionFila[];
}

export interface IPacienteImportacionResultadoFila {
  numeroFila: number;
  dni: string;
  estado: EstadoFilaImportacion;
  idPaciente?: number;
  errores: IPacienteImportacionError[];
}

export interface IPacienteImportacionConfirmacionResumen {
  filasValidasEnPrevisualizacion: number;
  pacientesRegistrados: number;
  omitidosPorDniExistente: number;
  erroresAlRegistrar: number;
}

export interface IPacienteImportacionConfirmacion {
  importacionId: string;
  estado: EstadoImportacionBackend;
  resumen: IPacienteImportacionConfirmacionResumen;
  resultados: IPacienteImportacionResultadoFila[];
}
