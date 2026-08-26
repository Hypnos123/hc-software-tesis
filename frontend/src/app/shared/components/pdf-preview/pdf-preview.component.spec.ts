import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ElementRef } from '@angular/core';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { PdfPreviewComponent } from './pdf-preview.component';

describe('PdfPreviewComponent', () => {
  let fixture: ComponentFixture<PdfPreviewComponent>;
  let component: PdfPreviewComponent;
  let createUrl: jasmine.Spy;
  let revokeUrl: jasmine.Spy;
  let contadorUrl: number;

  beforeEach(async () => {
    contadorUrl = 0;
    createUrl = spyOn(URL, 'createObjectURL').and.callFake(() => `blob:reporte-${++contadorUrl}`);
    revokeUrl = spyOn(URL, 'revokeObjectURL');
    await TestBed.configureTestingModule({ imports: [PdfPreviewComponent, BrowserAnimationsModule] }).compileComponents();
    fixture = TestBed.createComponent(PdfPreviewComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => fixture.destroy());

  function mostrar(blob = new Blob(['%PDF'], { type: 'application/pdf' })): Blob {
    fixture.componentRef.setInput('visible', true);
    fixture.componentRef.setInput('pdfBlob', blob);
    fixture.componentRef.setInput('nombreArchivo', 'evaluacion-medica.pdf');
    fixture.detectChanges();
    return blob;
  }

  it('crea una sola URL temporal y muestra el PDF válido', () => {
    const blob = mostrar();
    fixture.detectChanges();

    expect(createUrl).toHaveBeenCalledOnceWith(blob);
    expect(component.mostrarVisor).toBeTrue();
    expect(fixture.nativeElement.querySelector('iframe')).not.toBeNull();
  });

  it('revoca la URL al cerrar y emite el cierre una sola vez', () => {
    mostrar();
    const cierre = spyOn(component.cerrarVista, 'emit');

    component.cerrar();
    component.alOcultar();

    expect(revokeUrl).toHaveBeenCalledOnceWith('blob:reporte-1');
    expect(cierre).toHaveBeenCalledTimes(1);
    expect(component.mostrarVisor).toBeFalse();
  });

  it('revoca la URL anterior cuando reemplaza el PDF', () => {
    mostrar();
    const segundo = new Blob(['%PDF-2'], { type: 'application/pdf' });

    fixture.componentRef.setInput('pdfBlob', segundo);
    fixture.detectChanges();

    expect(revokeUrl).toHaveBeenCalledWith('blob:reporte-1');
    expect(createUrl).toHaveBeenCalledTimes(2);
    expect(createUrl).toHaveBeenCalledWith(segundo);
  });

  it('descarga mediante la misma URL asociada al Blob mostrado', () => {
    mostrar();
    const click = spyOn(HTMLAnchorElement.prototype, 'click');
    let enlace: HTMLAnchorElement | undefined;
    const append = spyOn(document.body, 'appendChild').and.callFake(node => {
      enlace = node as unknown as HTMLAnchorElement;
      return node;
    });

    component.descargar();

    expect(append).toHaveBeenCalled();
    expect(enlace?.href).toBe('blob:reporte-1');
    expect(enlace?.download).toBe('evaluacion-medica.pdf');
    expect(click).toHaveBeenCalled();
    expect(createUrl).toHaveBeenCalledTimes(1);
  });

  it('imprime el mismo documento cargado en el iframe', () => {
    mostrar();
    const ventana = jasmine.createSpyObj('ventanaPdf', ['focus', 'print']);
    component.visorPdf = new ElementRef({ contentWindow: ventana } as HTMLIFrameElement);

    component.imprimir();

    expect(ventana.focus).toHaveBeenCalled();
    expect(ventana.print).toHaveBeenCalled();
    expect(createUrl).toHaveBeenCalledTimes(1);
  });

  it('desactiva acciones y no muestra iframe durante la carga', () => {
    fixture.componentRef.setInput('visible', true);
    fixture.componentRef.setInput('cargando', true);
    fixture.componentRef.setInput('pdfBlob', new Blob(['%PDF'], { type: 'application/pdf' }));
    fixture.detectChanges();

    expect(component.accionesDeshabilitadas).toBeTrue();
    expect(component.mostrarVisor).toBeFalse();
    expect(createUrl).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Generando evaluación médica...');
  });

  it('un error oculta el iframe y conserva un mensaje seguro', () => {
    fixture.componentRef.setInput('visible', true);
    fixture.componentRef.setInput('pdfBlob', new Blob(['%PDF'], { type: 'application/pdf' }));
    fixture.componentRef.setInput('error', 'No se encontró la consulta seleccionada.');
    fixture.detectChanges();

    expect(component.mostrarVisor).toBeFalse();
    expect(component.accionesDeshabilitadas).toBeTrue();
    expect(fixture.nativeElement.querySelector('iframe')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('No se encontró la consulta seleccionada.');
  });

  it('rechaza un Blob que no sea PDF sin crear una URL', () => {
    fixture.componentRef.setInput('visible', true);
    fixture.componentRef.setInput('pdfBlob', new Blob(['html'], { type: 'text/html' }));
    fixture.detectChanges();

    expect(component.mensajeError).toContain('no es un PDF válido');
    expect(component.mostrarVisor).toBeFalse();
    expect(createUrl).not.toHaveBeenCalled();
  });

  it('revoca la URL al destruirse', () => {
    mostrar();
    fixture.destroy();
    expect(revokeUrl).toHaveBeenCalledWith('blob:reporte-1');
  });
});
