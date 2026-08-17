import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { AuditoriaLayoutComponent } from './auditoria-layout.component';
import { AUDITORIA_ROUTES } from '../../auditoria.routes';

describe('AuditoriaLayoutComponent', () => {
  let fixture: ComponentFixture<AuditoriaLayoutComponent>;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AuditoriaLayoutComponent],
      providers: [provideRouter(AUDITORIA_ROUTES)],
    }).compileComponents();
    fixture = TestBed.createComponent(AuditoriaLayoutComponent);
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('muestra el título y ambas pestañas', () => {
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Archivo y auditoría');
    expect(text).toContain('Pacientes archivados');
    expect(text).toContain('Historial de fusiones');
  });

  it('actualiza la URL al seleccionar la pestaña de fusiones', async () => {
    const links: HTMLAnchorElement[] = Array.from(fixture.nativeElement.querySelectorAll('a'));
    links.find((link) => link.textContent?.includes('Historial de fusiones'))?.click();
    await fixture.whenStable();
    expect(router.url).toBe('/fusiones-historias');
  });
});
