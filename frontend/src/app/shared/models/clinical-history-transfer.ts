export interface ClinicalHistoryTransferCandidate {
  idPaciente: number;
  dni: string;
  nombres: string;
  apellidos: string;
  fechaIngreso: string;
  fechaNacimiento: string;
  estadoCivil: string;
  enfermedadesPrevias: string | null;
  cirugiasPrevias: string | null;
  alergiaMedicamentos: string | null;
}

export interface ClinicalHistoryPrefillTransfer {
  source: 'chatbot';
  createdAt: number;
  expiresAt: number;
  candidate: ClinicalHistoryTransferCandidate;
}
