import { Routes } from "@angular/router";
import { MantenimientoHistoriasClinicasComponent } from "./pages/mantenimiento-historias-clinicas/mantenimiento-historias-clinicas.component";
import { HistoriasClinicasComponent } from "./pages/historias-clinicas/historias-clinicas.component";

export const HISTORIA_CLINICA_ROUTES: Routes = [
  { path: '', component: HistoriasClinicasComponent },
  { path: 'mantenimiento-historias-clinicas', component: MantenimientoHistoriasClinicasComponent },
  { path: 'mantenimiento-historias-clinicas/:id', component: MantenimientoHistoriasClinicasComponent },
];