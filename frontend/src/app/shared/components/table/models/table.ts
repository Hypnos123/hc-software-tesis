import { IColumnasTabla } from "@app/shared/models/columnas";

export interface ITable {
  listaElementos: any[];
  columnas: IColumnasTabla[];
  filas: number;
  isPaginador: boolean;
  listaElementosAFiltrar: string[];
  tipoResponsivo: boolean;
  isHoverAFila: boolean;
  isCantidadElementos: boolean;
  isMostrarCaption: boolean;
  isMostrarHeader:boolean;
  isMostrarBody: boolean;
  isMostrarFooter: boolean;
  isMostrarTabla: boolean;
}


export interface ITableCaption {
  titulo: string;
  withBuscador: boolean;
  idTabla: any;
}

export interface ITableHeader {
  isAcciones: boolean;
}

export interface IButton {
  icono: string;
  clase: string;
  evento: string;
  estado: boolean;
  isDisabled?: boolean;
  tooltip: string;
}

