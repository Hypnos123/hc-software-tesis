import { Routes } from '@angular/router';
import { LayoutComponent } from './shared/components/layout/layout.component';
import { LoginComponent } from './auth/pages/login/login.component';
import { AuthGuard } from './auth/guards/auth.guard';
import { PermissionGuard } from './auth/guards/permission.guard';

export const routes: Routes = [
  {
    path: '',
    component: LoginComponent,
  },
  {
    path: '',
    component: LayoutComponent,
    children: [
      {
        path: 'dashboard',
        redirectTo: 'paciente',
        pathMatch: 'full'
      },
      {
        path: 'usuarios',
        loadChildren: () =>
          import('./modules/usuario/usuario.routes').then(
            (r) => r.USUARIO_ROUTES
          ),
        canActivate: [AuthGuard, PermissionGuard],
        data: { permisoRuta: '/usuarios' }
      },
      {
        path: 'consultas',
        loadChildren: () =>
          import('./modules/consultas/consultas.routes').then(
            (r) => r.CONSULTAS_ROUTES
          ),
        canActivate: [AuthGuard, PermissionGuard],
        data: { permisoRuta: '/consultas' }
      },
      {
        path: 'historiaClinica',
        loadChildren: () =>
          import('./modules/historiaClinica/historiaClinica.routes').then(
            (r) => r.HISTORIA_CLINICA_ROUTES
          ),
        canActivate: [AuthGuard, PermissionGuard],
        data: { permisoRuta: '/historiaClinica' }
      },
      {
        path: 'paciente',
        loadChildren: () =>
          import('./modules/paciente/paciente.routes').then(
            (r) => r.PACIENTE_ROUTES
          ),
        canActivate: [AuthGuard, PermissionGuard],
        data: { permisoRuta: '/paciente' }
      },
      {
        path: 'empleados',
        loadChildren: () =>
          import('./modules/empleado/empleado.routes').then(
            (r) => r.EMPLEADO_ROUTES
          ),
        canActivate: [AuthGuard, PermissionGuard],
        data: { permisoRuta: '/empleados' }
      },
    ],
  },
];
