import { Routes } from "@angular/router";
import { MantenimientoConsultasComponent } from "./pages/mantenimiento-consultas/mantenimiento-consultas.component";
import { ConsultasComponent } from "./pages/consultas/consultas.component";
import { DetalleConsultaComponent } from "./pages/detalle-consulta/detalle-consulta.component";

export const CONSULTAS_ROUTES: Routes = [
  { path: '', component: ConsultasComponent },
  { path: 'lista-consultas/:id', component: MantenimientoConsultasComponent },
  { path: 'lista-consultas/detalle/:id', component: DetalleConsultaComponent },
];
