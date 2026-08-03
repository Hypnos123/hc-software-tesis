export type EstadoGestionDuplicados =
  | 'SOLICITANDO_DNI'
  | 'CONSULTANDO_DUPLICADOS'
  | 'MOSTRANDO_RESULTADOS'
  | 'CONFIRMANDO_PRINCIPAL'
  | 'REQUIERE_REVISION_CLINICA'
  | 'SOLICITANDO_CONTRASENA'
  | 'ARCHIVANDO'
  | 'COMPLETADO'
  | 'ERROR'
  | 'CANCELADO';

export interface PacienteDuplicadoDetalle {
  idPaciente: number;
  nombres?: string;
  apellidos?: string;
  nombreCompleto: string;
  tipoDocumento?: string;
  dni: string;
  fechaCreacion?: string;
  ultimaActualizacion?: string;
  estadoRegistro: 'ACTIVO' | 'ARCHIVADO';
  cantidadHistoriasClinicas: number;
  cantidadConsultas: number;
  cantidadAntecedentes: number;
  cantidadCamposPersonalesCompletos: number;
  cantidadGruposClinicosCompletos: number;
  ultimaActividadClinica?: string;
  tieneInformacionClinicaRelevante: boolean;
}

export interface PacienteDuplicadoAnalisisResponse {
  dni: string;
  cantidadPacientesActivos: number;
  esDuplicado: boolean;
  pacientes: PacienteDuplicadoDetalle[];
  idPacienteRecomendado?: number;
  razonesRecomendacion: string[];
  permitirArchivadoSimple: boolean;
  requiereRevision: boolean;
  resultado: string;
  mensaje: string;
  advertencia?: string;
}

export interface ArchivadoPacienteDuplicadoRequest {
  idPacientePrincipal: number;
  motivo: 'PACIENTE_DUPLICADO';
  detalleMotivo: string;
  contrasena: string;
  confirmarRevisionClinica: boolean;
  origen: 'CHATBOT';
}

export interface ArchivadoPacienteDuplicadoResponse {
  archivado: boolean;
  idPacienteArchivado?: number;
  idPacientePrincipal?: number;
  dni?: string;
  estadoAnterior?: string;
  estadoNuevo?: string;
  idAuditoria?: number;
  usuarioResponsable?: string;
  cargoResponsable?: string;
  requiereRevisionClinica?: boolean;
  revisionClinicaConfirmada?: boolean;
  resultado: string;
  mensaje: string;
}

export interface GestionDuplicadosChatState {
  estado: EstadoGestionDuplicados;
  dni: string;
  intentosRestantes: number;
  analisis?: PacienteDuplicadoAnalisisResponse;
  pacienteArchivado?: PacienteDuplicadoDetalle;
  pacientePrincipal?: PacienteDuplicadoDetalle;
  revisionClinicaConfirmada: boolean;
  respuestaArchivado?: ArchivadoPacienteDuplicadoResponse;
  mensajeError?: string;
  cancelarSolicitud?: () => void;
}

export type GestionDuplicadosVista =
  | 'dni'
  | 'loading'
  | 'results'
  | 'confirmation'
  | 'warning'
  | 'password'
  | 'archiving'
  | 'success'
  | 'cancelled'
  | 'error';

export interface GestionDuplicadosEvento {
  remitente: 'user' | 'bot';
  texto: string;
  vistaSiguiente?: GestionDuplicadosVista;
  reemplazarVistaActiva?: boolean;
  volverPacientes?: boolean;
  inicioGrupo?: boolean;
}

export function crearGestionDuplicadosState(): GestionDuplicadosChatState {
  return {
    estado: 'SOLICITANDO_DNI',
    dni: '',
    intentosRestantes: 3,
    revisionClinicaConfirmada: false
  };
}
