import { Routes } from '@angular/router';
import { LayoutComponent } from './shared/components/layout/layout.component';
import { LoginComponent } from './auth/pages/login/login.component';

export const routes: Routes = [
  {
    path: '', component: LoginComponent,
  },
  {
    path: '',
    component: LayoutComponent,
    children: [
      {
        path: 'dashboard',
        loadChildren: () => import('./modules/dashboard/dashboard.routes').then(r => r.DASHBOARD_ROUTES),
        // canLoad: [AuthGuard],
        // canActivate: [AuthGuard]
      },
      {
        path: 'empleados',
        loadChildren: () => import('./modules/empleado/empleado.routes').then(r => r.EMPLEADO_ROUTES),
        // canLoad: [AuthGuard],
        // canActivate: [AuthGuard]
      },
      {
        path: 'usuarios',
        loadChildren: () => import('./modules/usuario/usuario.routes').then(r => r.USUARIO_ROUTES),
        // canLoad: [AuthGuard],
        // canActivate: [AuthGuard]
      },
    ]
  }
];
