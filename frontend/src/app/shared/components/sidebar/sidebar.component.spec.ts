import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SidebarComponent } from './sidebar.component';
import { AuthService } from '@app/auth/services/auth.service';

describe('SidebarComponent', () => {
  let component: SidebarComponent;
  let fixture: ComponentFixture<SidebarComponent>;
  let auth: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    auth = jasmine.createSpyObj('AuthService', ['esRutaTemporalmenteDeshabilitada', 'esAdministrador'], {
      usuario: { idUsuario: 1, cargo: 'ADMINISTRADOR' },
      detallePermisos: [
        { idMenu: 1, nombre: 'Pacientes', ruta: '/paciente' },
        { idMenu: 8, nombre: 'Archivo y auditoría', ruta: '/auditoria', imagen: 'pi pi-history' },
      ],
    });
    auth.esRutaTemporalmenteDeshabilitada.and.returnValue(false);
    await TestBed.configureTestingModule({
      imports: [SidebarComponent],
      providers: [{ provide: AuthService, useValue: auth }],
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(SidebarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('muestra Archivo y auditoría al Administrador con permiso y conserva el resto del menú', () => {
    auth.esAdministrador.and.returnValue(true);
    component.getMenu();
    expect(component.menu.map((item) => item.ruta)).toEqual(['/paciente', '/auditoria']);
  });

  ['Enfermero', 'Doctor'].forEach((cargo) => {
    it(`oculta Archivo y auditoría al ${cargo} aunque tenga el permiso`, () => {
      auth.esAdministrador.and.returnValue(false);
      component.getMenu();
      expect(component.menu.map((item) => item.ruta)).toEqual(['/paciente']);
    });
  });
});
