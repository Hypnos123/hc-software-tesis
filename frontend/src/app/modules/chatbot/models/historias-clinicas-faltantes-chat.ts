import {
  CrearHistoriasClinicasFaltantesResponse,
  HistoriasClinicasFaltantesPreview
} from '@app/modules/historiaClinica/models/historias-clinicas-faltantes';

export type EstadoHistoriasClinicasFaltantes =
  | 'CARGANDO'
  | 'SELECCIONANDO'
  | 'CONFIRMANDO'
  | 'CREANDO'
  | 'COMPLETADO'
  | 'SIN_CANDIDATOS'
  | 'ERROR'
  | 'ERROR_CREACION'
  | 'CANCELADO';

export type HistoriasClinicasFaltantesVista =
  | 'loading'
  | 'selection'
  | 'confirmation'
  | 'creating'
  | 'result'
  | 'creation-error'
  | 'empty'
  | 'error';

export interface HistoriasClinicasFaltantesChatState {
  estado: EstadoHistoriasClinicasFaltantes;
  preview?: HistoriasClinicasFaltantesPreview;
  idsSeleccionados: number[];
  idsConfirmados: number[];
  resultado?: CrearHistoriasClinicasFaltantesResponse;
  mensajeError?: string;
  cancelarSolicitud?: () => void;
}

export interface HistoriasClinicasFaltantesEvento {
  remitente: 'user' | 'bot';
  texto: string;
  vistaSiguiente?: HistoriasClinicasFaltantesVista;
  reemplazarVistaActiva?: boolean;
  volverHistorias?: boolean;
  inicioGrupo?: boolean;
  ejecutarCreacion?: boolean;
  accionPosterior?: 'REVISAR' | 'HISTORIAS' | 'PRINCIPAL';
}

export function crearHistoriasClinicasFaltantesState(): HistoriasClinicasFaltantesChatState {
  return {
    estado: 'CARGANDO',
    idsSeleccionados: [],
    idsConfirmados: []
  };
}
