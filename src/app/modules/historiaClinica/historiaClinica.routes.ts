import { Routes } from "@angular/router";
import { MantenimientoHistoriasClinicasComponent } from "./pages/mantenimiento-historias-clinicas/mantenimiento-historias-clinicas.component";
import { HistoriasClinicasComponent } from "./pages/historias-clinicas/historias-clinicas.component";
import { ConsultasHistoriaClinicaComponent } from "./pages/ver-consultas/ver-consultas.component";
import { DetallesConsultasComponent } from "./pages/detalles-consultas/detalles-consultas.component";


export const HISTORIA_CLINICA_ROUTES: Routes = [
  { path: '', component: HistoriasClinicasComponent },
  { path: 'mantenimiento-historias-clinicas/:modo/:id', component: MantenimientoHistoriasClinicasComponent },
  { path: 'mantenimiento-historias-clinicas/:modo', component: MantenimientoHistoriasClinicasComponent },
  { path: "ver-consultas/:id", component: ConsultasHistoriaClinicaComponent },
  { path: "ver-consultas/:id/:modo", component: DetallesConsultasComponent },

];