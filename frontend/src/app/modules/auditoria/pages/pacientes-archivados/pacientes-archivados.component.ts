import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';

@Component({
  selector: 'app-pacientes-archivados',
  standalone: true,
  imports: [FormsModule, TableModule, InputTextModule, ButtonModule],
  templateUrl: './pacientes-archivados.component.html',
  styleUrl: '../auditoria-placeholder.scss',
})
export class PacientesArchivadosComponent {
  searchValue = '';
  loading = false;
  errorMessage = '';
  readonly records: never[] = [];
}
