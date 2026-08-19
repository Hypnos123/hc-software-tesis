export interface ResumenPacienteBasico {
  idPaciente: number;
  nombreCompleto: string;
  dni: string;
  fechaNacimiento?: string | Date;
  edad?: number;
  estado: string;
  cantidadHistoriasClinicas: number;
  idsHistoriasClinicas: number[];
}

export interface EstadisticaVitalResumen {
  ultimoValor?: number;
  promedio?: number;
  minimo?: number;
  maximo?: number;
  cantidadRegistrosValidos: number;
  cantidadRegistrosDescartados: number;
  unidad: string;
  tendencia: 'ASCENDENTE' | 'DESCENDENTE' | 'ESTABLE' | 'SIN_DATOS_SUFICIENTES';
}

export interface CategoriaResumen { id?: number; nombre: string; cantidad: number; porcentaje: number; }
export interface ConsultaRecienteResumen { idConsulta: number; idHistoriaClinica: number; fecha: string | Date; especialidad?: string; doctor?: string; relatoPaciente?: string; diagnostico?: string; tratamiento?: string; }
export interface EvaluacionRecienteResumen { idConsulta: number; diagnostico?: string; examenesRecetados?: string; receta?: string; tratamiento?: string; proximaCita?: string | Date; }

export interface ResumenConsultasPaciente {
  paciente: ResumenPacienteBasico;
  antecedentes: { enfermedadesPrevias?: string; cirugiasPrevias?: string; alergiaMedicamentos?: string };
  resumenAtencion: { totalConsultasAtendidas: number; fechaPrimeraConsulta?: string | Date; fechaUltimaConsulta?: string | Date; ultimaEspecialidad?: string; ultimoDoctor?: string; proximasCitas: Array<string | Date> };
  tiposEnfermedad: CategoriaResumen[];
  especialidades: CategoriaResumen[];
  funcionesVitales: Record<string, EstadisticaVitalResumen>;
  evaluacionesRecientes: EvaluacionRecienteResumen[];
  consultasRecientes: ConsultaRecienteResumen[];
  calidadDatos: { consultasSinFecha: number; consultasSinTipoEnfermedad: number; consultasSinEspecialidad: number; valoresVitalesDescartados: number; consultasConRelacionInconsistente: number };
}

export interface ResumenPacienteCandidato {
  idPaciente: number;
  nombreCompleto: string;
  dni: string;
  edad?: number;
  estado: string;
  cantidadHistoriasClinicas?: number;
}

export type ResumenConsultasVista = 'prompt' | 'searching' | 'multiple' | 'confirmation' | 'loading' | 'summary' | 'error';
export interface ResumenConsultasChatState {
  vista: ResumenConsultasVista;
  candidatos: ResumenPacienteCandidato[];
  paciente?: ResumenPacienteCandidato;
  resumen?: ResumenConsultasPaciente;
  mensajeError?: string;
  accionesHabilitadas: boolean;
}

export const crearResumenConsultasState = (): ResumenConsultasChatState => ({
  vista: 'prompt', candidatos: [], accionesHabilitadas: true
});
