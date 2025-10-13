import { IUsuarioLogged } from "@app/modules/usuario/models/usuario";
import { IItemMenu } from "@app/shared/components/sidebar/models/sidebar";

export interface IAuth {
  contrasena: string,
  usuario: string
}

export interface IAuthSuccess {
  usuario: IUsuarioLogged,
  detallePermisos: IItemMenu[],
  mensaje: string
}

export class IAuthSuccessModel {
  usuario: IUsuarioLogged;
  detallePermisos: IItemMenu[];
  mensaje: string;

  constructor(authSucces: IAuthSuccess) {
    this.usuario = authSucces.usuario;
    this.detallePermisos = authSucces.detallePermisos;
    this.mensaje = authSucces.mensaje
  }
}
