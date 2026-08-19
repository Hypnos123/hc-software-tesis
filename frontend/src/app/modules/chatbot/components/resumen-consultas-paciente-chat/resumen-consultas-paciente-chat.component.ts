import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ResumenConsultasChatState, EstadisticaVitalResumen, ConsultaRecienteResumen } from '../../models/resumen-consultas-paciente';

@Component({
  selector: 'app-resumen-consultas-paciente-chat', standalone: true, imports: [CommonModule],
  templateUrl: './resumen-consultas-paciente-chat.component.html',
  styleUrl: './resumen-consultas-paciente-chat.component.scss'
})
export class ResumenConsultasPacienteChatComponent {
  @Input({ required: true }) state!: ResumenConsultasChatState;
  @Input() active = false;
  @Output() seleccionar = new EventEmitter<number>();
  @Output() generar = new EventEmitter<void>();
  @Output() cancelar = new EventEmitter<void>();
  @Output() buscarOtro = new EventEmitter<void>();
  consultaExpandida?: number;
  evaluacionExpandida?: number;

  readonly vitales = [
    ['presionSistolica', 'Presión sistólica'], ['presionDiastolica', 'Presión diastólica'],
    ['frecuenciaCardiaca', 'Frecuencia cardíaca'], ['frecuenciaRespiratoria', 'Frecuencia respiratoria'],
    ['talla', 'Talla'], ['temperatura', 'Temperatura'], ['peso', 'Peso']
  ];

  vital(clave: string): EstadisticaVitalResumen | undefined { return this.state.resumen?.funcionesVitales?.[clave]; }
  valor(valor: number | undefined, unidad?: string): string { return valor === null || valor === undefined ? '—' : `${valor}${unidad ? ` ${unidad}` : ''}`; }
  dato(valor: unknown): string { return valor === null || valor === undefined || String(valor).trim() === '' ? 'No registrado' : String(valor); }
  iconoTendencia(tendencia?: string): string {
    if (tendencia === 'ASCENDENTE') return 'pi pi-arrow-up';
    if (tendencia === 'DESCENDENTE') return 'pi pi-arrow-down';
    if (tendencia === 'ESTABLE') return 'pi pi-arrow-right';
    return 'pi pi-minus';
  }
  etiquetaTendencia(tendencia?: string): string { return (tendencia ?? 'SIN_DATOS_SUFICIENTES').replaceAll('_', ' '); }
  toggleConsulta(consulta: ConsultaRecienteResumen): void { this.consultaExpandida = this.consultaExpandida === consulta.idConsulta ? undefined : consulta.idConsulta; }
  calidadVisible(): boolean {
    const calidad = this.state.resumen?.calidadDatos;
    return !!calidad && Object.values(calidad).some(value => Number(value) > 0);
  }
}
