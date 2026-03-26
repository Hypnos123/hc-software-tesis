import { Routes } from '@angular/router';
import { MantenimientoPacienteComponent } from './pages/mantenimiento-paciente/mantenimiento-paciente.component';
import { PacientesComponent } from './pages/pacientes/pacientes.component';


export const PACIENTE_ROUTES: Routes = [
  { path: '', component: PacientesComponent },
  { path: 'mantenimiento-paciente/:modo/:id', component: MantenimientoPacienteComponent },
  { path: 'mantenimiento-paciente/:modo', component: MantenimientoPacienteComponent },
];
