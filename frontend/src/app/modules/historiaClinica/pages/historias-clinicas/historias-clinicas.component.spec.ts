import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HistoriasClinicasComponent } from './historias-clinicas.component';
import { AuthService } from '@app/auth/services/auth.service';
import { HistoriaClinicaService } from '../../services/consultas.service';
import { Router } from '@angular/router';
import { of } from 'rxjs';

describe('HistoriasClinicasComponent', () => {
  let component: HistoriasClinicasComponent;
  let fixture: ComponentFixture<HistoriasClinicasComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HistoriasClinicasComponent],
      providers: [
        { provide: AuthService, useValue: { puedeCrearHistoriasClinicas: () => false } },
        { provide: HistoriaClinicaService, useValue: { getAll: () => of([]) } },
        { provide: Router, useValue: { navigate: () => undefined } }
      ]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(HistoriasClinicasComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(fixture.nativeElement.textContent).not.toContain('Agregar HC');
  });
});
