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
  pacientes?: unknown[];
  gruposDuplicados?: unknown;
  comparacionDuplicados?: unknown;
}
