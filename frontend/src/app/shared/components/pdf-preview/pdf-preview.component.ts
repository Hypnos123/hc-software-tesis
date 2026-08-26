import { CommonModule, DOCUMENT } from '@angular/common';
import {
  Component, ElementRef, EventEmitter, Inject, Input, OnChanges, OnDestroy, Output,
  SimpleChanges, ViewChild
} from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

@Component({
  selector: 'app-pdf-preview',
  standalone: true,
  imports: [CommonModule, DialogModule, ButtonModule, ProgressSpinnerModule],
  templateUrl: './pdf-preview.component.html',
  styleUrl: './pdf-preview.component.scss'
})
export class PdfPreviewComponent implements OnChanges, OnDestroy {
  @Input() visible = false;
  @Input() pdfBlob?: Blob | null;
  @Input() nombreArchivo = 'reporte-medico.pdf';
  @Input() cargando = false;
  @Input() error?: string | null;
  @Input() titulo = 'Vista previa del reporte';
  @Input() textoCarga = 'Generando evaluación médica...';
  @Output() cerrarVista = new EventEmitter<void>();
  @ViewChild('visorPdf') visorPdf?: ElementRef<HTMLIFrameElement>;

  dialogVisible = false;
  pdfUrl?: SafeResourceUrl;
  errorInterno?: string;
  private objectUrl?: string;
  private cierreEmitido = false;

  constructor(@Inject(DOCUMENT) private document: Document, private sanitizer: DomSanitizer) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['pdfBlob']) {
      this.revocarObjectUrl();
      this.errorInterno = undefined;
    }
    if (changes['visible']) {
      this.dialogVisible = this.visible;
      if (this.visible) this.cierreEmitido = false;
    }
    if (this.dialogVisible && !this.cargando && !this.mensajeError) this.asegurarObjectUrl();
    if (this.cargando || this.mensajeError) this.revocarObjectUrl();
  }

  ngOnDestroy(): void {
    this.revocarObjectUrl();
  }

  get mensajeError(): string | undefined {
    return this.error?.trim() || this.errorInterno;
  }

  get mostrarVisor(): boolean {
    return this.dialogVisible && !this.cargando && !this.mensajeError && !!this.pdfUrl;
  }

  get accionesDeshabilitadas(): boolean {
    return this.cargando || !!this.mensajeError || !this.objectUrl || !this.pdfBlob;
  }

  descargar(): void {
    if (this.accionesDeshabilitadas || !this.objectUrl) return;
    const enlace = this.document.createElement('a');
    enlace.href = this.objectUrl;
    enlace.download = this.nombreSeguro();
    enlace.style.display = 'none';
    this.document.body.appendChild(enlace);
    enlace.click();
    enlace.remove();
  }

  imprimir(): void {
    if (this.accionesDeshabilitadas) return;
    const ventana = this.visorPdf?.nativeElement.contentWindow;
    if (!ventana) {
      this.errorInterno = 'No se pudo abrir el diálogo de impresión del reporte.';
      return;
    }
    ventana.focus();
    ventana.print();
  }

  cerrar(): void {
    this.dialogVisible = false;
    this.finalizarCierre();
  }

  alOcultar(): void {
    this.dialogVisible = false;
    this.finalizarCierre();
  }

  private finalizarCierre(): void {
    this.revocarObjectUrl();
    if (!this.cierreEmitido) {
      this.cierreEmitido = true;
      this.cerrarVista.emit();
    }
  }

  private asegurarObjectUrl(): void {
    if (this.objectUrl || !this.pdfBlob) return;
    const tipo = this.pdfBlob.type?.split(';')[0].trim().toLowerCase();
    if (tipo !== 'application/pdf') {
      this.errorInterno = 'El archivo recibido no es un PDF válido.';
      return;
    }
    this.objectUrl = URL.createObjectURL(this.pdfBlob);
    this.pdfUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.objectUrl);
  }

  private revocarObjectUrl(): void {
    if (this.objectUrl) URL.revokeObjectURL(this.objectUrl);
    this.objectUrl = undefined;
    this.pdfUrl = undefined;
  }

  private nombreSeguro(): string {
    const nombre = this.nombreArchivo?.split(/[\\/]/).pop()?.trim();
    return nombre?.toLowerCase().endsWith('.pdf') ? nombre : 'reporte-medico.pdf';
  }
}
