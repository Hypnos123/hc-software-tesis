export interface FusionHistoriaAuditoriaResumen {
  idAuditoria: number;
  idPaciente: number;
  nombrePaciente: string;
  dni: string;
  idHistoriaPrincipal: number;
  idHistoriaEliminada: number;
  consultasTransferidas: number;
  fecha: string;
  usuarioResponsable: string;
  resultado: string;
}

export interface FusionHistoriaAuditoriaDetalle extends FusionHistoriaAuditoriaResumen {
  origen: string;
  motivo: string;
  detalle?: string | null;
  consultasAntesPrincipal: number;
  consultasAntesSecundaria: number;
  consultasDespuesPrincipal: number;
  idUsuario: number;
  idEmpleado: number;
  empleadoResponsable: string;
  cargo: string;
  explicacion: string;
}

export interface FusionesHistoriasFiltros {
  page: number;
  size: number;
  sort?: string;
  search?: string;
  dni?: string;
  idPaciente?: number;
  idHistoriaPrincipal?: number;
  idHistoriaEliminada?: number;
  idUsuario?: number;
  resultado?: string;
  desde?: string;
  hasta?: string;
}
