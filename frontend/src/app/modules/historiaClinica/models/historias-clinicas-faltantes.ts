export interface PacienteSinHistoriaClinica {
  idPaciente: number;
  nombreCompleto: string;
  dniEnmascarado: string;
}

export interface HistoriasClinicasFaltantesPreview {
  cantidad: number;
  pacientes: PacienteSinHistoriaClinica[];
}
