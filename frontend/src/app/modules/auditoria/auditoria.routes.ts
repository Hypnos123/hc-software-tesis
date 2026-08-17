import { Routes } from '@angular/router';
import { AuditoriaLayoutComponent } from './pages/auditoria-layout/auditoria-layout.component';
import { PacientesArchivadosComponent } from './pages/pacientes-archivados/pacientes-archivados.component';
import { FusionesHistoriasComponent } from './pages/fusiones-historias/fusiones-historias.component';

export const AUDITORIA_ROUTES: Routes = [
  {
    path: '',
    component: AuditoriaLayoutComponent,
    children: [
      { path: '', redirectTo: 'pacientes-archivados', pathMatch: 'full' },
      { path: 'pacientes-archivados', component: PacientesArchivadosComponent },
      { path: 'fusiones-historias', component: FusionesHistoriasComponent },
    ],
  },
];
