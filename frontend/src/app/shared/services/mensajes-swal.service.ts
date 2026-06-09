import { Injectable } from "@angular/core";
import Swal, { SweetAlertResult } from 'sweetalert2';

@Injectable({
  providedIn: 'root'
})
export class MensajesSwalService {

  mensajeExito(mensaje: string) {
    Swal.fire({
      position: 'center',
      icon: 'success',
      title: 'Exito !',
      text: mensaje,
      showConfirmButton: false,
      timer: 2000
    });
  }

  mensajeGrabadoSatisfactorio() {
    Swal.fire({
      position: 'center',
      icon: 'success',
      title: 'Guardado !',
      text: '¡El Registro ha sido Guardado!',
      showConfirmButton: false,
      timer: 2000
    });
  }

  mensajeImportacionSatisfactorio() {
    Swal.fire({
      position: 'center',
      icon: 'success',
      title: 'Importacion !',
      text: '¡Importacion Satisfactoria!',
      showConfirmButton: false,
      timer: 2000
    });
  }

  mensajeActualizadoSatisfactorio() {
    Swal.fire({
      position: 'center',
      icon: 'success',
      title: 'Actualizado !',
      text: '¡El Registro ha sido Actualizado!',
      showConfirmButton: false,
      timer: 2000
    });
  }

  mensajeRegistroEliminado() {
    Swal.fire({
      position: 'center',
      icon: 'success',
      title: 'Eliminado !',
      text: '¡El Registro ha sido Eliminado!',
      showConfirmButton: false,
      timer: 2000
    });
  }

  mensajeRegistroInactivado() {
    Swal.fire({
      position: 'center',
      icon: 'success',
      title: 'Inactivado !',
      text: '¡El Registro ha sido Inactivado!',
      showConfirmButton: false,
      timer: 2000
    });
  }

  mensajeInformacion(mensaje: string) {
    Swal.fire({
      position: 'center',
      icon: 'success',
      title: 'Información',
      text: mensaje,
      showConfirmButton: false,
      timer: 2000
    });
  }

  mensajeAdvertencia(mensaje: string, mensajeEnHtml?: string) {
    Swal.fire({
      position: 'center',
      icon: 'warning',
      title: 'Advertencia',
      text: mensaje ? mensaje : '',
      showConfirmButton: true,
      html: mensajeEnHtml ? mensajeEnHtml : ''
    });
  }

  mensajeError(mensaje: any) {
    Swal.fire({
      position: 'center',
      icon: 'error',
      title: 'Error',
      html: mensaje,
      showConfirmButton: true,
    });
  }

  mensajeAdvertenciaOpcion(mensaje: string): Promise<SweetAlertResult<any>> {
    const promesa = new Promise<SweetAlertResult<any>>((resolve, reject) => {
      Swal.fire({
        title: 'Advertencia',
        text: mensaje,
        icon: 'warning',
        confirmButtonColor: '#3085d6',
      }).then(
        respuesta => {
          resolve(respuesta);
        }
      )
    });

    return promesa;
  }

  mensajePregunta(mensaje: string): Promise<SweetAlertResult<any>> {
    const promesa = new Promise<SweetAlertResult<any>>((resolve, reject) => {
      Swal.fire({
        title: 'Confirmación',
        text: mensaje,
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#3085d6',
        cancelButtonColor: '#d33',
        confirmButtonText: 'Si',
        cancelButtonText: 'No'
      }).then(
        respuesta => {
          resolve(respuesta);
        }
      )
    });

    return promesa;
  }

  mensajePreguntaUroCultivo(mensaje: string): Promise<SweetAlertResult<any>> {
    const promesa = new Promise<SweetAlertResult<any>>((resolve, reject) => {
      Swal.fire({
        title: 'Consulta',
        text: mensaje,
        icon: 'info',
        showDenyButton: true,
        confirmButtonColor: '#3085d6',
        denyButtonColor: '#d33',
        confirmButtonText: 'Positivo',
        denyButtonText: 'Negativo',
        showCloseButton: true
      }).then(
        respuesta => {
          resolve(respuesta);
        }
      )
    });

    return promesa;
  }

  mensajePreguntaSiNoCancel(mensaje: string, textoConfirmar?: string, textoDenegar?: string): Promise<SweetAlertResult<any>> {
    const promesa = new Promise<SweetAlertResult<any>>((resolve, reject) => {
      Swal.fire({
        title: 'Confirmación',
        text: mensaje,
        icon: 'question',
        showDenyButton: true,
        showCancelButton: true,
        confirmButtonColor: '#3085d6',
        cancelButtonColor: '#d33',
        confirmButtonText: textoConfirmar ? textoConfirmar : 'Si',
        denyButtonText: textoDenegar ? textoDenegar : 'No',
        cancelButtonText: 'Cancel'
      }).then(
        respuesta => {
          resolve(respuesta);
        }
      )
    });

    return promesa;
  }

  mensajePreguntaEnviar(numero: string, textoConfirmar?: string, textoDenegar?: string): Promise<SweetAlertResult<any>> {
    const promesa = new Promise<SweetAlertResult<any>>((resolve, reject) => {
      Swal.fire({
        title: 'Envía los resultados',
        html: `Adjunta y envia el PDF de resultados al <br> WhatsApp <b>+51 ${numero}</b>`,
        icon: 'info',
        showDenyButton: true,
        showCancelButton: false,
        confirmButtonColor: "#F79619",
        denyButtonColor: '#CCCCCC',
        confirmButtonText: `<i class="pi pi-whatsapp"></i> Enviar`,
        denyButtonText: `<i class="pi pi-times"></i> No, gracias`
      }).then(
        respuesta => {
          resolve(respuesta);
        }
      )
    });

    return promesa;
  }
}
