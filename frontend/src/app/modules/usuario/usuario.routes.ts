import { Routes } from "@angular/router";
import { MantenimientoUsuarioComponent } from "./pages/mantenimiento-usuario/mantenimiento-usuario.component";
import { UsuariosComponent } from "./pages/usuarios/usuarios.component";

export const USUARIO_ROUTES: Routes = [
  { path: '', component: UsuariosComponent },
  { path: 'mantenimiento-usuario', component: MantenimientoUsuarioComponent },
  { path: 'mantenimiento-usuario/:id', component: MantenimientoUsuarioComponent },
];
