import { Component } from '@angular/core';
import { TableModule } from 'primeng/table';

@Component({
  selector: 'app-fusiones-historias',
  standalone: true,
  imports: [TableModule],
  templateUrl: './fusiones-historias.component.html',
  styleUrl: '../auditoria-placeholder.scss',
})
export class FusionesHistoriasComponent {
  loading = false;
  errorMessage = '';
  readonly records: never[] = [];
}
