import { Routes } from '@angular/router';
import { EmpleadosComponent } from './pages/empleados/empleados.component';
import { MantenimientoEmpleadoComponent } from './pages/mantenimiento-empleado/mantenimiento-empleado.component';

export const EMPLEADO_ROUTES: Routes = [
  { path: '', component: EmpleadosComponent },
  { path: 'mantenimiento-empleado', component: MantenimientoEmpleadoComponent },
  { path: 'mantenimiento-empleado/:id', component: MantenimientoEmpleadoComponent },
];
