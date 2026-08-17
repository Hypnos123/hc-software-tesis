export interface PacienteArchivadoResumen {
  idPaciente: number;
  nombreCompleto: string;
  dni: string;
  fechaArchivado: string;
  usuarioResponsable?: string | null;
  motivoArchivado?: string | null;
  estadoRegistro: 'ARCHIVADO';
  idPacientePrincipal?: number | null;
  nombrePacientePrincipal?: string | null;
  idAuditoria?: number | null;
}

export interface PacientePrincipalArchivado {
  idPaciente: number;
  nombreCompleto: string;
  dni: string;
  estadoRegistro: string;
}

export interface PacienteArchivadoDetalle {
  idPaciente: number;
  nombres: string;
  apellidos: string;
  dni: string;
  estadoRegistro: string;
  fechaArchivado: string;
  motivoArchivado?: string | null;
  detalleMotivoArchivado?: string | null;
  idAuditoria?: number | null;
  usuarioResponsable?: string | null;
  idEmpleado?: number | null;
  empleadoResponsable?: string | null;
  cargo?: string | null;
  origen?: string | null;
  fechaAuditoria?: string | null;
  estadoAnterior?: string | null;
  estadoNuevo?: string | null;
  requirioRevisionClinica: boolean;
  confirmoRevisionClinica: boolean;
  pacientePrincipal?: PacientePrincipalArchivado | null;
  cantidadHistoriasClinicas: number;
  cantidadConsultas: number;
  cantidadAntecedentes: number;
}

export interface PaginaResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface PacientesArchivadosFiltros {
  page: number;
  size: number;
  sort?: string;
  search?: string;
  dni?: string;
  idPaciente?: number;
  desde?: string;
  hasta?: string;
}
