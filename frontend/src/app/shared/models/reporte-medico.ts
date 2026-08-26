export type ReporteConsultaAlcance = 'ULTIMA' | 'TODAS' | 'FECHA' | 'RANGO_FECHAS';

export interface ReporteConsultaFiltro {
  alcance: ReporteConsultaAlcance;
  fecha?: string;
  fechaDesde?: string;
  fechaHasta?: string;
}

export interface ReporteConsultaSeleccion {
  idPaciente: number;
  paciente?: string;
  alcance: ReporteConsultaAlcance;
  fecha?: string;
  fechaDesde?: string;
  fechaHasta?: string;
  totalConsultasEncontradas: number;
  consultasAtendidasIncluidas: number;
  consultasNoAtendidasExcluidas: number;
  idsHistoriasClinicasIncluidas: number[];
  puedeGenerar: boolean;
  mensaje: string;
}

export interface ReportePdfArchivo {
  blob: Blob;
  nombreArchivo: string;
}

export interface ApiReporteError {
  codigo?: string;
  mensaje?: string;
}

export interface ReportePacienteOpcion {
  idPaciente: number;
  nombreCompleto: string;
  dni?: string;
  etiqueta: string;
}
