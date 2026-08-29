import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ResumenConsultasPacienteChatComponent } from './resumen-consultas-paciente-chat.component';

describe('ResumenConsultasPacienteChatComponent', () => {
  let fixture: ComponentFixture<ResumenConsultasPacienteChatComponent>;
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ResumenConsultasPacienteChatComponent] }).compileComponents();
    fixture = TestBed.createComponent(ResumenConsultasPacienteChatComponent);
  });

  it('renders five summary sections and insufficient vital data without clinical interpretation', () => {
    fixture.componentInstance.state = { vista: 'summary', candidatos: [], accionesHabilitadas: false, resumen: {
      paciente: { idPaciente: 6, nombreCompleto: 'Paciente Demo', dni: '12345678', edad: 40, estado: 'ACTIVO', cantidadHistoriasClinicas: 2, idsHistoriasClinicas: [1, 2] },
      antecedentes: {}, resumenAtencion: { totalConsultasAtendidas: 2, proximasCitas: [] }, tiposEnfermedad: [], especialidades: [],
      funcionesVitales: { presionSistolica: { cantidadRegistrosValidos: 0, cantidadRegistrosDescartados: 2, unidad: 'mmHg', tendencia: 'SIN_DATOS_SUFICIENTES' } },
      evaluacionesRecientes: [], consultasRecientes: [], calidadDatos: { consultasSinFecha: 0, consultasSinTipoEnfermedad: 0, consultasSinEspecialidad: 0, valoresVitalesDescartados: 2, consultasConRelacionInconsistente: 0 }
    }};
    fixture.detectChanges();
    expect(fixture.debugElement.queryAll(By.css('.summary-section')).length).toBe(5);
    expect(fixture.nativeElement.textContent).toContain('Sin datos válidos suficientes');
    expect(fixture.nativeElement.textContent).not.toContain('presión alta');
  });

  it('muestra historias y consultas para distinguir candidatos coincidentes', () => {
    fixture.componentInstance.state = { vista: 'multiple', accionesHabilitadas: true, candidatos: [{
      idPaciente: 19, nombreCompleto: 'Daniela Alejandra Ramírez Soto', dni: '74296831', edad: 26,
      estado: 'ACTIVO', cantidadHistoriasClinicas: 1, cantidadConsultas: 2
    }] };
    fixture.componentInstance.active = true;
    fixture.detectChanges();

    const texto = fixture.nativeElement.textContent;
    expect(texto).toContain('Daniela Alejandra Ramírez Soto');
    expect(texto).toContain('ID paciente: 19');
    expect(texto).toContain('DNI: 74296831');
    expect(texto).toContain('Historias clínicas: 1');
    expect(texto).toContain('Consultas: 2');
  });

  it('muestra la cantidad total de consultas en la tarjeta del paciente', () => {
    fixture.componentInstance.state = { vista: 'confirmation', candidatos: [], accionesHabilitadas: true, paciente: {
      idPaciente: 19, nombreCompleto: 'Daniela Alejandra Ramírez Soto', dni: '74296831', edad: 26,
      estado: 'ACTIVO', cantidadHistoriasClinicas: 1, cantidadConsultas: 2
    } };
    fixture.componentInstance.active = true;
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Consultas');
    expect(fixture.nativeElement.querySelector('.patient-card dl').textContent).toContain('2');
  });
});
