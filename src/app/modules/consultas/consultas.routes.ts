import { Routes } from "@angular/router";
import { MantenimientoConsultasComponent } from "./pages/mantenimiento-consultas/mantenimiento-consultas.component";
import { ConsultasComponent } from "./pages/consultas/consultas.component";

export const CONSULTAS_ROUTES: Routes = [
  { path: '', component: ConsultasComponent },
  { path: 'mantenimiento-consultas', component: MantenimientoConsultasComponent },
  { path: 'mantenimiento-consultas/:id', component: MantenimientoConsultasComponent },
];
