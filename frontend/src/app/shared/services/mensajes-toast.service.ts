import { Injectable } from "@angular/core";
import { MessageService } from "primeng/api";

@Injectable({
  providedIn: 'root'
})
export class MensajesToastService {

  constructor(public messageService: MessageService) { }

  showSuccess(detalle?: string, sticky?: boolean) {
    this.clear();
    this.messageService.add({ severity: 'success', summary: 'Éxito', detail: detalle, sticky: !sticky ? false : true });
  }

  showInfo(detalle?: string, sticky?: boolean) {
    this.clear();
    this.messageService.add({ severity: 'info', summary: 'Información', detail: detalle, sticky: !sticky ? false : true });
  }

  showWarn(detalle?: string, sticky?: boolean) {
    this.clear();
    this.messageService.add({ severity: 'warn', summary: 'Advertencia', detail: detalle, sticky: !sticky ? false : true });
  }

  showError(detalle?: string, sticky?: boolean) {
    this.clear();
    this.messageService.add({ severity: 'error', summary: 'Error', detail: detalle, sticky: !sticky ? false : true });
  }

  showCustom(detalle?: string, icono?: string, sticky?: boolean) {
    this.clear();
    this.messageService.add({ severity: 'custom', summary: 'Custom', detail: detalle, icon: icono ? icono : 'pi-file', sticky: !sticky ? false : true });
  }

  clear() {
    this.messageService.clear();
  }

}

