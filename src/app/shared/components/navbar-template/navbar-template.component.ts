import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '@app/auth/services/auth.service';

@Component({
  selector: 'app-navbar-template',
  templateUrl: './navbar-template.component.html',
  styleUrls: ['./navbar-template.component.scss'],
  standalone: true,
  imports: [CommonModule]
})
export class NavbarTemplateComponent implements OnInit {

  constructor(private router: Router,
              private authService: AuthService) { }

  ngOnInit(): void {
  }

  cerrarSesion() {
    this.authService.logout();
    this.router.navigate(['./']);
  }

}
