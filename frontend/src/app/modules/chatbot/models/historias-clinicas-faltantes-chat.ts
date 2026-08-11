import { HistoriasClinicasFaltantesPreview } from '@app/modules/historiaClinica/models/historias-clinicas-faltantes';

export type EstadoHistoriasClinicasFaltantes =
  | 'CARGANDO'
  | 'SELECCIONANDO'
  | 'CONFIRMANDO'
  | 'SIN_CANDIDATOS'
  | 'ERROR'
  | 'CANCELADO';

export type HistoriasClinicasFaltantesVista =
  | 'loading'
  | 'selection'
  | 'confirmation'
  | 'empty'
  | 'error';

export interface HistoriasClinicasFaltantesChatState {
  estado: EstadoHistoriasClinicasFaltantes;
  preview?: HistoriasClinicasFaltantesPreview;
  idsSeleccionados: number[];
  idsConfirmados: number[];
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
}

export function crearHistoriasClinicasFaltantesState(): HistoriasClinicasFaltantesChatState {
  return {
    estado: 'CARGANDO',
    idsSeleccionados: [],
    idsConfirmados: []
  };
}
