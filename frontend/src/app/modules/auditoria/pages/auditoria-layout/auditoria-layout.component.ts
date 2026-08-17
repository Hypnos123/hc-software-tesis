import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-auditoria-layout',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './auditoria-layout.component.html',
  styleUrl: './auditoria-layout.component.scss',
})
export class AuditoriaLayoutComponent {}
