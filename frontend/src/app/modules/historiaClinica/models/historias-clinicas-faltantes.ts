export interface PacienteSinHistoriaClinica {
  idPaciente: number;
  nombreCompleto: string;
  dniEnmascarado: string;
}

export interface HistoriasClinicasFaltantesPreview {
  cantidad: number;
  pacientes: PacienteSinHistoriaClinica[];
}

export type EstadoCreacionHistoriaClinicaFaltante =
  | 'CREADA'
  | 'OMITIDA_YA_TIENE_HISTORIA'
  | 'PACIENTE_NO_ENCONTRADO'
  | 'PACIENTE_INACTIVO'
  | 'ERROR';

export interface CrearHistoriasClinicasFaltantesRequest {
  idsPacientes: number[];
}

export interface CreacionHistoriaClinicaFaltanteResultado {
  idPaciente: number;
  estado: EstadoCreacionHistoriaClinicaFaltante;
  idHistoriaClinica?: number | null;
}

export interface CrearHistoriasClinicasFaltantesResponse {
  totalSolicitados: number;
  totalProcesados: number;
  creadas: number;
  omitidas: number;
  noEncontrados: number;
  inactivos: number;
  errores: number;
  resultados: CreacionHistoriaClinicaFaltanteResultado[];
}
