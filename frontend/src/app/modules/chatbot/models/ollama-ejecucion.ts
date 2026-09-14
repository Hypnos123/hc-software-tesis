export interface IOllamaEjecucionRequest {
  mensaje: string;
}

export interface IOllamaEjecucionResponse {
  categoria: string;
  intencion: string;
  mensaje: string;
  encontrado?: boolean;
  dni?: string;
  nombre?: string;
  pacientes?: IOllamaPaciente[];
  gruposDuplicados?: IOllamaGruposDuplicados;
  comparacionDuplicados?: IOllamaComparacionDuplicados;
}

export interface IOllamaPaciente {
  idPaciente?: number;
  nombres?: string;
  apellidos?: string;
  fechaIngreso?: string;
  fechaNacimiento?: string;
  edad?: number;
  numDocumento?: string;
  sexo?: string;
}

export interface IOllamaPacienteDuplicadoItem {
  idPaciente?: number;
  dni?: string;
  nombreCompleto?: string;
}

export interface IOllamaGrupoDuplicado {
  tipo?: string;
  valorCoincidente?: string;
  cantidad?: number;
  pacientes?: IOllamaPacienteDuplicadoItem[];
}

export interface IOllamaGruposDuplicados {
  hayDuplicados: boolean;
  totalGrupos: number;
  duplicados?: IOllamaGrupoDuplicado[];
}

export interface IOllamaPacienteDuplicadoDetalle extends IOllamaPacienteDuplicadoItem {
  nombres?: string;
  apellidos?: string;
  fechaCreacion?: string;
  estadoRegistro?: string;
}

export interface IOllamaComparacionDuplicados {
  dni?: string;
  cantidadPacientesActivos: number;
  esDuplicado: boolean;
  pacientes?: IOllamaPacienteDuplicadoDetalle[];
}
