import { Routes } from '@angular/router';
import { MantenimientoPacienteComponent } from './pages/mantenimiento-paciente/mantenimiento-paciente.component';
import { PacientesComponent } from './pages/pacientes/pacientes.component';
import { ImportacionPacientesComponent } from './pages/importacion-pacientes/importacion-pacientes.component';


export const PACIENTE_ROUTES: Routes = [
  { path: '', component: PacientesComponent },
  { path: 'importacion', component: ImportacionPacientesComponent },
  { path: 'mantenimiento-paciente/:modo/:id', component: MantenimientoPacienteComponent },
  { path: 'mantenimiento-paciente/:modo', component: MantenimientoPacienteComponent },
];
