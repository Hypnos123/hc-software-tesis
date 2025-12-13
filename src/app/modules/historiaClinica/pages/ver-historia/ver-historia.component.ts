import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { DividerModule } from 'primeng/divider';


@Component({
  selector: 'app-ver-historia',
  standalone: true,
  imports: [
     CommonModule,
     ButtonModule,
     DividerModule],
  templateUrl: './ver-historia.component.html',
  styleUrl: './ver-historia.component.scss'
})

export class VerHistoriaComponent {
private route = inject(ActivatedRoute);
  private router = inject(Router);


  ngOnInit(): void {

    // TODO: reemplazar por llamada real al servicio
    
  }

  

  volver() {
    this.router.navigate(['/historiaClinica']);
  }

  
}
