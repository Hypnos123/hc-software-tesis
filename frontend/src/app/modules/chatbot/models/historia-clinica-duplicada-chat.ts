export type TipoDuplicidadHistoria = 'MISMO_PACIENTE' | 'MISMO_DNI_DIFERENTE_PACIENTE';

export interface HistoriaClinicaDuplicadaItem {
  idHistoriaClinica: number;
  idPaciente: number;
  dni?: string;
  nombreCompleto: string;
  fechaCreacion?: string;
  ultimaActualizacion?: string;
  cantidadConsultas: number;
  ultimaActividadClinica?: string;
  estado: string;
}

export interface GrupoHistoriasClinicasDuplicadas {
  tipo: 'dni' | 'idPaciente';
  valorCoincidente: string;
  cantidad: number;
  historiasClinicas: HistoriaClinicaDuplicadaItem[];
  idHistoriaClinicaRecomendada?: number;
  recomendacion?: string;
}

export interface DeteccionHistoriasClinicasDuplicadasResponse {
  hayDuplicados: boolean;
  totalGrupos: number;
  duplicados: GrupoHistoriasClinicasDuplicadas[];
  dniConsultado?: string;
  mensaje: string;
}

export interface ConsultaHistoriaAnalisis {
  idConsulta: number;
  estado?: string;
  fechaActividad?: string;
  idEmpleado?: number;
  medico?: string;
  diagnosticoResumen?: string;
  camposClinicosInformados: number;
  puntajeRiquezaClinica: number;
}

export interface HistoriaClinicaAnalisisDetallado {
  idHistoriaClinica: number;
  idPaciente: number;
  dni?: string;
  nombreCompleto: string;
  fechaCreacion?: string;
  ultimaActualizacion?: string;
  cantidadConsultas: number;
  ultimaActividadClinica?: string;
  cantidadConsultasPendientes: number;
  cantidadConsultasAtendidas: number;
  camposClinicosInformados: number;
  puntajeRiquezaClinica: number;
  cantidadConsultasExclusivas: number;
  consultasExclusivas: ConsultaHistoriaAnalisis[];
}

export interface PosibleCoincidenciaConsulta {
  clasificacion: 'POSIBLE_COINCIDENCIA';
  idConsultaA: number;
  idHistoriaClinicaA: number;
  idConsultaB: number;
  idHistoriaClinicaB: number;
  criteriosCoincidentes: string[];
  advertencia: string;
}

export interface AnalisisHistoriasClinicasDuplicadas {
  tipoDuplicidad: TipoDuplicidadHistoria;
  idHistoriaClinicaRecomendada: number;
  motivosRecomendacion: string[];
  resumenComparativo: string;
  historiasComparadas: HistoriaClinicaAnalisisDetallado[];
  posiblesCoincidencias: PosibleCoincidenciaConsulta[];
  futuraFusionPermitida: boolean;
  motivoBloqueo?: string;
  advertenciasIntegridad: string[];
  mensaje: string;
  tokenAnalisis: string;
}

export interface FusionarHistoriasClinicasRequest {
  idHistoriaPrincipal: number;
  contrasena: string;
  confirmacion: true;
  motivo: 'HISTORIA_CLINICA_DUPLICADA';
  detalle: string;
  origen: 'CHATBOT';
  cantidadEsperadaPrincipal: number;
  cantidadEsperadaSecundaria: number;
  idsConsultasEsperadasPrincipal: number[];
  idsConsultasEsperadasSecundaria: number[];
  tokenAnalisis: string;
}

export interface FusionarHistoriasClinicasResponse {
  fusionada: boolean;
  idHistoriaPrincipal?: number;
  idHistoriaEliminada?: number;
  idPaciente?: number;
  cantidadConsultasAntesPrincipal?: number;
  cantidadConsultasAntesSecundaria?: number;
  cantidadConsultasTransferidas?: number;
  cantidadConsultasFinalPrincipal?: number;
  posiblesCoincidencias?: number;
  idAuditoria?: number;
  resultado: string;
  mensaje: string;
}

export type EstadoGestionHistoriasDuplicadas =
  | 'CONSULTANDO_DUPLICADOS'
  | 'MOSTRANDO_HISTORIAS'
  | 'SELECCIONANDO_HISTORIAS'
  | 'ANALIZANDO_HISTORIAS'
  | 'MOSTRANDO_COMPARACION'
  | 'SELECCIONANDO_PRINCIPAL'
  | 'MOSTRANDO_VISTA_PREVIA'
  | 'SOLICITANDO_CONTRASENA'
  | 'FUSIONANDO'
  | 'COMPLETADO'
  | 'CANCELADO'
  | 'ERROR';

export type GestionHistoriasDuplicadasVista =
  | 'loading'
  | 'groups'
  | 'selection'
  | 'analyzing'
  | 'comparison'
  | 'principal-selection'
  | 'preview'
  | 'password'
  | 'fusing'
  | 'success'
  | 'cancelled'
  | 'error';

export interface GestionHistoriasDuplicadasState {
  estado: EstadoGestionHistoriasDuplicadas;
  deteccion?: DeteccionHistoriasClinicasDuplicadasResponse;
  grupoSeleccionado?: GrupoHistoriasClinicasDuplicadas;
  idsSeleccionados: number[];
  analisis?: AnalisisHistoriasClinicasDuplicadas;
  idHistoriaPrincipal?: number;
  idHistoriaSecundaria?: number;
  intentosRestantes: number;
  respuestaFusion?: FusionarHistoriasClinicasResponse;
  mensajeError?: string;
  cancelarSolicitud?: () => void;
}

export interface GestionHistoriasDuplicadasEvento {
  remitente: 'user' | 'bot';
  texto: string;
  vistaSiguiente?: GestionHistoriasDuplicadasVista;
  reemplazarVistaActiva?: boolean;
  inicioGrupo?: boolean;
  volverHistorias?: boolean;
}

export function crearGestionHistoriasDuplicadasState(): GestionHistoriasDuplicadasState {
  return { estado: 'CONSULTANDO_DUPLICADOS', idsSeleccionados: [], intentosRestantes: 3 };
}
