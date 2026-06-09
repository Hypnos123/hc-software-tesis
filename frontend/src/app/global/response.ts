export interface IResponse {
  data?: any[];
  mensaje: string;
  error: string;
}

export interface IResponseTicket  {
  data: DataPrint[];
  mensaje: string;
  error: string;
}

export interface DataPrint {
  file: string;
  fileName: string;
}
