export interface IAsistenteRequest { pregunta: string; }
export interface IAsistenteResponse { intencion: string; respuesta: string; datos?: Record<string, unknown>; }
